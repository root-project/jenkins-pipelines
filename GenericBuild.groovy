#!groovy

properties([
    parameters([
        string(name: 'ROOT_REFSPEC', defaultValue: '', description: 'Refspec for ROOT repository'),
        string(name: 'ROOTTEST_REFSPEC', defaultValue: '', description: 'Refspec for ROOTtest repository'),
        string(name: 'ROOTTEST_BRANCH', defaultValue: 'master', description: 'Name of the ROOT branch to work with'),
        string(name: 'ROOT_BRANCH', defaultValue: 'master', description: 'Name of the roottest branch to work with'),
        string(name: 'BUILD_NOTE', defaultValue: '', description: 'Note to add after label/compiler in job name'),
        string(name: 'BUILD_DESCRIPTION', defaultValue: '', description: 'Build description')
    ])
])


// Treat parameters as environment variables
for (ParameterValue p in params) {
    env[p.key] = p.value
}

env.GIT_URL = 'http://root.cern/git/root.git'

currentBuild.setDisplayName("#$BUILD_NUMBER $LABEL/SPEC $BUILD_NOTE")
currentBuild.setDescription("$BUILD_DESCRIPTION")

node(LABEL) {
    timestamps {
        stage('Checkout') {
            dir('root') {
                retry(3) {
                    // TODO: Use the git step when it has implemented specifying refspecs
                    // See https://jenkins.io/doc/pipeline/steps/workflow-scm-step/ for CloneOption
                    checkout([$class: 'GitSCM', branches: [[name: ROOT_BRANCH]], doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'CloneOption', timeout: 10, noTags: true, shallow: false]]
                                       +[[$class: 'LocalBranch', localBranch: '']],
                            submoduleCfg: [], userRemoteConfigs: [[refspec: ROOT_REFSPEC, url: env.GIT_URL]]])
                }
            }

            dir('roottest') {
                retry(3) {
                    def rootTestUrl = 'http://root.cern/git/roottest.git';
                    // TODO: Use the git step when it has implemented specifying refspecs
                    checkout([$class: 'GitSCM', branches: [[name: ROOTTEST_BRANCH]], doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'CloneOption', timeout: 10, noTags: true, shallow: false]]
                                       +[[$class: 'LocalBranch', localBranch: '']],
                            submoduleCfg: [], userRemoteConfigs: [[refspec: ROOTTEST_REFSPEC, url: rootTestUrl]]])
                }
            }

            dir('rootspi') {
                retry(3) {
                    git url: 'http://root.cern/git/rootspi.git'
                }
            }
        }

        try {
            stage('Build') {
              timeout(time: 240, unit: 'MINUTES') {
                if (LABEL == 'windows10') {
                    bat 'rootspi/jenkins/jk-all.bat'
                } else {
                    sh 'rootspi/jenkins/jk-all build'
                }
              }
            }

            if (LABEL != 'windows10') {
                stage('Test') {
                  timeout(time: 240, unit: 'MINUTES') {
                    sh 'rootspi/jenkins/jk-all test'

                    def testThreshold = [[$class: 'FailedThreshold',
                            failureNewThreshold: '0', failureThreshold: '0', unstableNewThreshold: '0',
                            unstableThreshold: '0'], [$class: 'SkippedThreshold', failureNewThreshold: '',
                            failureThreshold: '', unstableNewThreshold: '', unstableThreshold: '']]

                    step([$class: 'XUnitBuilder',
                            testTimeMargin: '3000', thresholdMode: 1, thresholds: testThreshold,
                            tools: [[$class: 'CTestType',
                                    deleteOutputFiles: true, failIfNotNew: false, pattern: 'build/Testing/*/Test.xml',
                                    skipNoTestFiles: false, stopProcessingIfError: true]]])

                    if (currentBuild.result == 'FAILURE') {
                        throw new Exception("Test result caused build to fail")
                    }
                  }
                }
            }
        } catch (err) {
            println 'Build failed because:'
            println err
            currentBuild.result = 'FAILURE'
        }


        //stage('Archive environment') {
        // TODO: Bundle and store build env in here
            //archiveArtifacts artifacts: 'build/'
        //}
        stash includes: 'rootspi/jenkins/logparser-rules/*', name: 'logparser-rules'
    }
}

// Log-parser-plugin will look for rules on master node. Unstash the rules and parse the rules. (JENKINS-38840)
node('master') {
    stage('Generate reports') {
        unstash 'logparser-rules'
        step([$class: 'LogParserPublisher',
                parsingRulesPath: "${pwd()}/rootspi/jenkins/logparser-rules/ROOT-incremental-LogParserRules.txt", 
                useProjectRule: false, unstableOnWarning: false, failBuildOnError: true])
    }
}
