#!groovy

node('docker-host') {
    timestamps {
        git 'https://github.com/root-project/rootspi.git'
        
        dir('docker/ubuntu16-base') {
            stage('Build') {
                sh "docker build -t rootproject/root-ubuntu16-base ."
            }
        
            stage('Push') {
                withCredentials([usernamePassword(credentialsId: 'root_dockerhub_deploy_user', passwordVariable: 'password', usernameVariable: 'username')]) {
                    sh "HOME=\$(pwd) && docker login -u '$username' -p '$password'"
                }
            
                sh "HOME=\$(pwd) && docker push rootproject/root-ubuntu16-base"
            }
        }
    }
}
