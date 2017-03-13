// Todo: Refactor library so that we could have:
// standardNode('Ubuntu 16.04', 'latest') {
//     ...
// }
// ... is wrapped by a try/finally that defines workspace cleanup

def configuration = "Release";
def dockerContainerName = BUILD_TAG
def dockerRepository = 'microsoft/dotnet-buildtools-prereqs'
def dockerTag = 'rhel7_prereqs_2'
def dockerImageName = "${dockerRepository}:${dockerTag}"
def dockerWorkspaceDir = '/root/workspace'
def targetHelixQueues = 'Redhat.72.Amd64'
def helixApiEndpoint = 'https://helix.dot.net/api/2016-06-28/jobs'

node('ubuntu1604-20170216') {
    try {
        docker.image(dockerImageName).inside {
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
                    sh "./Tools/msbuild.sh src/upload-tests.proj /p:ArchGroup=x64 /p:ConfigurationGroup=${configuration} /p:EnableCloudTest=true /p:TestProduct=corefx /p:TimeoutInSeconds=1200 /p:TargetOS=Linux /p:CloudDropAccountName=dotnetbuilddrops /p:CloudResultsAccountName=dotnetjobresults /p:CloudDropAccessToken=\$CloudDropAccessToken /p:\"CloudResultsAccessToken=\$OutputCloudResultsAccessToken /p:HelixApiAccessKey=\$HelixApiAccessKey /p:HelixApiEndpoint=https://helix.dot.net/api/2016-06-28/jobs /p:Branch=${ghprbTargetBranch} /p:TargetQueues=Redhat.72.Amd64 /p:HelixLogFolder=bin /p:HelixCorrelationInfoFileName=SubmittedHelixRuns.txt"
                }

                // Grab the output info
                sh "cat bin/SubmittedHelixRuns.txt"
            }
        }
    }
    finally {
        stage ("Clean-up workspace") {
            step([$class: 'WsCleanup'])
        }
    }
}