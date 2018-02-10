package generator


import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.*
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by r.oblak on 9/8/2015.
 */
class AndroidWriter(clientName: String,
                    languageCodeResolver: LanguageCodeResolver,
                    translationItems: List<TranslationItem>,
                    languages: List<String>,
                    defaultLanguage: String,
                    outputPathPrefix: Path,
                    verbosePrintout: Boolean = false) : TranslationWriter(clientName, languageCodeResolver, translationItems, languages, defaultLanguage, outputPathPrefix, verbosePrintout) {

    override val platform = Platform.ANDROID

    override fun setupOutputPath(language: String): Path {
        val path = Paths.get(clientName)
                .resolve(Paths.get("src"))
                .resolve(Paths.get("main"))
                .resolve(Paths.get("res"))

        var valuesFolderName = "values"
        if (defaultLanguage != language) {
            valuesFolderName += "-" + languageCodeResolver.resolveLangCode(language)

        }
        return path.resolve(Paths.get(valuesFolderName))
                .resolve(Paths.get("strings.xml"))
    }

    override fun write(language: String, file: File) {
        try {
            if (!file.parentFile.exists()) {
                System.err.println("Folder " + file.parentFile.toString() + " does not exist. Will not generate.")
                return
            }

            val docFactory = DocumentBuilderFactory.newInstance()
            val docBuilder = docFactory.newDocumentBuilder()

            val doc = docBuilder.newDocument()
            doc.xmlStandalone = true // to remove the standalone="false" attribute
            val rootElement = doc.createElement("resources")
            doc.appendChild(rootElement)

            var currentSection = ""
            for (item in translationItems) {
                // check whether the item has a new section specified
                val section = item.section
                if (section.isNotEmpty() && section != currentSection) {
                    currentSection = section
                    // insert a new section element
                    val sectionComment = doc.createComment(currentSection)
                    rootElement.appendChild(sectionComment)
                }

                val translation = doc.createElement("string")
                translation.setAttribute("name", item.key)

                translation.appendChild(doc.createTextNode(item.translations[language]))
                rootElement.appendChild(translation)
            }

            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            transformer.setOutputProperty(OutputKeys.INDENT, "yes") // print with newlines

            val source = DOMSource(doc)
            val result = StreamResult(file)

            transformer.transform(source, result)

        } catch (pce: ParserConfigurationException) {
            pce.printStackTrace()
        } catch (tfe: TransformerException) {
            throw ClientDoesNotExistException(clientName, outputPath!!.toString())
        }
    }
}
