// Todo: Refactor library so that we could have:
// standardNode('Ubuntu 16.04', 'latest') {
//     ...
// }
// ... is wrapped by a try/finally that defines workspace cleanup

@Library('dotnet-ci') _

def configuration = "Release";
def dockerRepository = 'microsoft/dotnet-buildtools-prereqs'
def dockerTag = 'rhel7_prereqs_2'
def dockerImageName = "${dockerRepository}:${dockerTag}"
def targetHelixQueues = 'Redhat.72.Amd64,Ubuntu.1604.Amd64'
def helixRuns

standardDockerNode(dockerImageName) {
    stage ('Checkout source') {
        checkout scm
    }
    stage ('Initialize tools') {
        // Init tools
        sh './init-tools.sh'
    }
    stage ('Generate version assets') {
        // Generate the version assets.  Do we need to even do this for non-official builds?
        sh "./build-managed.sh -- /t:GenerateVersionSourceFile /p:GenerateVersionSourceFile=true"
    }
    stage ('Sync') {
        sh "./sync.sh -p -portableLinux -- /p:ArchGroup=x64"
    }
    stage ('Build Product') {
        sh "./build.sh -buildArch=x64 -${configuration} -portableLinux"
    }
    stage ('Build Tests') {
        sh "./build-tests.sh -buildArch=x64 -${configuration} -SkipTests -- /p:ArchiveTests=true /p:EnableDumpling=true"
    }
    stage ('Submit To Helix For Testing') {
        // Bind the credentials
        withCredentials([string(credentialsId: 'CloudDropAccessToken', variable: 'CloudDropAccessToken'),
                            string(credentialsId: 'OutputCloudResultsAccessToken', variable: 'OutputCloudResultsAccessToken'),
                            string(credentialsId: 'HelixApiAccessKey', variable: 'HelixApiAccessKey')]) {
            sh "./Tools/msbuild.sh src/upload-tests.proj /p:ArchGroup=x64 /p:ConfigurationGroup=${configuration} /p:EnableCloudTest=true /p:TestProduct=corefx /p:TimeoutInSeconds=1200 /p:TargetOS=Linux /p:CloudDropAccountName=dotnetbuilddrops /p:CloudResultsAccountName=dotnetjobresults /p:CloudDropAccessToken=\$CloudDropAccessToken /p:CloudResultsAccessToken=\$OutputCloudResultsAccessToken /p:HelixApiAccessKey=\$HelixApiAccessKey /p:HelixApiEndpoint=https://helix.dot.net/api/2016-06-28/jobs /p:Branch=${ghprbPullId} /p:TargetQueues=\\\"${targetHelixQueues}\\\" /p:HelixLogFolder=${WORKSPACE}/bin/ /p:HelixCorrelationInfoFileName=SubmittedHelixRuns.txt"
        }

        helixRuns = readJSON file: 'bin/SubmittedHelixRuns.txt'
    }
    stage ('Execute Tests') {
        waitForHelixRuns(helixRuns)
    }
}