package gov.takx.platform.utils;

import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.platform.schema.connection.ConnectionDefinitionType;
import gov.takx.platform.schema.connection.ConnectionDefinitionsType;
import gov.takx.platform.schema.connection.ObjectFactory;
import gov.takx.platform.schema.connection.PropertiesType;
import gov.takx.platform.schema.connection.PropertyType;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.helpers.DefaultValidationEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Map;

/**
 * Connection Template Manager for reading connection templates and for writing them out. Uses the JAXB created
 * classes for the connection template XSD (in TAKX Designs).
 *
 */
public class ConnectionTemplateManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Load a connection template from disk and marshall to the JAXB {@link ConnectionDefinitionsType} object
     *
     * @param xmlConnectionDefinitionUrl URL of template file
     * @return the connections read from the template file
     * @throws IOException
     * @throws JAXBException
     */
    public static ConnectionDefinitionsType loadConnectionTemplate(@Nonnull URL xmlConnectionDefinitionUrl)
            throws IOException, JAXBException
    {
        InputStream is = xmlConnectionDefinitionUrl.openStream();
        JAXBContext context = JAXBContext.newInstance(ConnectionDefinitionsType.class.getPackage().getName());
        Unmarshaller unmarshaller = context.createUnmarshaller();

        // this implementation is a part of the API and convenient for trouble-shooting,
        // as it prints out errors to System.out
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());

        JAXBElement<ConnectionDefinitionsType> root =
                unmarshaller.unmarshal(new StreamSource(is), ConnectionDefinitionsType.class);

        return root.getValue();
    }

    /**
     * Create and save a list of connections to a template file.
     *
     * @param controllerMap map of controllers by connection name to the controller.
     * @param file          template file
     */
    public static void saveConnectionTemplate(Map<String, IConnectionPluginController> controllerMap, File file)
    {
        try
        {
            JAXBContext context = JAXBContext.newInstance(ConnectionDefinitionsType.class.getPackage().getName());
            Marshaller marshaller = context.createMarshaller();

            ObjectFactory factory = new ObjectFactory();
            ConnectionDefinitionsType definitionsType = factory.createConnectionDefinitionsType();

            for (Map.Entry<String, IConnectionPluginController> entry : controllerMap.entrySet())
            {
                String name = entry.getKey();
                IConnectionPluginController controller = entry.getValue();
                ConnectionDefinitionType definitionType = factory.createConnectionDefinitionType();
                definitionType.setFamily(controller.getFamily());
                definitionType.setType(controller.getType());
                definitionType.setName(name);
                definitionType.setAutoConnect(true);    // Always writing true for now...
                PropertiesType propertiesType = factory.createPropertiesType();
                for (Map.Entry<String, String> restorableEntry : controller.getRestorableParametersMap().entrySet())
                {
                    PropertyType type = factory.createPropertyType();
                    type.setName(restorableEntry.getKey());
                    type.setValue(restorableEntry.getValue());
                    propertiesType.getProperty().add(type);
                }
                definitionType.setProperties(propertiesType);
                definitionsType.getConnectionDefinition().add(definitionType);
            }

            marshaller.marshal(factory.createConnectionDefinitions(definitionsType), file);
        } catch (JAXBException e)
        {
            logger.error("Error converting connection objects into XML", e);
        }
    }
}
