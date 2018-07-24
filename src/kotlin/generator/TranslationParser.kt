package generator

import org.apache.commons.csv.CSVFormat

import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * The platforms the translations can be generated for
 */
enum class Platform {
    IOS, ANDROID
}

/**
 * Represents a single translation item
 */
class TranslationItem(
        /**
        * The item key
        */
        val key: String,
        /**
        * The platforms the item corresponds to
        */
        val platforms: List<Platform>,
        /**
        * The mapping of languages and translations
        */
        val translations: MutableMap<String, String>,
        /**
        * The section the item belongs to
        */
        val section: String) {

    /**
     * Debugging helper
     */
    override fun toString(): String = "$key (${translations.size} translations)"
}

/**
 * The parsed translations data
 */
class TranslationData(
        /**
         * The items - mutable in case they are being overridden at a later point
         */
        val items: MutableList<TranslationItem>,
        /**
         * The languages parsed (for logging only)
         */
        val languages: List<String>,
        /**
         * The translations keys parsed (testing only)
         */
        val keys: List<String>)

/**
 * The translations parser singleton
 */
object TranslationParser {

    /**
     * The static enumeration defining the .csv file header format
     */
    enum class HeaderKeys(val idx: Int) {
        Section(0),
        Key(1),
        Android(2),
        IOS(3),
        Notes(4)
    }

    /**
     * Parses the .csv file on csvFilePath and returns the translations data
     */
    fun parse(csvFilePath: String, debugMode: Boolean, writeEnglishIfMissing: Boolean = false) : TranslationData {
        val languages = LinkedList<String>()
        val keys = LinkedList<String>()
        val items = mutableListOf<TranslationItem>()

        try {
            val reader = InputStreamReader(FileInputStream(csvFilePath), "UTF-8")

            val records = CSVFormat.DEFAULT.withDelimiter(';')
                    .withEscape('\\').parse(reader).iterator()

            // the first line is the header
            val headerRecord = records.next()

            val headerIdxAndroid: Int
            val headerIdxIOS: Int
            val headerKeyAndroid = headerRecord.get(HeaderKeys.Android.idx)
            val headerKeyIOS = headerRecord.get(HeaderKeys.IOS.idx)
            // Swap if .csv header has a different order
            if (headerKeyAndroid.toLowerCase().contains("ios") || headerKeyIOS.toLowerCase().contains("android")) {
                headerIdxAndroid = HeaderKeys.IOS.idx
                headerIdxIOS = HeaderKeys.Android.idx
            } else {
                headerIdxAndroid = HeaderKeys.Android.idx
                headerIdxIOS = HeaderKeys.IOS.idx
            }

            // index of key and import language
            val languageIndices = HashMap<String, Int>()
            for (i in 0 until headerRecord.size()) {
                val headerValue = headerRecord.get(i)

                // the indices after "Notes" are targetLanguages
                if (i > HeaderKeys.values().map { it.idx }.sorted().last()) {
                    languages.add(headerValue)
                    // check if the language is supported
                    if (!Utils.languageCodes.containsKey(headerValue.toLowerCase())) {
                        Utils.printUnknownLanguageError(headerValue.toLowerCase())
                    }
                    languageIndices[headerValue] = i
                }
            }

            // parse all the .csv rows (records)
            var currentSection = ""
            while (records.hasNext()) {
                val record = records.next()

                // get the section in the current row, otherwise take the latest one
                val sectionName = record.get(HeaderKeys.Section.idx)
                if (sectionName != null && sectionName.isNotEmpty()) {
                    currentSection = sectionName
                }

                // make sure the column size equals header size (helps with debugging in case of a wrongly-formatted .csv file)
                if (record.size() != headerRecord.size()) {
                    throw IndexOutOfBoundsException("Number of columns in row " + record.recordNumber + " doesn't match header size. \n" + record.toString())
                }

                val key = record.get(HeaderKeys.Key.idx)
                if (key.isBlank()) continue  // "" keys are invalid

                // add the platforms where translations are available
                val platforms = mutableListOf<Platform>().apply {
                    if (record.get(headerIdxIOS) == "x") add(Platform.IOS)
                    if (record.get(headerIdxAndroid) == "x") add(Platform.ANDROID)
                }

                // add the translations
                val translations = LinkedHashMap<String, String>()
                for (lang in languages) {
                    if (!debugMode) {
                        val translationForLang = record.get(languageIndices[lang]!!)
                        if (writeEnglishIfMissing && translationForLang.isBlank()) {
                            translations[lang] = record.get(languageIndices[languages.first]!!)
                        } else {
                            translations[lang] = translationForLang
                        }
                    } else {
                        translations[lang] = key
                    }
                }

                // create and add the item
                items.add(TranslationItem(key, platforms, translations, currentSection))
                keys.add(key)
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return TranslationData(items, languages, keys)
    }
}
