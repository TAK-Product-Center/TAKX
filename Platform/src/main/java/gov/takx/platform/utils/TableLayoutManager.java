package gov.takx.platform.utils;

import gov.takx.platform.schema.layout.LayoutsDefinitionsCollection;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;

/**
 * Provides a collection of methods for defining and loading JXTable layout definitions.
 *
 * @since 1.11
 */
public final class TableLayoutManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private TableLayoutManager()
    {
    }

    /**
     * Loads a layout definitions file from a given URL.
     *
     * @param xmlLayoutDefinitionUrl URL to a valid layout definition XML document
     * @return Collection of layout definitions
     * @throws IOException   if the xmlLayoutDefinitionUrl URL cannot be accessed
     * @throws JAXBException if the XML document pointed to by xmlLayoutDefinitionUrl is invalid and
     *                       cannot be bound to Java objects
     */
    public static LayoutsDefinitionsCollection loadLayoutDefinitions(@Nonnull URL xmlLayoutDefinitionUrl)
            throws IOException, JAXBException
    {
        InputStream is = xmlLayoutDefinitionUrl.openStream();
        JAXBContext context = JAXBContext.newInstance(LayoutsDefinitionsCollection.class.getPackage().getName());
        Unmarshaller unmarshaller = context.createUnmarshaller();

        JAXBElement<LayoutsDefinitionsCollection> root =
                unmarshaller.unmarshal(new StreamSource(is), LayoutsDefinitionsCollection.class);

        return root.getValue();
    }

    public static void saveLayoutDefinitions(LayoutsDefinitionsCollection collection, File file)
            throws FileNotFoundException, JAXBException
    {
        JAXBContext context = JAXBContext.newInstance(LayoutsDefinitionsCollection.class.getPackage().getName());
        Marshaller marshaller = context.createMarshaller();
        marshaller.marshal(collection, new FileOutputStream(file));
    }
}
