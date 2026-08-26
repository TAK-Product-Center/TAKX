package gov.takx.commons.utils;

import jakarta.annotation.Nonnull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

/**
 * XML Document utilities for prevention of XML External Entity attacks.  See the
 * <a href="https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html">OWASP
 * XML External Entity Prevention Cheat Sheet</a> for more information.
 *
 * @since 5.1
 */
@SuppressWarnings("HttpUrlsUsage")
public class SafeXmlDocumentUtils
{
    // Settings to use for XML parser factories as specified in the OWASP cheat sheet
    public static final String SAX_FEATURES = "http://xml.org/sax/features/";
    public static final String FEATURES_EXTERNAL_GENERAL_ENTITIES = SAX_FEATURES + "external-general-entities";
    public static final String FEATURES_EXTERNAL_PARAMETER_ENTITIES = SAX_FEATURES + "external-parameter-entities";

    public static final String APACHE_FEATURES = "http://apache.org/xml/features/";
    public static final String FEATURES_DISALLOW_DOCTYPE_DECL = APACHE_FEATURES + "disallow-doctype-decl";
    public static final String FEATURES_LOAD_EXTERNAL_DTD = APACHE_FEATURES + "nonvalidating/load-external-dtd";

    // XSLT to format an XML document.  Sadly, just turning on "indent" in the standard output transformer
    // results in an extra blank line between every line in the file.  This approach is not perfect, as it does not
    // preserve the vertical whitespace in the original, but it doesn't add extra blank lines.
    private static final String PRETTY_PRINT_XSLT = """
            <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                <xsl:output indent="yes" cdata-section-elements="cdata-other-elements"/>
                <xsl:strip-space elements="*"/>
                <xsl:template match="@*|node()">
                    <xsl:copy>
                        <xsl:apply-templates select="@*|node()"/>
                    </xsl:copy>
                </xsl:template>
            </xsl:stylesheet>
            """;

    /**
     * @return a safe {@link XMLInputFactory}, disabling external entities and DTD support.
     */
    @Nonnull
    public static XMLInputFactory createXmlInputFactory()
    {
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return inputFactory;
    }

    /**
     * Create a safe {@link XMLStreamReader} from an XML source, disabling external entities and DTD support.
     *
     * @param source XML source
     * @return the safe XML stream reader
     * @throws XMLStreamException exception creating the XML stream reader
     */
    @Nonnull
    public static XMLStreamReader getXmlStreamReader(@Nonnull Source source) throws XMLStreamException
    {
        return createXmlInputFactory().createXMLStreamReader(source);
    }

    /**
     * Create a safe {@link XMLStreamReader} from a reader, disabling external entities and DTD support.
     *
     * @param reader stream reader
     * @return the safe XML stream reader
     * @throws XMLStreamException exception creating the XML stream reader
     */
    @Nonnull
    public static XMLStreamReader getXmlStreamReader(@Nonnull Reader reader) throws XMLStreamException
    {
        return createXmlInputFactory().createXMLStreamReader(reader);
    }

