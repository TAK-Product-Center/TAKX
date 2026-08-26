package gov.takx.commons.utils;

import gov.takx.api.annotation.SuppressLogging;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for SafeXmlDocumentUtils.
 *
 * @since 2.36
 */
public class SafeXmlDocumentUtilsTest
{
    private static final String BASE_XML = "<dataMessage>" +
            "  <receiveTime>1579662981938</receiveTime>" +
            "  <motion>null</motion>" +
            "  <isImported>false</isImported>" +
            "  <messageType>ARRAY_MESSAGE</messageType>" +
            "</dataMessage>";

    // Define and use xxe, which loads the system password file into a message field
    private static final String XXE_XML = "<!DOCTYPE dataMessage [" +
            "<!ELEMENT receiveTime (#PCDATA)>" +
            "<!ELEMENT motion (#PCDATA)>" +
            "<!ELEMENT isImported (#PCDATA)>" +
            "<!ENTITY   xxe   SYSTEM   \"file:///etc/passwd\">" +
            "]>" +
            System.lineSeparator() +
            "<dataMessage>" +
            "  <receiveTime>1579662981938</receiveTime>" +
            "  <motion>null</motion>" +
            "  <isImported>false</isImported>" +
            "  <messageType>ARRAY_MESSAGE</messageType>" +
            "  <field name=\"vulnerability\" type=\"string\">&xxe;</field>" +
            "</dataMessage>";

    /**
     * Expect this message to be parsed
     */
    @Test
    public void testBaseXMLParse() throws Exception
    {
        Document maybeDocument = SafeXmlDocumentUtils.parseXML(BASE_XML);
        assertThat(maybeDocument).isNotNull();
    }

    /**
     * We don't allow DOCTYPE or other forms of external entities.
     * Vul ID: V-70243	   	Rule ID: SV-84865r1_rule	   	STIG ID: APSC-DV-002420
     * Severity: CAT II
     */
    @SuppressLogging(LoggingSaxErrorHandler.class)
    @Test
    public void testDOCTYPEXMLParse()
    {
        assertThatThrownBy(() -> SafeXmlDocumentUtils.parseXML(XXE_XML))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("DOCTYPE");
    }
}
