package generator

import java.util.*

/**
 * Created by r.oblak on 9/8/2015.
 */

/**
 * Helper for resolving language codes
 */
class LanguageCodeResolver(orgOverriddenMapping: Map<String, String>? = null) {

    private class UnrecognizedLanguageException(message: String): RuntimeException(message)

    private val overriddenMapping = orgOverriddenMapping?.mapKeys { (key, _) -> key.toLowerCase() }

    fun resolveLangCode(language: String): String {
        val lang = language.toLowerCase()
        if (!Utils.languageCodes.containsKey(lang)) {
            Utils.printUnknownLanguageError(lang)
        }
        return overriddenMapping?.get(lang) ?: Utils.languageCodes[lang] ?: throw UnrecognizedLanguageException("Language '$language' not among standard ISO English languages and not in provided language mapping in the settings file.")
    }
}

/**
 * Utilities
 */
object Utils {
    /**
     * The default language -> language code mapping
     */
    val languageCodes: Map<String, String> by lazy {
        Locale.getISOLanguages().map { Locale(it).displayLanguage.toLowerCase() to it }.toMap()
    }

    fun printUnknownLanguageError(language: String) {
        System.err.println("Language $language not among standard ISO English languages.")
        System.err.println("Possible candidates:")
        val langChars = language.toCharArray()
        languageCodes.keys.filter { it.first() == language.first() }.sortedByDescending { it.toCharArray().count { langChars.contains(it) } }.forEach {
            System.err.println(it)
        }
    }

    /**
     * overrides default translations items with client-specific translations items
     * @param defaultItems default translations items - mutable so that new items can be added
     * @param clientItems client-specific translations items
     * @return array containing the number of overridden items and the number of added items
     */
    fun overrideTranslations(defaultItems: MutableList<TranslationItem>, clientItems: List<TranslationItem>): Pair<Int, Int> {
        var overriddenItems = 0
        val newItems = ArrayList<TranslationItem>()
        // add overriding translations to defaultItems
        for (clientItem in clientItems) {
            // check if this item already exists in defaultItems
            var existsInDefaultItems = false
            for (defaultItem in defaultItems) {
                if (defaultItem.key == clientItem.key && defaultItem.platforms.size == clientItem.platforms.size) {
                    // check whether the platforms match
                    val samePlatforms = defaultItem.platforms.all { clientItem.platforms.contains(it) }
                    if (samePlatforms) {
                        // item already exists, override it
                        val defaultTranslations = defaultItem.translations
                        val clientTranslations = clientItem.translations

                        for (lang in clientTranslations.keys) {
                            defaultTranslations[lang] = clientTranslations[lang]!!
                        }

                        existsInDefaultItems = true
                        overriddenItems++
                        break
                    }
                }
            }

            // this is a new item, add it to default items
            if (!existsInDefaultItems) {
                newItems.add(clientItem)
                defaultItems.add(clientItem)
            }
        }
        return overriddenItems to newItems.size
    }
}
