package generator

import org.junit.Assert
import org.junit.Test

/**
 * Created by r.oblak on 9/8/2015.
 */
class DemoAppTest {

    /**
     * Generate the files for a test app
     */
    @Test
    fun testGenerateForApp() {

        val settings = TranslationsGenerator.GeneratorSettings().apply {
            csvFolderName = "translations"
            projectPathStr = "TestApp"
            settingsFilename = "settings.json"
            csvFilename = "translations.csv"
            generateAllNonExistingPaths = true
        }

        val (writers, paths, _) = TranslationsGenerator.prepare(settings)

        Assert.assertEquals(3, writers.size)
        Assert.assertEquals(28, paths.size)

        TranslationsGenerator.validateAndGeneratePaths(paths, settings)
        TranslationsGenerator.writeAll(writers)
    }
}
