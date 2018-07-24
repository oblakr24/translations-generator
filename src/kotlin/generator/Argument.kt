package generator

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionBuilder

/**
 * The available commandline options
 */
enum class Argument(
        private val description: String,
        val opt: String,
        private val hasArg: Boolean,
        val usageInstruction: String,
        private val action: (commandLine: CommandLine, argument: Argument, settings: TranslationsGenerator.GeneratorSettings) -> Unit) {

    CSVFolderPath(".csv folder path", "fl", true,
            "-fl <csv folder> : name of the folder containing the settings and translations files, default: null - same folder where the .jar is",
            { cl, arg, settings ->
                settings.csvFolderName = TranslationsGenerator.getOptionValue(cl, arg)
            }),

    Platform("Platform", "p", true,
            "-p <platform> : only write for the given platform (ios/android, case insensitive, default: null - both platforms)",
            { cl, arg, settings ->
                when (TranslationsGenerator.getOptionValue(cl, arg).toLowerCase()) {
                    "android" -> settings.wantedPlatform = generator.Platform.ANDROID
                    "ios" -> settings.wantedPlatform = generator.Platform.IOS
                }
            }),

    Client("Client", "c", true,
            "-c <client name> : only write for the given client (optional)",
            { cl, arg, settings ->
                // only one client was selected to have its translations generated
                settings.selectedClient = TranslationsGenerator.getOptionValue(cl, arg)
                settings.onlyDoSelectedClient = true
            }),

    DisplayHelp("Display help", "h", false,
            "-h : display help",
            { _, _, _ ->
                TranslationsGenerator.printUsage()
                java.lang.System.exit(0)
            }),

    ProjectFolder("Project folder", "fp", true,
            "-fp <project folder> : relative path to the project (default: null - .jar's parent directory)",
            { cl, arg, settings ->
                settings.projectPathStr = TranslationsGenerator.getOptionValue(cl, arg)
            }),

    CSVFilename(".csv filename", "l", true,
            "-l <.csv filename> : .csv file name (default: translations.csv)",
            { cl, arg, settings ->
                settings.csvFilename = TranslationsGenerator.getOptionValue(cl, arg)
            }),

    ClientList("Client list", "cl", false,
            "-cl : display the list of clients",
            { _, _, settings ->
                settings.printClientList = true
            }),

    DebugMode("Replaces the translations with their keys", "d", false,
            "-d : replaces the translations with their keys",
            { _, _, settings ->
                settings.debugMode = true
            }),

    VerbosePrintout("Verbose printout", "vp", false,
            "-vp : more verbose printout (missing or empty translation items)",
            { _, _, settings ->
                settings.verbosePrintout = true
            });

    fun checkAndExecute(commandLine: CommandLine, settings: TranslationsGenerator.GeneratorSettings) {
        if (commandLine.hasOption(opt)) {
            action(commandLine, this, settings)
        }
    }

    fun createOptionWithDescription(): Option {
        if (hasArg) OptionBuilder.hasArg()
        OptionBuilder.withDescription(description)
        return OptionBuilder.create(opt)
    }
}