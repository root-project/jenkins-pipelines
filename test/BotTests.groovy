import cern.root.pipeline.BotParser
import cern.root.pipeline.BuildConfiguration

@interface NonCPS { }

// Mock class for GitHub
class GitHub {
    String postedComment
    def postComment(String comment) {
        this.postedComment = comment
    }
}
def assertConfiguration(actual, expected) {
    assert (actual.size() == expected.size())

    actual.each { actualConfiguration ->
        assert (expected.contains(actualConfiguration))
    }
}

// Assert default build config is discarded
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build just on mac1011/gcc49"))
parser.parse()
assert(parser.overrideDefaultConfiguration)
assertConfiguration(parser.validBuildConfigurations, [[compiler: 'gcc49', platform: 'mac1011']])


// Assert default build config is discarded
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build on mac1011/gcc49"))
parser.parse()
assert(parser.overrideDefaultConfiguration)
assertConfiguration(parser.validBuildConfigurations, [[compiler: 'gcc49', platform: 'mac1011']])

// Bot should run default build with no recognizable command
parser = new BotParser(this, "")
assert(!parser.isParsableComment("@phsft-bot build!"))
parser.parse()
assert(!parser.overrideDefaultConfiguration)


// Default build config is not discarded
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build also on mac1011/gcc49"))
parser.parse()
assert(!parser.overrideDefaultConfiguration)
assertConfiguration(parser.validBuildConfigurations, [[compiler: 'gcc49', platform: 'mac1011']])


// Just cmake options are read
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build with flags -Dfoo=bar"))
parser.parse()
assert(parser.extraCMakeOptions == "-Dfoo=bar")


// Cmake flags are overwritten
parser = new BotParser(this, "-Dfoo=don")
assert(parser.isParsableComment("@phsft-bot build with flags -Dfoo=bar"))
parser.parse()
assert(parser.extraCMakeOptions == "-Dfoo=bar")


// Multiple platforms are added
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build just on mac1011/gcc49 ubuntu14/native"))
parser.parse()
assert(parser.overrideDefaultConfiguration)
assertConfiguration(parser.validBuildConfigurations, [[compiler: "gcc49", platform: "mac1011"],
                                                      [compiler: "native", platform: "ubuntu14"]])


// Multiple platforms are added separated by comma
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build just on mac1011/gcc49, ubuntu14/native"))
parser.parse()
assert(parser.overrideDefaultConfiguration)
assertConfiguration(parser.validBuildConfigurations, [[compiler: "gcc49", platform: "mac1011"],
                                                      [compiler: "native", platform: "ubuntu14"]])


// Ignore unsupported platforms
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build just on mac1011/blaah, blaah/native"))
parser.parse()
assert(parser.overrideDefaultConfiguration)
assertConfiguration(parser.invalidBuildConfigurations, [[compiler: "blaah", platform: "mac1011"],
                                                        [compiler: "native", platform: "blaah"]])
assertConfiguration(parser.validBuildConfigurations, [])


// Newlines are not part of the command with flags
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build with flags -Dfoo=bar\nhello this is do"))
parser.parse()
assert(!parser.overrideDefaultConfiguration)
assert(parser.extraCMakeOptions == "-Dfoo=bar")


// Period are not part of the command with platforms
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build just on mac1011/gcc49."))
parser.parse()
assert(parser.overrideDefaultConfiguration)
assertConfiguration(parser.validBuildConfigurations, [[compiler: "gcc49", platform: "mac1011"]])


// Underscores are recognized
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build just on slc6/clang_gcc52"))
parser.parse()
assert(parser.overrideDefaultConfiguration)
assertConfiguration(parser.validBuildConfigurations, [[compiler: "clang_gcc52", platform: "slc6"]])


// Right comment is posted
gitHub = new GitHub()
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build just on slc6/clang_gcc52"))
parser.postStatusComment(gitHub)
assert(gitHub.postedComment.size() > 0)


// Assert cmake flags are posted in comments
gitHub = new GitHub()
parser = new BotParser(this, "")
assert(parser.isParsableComment("@phsft-bot build with flags -Dfoo=bar"))
parser.parse()
parser.postStatusComment(gitHub)
println parser.extraCMakeOptions
assert(gitHub.postedComment.size() > 0)
assert(gitHub.postedComment.contains("-Dfoo=bar"))


println 'All tests passing'
