package gov.takx.commons.xml.kml;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * {@link Kml} that uses a custom prefix mapper. The mapper effectively makes "kml" the assumed default namespace.
 * This is necessary for compatibility with ATAK/WinTAK which operate on KML element names as-is without considering
 * prefix/namespaces.
 *
 * @since 5.8
 */
public class DefaultNamespaceKml extends Kml
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final NamespacePrefixMapper PREFIX_MAPPER = new NamespacePrefixMapper()
    {
        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix)
        {
            return "kml".equals(suggestion) ? "" : suggestion;
        }
    };

    @Override
    protected void decorateMarshaller(JAXBContext context, Marshaller marshaller)
    {
        try
        {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", PREFIX_MAPPER);
        } catch (PropertyException e)
        {
            logger.error("Failed to update Kml namespace prefix mapper for routes", e);
        }
    }
}
