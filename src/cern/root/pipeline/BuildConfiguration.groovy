package cern.root.pipeline

class BuildConfiguration {
    static def getAvailablePlatforms() {
        return ['centos7', 'mac1011', 'slc6', 'ubuntu14']
    }

    static def getAvailableCompilers() {
        return ['gcc49', 'gcc62', 'native', 'clang_gcc52', 'clang_gcc62']
    }

    static def getPullrequestConfiguration() {
        return [
            [label: 'centos7', compiler: 'gcc62', buildType: 'Debug'],
            [label: 'slc6', compiler: 'gcc62', buildType: 'Debug']
        ]
    }

    static def getIncrementalConfiguration() {
        return [
            [label: 'centos7', compiler: 'gcc62', buildType: 'Debug'],
            [label: 'slc6', compiler: 'gcc62', buildType: 'Debug']
        ]
    }

    static boolean recognizedPlatform(String compiler, String platform) {
        return getAvailableCompilers().contains(compiler) && getAvailablePlatforms().contains(platform)
    }
}