    /**
     * Get a safe {@link DocumentBuilderFactory} factory, disabling external entities and DTD support.
     *
     * @param allowDocType Pass {@code true} if DOCTYPE declarations should be allowed
     * @return A DocumentBuilderFactory with our standard configuration
     * @throws ParserConfigurationException Should not happen
     * @since 2.36
     */
    @Nonnull
    public static DocumentBuilderFactory getDocumentBuilderFactory(boolean allowDocType) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(FEATURES_DISALLOW_DOCTYPE_DECL, !allowDocType);
        factory.setFeature(FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
        factory.setFeature(FEATURES_LOAD_EXTERNAL_DTD, false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    /**
     * Create a safe {@link DocumentBuilder} by disabling external entities, DOCTYPE, and DTD support.
     *
     * @return the safe Document builder
     * @see #getDocumentBuilderFactory(boolean)
     * @throws ParserConfigurationException exception creating the safe document builder
     */
    @Nonnull
    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException
    {
        return getDocumentBuilder(false);
    }

    /**
     * Create a safe {@link DocumentBuilder} by disabling external entities and optionally DOCTYPE support.
     * <p/>
     * Note that the returned reader has been configured to use a special error handler to prevent the default
     * behavior of writing directly to stderr.  Instead, the messages are logged using {@link LoggingSaxErrorHandler}.
     * This ensures that the messages are properly recorded in the log.
     *
     * @param allowDocType Pass {@code true} if DOCTYPE declarations should be allowed
     * @return the safe Document builder
     * @see #getDocumentBuilderFactory(boolean)
     * @see LoggingSaxErrorHandler
     * @throws ParserConfigurationException exception creating the safe document builder
     */
    @Nonnull
    public static DocumentBuilder getDocumentBuilder(boolean allowDocType) throws ParserConfigurationException
    {
        final DocumentBuilderFactory factory = getDocumentBuilderFactory(allowDocType);
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        documentBuilder.setErrorHandler(new LoggingSaxErrorHandler());
        return documentBuilder;
    }

    /**
     * Create a safe {@link SAXParserFactory} by disabling external entities, DOCTYPE, and DTD support.
     * 
     * @param namespaceAware Pass {@code true} to make the factory namespace aware
     * @return SAX parser factory
     * @since 5.2
     * @see SAXParserFactory#setNamespaceAware(boolean)
     * @throws ParserConfigurationException exception configuring the parser factory
     * @throws SAXException                 exception creating the SAX parser factory
     */
    @Nonnull
    public static SAXParserFactory getSaxParserFactory(boolean namespaceAware) throws ParserConfigurationException, SAXException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature(FEATURES_DISALLOW_DOCTYPE_DECL, true);
        factory.setFeature(FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
        factory.setFeature(FEATURES_LOAD_EXTERNAL_DTD, false);
        factory.setNamespaceAware(namespaceAware);
        return factory;
    }

    /**
     * Create a safe {@link SAXParser} by disabling external entities, DOCTYPE, and DTD support.  The returned parser
     * is not namespace aware.
     *
     * @return SAX parser
     * @see #getSaxParser(boolean)
     * @throws ParserConfigurationException exception configuring the parser factory
     * @throws SAXException                 exception creating the safe SAX parser
     */
    @Nonnull
    public static SAXParser getSaxParser() throws SAXException, ParserConfigurationException
    {
        return getSaxParser(false);
    }

    /**
     * Create a safe {@link SAXParser} by disabling external entities, DOCTYPE, and DTD support.
     *
     * @return SAX parser
     * @param namespaceAware Pass {@code true} to make the parser namespace aware
     * @see #getSaxParserFactory(boolean)
     * @since 5.2
     * @throws ParserConfigurationException exception configuring the parser factory
     * @throws SAXException                 exception creating the safe SAX parser
     */
    @Nonnull
    public static SAXParser getSaxParser(boolean namespaceAware) throws SAXException, ParserConfigurationException
    {
        return getSaxParserFactory(namespaceAware).newSAXParser();
    }

    /**
     * Create a safe {@link XMLReader} from a safe SAX parser.  The returned reader is <em>NOT</em> namespace aware.
     * <p/>
     * Note that the returned reader has been configured to use a special error handler to prevent the default
     * behavior of writing directly to stderr.  Instead, the messages are logged using {@link LoggingSaxErrorHandler}.
     * This ensures that the messages are properly recorded in the log.
     *
     * @return XML reader
     * @throws ParserConfigurationException exception configuring the parser factory
     * @throws SAXException                 exception creating the safe SAX parser
     * @see #getSaxParser()
     * @see LoggingSaxErrorHandler
     */
    @Nonnull
    public static XMLReader getSaxReader() throws ParserConfigurationException, SAXException
    {
        return getSaxReader(false);
    }

    /**
     * Create a safe {@link XMLReader} from a safe SAX parser.
     * <p/>
     * Note that the returned reader has been configured to use a special error handler to prevent the default
     * behavior of writing directly to stderr.  Instead, the messages are logged using {@link LoggingSaxErrorHandler}.
     * This ensures that the messages are properly recorded in the log.
     *
     * @param namespaceAware Pass {code true} make the reader namespace aware, {@code false} to ignore namespaces
     * @return XML reader
     * @throws ParserConfigurationException exception configuring the parser factory
     * @throws SAXException                 exception creating the safe SAX parser
     * @see #getSaxParser(boolean)
     * @see LoggingSaxErrorHandler
     * @since 5.5
     */
    @Nonnull
    public static XMLReader getSaxReader(boolean namespaceAware) throws ParserConfigurationException, SAXException
    {
        final XMLReader xmlReader = getSaxParser(namespaceAware).getXMLReader();
        xmlReader.setErrorHandler(new LoggingSaxErrorHandler());
        return xmlReader;
    }

    /**
     * Create a {@link TransformerFactory} safe from XXE.
     *
     * @return a safe Transformer
     * @throws TransformerConfigurationException exception creating the safe Transformer
     */
    @Nonnull
    public static TransformerFactory getTransformerFactory() throws TransformerConfigurationException
    {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
    }

    /**
     * Create a {@link Transformer} safe from XXE.
     *
     * @return a safe Transformer
     * @see #getTransformerFactory()
     * @throws TransformerConfigurationException exception creating the safe Transformer
     */
    @Nonnull
    public static Transformer getTransformer() throws TransformerConfigurationException
    {
        return getTransformerFactory().newTransformer();
    }

    /**
     * Safe unmarshal from Source and class type to an object of the specified class type.
     * <p/>
     * Since a {@link Unmarshaller} parses XML and does not support any flags for disabling XXE, this method calls
     * {@link #getXmlStreamReader(Reader)} to safely read the XML input.
     *
     * @param source source to unmarshall from
     * @param type   unmarshall to class type
     * @param <T>    inferred type
     * @return the reified object
     * @throws XMLStreamException error creating the XML Stream reader
     * @throws JAXBException      error unmarshalling the source
     */
    @Nonnull
    public static <T> T unmarshal(@Nonnull Source source, @Nonnull Class<T> type) throws XMLStreamException, JAXBException
    {
        return JAXBContext.newInstance(type).createUnmarshaller()
                .unmarshal(getXmlStreamReader(source), type)
                .getValue();
    }

    /**
     * Safe unmarshal from Reader and class type to an object of the specified class type.  This is the preferred
     * method when an XML String needs to be unmarshalled into an object.
     * <p/>
     * Since a {@link Unmarshaller} parses XML and does not support any flags for disabling XXE, this method calls
     * {@link #getXmlStreamReader(Source)} to safely read the XML input.
     *
     * @param reader reader to unmarshall from
     * @param type   unmarshall to class type
     * @param <T>    inferred type
     * @return the reified object
     * @throws XMLStreamException error creating the XML Stream reader
     * @throws JAXBException      error unmarshalling the source
     */
    @Nonnull
    public static <T> T unmarshal(@Nonnull Reader reader, @Nonnull Class<T> type) throws XMLStreamException, JAXBException
    {
        return JAXBContext.newInstance(type).createUnmarshaller()
                .unmarshal(getXmlStreamReader(reader), type)
                .getValue();
    }

    /**
     * Parse XML into a document while minimizing vulnerabilities using a safe document builder.
     *
     * @param xml An XML formatted string
     * @return A new DOM Document object
     * @see #getDocumentBuilder()
     */
    @Nonnull
    public static Document parseXML(@Nonnull String xml) throws ParserConfigurationException, SAXException, IOException
    {
        return getDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Process a given XML document and produce formatted XML output.
     *
     * @param document Document to process
     * @return Formatted XML
     * @throws IOException If the transformation process fails
     */
    @Nonnull
    public static String formatXml(@Nonnull Document document) throws IOException
    {
        return formatXml(new DOMSource(document.getDocumentElement()), Collections.emptyMap());
    }

    /**
     * Process a given XML document and produce formatted XML output, using the given output properties.
     *
     * @param document Document to process
     * @param outputProperties Map of output properties to set on the transformer before applying it
     * @return Formatted XML
     * @see Transformer#setOutputProperty(String, String)
     * @throws IOException If the transformation process fails
     */
    @Nonnull
    public static String formatXml(@Nonnull Document document, @Nonnull Map<String, String> outputProperties) throws IOException
    {
        return formatXml(new DOMSource(document.getDocumentElement()), outputProperties);
    }

    /**
     * Process a given XML input source and produce formatted XML output.
     *
     * @param sourceXml Source to process
     * @return Formatted XML
     * @throws IOException If the transformation process fails
     */
    @Nonnull
    public static String formatXml(@Nonnull InputSource sourceXml) throws IOException
    {
        return formatXml(sourceXml, Collections.emptyMap());
    }

    /**
     * Process a given XML input source and produce formatted XML output, using the given output properties.
     *
     * @param sourceXml Source to process
     * @return Formatted XML
     * @throws IOException If the transformation process fails
     */
    @Nonnull
    public static String formatXml(@Nonnull InputSource sourceXml, @Nonnull Map<String, String> outputProperties) throws IOException
    {
        try
        {
            return formatXml(new DOMSource(getDocumentBuilder().parse(sourceXml)), outputProperties);
        } catch (ParserConfigurationException | SAXException | IOException e)
        {
            throw new IOException("Error formatting XML document", e);
        }
    }

    /**
     * Process a given XML source and produce formatted XML output.
     *
     * @param source Source to process
     * @return Formatted XML
     * @throws IOException If the transformation process fails
     */
    @Nonnull
    public static String formatXml(@Nonnull Source source) throws IOException
    {
        return formatXml(source, Collections.emptyMap());
    }

    /**
     * Process a given XML source and produce formatted XML output, using the given output properties.
     *
     * @param source Source to process
     * @return Formatted XML
     * @throws IOException If the transformation process fails
     */
    @Nonnull
    public static String formatXml(@Nonnull Source source, @Nonnull Map<String, String> outputProperties) throws IOException
    {
        try (StringWriter stringWriter = new StringWriter())
        {
            final Transformer transformer = getTransformerFactory().newTransformer(new StreamSource(new StringReader(PRETTY_PRINT_XSLT)));

            // add extra standalone to break the root node to a new line
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

            outputProperties.forEach(transformer::setOutputProperty);

            transformer.transform(source, new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (TransformerException e)
        {
            throw new IOException("Error formatting XML document", e);
        }
    }
}
