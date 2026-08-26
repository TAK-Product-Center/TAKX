package gov.takx.platform.utils;

import gov.takx.platform.schema.connection.ConnectionDefinitionType;
import gov.takx.platform.schema.connection.ConnectionDefinitionsType;
import gov.takx.platform.schema.connection.ObjectFactory;
import gov.takx.platform.schema.connection.PropertiesType;
import gov.takx.platform.schema.connection.PropertyType;
import gov.takx.test.AContainerTest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 */
public class ConnectionTemplateManagerTest extends AContainerTest
{
    @Test
    public void testConnectionSchema() throws IOException, JAXBException
    {
        URL url = getClass().getClassLoader().getResource("schemas/connectionTemplate.xml");
        ConnectionDefinitionsType connectionDefinitionsType = ConnectionTemplateManager.loadConnectionTemplate(url);
        List<ConnectionDefinitionType> connectionTypes = connectionDefinitionsType.getConnectionDefinition();

        assertThat(connectionTypes).withFailMessage("Correct number of Connections from XML").hasSize(2);
        ConnectionDefinitionType type = connectionTypes.get(0);
        assertThat(type.getFamily()).withFailMessage("Correct CoT family name").isEqualTo("gov.takx.CursorOnTarget");
        assertThat(type.getProperties().getProperty()).withFailMessage("Correct number of properties for CoT connection").hasSize(3);
        type = connectionTypes.get(1);
        assertThat(type.getFamily()).withFailMessage("Correct Acme family name").isEqualTo("gov.takx.acme");
        assertThat(type.getProperties().getProperty()).withFailMessage("Correct number of properties for Acme connection").hasSize(2);
    }

    @Test
    public void testSchemaMarshalling() throws JAXBException, IOException
    {
        JAXBContext context = JAXBContext.newInstance(ConnectionDefinitionsType.class.getPackage().getName());
        Marshaller marshaller = context.createMarshaller();

        ObjectFactory factory = new ObjectFactory();
        ConnectionDefinitionsType definitionsType = factory.createConnectionDefinitionsType();

        ConnectionDefinitionType definitionType = factory.createConnectionDefinitionType();
        definitionType.setFamily("TestFamily");
        definitionType.setType("TestType");
        definitionType.setName("TestName");
        definitionType.setAutoConnect(true);    // Always writing true for now...
        PropertiesType propertiesType = factory.createPropertiesType();

        Map<String, String> configMap = new HashMap<>();
        configMap.put("IPAddress", "127.0.0.1");
        configMap.put("Port", "12345");

        for (Map.Entry<String, String> restorableEntry : configMap.entrySet())
        {
            PropertyType type = factory.createPropertyType();
            type.setName(restorableEntry.getKey());
            type.setValue(restorableEntry.getValue());
            propertiesType.getProperty().add(type);
        }
        definitionType.setProperties(propertiesType);
        definitionsType.getConnectionDefinition().add(definitionType);

        File tempFile = File.createTempFile("connectionTemplate-", ".xml");

        marshaller.marshal(factory.createConnectionDefinitions(definitionsType), tempFile);

        // And now read back in the file we wrote

        ConnectionDefinitionsType connectionDefinitionsType = ConnectionTemplateManager.loadConnectionTemplate(tempFile.toURI().toURL());

        //noinspection ResultOfMethodCallIgnored
        tempFile.delete();

        List<ConnectionDefinitionType> connectionTypes = connectionDefinitionsType.getConnectionDefinition();

        assertThat(connectionTypes).withFailMessage("Correct number of Connections from XML").hasSize(1);
        ConnectionDefinitionType type = connectionTypes.get(0);
        assertThat(type.getFamily()).withFailMessage("Correct family name").isEqualTo("TestFamily");
        assertThat(type.getType()).withFailMessage("Correct type name").isEqualTo("TestType");
        assertThat(type.getName()).withFailMessage("Correct type name").isEqualTo("TestName");
        assertThat(type.isAutoConnect()).withFailMessage("correct auto-connect setting").isTrue();

        final List<PropertyType> properties = type.getProperties().getProperty();
        assertThat(properties).withFailMessage("Correct number of properties").hasSize(2);

        for (PropertyType property : properties)
        {
            switch (property.getName())
            {
                case "IPAddress" -> assertThat(property.getValue()).isEqualTo("127.0.0.1");
                case "Port" -> assertThat(property.getValue()).isEqualTo("12345");
                default -> fail("Unknown property: " + property.getName());
            }
        }
    }
}
