package cern.root.pipeline

import hudson.model.Result
import hudson.plugins.emailext.plugins.content.ScriptContentBuildWrapper
import java.io.DataOutputStream
import java.net.Socket
import jenkins.metrics.impl.TimeInQueueAction
import jenkins.model.Jenkins

/**
 * Class for setting up a generic build of ROOT across a number of platforms.
 */
class GenericBuild implements Serializable {
    private def configuration = [:]
    private def buildResults = []
    private def postBuildSteps = []
    private def script
    private def mode
    private def graphiteReporter
    private def buildParameters = []
    private def jobName

    /**
     * Creates a new generic build.
     * @param script Script context.
     * @param jobName Name of generic job that will execute across all platforms.
     */
    GenericBuild(script, jobName, mode) {
        this.script = script
        this.mode = mode
        this.graphiteReporter = new GraphiteReporter(script, mode)
        this.jobName = jobName

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

        jobParameters << script.string(name: 'LABEL', value: label)
        jobParameters << script.string(name: 'COMPILER', value: compiler)
        jobParameters << script.string(name: 'BUILDTYPE', value: buildType)

        def result = script.build job: jobName, parameters: jobParameters, propagate: false
        def resultWrapper = [result: result, label: label, compiler: compiler, buildType: buildType]
        buildResults << resultWrapper

        for (postStep in postBuildSteps) {
            postStep(resultWrapper)
        }

        graphiteReporter.reportBuild(result.rawBuild)

        // Propagate build result
        if (result.result != Result.SUCCESS) {
            script.currentBuild.result = result.result
            throw new Exception("Build completed with result: ${result.result}")
        }
    }

    /**
     * Adds a configuration that ROOT should be built on.
     * @param label Label to build on, e.g. slc6.
     * @param compiler Compiler to build on, e.g. gcc62.
     * @param buildType Build type, e.g. Debug.
     */
    void buildOn(label, compiler, buildType) {
        script.println "Preparing build on $label"
        def configurationLabel = "$label-$compiler-$buildType"
        configuration[configurationLabel] = { performBuild(label, compiler, buildType) }
    }

    /**
     * Adds a set of pre-defined configurations.
     * @param configs Configurations to add.
     */
    void addConfigurations(configs) {
        for (config in configs) {
            buildOn(config.label, config.compiler, config.buildType)
        }
    }

    /**
     * Starts the build.
     */
    void build() {
        try {
            script.parallel(configuration)

            if (script.currentBuild.result == null) {
                script.currentBuild.result = Result.SUCCESS
            }
        } catch (e) {
            script.println "Build failed because: ${e.message}"
        }
    }

    /**
     * Adds a post-build step that will be executed after each build.
     * @param postStep Closure that will execute after the build.
     */
    void afterBuild(postStep) {
        postBuildSteps << postStep
    }

    /**
     * Adds a build parameter to the build.
     * @param key Name of the build parameter.
     * @param value Value of the build parameter.
     */
    @NonCPS
    void addBuildParameter(key, value) {
        buildParameters << script.string(name: key, value: String.valueOf(value))
    }

    /**
     * Sends an email report about the current build to a set of participants.
     * The email is generated from the template in resources/jenkins-pipeline-email-html.template.
     */
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

        def recipients = 'axel.naumann@cern.ch, pcanal@fnal.gov, patricia.mendez@cern.ch, pere.mato@cern.ch, martin.storo.nyflott@cern.ch, danilo.piparo@cern.ch'

        script.emailext(
                body: result, mimeType: 'text/html',
                recipientProviders:
                        [[$class: 'CulpritsRecipientProvider'], [$class: 'DevelopersRecipientProvider']],
                replyTo: '$DEFAULT_REPLYTO', subject: '$DEFAULT_SUBJECT',
                to: recipients)
    }
}
