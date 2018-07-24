package generator

import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

import org.apache.commons.cli.*

/**
 * Created by r.oblak on 9/9/2015.
 * The main application singleton
 */
object TranslationsGenerator {

    /**
     * The translations generator settings variables
     */
    class GeneratorSettings {
        var projectPathStr: String? = null
        var csvFolderName: String? = null
        var settingsFilename = "settings.json"
        var csvFilename = "translations.csv"
        var selectedClient: String? = null
        var wantedPlatform: Platform? = null
        var verbosePrintout = false  // if true, print more verbose output like missing translations
        var onlyDoSelectedClient = false
        var printClientList = false
        var debugMode = false
        var generateAllNonExistingPaths = false
    }

    /**
     * Holds the writers for each client and all the generated file paths of the translations files
     */
    data class GenerationResult(
            val writers: List<MainWriter>,
            val generatedFilePaths: List<Path>,
            val selectedClientDone: Boolean)

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // the defaults
        val defaultSettings = GeneratorSettings()

        // parse the arguments to modify the defaults
        parseArguments(args, defaultSettings)

        val (writers, generatedPaths, selectedClientDone) = prepare(defaultSettings)

        if (defaultSettings.onlyDoSelectedClient && !selectedClientDone) {
            System.err.println("Client $defaultSettings.selectedClient not found in settings file.")
            System.exit(0)
        }

        validateAndGeneratePaths(generatedPaths, defaultSettings)

