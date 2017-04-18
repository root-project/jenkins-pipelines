package cern.root.pipeline

import hudson.tasks.test.AbstractTestResultAction
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

void postMattermostReport(buildWrapper) {
    def result = buildWrapper.result
    def lastBuildFailed = !hudson.model.Result.SUCCESS.equals(currentBuild.rawBuild.getPreviousBuild()?.getResult())

    if (lastBuildFailed || result.getResult() != 'SUCCESS') {
        def messageBuilder = new StringBuilder()
        messageBuilder.append("${currentBuild.fullProjectName} - ${currentBuild.displayName} ")
        messageBuilder.append("completed with status ${result.result} ")
        messageBuilder.append("[Open](${currentBuild.absoluteUrl})\n")

        def summary = buildTestSummary(result)
        messageBuilder.append(summary)

        mattermostSend message: messageBuilder.toString(), color: getBuildColor(result)
    }
}

private def buildTestSummary(result) {
    def summary = new StringBuilder()

    def action = result.getRawBuild().getAction(AbstractTestResultAction.class)
    if (action != null) {
        int total = action.getTotalCount()
        int failed = action.getFailCount()
        int skipped = action.getSkipCount()
        summary.append('\nTest Status:\n')
        summary.append("\tPassed: ${(total - failed - skipped)}")
        summary.append(", Failed: $failed")
        summary.append(", Skipped: $skipped")
    } else {
        summary.append('\nNo Tests found.')
    }
    return summary.toString()
}

private def getBuildColor(RunWrapper r) {
    def result = r.getResult()
    if (result == 'SUCCESS') {
        return 'good'
    } else if (result == 'FAILURE') {
        return 'danger'
    } else {
        return 'warning'
    }
}
