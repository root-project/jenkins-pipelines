package cern.root.pipeline

/**
 * Contains available and default build configurations.
 */
class BuildConfiguration {
    /**
     * @return The available platforms/labels that can be used.
     */
    @NonCPS
    static def getAvailablePlatforms() {
        return ['centos7', 'mac1011', 'slc6', 'ubuntu14', 'mac1012', 'ubuntu16', 'arm64', 'fedora25']
    }

    /**
     * @return The available compilers that can be used.
     */
    @NonCPS
    static def getAvailableCompilers() {
        return ['gcc49', 'gcc62', 'native', 'clang_gcc52', 'clang_gcc62', 'icc15', 'icc17', 'classic']
    }

    /**
     * @return Build configuration for pull requests.
     */
    static def getPullrequestConfiguration() {
        return [
            [label: 'centos7', compiler: 'gcc49', buildType: 'Debug'],
            [label: 'mac1012', compiler: 'native', buildType: 'Debug'],
            [label: 'slc6', compiler: 'gcc49', buildType: 'Debug'],
            [label: 'slc6', compiler: 'gcc62', buildType: 'Debug'],
            [label: 'ubuntu14', compiler: 'native', buildType: 'Debug']
        ]
    }

    /**
     * @return Build configuration for incrementals.
     */
    static def getIncrementalConfiguration() {
        return [
            [label: 'centos7', compiler: 'gcc62', buildType: 'Debug'],
            [label: 'slc6', compiler: 'gcc62', buildType: 'Debug']
        ]
    }

    /**
     * Checks if a specified configuration is valid or not.
     * @param compiler Compiler to check.
     * @param platform Platform to check.
     * @return True if recognized, otherwise false.
     */
    @NonCPS
    static boolean recognizedPlatform(String compiler, String platform) {
        return getAvailableCompilers().contains(compiler) && getAvailablePlatforms().contains(platform)
    }
}