        writeAll(writers)
    }

    /**
     * Print the usage instructions to the console
     */
    fun printUsage() {
        System.err.println("Usage (all clients): java -jar TranslationsGenerator.jar -p <platform> ")
        System.err.println("Usage (specific client): java -jar TranslationsGenerator.jar -p <platform> -c <clientName> ")
        System.err.println("All options: ")
        Argument.values().forEach {
            System.err.println(it.usageInstruction)
        }
    }

    /**
     * Get the option value from the commandline
     */
    fun getOptionValue(commandLine: CommandLine, argument: Argument): String = commandLine.getOptionValue(argument.opt)

    /**
     * Parse the commandline arguments to modify the provided settings
     */
    private fun parseArguments(args: Array<String>, settings: GeneratorSettings) {
        // add the commandline options
        val options = Options().apply {
            Argument.values().forEach {
                addOption(it.createOptionWithDescription())
            }
        }

        try {
            val commandLine = GnuParser().parse(options, args)

            if (args.isEmpty()) {
                printUsage()
                System.exit(0)
            }

            // check and execute the options
            Argument.values().forEach {
                it.checkAndExecute(commandLine, settings)
            }
        } catch (exception: ParseException) {
            System.err.print("Command line parse error: ")
            System.err.println(exception.message)
            printUsage()
            System.exit(0)
        }
    }

    /**
     * Prepare the writers and the translation files paths
     */
    fun prepare(settings: GeneratorSettings): GenerationResult {
        // resolve the project path
        val projectPath = if (settings.projectPathStr != null) {
            // project folder given
            Paths.get(settings.projectPathStr).toAbsolutePath()
        } else {
            // project folder not given, take the parent folder as the default project folder
            Paths.get("").toAbsolutePath().parent
        }

        // resolve the folder of the .csv, the current folder by default
        val csvFolderPath = settings.csvFolderName?.let { projectPath.resolve(Paths.get(it)) } ?: Paths.get("")

        val settingsPath = csvFolderPath.resolve(Paths.get(settings.settingsFilename))
        val csvPath = csvFolderPath.resolve(Paths.get(settings.csvFilename))

        if (!validateArguments(settingsPath.toString(), csvPath.toString())) {
            System.exit(0)
        }

        // parse the settings
        val translationsSettings = SettingsParser.parse(File(settingsPath.toString()))
        val languageCodeResolver = LanguageCodeResolver(translationsSettings.languageCodeMapping)

        if (translationsSettings.targets.isEmpty()) {
            System.err.println("No clients found in the settings file.")
            System.exit(0)
        }

        if (settings.printClientList) {
            // print all the client names and exit
            for (targetSetting in translationsSettings.targets) {
                print(targetSetting.clientName + " ")
            }
            println()
            System.exit(1)
        }

        settings.wantedPlatform?.let {
            println("Generating for " + it.toString())
        }

        // get the main translations items
        val mainItems = TranslationParser.parse(csvPath.toString(), settings.debugMode, translationsSettings.writeEnglishIfMissing).items

        val allGeneratedFilePaths = mutableListOf<Path>()

        // the list of writers
        val clientWriters = mutableListOf<MainWriter>()

        // for all clients in target settings
        var selectedClientDone = false
        loop@ for (targetSetting in translationsSettings.targets) {

            // if generating only for the specified client, continue until the client is found
            if (settings.onlyDoSelectedClient && targetSetting.clientName != settings.selectedClient) continue

            var doAndroid = true
            var doIOS = true
            // if there was a wanted platform specified, override the target settings' platforms with it (ONLY IF PLATFORM ALREADY IN SETTINGS)
            if (settings.wantedPlatform != null) {
                when (settings.wantedPlatform) {
                    Platform.IOS -> {
                        if (!targetSetting.doIOS) {
                            System.err.println("${targetSetting.clientName} setting does not include iOS! Will not generate.")
                            continue@loop
                        }
                        doAndroid = false
                    }
                    Platform.ANDROID -> {
                        if (!targetSetting.doAndroid) {
                            System.err.println("${targetSetting.clientName} setting does not include Android! Will not generate.")
                            continue@loop
                        }
                        doIOS = false
                    }
                }
            }

            // if the client's settings has a client translations filename, override the default translations
            if (targetSetting.clientCSVFilename?.isNotEmpty() == true) {
                val csvClientPath = csvFolderPath.resolve(Paths.get(targetSetting.clientCSVFilename))

                // check whether the additional client translations file exists
                if (!File(csvClientPath.toString()).exists()) {
                    System.err.println("Client .csv file: " + csvClientPath.toString() + " doesn't exist.")
                    continue
                }

                val (overriddenItems, newItems) = Utils.overrideTranslations(mainItems, TranslationParser.parse(csvClientPath.toString(), settings.debugMode, translationsSettings.writeEnglishIfMissing).items)
                println("$overriddenItems translations overridden, $newItems translations added for client ${targetSetting.clientName}.")
            }

            val clientWriter = MainWriter(targetSetting, doIOS, doAndroid, languageCodeResolver, mainItems, projectPath, settings.verbosePrintout, translationsSettings.iosKeyCaseType)
            clientWriters.add(clientWriter)
            allGeneratedFilePaths.addAll(clientWriter.generatedFilePaths)

            // generated for the specified client, end here
            if (settings.onlyDoSelectedClient) {
                selectedClientDone = true
                break
            }
        }

        return GenerationResult(clientWriters, allGeneratedFilePaths, selectedClientDone)
    }

    /**
     * Validate the provided paths to ensure they exist, otherwise prompt the user to create them
     */
    fun validateAndGeneratePaths(generatedFilePaths: List<Path>, settings: GeneratorSettings) {
        /* check all the files that are to be generated - they all need to exist */
        var createAll = settings.generateAllNonExistingPaths
        label@ for (path in generatedFilePaths) {
            val generated = path.toFile()
            if (!generated.exists()) {
                if (!createAll) {
                    System.err.println("File " + path.toString() + " does not exist. Create?")
                    System.err.println("Y (yes) / _ (no) / ALL (yes to all)")
                    val response = System.console().readLine().toUpperCase()

                    when (response) {
                        "Y", "YES" -> { }
                        "YES TO ALL", "ALL" -> createAll = true
                        else -> {
                            System.err.println("File will not be created. Either fix the paths or the targetLanguages specified in the settings file.")
                            break@label
                        }
                    }
                }
                /* create the file if specified */
                generated.parentFile.mkdirs()
                generated.createNewFile()
            }
        }
    }

    /**
     * Have all the writers write the translation files
     */
    fun writeAll(writers: List<MainWriter>) {
        for (mainWriter in writers) {
            val successful = mainWriter.writeAll()
            if (successful) println("Translations for client " + mainWriter.targetSetting.clientName + " generated successfully.")
        }
    }

    /**
     * Validate the arguments
     */
    private fun validateArguments(settingsFilename: String, defaultCSVFilename: String): Boolean {
        // log all problems (don't return immediately)
        var success = true
        // check if provided filenames are valid
        if (!File(settingsFilename).exists()) {
            System.err.println("SettingsParser file: $settingsFilename doesn't exist.")
            success = false
        }
        // check if the main .csv exists
        if (!File(defaultCSVFilename).exists()) {
            System.err.println("Main .csv file: $defaultCSVFilename doesn't exist.")
            success = false
        }
        return success
    }
}