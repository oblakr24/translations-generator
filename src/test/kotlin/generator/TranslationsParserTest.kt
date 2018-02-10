package generator

import org.junit.Test


import org.junit.Assert.assertEquals

class TranslationsParserTest {

    /**
     * Test the main translation file parsing
     */
    @Test
    fun mainTranslationsFileTest() {
        val defaultData = TranslationParser.parse(javaClass.getResource("/translations.csv").file)

        // test the number of target languages
        assertEquals(6, defaultData.languages.size.toLong())

        // test the number of items
        assertEquals(14, defaultData.items.size)
    }

    /**
     * Test the client-specific parsing of translations
     */
    @Test
    fun clientTranslationsFileTest() {
        val clientData = TranslationParser.parse(javaClass.getResource("/translations_clientTwo.csv").file)

        val clientItems = clientData.items

        // test the number of target languages
        assertEquals(2, clientData.languages.size.toLong())

        // test the number of items
        assertEquals(2, clientItems.size.toLong())
    }

    /**
     * Test the client-specific overrides of translations
     */
    @Test
    fun clientOverrideTranslationsFileTest() {

        val defaultData = TranslationParser.parse(javaClass.getResource("/translations.csv").file)
        val clientData = TranslationParser.parse(javaClass.getResource("/translations_clientTwo.csv").file)

        assertEquals(2, clientData.items.size)
        assertEquals(14, defaultData.items.size)

        Utils.overrideTranslations(defaultData.items, clientData.items)

        assertEquals(15, defaultData.items.size)

        val firstItem = defaultData.items.first()
        assertEquals(7, firstItem.translations.size)
        assertEquals("CustomError", firstItem.translations["English"])
        assertEquals("CustomError_German", firstItem.translations["German"])

        val newItem = defaultData.items.last()
        assertEquals("key_new", newItem.key)
        assertEquals(2, newItem.translations.size)
    }
}
