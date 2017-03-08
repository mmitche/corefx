node('ubuntu1604-20170307') {
    checkout scm
    sh 'ls'
}

// Delete the directory to clean up the workspace
deleteDir()