package cern.root.pipeline

import hudson.model.Result
import hudson.plugins.emailext.plugins.content.ScriptContentBuildWrapper
import java.io.DataOutputStream
import java.net.Socket
import jenkins.metrics.impl.TimeInQueueAction
import jenkins.model.Jenkins

class GenericBuild implements Serializable {
    private def configuration = [:]
    private def buildResults = []
    private def postBuildSteps = []
    private def script
    private def mode
    private def graphiteReporter
    private def buildParameters = []

    GenericBuild(script) {
        this.script = script
        this.mode = script.params.MODE
        this.graphiteReporter = new GraphiteReporter(script, mode)

        for (ParameterValue p in script.currentBuild.rawBuild.getAction(ParametersAction.class)) {
           addBuildParameter(p.name, String.valueOf(p.value))
        }

        // Always build the same branch on root and roottest
        addBuildParameter('ROOT_BRANCH', script.params.VERSION)
        addBuildParameter('ROOTTEST_BRANCH', script.params.VERSION)
    }

    private def performBuild(label, compiler, buildType) {
        def jobParameters = []

        // Copy parameters from build parameters
        for (parameter in buildParameters) {
            jobParameters << parameter
        }

        jobParameters << script.string(name: 'label', value: label)
        jobParameters << script.string(name: 'COMPILER', value: compiler)
        jobParameters << script.string(name: 'BUILDTYPE', value: buildType)

        def result = script.build job: 'ROOT-generic-build', parameters: jobParameters, propagate: false
        def resultWrapper = [result: result, label: label, compiler: compiler, buildType: buildType]
        buildResults << resultWrapper

        // Propagate build result without throwing an exception
        if (result.result != Result.SUCCESS) {
            script.currentBuild.result = result.result
        }

        postBuildSteps.each { postStep ->
            postStep(resultWrapper)
        }

        graphiteReporter.reportBuild(result.rawBuild)
    }

    void buildOn(label, compiler, buildType) {
        script.println "Preparing build on $label"
        def configurationLabel = "$label-$compiler-$buildType"
        configuration[configurationLabel] = { performBuild(label, compiler, buildType) }
    }

    void addConfigurations(configs) {
        configs.each { config -> 
            buildOn(config.label, config.compiler, config.buildType)
        }
    }

    void build() {
        script.parallel(configuration)

        if (script.currentBuild.result == null) {
            script.currentBuild.result = Result.SUCCESS
        }
    }

    void afterBuild(postStep) {
        postBuildSteps << postStep
    }

    @NonCPS
    void addBuildParameter(key, value) {
        buildParameters << script.string(name: key, value: String.valueOf(value))
    }

    @NonCPS
    void sendEmails() {
        def binding = ['build': script.currentBuild.rawBuild, 
                'rooturl': Jenkins.getActiveInstance().getRootUrl(), 
                'buildResults': buildResults,
                'it': new ScriptContentBuildWrapper(script.currentBuild.rawBuild),
                'project': script.currentBuild.rawBuild.getParent()]

        def classLoader = Jenkins.getActiveInstance().getPluginManager().uberClassLoader
        def shell = new GroovyShell(classLoader)
        def engine = new groovy.text.SimpleTemplateEngine(shell)
        def template = engine.createTemplate(script.libraryResource('jenkins-pipeline-email-html.template'))

        def result = template.make(binding).toString()

        def recpipients = 'martin.storo.nyflott@cern.ch'

        script.emailext(
                body: result, mimeType: 'text/html',
                //recipientProviders:
                //        [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider']],
                replyTo: '$DEFAULT_REPLYTO', subject: '$DEFAULT_SUBJECT',
                to: recpipients)
    }
}
