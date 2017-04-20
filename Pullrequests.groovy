#!groovy

@Library('root-pipelines')
import cern.root.pipeline.*

properties([
    parameters([
        string(name: 'ghprbPullId', defaultValue: '1'),
        string(name: 'ghprbGhRepository', defaultValue: 'root/root'),
        string(name: 'ghprbCommentBody', defaultValue: '@phsft-bot build'),
        string(name: 'ghprbTargetBranch', defaultValue: 'master'),
        string(name: 'sha1', defaultValue: '48214f30056e120818ea73b9fadf7b72268bc7de'),
        string(name: 'VERSION', defaultValue: 'master', description: 'Branch to be built'),
        string(name: 'EXTERNALS', defaultValue: 'ROOT-latest', description: ''),
        string(name: 'EMPTY_BINARY', defaultValue: 'true', description: 'Boolean to empty the binary directory (i.e. to force a full re-build)'),
        string(name: 'ExtraCMakeOptions', defaultValue: '-Dvc=OFF -Dimt=OFF -Dccache=ON', description: 'Additional CMake configuration options of the form "-Doption1=value1 -Doption2=value2"'),
        string(name: 'MODE', defaultValue: 'pullrequests', description: 'The build mode'),
        string(name: 'PARENT', defaultValue: 'root-pullrequests-trigger', description: 'Trigger job name')
    ])
])

GitHub gitHub = new GitHub(this, PARENT, ghprbGhRepository, ghprbPullId, sha1)
BotParser parser = new BotParser(this, params.ExtraCMakeOptions)
GenericBuild build = new GenericBuild(this, 'root-pullrequests-build')

build.addBuildParameter('ROOT_REFSPEC', '+refs/pull/*:refs/remotes/origin/pr/*')
build.addBuildParameter('ROOT_BRANCH', "origin/pr/${ghprbPullId}/merge")
build.addBuildParameter('ROOTTEST_BRANCH', "${params.ghprbTargetBranch}")

if (parser.isParsableComment(ghprbCommentBody.trim())) {
    parser.parse()
}

parser.postStatusComment(gitHub)
parser.configure(build)

stage('Building') {
    build.build()

    if (currentBuild.result == 'SUCCESS') {
        gitHub.setSucceedCommitStatus('Build passed')
    } else {
        gitHub.setFailedCommitStatus('Build failed')
    }
}

stage('Publish reports') {
    build.sendEmails()
}
