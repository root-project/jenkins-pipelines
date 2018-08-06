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
            'centos7',
            'fedora25',
            'fedora26',
            'fedora27',
            'fedora28',
            'mac1011',
            'mac1012',
            'mac1013',
            'slc6',
            'slc6-i686',
            'ubuntu14',
            'ubuntu16',
            'ubuntu17',
            'ubuntu18',
            'windows10'
        ]
    }

    /**
     * @return The available compilers that can be used.
     */
    @NonCPS
    static def getAvailableCompilers() {
        return [
            'clang39',
            'clang_gcc52',
            'clang_gcc62',
            'classic',
            'gcc48',
            'gcc49',
            'gcc52',
            'gcc54',
            'gcc62',
            'gcc7',
            'icc17',
            'icc18',
            'native',
            'vc15'
        ]
    }

    /**
     * @return Build configuration for pull requests.
     */
    static def getPullrequestConfiguration(extraCMakeOptions) {
        return [
            [ label: 'slc6',      compiler: 'gcc48',   buildType: 'Release', opts: ''                   + extraCMakeOptions ],
            [ label: 'slc6-i686', compiler: 'gcc49',   buildType: 'Release', opts: ''                   + extraCMakeOptions ],
            [ label: 'centos7',   compiler: 'clang39', buildType: 'Release', opts: ''                   + extraCMakeOptions ],
            [ label: 'centos7',   compiler: 'gcc62',   buildType: 'Release', opts: '-Dcxx14=ON'         + extraCMakeOptions ],
            [ label: 'centos7',   compiler: 'gcc7',    buildType: 'Release', opts: '-Dcxx17=ON'         + extraCMakeOptions ],
            [ label: 'fedora28',  compiler: 'native',  buildType: 'Release', opts: '-Dpython_version=3' + extraCMakeOptions ],
            [ label: 'ubuntu16',  compiler: 'native',  buildType: 'Release', opts: '-Dimt=OFF'          + extraCMakeOptions ],
            [ label: 'mac1013',   compiler: 'native',  buildType: 'Release', opts: ''                   + extraCMakeOptions ],
            [ label: 'windows10', compiler: 'vc15',    buildType: 'Release', opts: ''                   + extraCMakeOptions ]
        ]
    }

    /**
     * @return Build configuration for incrementals.
     */
    static def getIncrementalConfiguration() {
        return [
            [label: 'centos7', compiler: 'gcc62', buildType: 'Debug', opts: '-Dcxx14=ON'],
            [label: 'slc6',    compiler: 'gcc62', buildType: 'Debug', opts: '-Dcxx14=ON']
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
