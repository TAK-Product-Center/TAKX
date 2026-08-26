package gov.tak.platform.datapackage.content;

import jakarta.annotation.Nonnull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

/**
 * Holds the manifest for a data package.
 *
 * @since 3.0
 */
@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "unused"})
@XmlRootElement(name = "MissionPackageManifest")
@XmlAccessorType(XmlAccessType.FIELD)
public class PackageManifest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @XmlAttribute
    private int version = 2;

    @XmlElement(name = "Configuration", required = true)
    private PackageConfiguration configuration;

    @XmlElement(name = "Contents", required = true)
    private PackageManifestContents contents;

    public int getVersion()
    {
        return version;
    }

    public PackageConfiguration getConfiguration()
    {
        return configuration;
    }

    public PackageManifestContents getContents()
    {
        return contents;
    }

    /**
     * @param configuration New configuration instance for the manifest.
     *
     * @since 3.5
     */
    public void setConfiguration(@Nonnull PackageConfiguration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * @param contents New contents instance for the manifest.
     *
     * @since 3.5
     */
    public void setContents(@Nonnull PackageManifestContents contents)
    {
        this.contents = contents;
    }

    /**
     * @return The XML string form of this manifest, empty string if marshalling fails
     */
    @Nonnull
    public String toXml()
    {
        try
        {
            final JAXBContext jaxbContext = JAXBContext.newInstance(PackageManifest.class);
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            final StringWriter sw = new StringWriter();
            marshaller.marshal(this, sw);

            return sw.toString();
        } catch (JAXBException e)
        {
            logger.error("Failed to marshal PackageManifest", e);
            return "";
        }
    }

    @Override
    public String toString()
    {
        return toXml();
    }
}
