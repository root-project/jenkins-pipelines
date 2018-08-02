#!groovy

def repoName = 'rootproject/root-ubuntu16'

node('docker-host') {
    timestamps {
        def stagingName = "rootbuild-${java.util.UUID.randomUUID()}"
        git 'https://github.com/root-project/rootspi.git'
        
        dir('docker/ubuntu16') {
            try {
                def ccacheVolumeName = "root-ccache-ubuntu16-native-Release-$branch"
                stage('Build') {
                    dir('root-build') {
                        dir('roottest') {
                            git url: 'http://root.cern/git/roottest.git', branch: branch
                        }
                        
                        dir('root') {
                            git url: 'http://root.cern/git/root.git', branch: branch
                        }
                        
                        dir('rootspi') {
                            git url: 'https://github.com/root-project/rootspi.git'
                        }
                    }
                    
                    sh "docker volume create $ccacheVolumeName"
                    sh "docker pull rootproject/root-ubuntu16-base"
                    sh "docker build -t $stagingName --build-arg uid=\$(id -u \$USER) ."
                    sh "HOME=\$(pwd) && docker run -t --name='$stagingName' -v $ccacheVolumeName:/ccache -v \$(pwd)/root-build:/root-build $stagingName /build.sh ubuntu16 native Release"

                    def testThreshold = [[$class: 'FailedThreshold', 
                            failureNewThreshold: '0', failureThreshold: '0', unstableNewThreshold: '0', 
                            unstableThreshold: '0'], [$class: 'SkippedThreshold', failureNewThreshold: '', 
                            failureThreshold: '', unstableNewThreshold: '', unstableThreshold: '']]

                    step([$class: 'XUnitBuilder', 
                            testTimeMargin: '3000', thresholdMode: 1, thresholds: testThreshold, 
                            tools: [[$class: 'CTestType', 
                                    deleteOutputFiles: true, failIfNotNew: false, pattern: 'root-build/Testing/*/Test.xml', 
                                    skipNoTestFiles: false, stopProcessingIfError: true]]])

                    if (currentBuild.result == 'FAILURE') {
                        throw new Exception("Test results failed the build")
                    }
                }
            
                stage('Push') {
                    sh "HOME=\$(pwd) && docker commit --change='CMD [\"root.exe\"]' $stagingName '$repoName:$tag'"                
                    withCredentials([usernamePassword(credentialsId: 'root_dockerhub_deploy_user', passwordVariable: 'password', usernameVariable: 'username')]) {
                        sh "HOME=\$(pwd) && docker login -u '$username' -p '$password'"
                    }
                
                    sh "HOME=\$(pwd) && docker push $repoName:$tag"

                    if (params['latestTag']) {
                        sh "HOME=\$(pwd) && docker tag $repoName:$tag $repoName:latest"
                        sh "HOME=\$(pwd) && docker push $repoName:latest"
                    }
                }
            } catch (e) {
                println 'Build failed because:'
                println e
                currentBuild.result = 'FAILURE'
            } finally {
                // Build back to green
                if (currentBuild.result == 'SUCCESS' && currentBuild.previousBuild?.result != 'SUCCESS') {
                    mattermostSend color: 'good', message: 'Docker build is back to green!'
                }

                // Build just failed
                if (currentBuild.result != 'SUCCESS' && currentBuild.previousBuild?.result == 'SUCCESS') {
                    mattermostSend color: 'danger', message: "Docker build [just failed](${currentBuild.absoluteUrl})"
                }

                // Remove containers/cleanup
                sh "HOME=\$(pwd) && docker rm -f \$(docker ps -a -f name=$stagingName -q)"
                sh "HOME=\$(pwd) && docker rmi -f $stagingName"
                sh "HOME=\$(pwd) && docker rmi -f $repoName:$tag"
            }
        }
    }
    cleanWs()
}
