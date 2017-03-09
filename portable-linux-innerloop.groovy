// Todo: Refactor library so that we could have:
// standardNode('Ubuntu 16.04', 'latest') {
//     ...
// }
// ... is wrapped by a try/finally that defines workspace cleanup

node('ubuntu1604-20170216') {
    def dockerContainerName = BUILD_TAG
    def dockerRepository = 'microsoft/dotnet-buildtools-prereqs'
    def dockerTag = 'rhel7_prereqs_2'
    def dockerImageName = "${dockerRepository}:${dockerTag}"
    def hostWorkspaceDir = pwd()
    def dockerWorkspaceDir = '/root/workspace'
    try {
        stage ('Checkout Source') {
            checkout scm // Check out source control (based on parent scm settings)
        }
        stage ("Initialize tools and docker") {
            // Initialize tools
            sh "./init-tools.sh"
            // Initialize docker
            sh "./Tools/scripts/docker/init-docker.sh ${dockerImageName}"
        }
        stage ("Start docker (Name: ${dockerContainerName}, Image: ${dockerImageName})") {
            // Below might be great to wrap:
            // withDocker (dockerImageName) { 
            // Start docker, expose the workspace directory to the docker container
            echo "docker run -d -v ${hostWorkspaceDir}:${dockerWorkspaceDir} --name ${dockerContainerName} ${dockerImageName} sleep 7200"
            sh "docker run -d -v ${hostWorkspaceDir}:${dockerWorkspaceDir} --name ${dockerContainerName} ${dockerImageName} sleep 7200"
        }
        stage ("Build corefx") {
            // Generate the version assets
            sh "docker exec ${dockerContainerName} ${dockerWorkspaceDir}/build-managed.sh -OfficialBuildId=${ghprbActualCommit} -- /t:GenerateVersionSourceFile /p:GenerateVersionSourceFile=true"
        }
    }
    finally {
        stage ("Clean-up docker") {
            try {
                sh "docker stop ${dockerContainerName}"
                sh "docker rm ${dockerContainerName}"
            }
            catch {
                // Do nothing.
            }
        }
        stage ("Clean-up workspace") {
            step([$class: 'WsCleanup'])
        }
    }
}