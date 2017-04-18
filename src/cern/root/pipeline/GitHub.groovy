package cern.root.pipeline

import org.jenkinsci.plugins.ghprb.GhprbTrigger
import org.kohsuke.github.GHCommitState

class GitHub implements Serializable {
    private def script
    private def parentJob
    private def repo
    private def prId
    private def sha1

    GitHub(script, parentJob, repo, prId, sha1) {
        this.script = script
        this.parentJob = parentJob
        this.repo = repo
        this.prId = prId
        this.sha1 = sha1
    }

    void setFailedCommitStatus(statusText, currentBuild) {
        setCommitStatus(GHCommitState.FAILURE, statusText, currentBuild)
    }

    void setSucceedCommitStatus(statusText, currentBuild) {
        setCommitStatus(GHCommitState.SUCCESS, statusText, currentBuild)
    }

    @NonCPS
    void setCommitStatus(status, statusText, currentBuild) {
        def triggerJob = script.manager.hudson.getJob(parentJob)
        def prbTrigger = triggerJob.getTrigger(GhprbTrigger.class)
        def repo = prbTrigger.getGitHub().getRepository(repo)

        repo.createCommitStatus(sha1, status, currentBuild.absoluteUrl, statusText, 'Jenkins CI build')
        script.println "Updating commit status to $status"
    }

    @NonCPS
    void postComment(String comment) {
        script.println "Posting comment $comment for pr $prId"
        def triggerJob = script.manager.hudson.getJob(parentJob)
        def prbTrigger = triggerJob.getTrigger(GhprbTrigger.class)
        prbTrigger.getRepository().addComment(Integer.valueOf(prId), comment)
    }
}

