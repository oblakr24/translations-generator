package generator

import com.google.gson.Gson
import org.apache.commons.io.FileUtils

import java.io.File

/**
 * Defines the settings for the translations generation
 */
class TranslationsSettings(
        /**
         * The list of targets (clients) the translations will be generated for
         */
        val targets: List<TargetSetting>,
        /**
         * An optional custom mapping of languages (used in the .csv header) to language codes (used in translation paths)
         */
        val languageCodeMapping: Map<String, String>?,
        /**
         * The case styles for generated iOS key properties
         * If null, leave keys as they are
         */
        val iosKeyCaseType: IOSKeyCaseType? = null,
        /**
         * If true, any items with missing translations in other languages
         * will take the English translation by detault
         */
        val writeEnglishIfMissing: Boolean = false
)

/**
 * Defines the settings for translations generation for a single target (client)
 */
data class TargetSetting(
        /**
         * The client's name (logging only)
         */
        val clientName: String,
        /**
         * The list of languages to generate for
         */
        var targetLanguages: List<String>,
        /**
         * Generate for iOS
         */
        val doIOS: Boolean,
        /**
         * Generate for Android
         */
        val doAndroid: Boolean,
        /**
         * Default language (for which the generated folder on Android is without the language code)
         */
        var defaultLanguage: String,
        /**
         * The optional client's custom .csv
         */
        var clientCSVFilename: String?,
        /**
         * The relative path to the client's Android module folder
         */
        var relativePathAndroid: String?,
        /**
         * The relative path to the client's iOS module folder
         */
        var relativePathIOS: String?
)

enum class IOSKeyCaseType {
    Pascal, // PascalCase
    Camel, // camelCase
    Snake // snake_case
}

/**
 * Settings parser singleton
 */
object SettingsParser {

    // the Gson instance
    private val gson by lazy { Gson() }

    /**
     * Parses the settings.json file
     */
    fun parse(settingsFile: File) : TranslationsSettings {
        return gson.fromJson(FileUtils.readFileToString(settingsFile), TranslationsSettings::class.java)
    }
}
