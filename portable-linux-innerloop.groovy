// Todo: Refactor library so that we could have:
// standardNode('Ubuntu 16.04', 'latest') {
//     ...
// }
// ... is wrapped by a try/finally that defines workspace cleanup

@Library('dotnet-ci') _

// Incoming parameters
// Note that Configuration is also a class name, so to avoid confusion, we reference it through the "params" variable.
def configuration = params.Configuration

// Additional variables local to this pipeline
def dockerRepository = 'microsoft/dotnet-buildtools-prereqs'
def dockerTag = 'rhel7_prereqs_2'
def dockerImageName = "${dockerRepository}:${dockerTag}"
def targetHelixQueues = 'Redhat.72.Amd64+Debian.82.Amd64+Ubuntu.1404.Amd64+Ubuntu.1604.Amd64+Ubuntu.1610.Amd64+suse.421.amd64+fedora.25.amd64'
def submittedHelixJson

// We need to determine if it's a PR or not, so we can pull in the target branch, pr id if applicable, commit, etc.

simpleDockerNode(dockerImageName) {
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
        sh "./sync.sh -p -portable -- /p:ArchGroup=x64"
    }
    stage ('Build Product') {
        sh "./build.sh -buildArch=x64 -${configuration} -portable"
    }
    stage ('Build Tests') {
        sh "./build-tests.sh -buildArch=x64 -${configuration} -SkipTests -Outerloop -- /p:ArchiveTests=true /p:EnableDumpling=true"
    }
    stage ('Submit To Helix For Testing') {
        // Bind the credentials
        withCredentials([string(credentialsId: 'CloudDropAccessToken', variable: 'CloudDropAccessToken'),
                            string(credentialsId: 'OutputCloudResultsAccessToken', variable: 'OutputCloudResultsAccessToken')]) {
            // The hash and other items should be generic enough to allow for PR and non PR.
            sh 'env'

            /*sh "./Tools/msbuild.sh src/upload-tests.proj /p:ArchGroup=x64 /p:ConfigurationGroup=${configuration} /p:EnableCloudTest=true /p:TestProduct=corefx /p:TimeoutInSeconds=1200 /p:TargetOS=Linux /p:CloudDropAccountName=dotnetbuilddrops /p:CloudResultsAccountName=dotnetjobresults /p:CloudDropAccessToken=\$CloudDropAccessToken /p:CloudResultsAccessToken=\$OutputCloudResultsAccessToken /p:HelixApiAccessKey=\$HelixApiAccessKey /p:HelixApiEndpoint=https://helix.dot.net/api/2016-06-28/jobs /p:Branch=${ghprbPullId} /p:TargetQueues=${targetHelixQueues} /p:HelixLogFolder=${WORKSPACE}/bin/ /p:HelixCorrelationInfoFileName=SubmittedHelixRuns.txt /p:Build=${ghprbActualCommit}"*/

            sh "./Tools/msbuild.sh src/upload-tests.proj /p:ArchGroup=x64 /p:ConfigurationGroup=${configuration} /p:TestProduct=corefx /p:TimeoutInSeconds=1200 /p:TargetOS=Linux /p:HelixJobType=test/functional/portable/cli/ /p:CloudDropAccountName=dotnetbuilddrops /p:CloudResultsAccountName=dotnetjobresults /p:CloudDropAccessToken=\$CloudDropAccessToken /p:CloudResultsAccessToken=\$OutputCloudResultsAccessToken /p:HelixApiEndpoint=https://helix.dot.net/api/2017-04-14/jobs /p:TargetQueues=${targetHelixQueues} /p:HelixLogFolder=${WORKSPACE}/bin/ /p:HelixCorrelationInfoFileName=SubmittedHelixRuns.txt"
        }

        // Read the json in
        submittedHelixJson = readJSON file: 'bin/SubmittedHelixRuns.txt'
        archiveArtifacts 'bin/SubmittedHelixRuns.txt'
    }

    stage ('Execute Tests') {
        waitForHelixRuns(submittedHelixJson, "Portable Linux ${configuration}")
    }
}