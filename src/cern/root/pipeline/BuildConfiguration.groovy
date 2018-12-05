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
            'centos7-manycore',
            'ROOT-fedora27',
            'ROOT-fedora28',
            'ROOT-fedora29',
            'mac1011',
            'mac1012',
            'mac1013',
            'mac1014',
            'ROOT-ubuntu14',
            'ROOT-ubuntu16',
            'ROOT-ubuntu18',
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
        'rtcxxmod',
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
            [ label: 'ROOT-centos7', opts: extraCMakeOptions, spec: 'python3' ],
            [ label: 'ROOT-fedora27',  opts: extraCMakeOptions, spec: 'noimt' ],
            [ label: 'ROOT-ubuntu16',  opts: extraCMakeOptions, spec: 'rtcxxmod' ],
            [ label: 'mac1014',   opts: extraCMakeOptions, spec: 'cxx17' ],
            [ label: 'windows10', opts: extraCMakeOptions, spec: 'default' ]
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
