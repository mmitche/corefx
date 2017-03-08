node('ubuntu1604-20170307') {
    try {
        checkout scm
        sh 'ls'
    }
    finally {
        step([$class: 'WsCleanup'])        
    }
}