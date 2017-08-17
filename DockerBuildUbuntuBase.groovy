#!groovy

def username = 'rootprojectsftnight'

node('docker-host') {
    timestamps {
        git 'https://github.com/root-project/rootspi.git'
        
        dir('docker/ubuntu16-base') {
            stage('Build') {
                sh "docker build -t rootproject/root-ubuntu16-base ."
            }
        
            stage('Push') {
                withCredentials([string(credentialsId: 'DOCKERHUB_ROOTPROJECT_PASSWORD', variable: 'password')]) {
                    sh "HOME=\$(pwd) && docker login -u '$username' -p '$password'"
                }
            
                sh "HOME=\$(pwd) && docker push rootproject/root-ubuntu16-base"
            }
        }
    }
}
