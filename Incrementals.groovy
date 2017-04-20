#!groovy

@Library('root-pipelines')
import cern.root.pipeline.*

properties([
    pipelineTriggers([githubPush(), pollSCM('H/10 * * * *')]),
    parameters([
        string(name: 'VERSION', defaultValue: 'master', description: 'Branch to be built'),
        string(name: 'EXTERNALS', defaultValue: 'ROOT-latest', description: ''),
        string(name: 'EMPTY_BINARY', defaultValue: 'false', description: 'Boolean to empty the binary directory (i.e. to force a full re-build)'),
        string(name: 'ExtraCMakeOptions', defaultValue: '-Dvc=OFF -Dimt=OFF -Dccache=ON', description: 'Additional CMake configuration options of the form "-Doption1=value1 -Doption2=value2"'),
        string(name: 'MODE', defaultValue: 'experimental', description: 'The build mode')
    ])
])

GenericBuild build = new GenericBuild(this, 'root-incrementals-build')

stage('Configuring') {
    node('master') {
        git url: 'https://github.com/root-project/root.git', branch: 'master'
    }

    build.addConfigurations(BuildConfiguration.incrementalConfiguration)

    def mattermost = new Mattermost()
    build.afterBuild({ finishedBuild ->
       // mattermost.postMattermostReport(finishedBuild)
    })
}

stage('Building') {
    build.build()
}

stage('Publish reports') {
    build.sendEmails()
}
