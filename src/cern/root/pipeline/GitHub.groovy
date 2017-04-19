package cern.root.pipeline

import org.jenkinsci.plugins.ghprb.GhprbTrigger
import org.kohsuke.github.GHCommitState

/**
 * Facade towards GitHub.
 */
class GitHub implements Serializable {
    private def script
    private def parentJob
    private def repo
    private def prId
    private def sha1

    /**
     * Initialized a new GitHub facade.
     * @param script Script context.
     * @param parentJob The job to read the GitHub auth from.
     * @param repo Repository used for this build.
     * @param prId The pull request ID for this build.
     * @param sha1 Sha1 for the commit that triggered this build.
     */
    GitHub(script, parentJob, repo, prId, sha1) {
        this.script = script
        this.parentJob = parentJob
        this.repo = repo
        this.prId = prId
        this.sha1 = sha1
    }

    /**
     * Sets the commit status of this current build to failure.
     * @param statusText Status text to add on GitHub.
     */
    void setFailedCommitStatus(statusText) {
        setCommitStatus(GHCommitState.FAILURE, statusText, currentBuild)
    }

    /**
     * Sets the commit status of this current build to success.
     * @param statusText Status text to add on GitHub.
     */
    void setSucceedCommitStatus(statusText) {
        setCommitStatus(GHCommitState.SUCCESS, statusText, currentBuild)
    }

    @NonCPS
    private void setCommitStatus(status, statusText) {
        def triggerJob = script.manager.hudson.getJob(parentJob)
        def prbTrigger = triggerJob.getTrigger(GhprbTrigger.class)
        def repo = prbTrigger.getGitHub().getRepository(repo)

        repo.createCommitStatus(sha1, status, script.currentBuild.absoluteUrl, statusText, 'Jenkins CI build')
        script.println "Updating commit status to $status"
    }

    /**
     * Posts a comment on GitHub on the current pull request.
     * @param comment Comment to post.
     */
    @NonCPS
    void postComment(String comment) {
        script.println "Posting comment $comment for pr $prId"
        def triggerJob = script.manager.hudson.getJob(parentJob)
        def prbTrigger = triggerJob.getTrigger(GhprbTrigger.class)
        prbTrigger.getRepository().addComment(Integer.valueOf(prId), comment)
    }
}
