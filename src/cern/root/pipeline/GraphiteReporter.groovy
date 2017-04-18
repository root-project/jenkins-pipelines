package cern.root.pipeline

import org.jenkinsci.plugins.plaincredentials.StringCredentials
import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.domains.DomainRequirement

import java.io.DataOutputStream
import java.net.Socket
import java.util.Collections

import hudson.security.ACL
import hudson.tasks.junit.TestResultAction
import jenkins.metrics.impl.TimeInQueueAction
import jenkins.model.Jenkins

/**
 * Reports build statistics to Graphite.
 */
class GraphiteReporter implements Serializable {
    private String graphiteServer
    private int graphiteServerPort
    private String graphiteMetricPath
    private def script
    private def mode

    /**
     * Creates a new GraphiteReporter.
     * The server configuration is read as secrets from Jenkins (secret text):
     *      graphite-server: Server hostname of the Graphite server.
     *      graphite-server-port: Server port of the Graphite server.
     *      graphite-metric-path: Metric path to send metrics to.
     * @param script Script context.
     * @param mode The build mode, e.g. continuous or experimental.
     */
    GraphiteReporter(script, mode) {
        this.script = script
        this.mode = mode
        this.graphiteServer = getSecret('graphite-server')
        this.graphiteServerPort = Integer.valueOf(getSecret('graphite-server-port'))
        this.graphiteMetricPath = getSecret('graphite-metric-path')
    }

    @NonCPS
    private def getSecret(secretId) {
        def creds = CredentialsMatchers.filter(
                CredentialsProvider.lookupCredentials(StringCredentials.class,
                        Jenkins.getInstance(), ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(secretId)
        )

        if (creds.size() == 0) {
            throw new Exception("No key for secret $secretId found, did you forget registering such a secret?")
        }

        return creds.get(0).getSecret().getPlainText()
    }
    
    @NonCPS
    private def reportMetric(metricName, x, y) {
        def connection = new Socket(graphiteServer, graphiteServerPort)
        def stream = new DataOutputStream(connection.getOutputStream())
        def metricPath = "${graphiteMetricPath}.${metricName}"
        def payload = "${metricPath} ${y} ${x}\n"

        stream.writeBytes(payload)
        connection.close()

        script.println("Posting graphite data: " + payload)
    }

    /**
     * Reports a build and its statistics to Graphite.
     * @param build Build to report.
     */
    def reportBuild(build) {
        def now = (long)(System.currentTimeMillis() / 1000)
        def totalRunTime = System.currentTimeMillis() - build.getTimeInMillis()
        script.println("Total build time: " + totalRunTime)

        reportMetric("build.${mode}.total_run_time", now, (long)(totalRunTime / 1000))

        def action = build.getAction(TimeInQueueAction)
        if (action != null) {
            // Time it takes to actually build
            def buildTime = totalRunTime - action.getQueuingDurationMillis()

            reportMetric("build.${mode}.run_time", now, (long)(buildTime / 1000))
            reportMetric("build.${mode}.queue_time", now, (long)(action.getQueuingDurationMillis() / 1000))
        }

        def testResults = build.getAction(TestResultAction)

        if (testResults != null) {
            def failedCount = testResults.failCount
            def skipCount = testResults.skipCount
            def totalCount = testResults.totalCount
            def passCount = totalCount - failedCount - skipCount

            testResults.getFailedTests().each { result ->
                String title = result.getFullName()
            }
        }
    }
}
