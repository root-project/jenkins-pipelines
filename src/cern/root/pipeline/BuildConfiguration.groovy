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
        return [
            'arm64',
            'ROOT-centos8',
            'ROOT-debian10-i386',
            'ROOT-performance-centos8-multicore',
            'mac12',
            'mac12arm',
            'mac13',
            'mac13arm',
            'macbeta',
            'ROOT-ubuntu2004',
            'ROOT-ubuntu2004-clang',
            'ROOT-ubuntu2204',
            'windows10'
        ]
    }

    /**
     * @return The available specializations that can be used.
     */
    @NonCPS
    static def getAvailableSpecializations() {
        return [
        'default',
        'cxx17',
        'cxx20',
        'python3',
        'noimt',
        'soversion',
        'rtcxxmod',
        'nortcxxmod',
        'cxxmod',
        ]
    }

    /**
     * @return Build configuration for pull requests.
     */
    static def getPullrequestConfiguration(extraCMakeOptions) {
        return [
            [ label: 'ROOT-performance-centos8-multicore', opts: extraCMakeOptions + ' -DCTEST_TEST_EXCLUDE_NONE=On', spec: 'soversion' ],
            [ label: 'ROOT-ubuntu2204',  opts: extraCMakeOptions, spec: 'nortcxxmod' ],
            [ label: 'ROOT-ubuntu2004',  opts: extraCMakeOptions, spec: 'python3' ],
            [ label: 'mac12arm',   opts: extraCMakeOptions + ' -DCTEST_TEST_EXCLUDE_NONE=On', spec: 'cxx20' ],
            [ label: 'windows10', opts: extraCMakeOptions, spec: 'default']
        ]
    }

    /**
     * Checks if a specified configuration is valid or not.
     * @param compiler Compiler to check.
     * @param platform Platform to check.
     * @return True if recognized, otherwise false.
     */
    @NonCPS
    static boolean recognizedPlatform(String spec, String platform) {
        return getAvailableSpecializations().contains(spec) && getAvailablePlatforms().contains(platform)
    }
}
