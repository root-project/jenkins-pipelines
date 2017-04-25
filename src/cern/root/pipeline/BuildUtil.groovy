package cern.root.pipeline

import hudson.model.Run

/**
 * Util class for resetting builds. Should be removed once JENKINS-26522 is resolved.
 */
class BuildUtil {

    /**
     * Resets the result/status of a build.
     * @param build The build to reset.
     */
    @NonCPS
    static def resetStatus(Run build) {
        def field = Run.getDeclaredField('result')
        field.setAccessible(true)
        field.set(build, null)
    }
}
