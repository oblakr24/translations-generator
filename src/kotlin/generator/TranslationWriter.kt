package generator

import java.io.File
import java.nio.file.Path

/**
 * Created by r.oblak on 9/8/2015.
 * A writer class for generating translations files for several languages
 */
abstract class TranslationWriter(protected var clientName: String,  // the client name
                                 protected val languageCodeResolver: LanguageCodeResolver,
                                 protected var translationItems: List<TranslationItem>,  // individual translation items
                                 private val languages: List<String>, protected var defaultLanguage: String,  // default language of the translations
                                 protected var outputPathPrefix: Path,
                                 private var verbosePrintout: Boolean = false) {

    protected class ClientDoesNotExistException(clientName: String, path: String) : Exception("Client $clientName does not exist for path $path")

    abstract val platform: Platform  // the platform being written for

    protected var outputPath: Path? = null  // the path where the translations will be written to

    /**
     * Write the translations for all target languages
     */
    open fun writeAll(): Boolean {
        var successfulWrites = 0
        // write for all target languages
        for (language in languages) {
            var successfulWrite = true

            // setup the path based on the language
            outputPath = resolveOutputPath(language)

            // filter invalid items
            translationItems = translationItems.filter { isValidItem(it, language) }

            try {
                write(language, File(outputPath.toString()))
            } catch (e: ClientDoesNotExistException) {
                // path to the file does not exist, which means the client is missing
                e.printStackTrace()
                successfulWrite = false
            }

            if (successfulWrite) successfulWrites++
        }
        return successfulWrites == languages.size
    }

    /**
     * Checks whether the given item is valid for this writer
     * @param item translations item
     * @return validity (whether the file will be written)
     */
    private fun isValidItem(item: TranslationItem, language: String): Boolean {
        // "" keys are not valid, skip
        if (item.key.isBlank()) {
            return false
        }

        // if the item is not available for this platform, skip
        if (!item.platforms.contains(platform)) {
            return false
        }

        // there is no translation for this language, skip
        if (item.translations[language] == null) {
            if (verbosePrintout) System.err.println("There is no translation for item " + item.key + " in " + language)
            return false
        }

        // the translation for this language is not valid, skip
        if (item.translations[language] == "" || item.translations[language]!!.isEmpty()) {
            if (verbosePrintout) System.err.println("Translation for item " + item.key + " in " + language + " is empty and will be skipped.")
            return false
        }

        return true
    }

    /**
     * Resolve the final output path for a language
     */
    private fun resolveOutputPath(language: String): Path = outputPathPrefix.resolve(setupOutputPath(language))

    /**
     * A helper for getting all paths to write to
     * Used by the MainWriter to check if all paths exist and query the user if needed
     */
    fun getAllResolvedPaths(languages: List<String>) = languages.map { resolveOutputPath(it) }

    /**
     * Writes the translations file
     * @throws ClientDoesNotExistException
     */
    abstract fun write(language: String, file: File)

    /**
     * Sets up the path the translations will be written to,
     * based on the language, default language and client name
     * @return the outputPath
     */
    abstract fun setupOutputPath(language: String): Path
}
