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
            stage ('Initialzie tools') {
                // Init tools
                sh './init-tools.sh'
            }
            stage ('Generate version assets') {
                // Generate the version assets
                sh "./build-managed.sh -OfficialBuildId=${ghprbActualCommit} -- /t:GenerateVersionSourceFile /p:GenerateVersionSourceFile=true"
            }
        }

        /*stage ('Checkout Source') {
            checkout scm // Check out source control (based on parent scm settings)
        }
        stage ("Initialize tools and docker") {
            // Initialize tools
            sh "./init-tools.sh"
            // Initialize docker
            sh "./Tools/scripts/docker/init-docker.sh ${dockerImageName}"
            // We should do this in a better, way but clean the enlistment up so that we un-bake info about the
            // host OS into the target enlistment.  The real reason the scripts above are called are to
            // ensure we have the latest docker image, and that no other docker containers happen to be running on the machine.
            sh 'git clean -fxd'
        }
        stage ("Start Docker") {
            // Below might be great to wrap:
            // withDocker (dockerImageName) { 
            // Start docker, expose the workspace directory to the docker container
            sh "docker run -d -v ${hostWorkspaceDir}:${dockerWorkspaceDir} --name ${dockerContainerName} ${dockerImageName} sleep 7200"
        }
        stage ("Build corefx") {
            // Initialize the tools
            sh "docker exec ${dockerContainerName} cd ${dockerWorkspaceDir};./init-tools.sh"
            // Generate the version assets
            sh "docker exec ${dockerContainerName} cd ${dockerWorkspaceDir};./build-managed.sh -OfficialBuildId=${ghprbActualCommit} -- /t:GenerateVersionSourceFile /p:GenerateVersionSourceFile=true"
            // Sync
            sh "docker exec ${dockerContainerName} cd ${dockerWorkspaceDir};./sync.sh -p -portableLinux -- /p:ArchGroup=x64"
            // Build product
            sh "docker exec ${dockerContainerName} cd ${dockerWorkspaceDir};./build.sh -buildArch=x64 -${configuration} -portableLinux"
            // Build tests 
            sh "docker exec ${dockerContainerName} cd ${dockerWorkspaceDir};./build-tests.sh -buildArch=x64 -${configuration}"" -SkipTests -- /p:ArchiveTests=true /p:EnableDumpling=true"
            // Submit to Helix.
            /*sh "docker exec ${dockerContainerName} cd ${dockerWorkspaceDir};./Tools/msbuild.sh src/tests.builds /t:CloudBuild /p:ArchGroup=x64 /p:ConfigurationGroup=${configuration} /p:EnableCloudTest=true /p:TestProduct=corefx /p:TimeoutInSeconds=1200 /p:TargetOS=Linux /p:BuildMoniker=none /p:CloudDropAccountName=dotnetbuilddrops /p:CloudResultsAccountName=dotnetjobresults /p:CloudDropAccessToken=$(CloudDropAccessToken) /p:CloudResultsAccessToken=$(OutputCloudResultsAccessToken) /p:HelixApiAccessKey=$(HelixApiAccessKey) /p:HelixApiEndpoint=${helixApiEndpoint} /p:Branch=${GIT_BRANCH} /p:TargetQueue=${targetHelixQueues} /p:OfficialBuildId=${ghprbActualCommit}"
            // Publish packages
        }*/
    }
    finally {
        stage ("Clean-up Docker") {
            try {
                sh "docker stop ${dockerContainerName}"
                sh "docker rm ${dockerContainerName}"
            }
            catch(Exception e) {
                // Do nothing.
            }
        }
        stage ("Clean-up workspace") {
            step([$class: 'WsCleanup'])
        }
    }
}