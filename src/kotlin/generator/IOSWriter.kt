package generator

import java.io.*
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by r.oblak on 9/8/2015.
 */
class IOSWriter(
        clientName: String,
        languageCodeResolver: LanguageCodeResolver,
        translationItems: List<TranslationItem>,
        languages: List<String>,
        defaultLanguage: String,
        outputPathPrefix: Path,
        verbosePrintout: Boolean = false) : TranslationWriter(clientName, languageCodeResolver, translationItems, languages, defaultLanguage, outputPathPrefix, verbosePrintout) {

    override val platform = Platform.IOS

    override fun setupOutputPath(language: String): Path = Paths.get(clientName)
                .resolve(Paths.get("Localization"))
                .resolve(Paths.get(languageCodeResolver.resolveLangCode(language) + ".lproj"))
                .resolve(Paths.get("Localizable.strings"))

    override fun write(translations: List<Translation>, file: File) {
        // return if the folder doesn't exist
        if (!file.parentFile.exists()) {
            System.err.println("Folder " + file.parentFile.toString() + " does not exist. Will not generate.")
            return
        }
        // create a new file if it doesn't exist
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (ex: IOException) {
                /* ignore */
            }
        }

        val fos = try {
            FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            throw ClientDoesNotExistException(clientName, outputPath.toString())
        }

        val osw = OutputStreamWriter(fos)

        var currentSection = ""
        for (item in translations) {
            try {
                // check whether the item has a new section specified
                val section = item.section
                if (section.isNotEmpty() && section != currentSection) {
                    currentSection = section
                    // insert new section element
                    val commentLine = "// $currentSection\";\n"
                    osw.write(commentLine)
                }

                val joinedTranslation = parseCDATAParts(item.translation).joinToString("") { it.second }

                val translation = joinedTranslation.toiOSSpecificFormattedString()
                val line = "\"" + item.key + "\" = \"" + translation + "\";\n"
                osw.write(line)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        try {
            osw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Do some iOS customizations for translations of formatted strings
     * e.g. '%s' -> '%@'
     */
    private fun String.toiOSSpecificFormattedString(): String {
        // replace base '%s' occurrences
        var str = replace("%s", "%@")
        // replace indexed occurrences, e.g. '%1$s'
        Regex("%\\d+(\\\$s)").findAll(str).asIterable().mapNotNull { it.groups[1]?.range }.forEach { range ->
            str = str.replaceRange(range, "$@")
        }
        return str
    }
}
