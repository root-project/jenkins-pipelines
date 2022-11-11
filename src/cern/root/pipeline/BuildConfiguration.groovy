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
            'ROOT-fedora29',
            'ROOT-fedora30',
            'ROOT-fedora31',
            'ROOT-fedora32',
            'ROOT-fedora34',
            'mac1015',
            'mac11',
            'mac11arm',
            'mac12',
            'mac12arm',
            'mac13',
            'mac13arm',
            'macbeta',
            'ROOT-ubuntu16',
            'ROOT-ubuntu18.04',
            'ROOT-ubuntu18.04-i386',
            'ROOT-ubuntu1904-clang',
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
        'cxx14',
        'cxx17',
        'python3',
        'noimt',
        'soversion',
        'rtcxxmod',
        'nortcxxmod',
        'cxxmod',
        'jemalloc',
        'tcmalloc'
        ]
    }

    /**
     * @return Build configuration for pull requests.
     */
    static def getPullrequestConfiguration(extraCMakeOptions) {
        return [
            [ label: 'ROOT-debian10-i386',  opts: extraCMakeOptions, spec: 'soversion' ],
            [ label: 'ROOT-performance-centos8-multicore', opts: extraCMakeOptions + ' -DCTEST_TEST_EXCLUDE_NONE=On', spec: 'cxx17' ],
            [ label: 'ROOT-ubuntu18.04',  opts: extraCMakeOptions, spec: 'nortcxxmod' ],
            [ label: 'ROOT-ubuntu2004',  opts: extraCMakeOptions, spec: 'python3' ],
            [ label: 'mac12',   opts: extraCMakeOptions, spec: 'noimt' ],
            [ label: 'mac11',   opts: extraCMakeOptions + ' -DCTEST_TEST_EXCLUDE_NONE=On', spec: 'cxx14' ],
            [ label: 'windows10', opts: extraCMakeOptions, spec: 'cxx14' ]
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
