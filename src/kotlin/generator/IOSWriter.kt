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

    override fun writeAll() = super.writeAll() && createTranslationKeysFile()

    override fun write(language: String, file: File) {
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
        for (item in translationItems) {
            try {
                // check whether the item has a new section specified
                val section = item.section
                if (section.isNotEmpty() && section != currentSection) {
                    currentSection = section
                    // insert new section element
                    val commentLine = "// $currentSection\";\n"
                    osw.write(commentLine)
                }
                val line = "\"" + item.key + "\" = \"" + item.translations[language] + "\";\n"
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
     * All credit to: Andraz Pajtler (a.pajtler@sportradar.com), 2016
     */
    private fun createTranslationKeysFile(): Boolean {
        val path = outputPathPrefix
                .resolve(clientName)
                .resolve(Paths.get("Localization"))
                .resolve(Paths.get("TranslationKey.swift"))
        System.err.println(path.toString())

        val file = File(path.toString())
        if (!file.parentFile.exists()) {
            System.err.println("Folder " + file.parentFile.toString() + " does not exist. Will not generate.")
            return false
        }

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
            return false
        }

        val osw = OutputStreamWriter(fos)

        try {
            // write the file header
            val header = "//\n" +
                    "//  TranslationKey.swift\n" +
                    "//  " + clientName + "\n" +
                    "//\n" +
                    "//\n" +
                    "\n" +
                    "struct TranslationKey {\n"
            osw.write(header)
            // write the keys
            var currentSection = ""
            for (item in translationItems) {
                // check whether the item has a new section specified
                val section = item.section
                if (section.isNotEmpty() && section != currentSection) {
                    currentSection = section
                    // insert a new section element
                    val commentLine = "\n\t// " + currentSection + "\n"
                    osw.write(commentLine)
                }
                val line = String.format("\tstatic let %s = \"%s\"\n", item.key, item.key)
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
        return true
    }
}
