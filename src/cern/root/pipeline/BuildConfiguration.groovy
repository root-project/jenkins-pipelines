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
            'ROOT-centos7',
            'ROOT-centos7-clangHEAD',
            'ROOT-performance-centos7-multicore',
            'ROOT-fedora27',
            'ROOT-fedora28',
            'ROOT-fedora29',
            'ROOT-fedora30',
            'mac1011',
            'mac1012',
            'mac1013',
            'mac1014',
            'ROOT-ubuntu14',
            'ROOT-ubuntu16',
            'ROOT-ubuntu18.04',
            'ROOT-ubuntu18.04-i386',
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
        'cxxmod',
        'pyroot_experimental',
        'jemalloc',
        'tcmalloc'
        ]
    }

    /**
     * @return Build configuration for pull requests.
     */
    static def getPullrequestConfiguration(extraCMakeOptions) {
        return [
            [ label: 'ROOT-performance-centos7-multicore', opts: extraCMakeOptions + ' -DCTEST_TEST_EXCLUDE_NONE=On', spec: 'default' ],
            [ label: 'ROOT-fedora27',  opts: extraCMakeOptions, spec: 'noimt' ],
            [ label: 'ROOT-fedora29',  opts: extraCMakeOptions, spec: 'python3' ],
            [ label: 'ROOT-fedora30',  opts: extraCMakeOptions, spec: 'cxx14' ],
            [ label: 'ROOT-ubuntu16',  opts: extraCMakeOptions, spec: 'nortcxxmod' ],
            [ label: 'ROOT-ubuntu18.04-i386',  opts: extraCMakeOptions, spec: 'cxx14' ],
            [ label: 'mac1014',   opts: extraCMakeOptions, spec: 'cxx17' ],
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
