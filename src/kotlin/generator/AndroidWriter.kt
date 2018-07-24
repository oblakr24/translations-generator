package generator


import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


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

    override fun write(translations: List<Translation>, file: File) {
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
            for (item in translations) {
                // check whether the item has a new section specified
                val section = item.section
                if (section.isNotEmpty() && section != currentSection) {
                    currentSection = section
                    // insert a new section element
                    val sectionComment = doc.createComment(currentSection)
                    rootElement.appendChild(sectionComment)
                }

                val translationStr = item.translation.parseApostrophes()

                val translation = doc.createElement("string")
                translation.setAttribute("name", item.key)

                // split the string into parts of normal text and CDATA parts
                parseCDATAParts(translationStr).forEach { (isCdata, text) ->
                    if (isCdata) {
                        translation.appendChild(doc.createCDATASection(text))
                    } else {
                        translation.appendChild(doc.createTextNode(text))
                    }
                }

                rootElement.appendChild(translation)
            }

            // add some info in the comment
            doc.insertBefore(doc.createComment("${file.parentFile.name}/${file.name}: ${translations.size} items"), doc.documentElement)

            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            transformer.setOutputProperty(OutputKeys.INDENT, "yes") // print with newlines

            val outputXmlStringWriter = StringWriter()

            val source = DOMSource(doc)
            val result = StreamResult(outputXmlStringWriter)

            transformer.transform(source, result)
            val outputXmlString = outputXmlStringWriter.toString().replaceFirst("<!--", "\n<!--").replaceFirst("-->", "-->\n")

            val outputXml = FileOutputStream(file)
            outputXml.write(outputXmlString.toByteArray(Charsets.UTF_8))


        } catch (pce: ParserConfigurationException) {
            pce.printStackTrace()
        } catch (tfe: TransformerException) {
            throw ClientDoesNotExistException(clientName, outputPath!!.toString())
        }
    }

    private fun String.parseApostrophes(): String {
        val sb = StringBuilder()
        var lastIdx = 0
        while (true) {

            val idx = indexOf('\'', lastIdx)

            if (idx <= 0) {
                sb.append(substring(lastIdx, length))
                break
            } else if (idx > lastIdx) {
                sb.append(substring(lastIdx, idx))
            } else {
            }


            if (idx > 0 && this[idx - 1] != '\\') {
                // Append a backslash char
                sb.append('\\')
            }

            sb.append('\'')

            lastIdx = idx + 1

        }

        return sb.toString()
    }
}
