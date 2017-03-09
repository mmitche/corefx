// Todo: Refactor library so that we could have:
// standardNode('Ubuntu 16.04', 'latest') {
//     ...
// }
// ... is wrapped by a try/finally that defines workspace cleanup
stage ('Build') {
    node('ubuntu1604-20170216') {
        try {
            checkout scm // Check out source control (based on parent scm settings)
            // Initialize tools
            sh './init-tools.sh'
            // Initialize docker
            sh './Tools/scripts/docker/init-docker.sh'
        }
        finally {
            step([$class: 'WsCleanup'])        
        }
    }
}