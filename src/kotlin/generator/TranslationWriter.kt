package generator

import java.io.File
import java.nio.file.Path

/**
 * Created by r.oblak on 9/8/2015.
 * A writer class for generating translations files for several languages
 */
abstract class TranslationWriter(protected var clientName: String,  // the client name
                                 protected val languageCodeResolver: LanguageCodeResolver,
                                 private val translationItems: List<TranslationItem>,  // individual translation items
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

            try {
                val translations = translationItems.mapNotNull { item ->
                    item.translations[language]?.let {
                        Translation(
                                key = item.key,
                                translation = it,
                                section = item.section
                        )
                    }?.takeIf { isValidItem(item, language) }
                }
                // write the items (filter invalid items)
                write(translations, File(outputPath.toString()))
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

    protected fun parseCDATAParts(translationStr: String): List<Pair<Boolean, String>> {
        // find any CDATA elements and get their ranges
        val cdataRanges = Regex("<!\\[CDATA\\[.*?]]>").findAll(translationStr)
                .asIterable().mapNotNull { it.groups.firstOrNull()?.range }

        if (cdataRanges.isEmpty()) {
            return listOf(false to translationStr)
        }

        val textsCdatas = mutableListOf<Pair<Boolean, String>>()
        for (i in 0 until cdataRanges.size) {
            val range = cdataRanges[i]
            val start = range.start
            val end = range.endInclusive

            if (i == 0 && start > 0) {
                // add the part before the first cdata
                val partBefore = translationStr.substring(0, start)
                textsCdatas.add(false to partBefore)
            }
            // add the cdata content
            val cdata = translationStr.substring(range)
            if (cdata.length > 12) {
                val cdataContent = cdata.substring(9, cdata.length - 3)
                textsCdatas.add(true to cdataContent)
            }

            if (i < cdataRanges.size - 1 && end + 1 < translationStr.length) {
                val nextRange = cdataRanges[i + 1]
                // add the part between this and the next cdata
                val partBetween = translationStr.substring(end + 1, nextRange.start)
                textsCdatas.add(false to partBetween)
            }

            if (i == cdataRanges.size - 1 && end + 1 < translationStr.length) {
                // add the part after the last cdata
                val partAfter = translationStr.substring(end + 1, translationStr.length)
                textsCdatas.add(false to partAfter)
            }
        }

        return textsCdatas
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
    abstract fun write(translations: List<Translation>, file: File)

    /**
     * Sets up the path the translations will be written to,
     * based on the language, default language and client name
     * @return the outputPath
     */
    abstract fun setupOutputPath(language: String): Path
}

/**
 * A single translation to be written
 */
data class Translation(val key: String, val translation: String, val section: String)