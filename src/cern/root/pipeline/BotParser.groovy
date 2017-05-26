package cern.root.pipeline

import java.util.regex.Pattern
import cern.root.pipeline.BuildConfiguration

/**
 * Handles parsing of build configurations from a user-specified command.
 * This command could for example be a comment that is posted on GitHub.
 */
class BotParser implements Serializable {
    private boolean parsableComment
    private String matrix
    private String flags
    private static final String COMMENT_REGEX = 'build ((?<overrideDefaultConfiguration>just|also) on (?<matrix>([a-z0-9_]*\\/[a-z0-9_]*,?\\s?)*))?(with flags (?<flags>.*))?'
    private def script

    /**
     * Whether the default configuration for the job should be discarded.
     */
    boolean overrideDefaultConfiguration = false

    /**
     * List of the build configurations that was not recognized.
     */
    def invalidBuildConfigurations = []

    /**
     * List of the recognized build configurations.
     */
    def validBuildConfigurations = []

    /**
     * The original/default ExtraCMakeOptions value from the original build.
     */
    String defaultExtraCMakeOptions

    /**
     * CMake options to use for this build.
     */
    String extraCMakeOptions

    /**
     * Initiates a new BotParser
     * @param script Pipeline script context.
     * @param defaultExtraCMakeOptions The default CMake options to use if user didn't specify anything.
     */
    BotParser(script, defaultExtraCMakeOptions) {
        this.script = script
        this.defaultExtraCMakeOptions = defaultExtraCMakeOptions
        this.extraCMakeOptions = defaultExtraCMakeOptions
    }

    @NonCPS
    private def appendFlagsToMap(flags, map) {
        def parsedCompilerFlags = flags.split(' ')
        for (unparsedFlag in parsedCompilerFlags) {
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

    /**
     * Checks if a comment is recognized as a comment or not. If it was, it will also pull out the recognized bits from
     * the comment. It will however, not parse the job configuration.
     * @param comment Comment to check.
     * @return True if is parsable, otherwise false.
     */
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

    /**
     * Parses and sets the build configuration based on the comment set in isParsableComment.
     */
    @NonCPS
    void parse() {
        script.println 'Comment recognized as a parseable command'

        if (matrix != null) {
            // Parse and set the config
            def patterns = matrix.trim().replace(',' ,'').split(' ')

            for (unparsedPattern in patterns) {
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
        }

        script.println "ExtraCMakeOptions set to $extraCMakeOptions"
        script.println "Add default matrix config: $overrideDefaultConfiguration"
        script.println "CMake flags: $flags"
    }

    /**
     * Posts a comment to GitHub about what configuration will be used.
     * @param gitHub GitHub.
     */
    void postStatusComment(gitHub) {
        // If someone posted a platform/compiler that isn't recognized, abort the build.
        if (invalidBuildConfigurations.size() > 0) {
            def unrecognizedPlatforms = new StringBuilder()

            for (config in invalidBuildConfigurations) {
                unrecognizedPlatforms.append('`' + config.compiler + '`/`' + config.platform + '`, ')
            }

            unrecognizedPlatforms.replace(unrecognizedPlatforms.length() - 2, unrecognizedPlatforms.length(), ' ')
            gitHub.postComment("Didn't recognize ${unrecognizedPlatforms.toString().trim()} aborting build.")

            throw new Exception("Unrecognized compiler(s)/platform(s): ${unrecognizedPlatforms.toString()}")
        } else {
            def commentResponse = new StringBuilder()
            commentResponse.append('Starting build on ')

            for (config in validBuildConfigurations) {
                commentResponse.append('`' + config.compiler + '`/`' + config.platform + '`, ')
            }

            if (!overrideDefaultConfiguration) {
                for (config in BuildConfiguration.getPullrequestConfiguration()) {
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

    /**
     * Configures a build to use the configuration that has been parsed.
     * @param script Script context.
     * @param build Build to configure.
     */
    void configure(build) {
        for (config in validBuildConfigurations) {
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
