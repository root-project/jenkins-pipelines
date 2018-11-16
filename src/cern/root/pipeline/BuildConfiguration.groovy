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
            'ROOT-fedora26',
            'ROOT-fedora27',
            'ROOT-fedora28',
            'mac1011',
            'mac1012',
            'mac1013',
            'mac1014',
            'ROOT-ubuntu14',
            'ROOT-ubuntu16',
            'ROOT-ubuntu17',
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
	    'cxx14',
	    'cxx17',
	    'python3',
	    'noimt'
        ]
    }

    /**
     * @return Build configuration for pull requests.
     */
    static def getPullrequestConfiguration(extraCMakeOptions) {
        return [
            [ buildType: 'Release', label: 'ROOT-centos7', opts: extraCMakeOptions, spec: 'noimt' ],
            [ buildType: 'Release', label: 'ROOT-fedora29',  opts: extraCMakeOptions, spec: 'python3' ],
            [ buildType: 'Release', label: 'ROOT-ubuntu16',  opts: extraCMakeOptions, spec: 'rtcxxmod' ],
            [ buildType: 'Release', label: 'mac1014',   opts: extraCMakeOptions, spec: 'cxx17' ],
            [ buildType: 'Release', label: 'windows10', opts: extraCMakeOptions, spec: 'default' ]
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
        return getAvailableSpecializations().confains(spec) && getAvailablePlatforms().contains(platform)
    }
}
