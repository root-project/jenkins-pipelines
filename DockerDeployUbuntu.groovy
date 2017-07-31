#!groovy

def username = 'rootprojectsftnight'
def repoName = 'rootproject/root-ubuntu16'

node('docker-host') {
    timestamps {
        def stagingName = "rootbuild-${java.util.UUID.randomUUID()}"
        git 'https://github.com/martinmine/ROOT-Docker.git'
        
        dir('ubuntu16') {
            try {
                def ccacheVolumeName = "root-ccache-ubuntu16-native-Release-$branch"
                stage('Build') {
                    dir('root-build') {
                        dir('roottest') {
                            git url: 'https://github.com/root-project/roottest.git', branch: branch
                        }
                        
                        dir('root') {
                            git url: 'https://github.com/root-project/root.git', branch: branch
                        }
                        
                        dir('rootspi') {
                            git url: 'https://github.com/root-project/rootspi.git'
                        }
                    }
                    
                    sh "docker volume create $ccacheVolumeName"
                    sh "docker pull rootproject/root-ubuntu16-base"
                    sh "docker build -t $stagingName ."
                    sh "HOME=\$(pwd) && docker run -t --name='$stagingName' -v $ccacheVolumeName:/ccache -v \$(pwd)/root-build:/root-build $stagingName /build.sh ubuntu16 native Release $branch"

                    def testThreshold = [[$class: 'FailedThreshold', 
                            failureNewThreshold: '0', failureThreshold: '0', unstableNewThreshold: '0', 
                            unstableThreshold: '0'], [$class: 'SkippedThreshold', failureNewThreshold: '', 
                            failureThreshold: '', unstableNewThreshold: '', unstableThreshold: '']]

                    step([$class: 'XUnitBuilder', 
                            testTimeMargin: '3000', thresholdMode: 1, thresholds: testThreshold, 
                            tools: [[$class: 'CTestType', 
                                    deleteOutputFiles: true, failIfNotNew: false, pattern: 'root-build/build/Testing/*/Test.xml', 
                                    skipNoTestFiles: false, stopProcessingIfError: true]]])

                    if (currentBuild.result == 'FAILURE') {
                        throw new Exception("Test results failed the build")
                    }
                }
            
                stage('Push') {
                    sh "HOME=\$(pwd) && docker commit --change='CMD [\"root.exe\"]' $stagingName '$repoName:$tag'"                
                    withCredentials([string(credentialsId: 'DOCKERHUB_ROOTPROJECT_PASSWORD', variable: 'password')]) {
                        sh "HOME=\$(pwd) && docker login -u '$username' -p '$password'"
                    }
                
                    sh "HOME=\$(pwd) && docker push $repoName:$tag"
                }
            }
            finally {
                // Remove containers/cleanup
                sh "HOME=\$(pwd) && docker rm -f \$(docker ps -a -f name=$stagingName -q)"
                sh "HOME=\$(pwd) && docker rmi -f $stagingName"
                sh "HOME=\$(pwd) && docker rmi -f $repoName:$tag"
            }
        }
    }
}
