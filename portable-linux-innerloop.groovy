// Todo: Refactor library so that we could have:
// standardNode('Ubuntu 16.04', 'latest') {
//     ...
// }
// ... is wrapped by a try/finally that defines workspace cleanup

// Define some top level parameters
stage ('Build') {
    node('ubuntu1604-20170216') {
        def dockerContainerName = BUILD_TAG
        def dockerRepository = 'microsoft/dotnet-buildtools-prereqs'
        def dockerTag = 'rhel7_prereqs_2'
        def dockerImageName = "${dockerRepository}:${dockerTag}"
        def hostWorkspaceDir = pwd()
        def dockerWorkspaceDir = '/root/workspace'
        try {
            checkout scm // Check out source control (based on parent scm settings)
            // Initialize tools
            sh "./init-tools.sh"
            // Initialize docker
            sh "./Tools/scripts/docker/init-docker.sh ${dockerImageName}"
            // Below might be great to wrap:
            // withDocker (dockerImageName) { 
            // Start docker, expose the workspace directory to the docker container
            sh "docker run -d -v ${workspaceDir}:${dockerWorkspaceDir} --name ${dockerContainerName} ${dockerImageName} sleep 7200"
            // Generate the version assets
            sh "docker exec ${dockerContainerName} ${dockerWorkspaceDir}/build-managed.sh -OfficialBuildId=${ghprbActualCommit} -- /t:GenerateVersionSourceFile /p:GenerateVersionSourceFile=true"
        }
        finally {
            step([$class: 'WsCleanup'])
            sh "docker stop ${dockerContainerName}"
            sh "docker rm ${dockerContainerName}"
        }
    }
}