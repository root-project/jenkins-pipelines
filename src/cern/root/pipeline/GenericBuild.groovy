package cern.root.pipeline

import hudson.model.Result
import hudson.plugins.emailext.plugins.content.ScriptContentBuildWrapper
import java.io.DataOutputStream
import java.net.Socket
import jenkins.metrics.impl.TimeInQueueAction;
import jenkins.model.Jenkins

class GenericBuild implements Serializable {
    private def configuration = [:]
    private def buildResults = []
    private def postBuildSteps = []
    private def script

    GenericBuild(script) {
        this.script = script
    }

    private def performBuild(label, compiler, buildType) {
        def buildParameters = []

        for (ParameterValue p in script.currentBuild.rawBuild.getAction(ParametersAction.class)) {
            buildParameters << script.string(name: p.name, value: String.valueOf(p.value))
        }

        buildParameters << script.string(name: 'label', value: label)
        buildParameters << script.string(name: 'COMPILER', value: compiler)
        buildParameters << script.string(name: 'BUILDTYPE', value: buildType)

        def result = script.build job: 'ROOT-generic-build', parameters: buildParameters, propagate: false
        def resultWrapper = [result: result, label: label, compiler: compiler, buildType: buildType]
        buildResults << resultWrapper

        // Propagate build result without throwing an exception
        if (result.result != Result.SUCCESS) {
            script.currentBuild.result = result.result
        }

        postBuildSteps.each { postStep ->
            postStep(resultWrapper)
        }
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

    def reportMetric(metricName, x, y) {
        /*def connection = new Socket(GRAPHITE_SERVER, GRAPHITE_SERVER_PORT);
        def stream = new DataOutputStream(connection.getOutputStream());
        def metricPath = "${GRAPHITE_METRIC_PATH}.${metricName}"
        def payload = "${metricPath} ${y} ${x}\n"

        stream.writeBytes(payload);
        connection.close();

        manager.listener.logger.println("Posting graphite data: " + payload)*/
    }

    def reportGrafiteStats(build) {
        /*def now = (long)(System.currentTimeMillis() / 1000)
        def totalRunTime = System.currentTimeMillis() - script.manager.build.getTimeInMillis()
        manager.listener.logger.println("Total build time: " + totalRunTime)

        reportMetric("build.${MODE}.total_run_time", now, (long)(totalRunTime / 1000))

        def action = manager.build.getAction(TimeInQueueAction.class)
        if (action != null) {
            // Time it takes to actually build
            def buildTime = totalRunTime - action.getQueuingDurationMillis()

            reportMetric("build.${MODE}.run_time", now, (long)(buildTime / 1000))
            reportMetric("build.${MODE}.queue_time", now, (long)(action.getQueuingDurationMillis() / 1000))
        }*/
    }
}
