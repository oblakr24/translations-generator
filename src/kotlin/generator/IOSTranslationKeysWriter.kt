package generator

import java.io.*
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by r.oblak on 29/4/2018.
 */
class IOSTranslationKeysWriter(
        clientName: String,
        languageCodeResolver: LanguageCodeResolver,
        translationItems: List<TranslationItem>,
        languages: List<String>,
        defaultLanguage: String,
        outputPathPrefix: Path,
        private val caseType: IOSKeyCaseType? = null,
        verbosePrintout: Boolean = false) : TranslationWriter(clientName, languageCodeResolver, translationItems, languages, defaultLanguage, outputPathPrefix, verbosePrintout) {

    override val platform = Platform.IOS

    override fun setupOutputPath(language: String): Path = outputPathPrefix
            .resolve(clientName)
            .resolve(Paths.get("Localization"))
            .resolve(Paths.get("TranslationKey.swift"))

    /**
     * Creates the Swift translation keys file
     * Credit: a.pajtler@sportradar.com
     */
    override fun write(translations: List<Translation>, file: File) {

        val fos = try {
            FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            return
        }

        val osw = OutputStreamWriter(fos)

        try {
            // write the file header
            val header = "//\n" +
                    "//  TranslationKey.swift\n" +
                    "//  " + clientName + "\n" +
                    "//\n" +
                    "//\n" +
                    "// swiftlint:disable:next type_body_length\n" +
                    "struct TranslationKey {\n"
            osw.write(header)
            // write the keys
            var currentSection = ""
            for (item in translations) {
                // check whether the item has a new section specified
                val section = item.section
                if (section.isNotEmpty() && section != currentSection) {
                    currentSection = section
                    // insert a new section element
                    val commentLine = "\n\t// $currentSection\n"
                    osw.write(commentLine)
                }
                val formattedKey = item.key.formatCase(caseType)
                val line = String.format("\tstatic let %s = \"%s\"\n", formattedKey, item.key)
                osw.write(line)
            }
            // write the ending bracket
            osw.write("}")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            osw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun String.formatCase(caseType: IOSKeyCaseType?): String {
        caseType?.let { type ->
            val splitParts = split('_')

            return when (type) {
                IOSKeyCaseType.Camel -> splitParts.joinToString("") { it.capitalize() }.takeIf { it.isNotBlank() }?.let { pascalCased ->
                    val chars = pascalCased.toCharArray()
                    chars[0] = chars[0].toLowerCase()
                    String(chars)
                } ?: this
                IOSKeyCaseType.Pascal -> splitParts.joinToString("") { it.capitalize() }
                IOSKeyCaseType.Snake -> this
            }

        }
        // do not modify if no case given
        return this
    }
}
