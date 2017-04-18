package cern.root.pipeline

import java.util.regex.Pattern
import cern.root.pipeline.BuildConfiguration

class BotParser implements Serializable {
    private GitHub gitHub
    private boolean parsableComment
    private String matrix
    private String flags
    private static final String COMMENT_REGEX = 'build ((?<overrideDefaultConfiguration>just|also) on (?<matrix>([a-z0-9_]*\\/[a-z0-9_]*,?\\s?)*))?(with flags (?<flags>.*))?'
    private def script

    boolean overrideDefaultConfiguration
    def invalidBuildConfigurations = []
    def validBuildConfigurations = []
    String defaultExtraCMakeOptions
    String extraCMakeOptions

    BotParser(script, gitHub, defaultExtraCMakeOptions) {
        this.script = script
        this.gitHub = gitHub
        this.defaultExtraCMakeOptions = defaultExtraCMakeOptions
    }

    private def appendFlagsToMap(flags, map) {
        def parsedCompilerFlags = flags.split(' ')
        parsedCompilerFlags.each { unparsedFlag ->
            if (unparsedFlag.contains('=')) {
                def flag = unparsedFlag.split('=')

                if (map.containsKey(flag[0])) {
                    map[flag[0]] = flag[1]
                } else {
                    map.put(flag[0], flag[1])
                }
            }
        }
    }

    @NonCPS
    boolean isParsableComment(comment) {
        def matcher = Pattern.compile(COMMENT_REGEX).matcher(comment)

        parsableComment = matcher.find()

        if (parsableComment) {
            overrideDefaultConfiguration = matcher.group('overrideDefaultConfiguration').equals('just')
            matrix = matcher.group('matrix')
            flags = matcher.group('flags')
        }

        return parsableComment
    }

    void parse() {
        script.println 'Comment recognized as a parseable command'

        if (matrix != null) {
            // Parse and set the config
            def patterns = matrix.trim().replace(',' ,'').split(' ')

            patterns.each { unparsedPattern ->
                def patternArgs = unparsedPattern.split('/')
                def compiler = patternArgs[1]
                def platform = patternArgs[0]

                script.println "Received label $platform with compiler $compiler"

                if (!BuildConfiguration.recognizedPlatform(compiler, platform)) {
                    invalidBuildConfigurations << [compiler: compiler, platform: platform]
                } else {
                    validBuildConfigurations << [compiler: compiler, platform: platform]
                }
            }
        }

        if (flags != null) {
            def cmakeFlagsMap = [:]
            appendFlagsToMap(defaultExtraCMakeOptions, cmakeFlagsMap)
            appendFlagsToMap(flags, cmakeFlagsMap)

            extraCMakeOptions = cmakeFlagsMap.collect { /$it.key=$it.value/ } join ' '
        } else {
            extraCMakeOptions = defaultExtraCMakeOptions
        }

        script.println "ExtraCMakeOptions set to $extraCMakeOptions"
        script.println "Add default matrix config: $overrideDefaultConfiguration"
        script.println "CMake flags: $flags"
    }

    void postStatusComment() {
        // If someone posted a platform/compiler that isn't recognized, abort the build.
        if (invalidBuildConfigurations.size() > 0) {
            def unrecognizedPlatforms = new StringBuilder()

            invalidBuildConfigurations.each { config ->
                unrecognizedPlatforms.append('`' + config.compiler + '`/`' + config.platform + '`, ')
            }

            unrecognizedPlatforms.replace(unrecognizedPlatforms.length() - 2, unrecognizedPlatforms.length(), ' ')
            gitHub.postComment("Didn't recognize ${unrecognizedPlatforms.toString().trim()} aborting build.")

            throw new Exception("Unrecognized compiler(s)/platform(s): ${unrecognizedPlatforms.toString()}")
        } else {
            def commentResponse = new StringBuilder()
            commentResponse.append('Starting build on ')

            validBuildConfigurations.each { config ->
                commentResponse.append('`' + config.compiler + '`/`' + config.platform + '`, ')
            }

            if (!overrideDefaultConfiguration) {
                BuildConfiguration.getPullrequestConfiguration().each { config ->
                    commentResponse.append('`' + config.compiler + '`/`' + config.label + '`, ')
                }
            }

            // Remove last ',' after platforms listing
            commentResponse.replace(commentResponse.length() - 2, commentResponse.length(), ' ')

            if (extraCMakeOptions != null && extraCMakeOptions.size() > 0) {
                commentResponse.append("with CMake flags `$extraCMakeOptions`")
            }

            gitHub.postComment(commentResponse.toString())
        }
    }

    void configure(script, build) {
        validBuildConfigurations.each { config ->
            build.buildOn(config.platform, config.compiler, 'Debug')
        }

        // If no override of the platforms, add the default ones
        if (!overrideDefaultConfiguration) {
            script.println 'Adding default config'
            build.addConfigurations(BuildConfiguration.getPullrequestConfiguration())
        }

        script.env.ExtraCMakeOptions = extraCMakeOptions
    }
}