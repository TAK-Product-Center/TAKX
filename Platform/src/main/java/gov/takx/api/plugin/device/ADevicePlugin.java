package gov.takx.api.plugin.device;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.ARaptorPlugin;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.gateway.GenericGatewayController;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.plugin.gateway.IGatewayControllerDelegate;
import gov.takx.commons.utils.StringUtils;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation for Device plugins.  Developers of plugins are encouraged to extend this
 * base class instead of implementing all the required methods directly.
 *
 * @since 2.0.0
 */
public abstract class ADevicePlugin extends ARaptorPlugin<DevicePlugin> implements IDevicePlugin
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Map that manages the plugin's set of IDevicePluginController instances.
     */
    protected Map<String, IDevicePluginController> controllerList = new ConcurrentHashMap<>();

    protected IDataStructureFactory dataStructureFactory = null;

    /**
     * Default constructor when the sub-class needs to extract data to determine the family, type, and version.
     */
    protected ADevicePlugin()
    {
        super(DevicePlugin.class);
    }

    /**
     * Special constructor for synthetic plugins, which don't have a defining annotation.  They are defined
     * dynamically, and the spec and descriptor are generated from that dynamic data.
     *
     * @param pluginSpecification Specification defining the plugin
     * @param pluginDescriptor    Descriptor defining the plugin
     */
    @Internal
    protected ADevicePlugin(PluginSpecification pluginSpecification, PluginDescriptor pluginDescriptor)
    {
        super(pluginSpecification, pluginDescriptor);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation manages a list of controller instances in {@link #controllerList}. If a controller for
     * the given unit Id is found, it is returned. If not, then the {@link #makeDeviceController(String)} method
     * is called to construct the new controller instance and it is added to the list before being returned to the
     * caller.
     *
     * @since 1.44
     */
    @Override
    public IDevicePluginController getDeviceController(String unitId)
    {
        return controllerList.computeIfAbsent(unitId, k -> makeDeviceController(unitId));
    }

    /**
     * Construct a new plugin device controller instance for the given unit Id. Override this method to construct
     * the specific type of controller for your plugin. This method is called from {@link #getDeviceController(String)}
     * when an existing controller is not found in the current list of plugin controllers.
     *
     * @param unitID Unit ID of device
     * @return controller instance.
     * @since 1.44
     */
    protected abstract IDevicePluginController makeDeviceController(String unitID);

    /**
     * {@inheritDoc}
     * <p/>
     * This base implementation just removes the entry from the {@link #controllerList}.  If any additional processing
     * is required, the plugin should override this method to perform that logic and then delegate to this method
     * to perform the standard cleanup.
     */
    @Override
    public void shutdownDevice(String unitID)
    {
        controllerList.remove(unitID);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Default implementation, uses the annotation properties {@link DevicePlugin#dataStructureXml()} and
     * {@link DevicePlugin#commandStructureXml()} to build the complete structure factory.
     */
    @Override
    public IDataStructureFactory getDataStructureFactory()
    {
        return dataStructureFactory == null
                ? dataStructureFactory = generateDataStructureFactory()
                : dataStructureFactory;
    }

    /**
     * Generate a data structure factory for this plugin using the configured XML files.
     *
     * @return configured data structure factory
     */
    protected IDataStructureFactory generateDataStructureFactory()
    {
        // Load the XML declarations and get them processed.

        final String dataStructureXml = getPluginAnnotation().dataStructureXml().trim();
        final Map<String, IRaptorDataStructure> dataStructureMap = processStructureXML(dataStructureXml);
        final List<String> excludeFromDeviceLog = Arrays.asList(getPluginAnnotation().excludeFromDeviceLog());

        // The command structure definitions are optional
        final String commandStructureXml = getPluginAnnotation().commandStructureXml().trim();
        final Map<String, IRaptorDataStructure> commandStructureMap = processStructureXML(commandStructureXml);

        // And generate the data structure factory from those definitions

        return new ADataStructureFactory()
        {
            @Override
            protected void createStructureMap()
            {
                // All the RDM structures
                dataStructureMap.values().stream()
                        .peek(s -> {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("{}: Define RDM struct: {}", getUniqueKey(), s.getMessageType());
                            }
                        })
                        .forEach(structure -> {
                            String messageType = structure.getMessageType();
                            boolean include = !excludeFromDeviceLog.contains(messageType);
                            defineDataStructure(messageType, structure, true, true, include);
                        });

                // All the RDM structures
                commandStructureMap.values().stream()
                        .peek(s -> {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("{}: Define RCM struct: {}", getUniqueKey(), s.getMessageType());
                            }
                        })
                        .forEach(structure -> defineDataStructure(structure.getMessageType(), structure, false));
            }

            @Override
            public IRaptorDataStructure getDefaultDeviceLogDataStructure()
            {
                return dataStructureMap.get(getPluginAnnotation().defaultDeviceLogMessage());
            }
        };
    }

    /**
     * Obtain the given XML resource from the plugin's class loader and feed it to
     * {@link DataStructureBuilder#buildFromXML(String, String, URL)} to produce the collection of structures.
     *
     * @param xmlPath Path to XML resource to process
     * @return Structure map, may be empty
     */
    protected Map<String, IRaptorDataStructure> processStructureXML(String xmlPath)
    {
        final ClassLoader classLoader = getClass().getClassLoader(); // Loader for this plugin
        Map<String, IRaptorDataStructure> structureMap = new HashMap<>();

        if (!StringUtils.isEmpty(xmlPath))
        {
            final URL xmlUrl = classLoader.getResource(xmlPath);

            if (xmlUrl == null)
            {
                logger.error("Device plugin {} failed to locate structure XML resource '{}'", getUniqueKey(), xmlPath);
            } else
            {
                try
                {
                    structureMap = DataStructureBuilder.buildFromXML(getFamily(), getType(), xmlUrl);
                } catch (IOException | JAXBException e)
                {
                    logger.error("Device plugin {} failed to process structure XML resource '{}'", getUniqueKey(), xmlPath, e);
                }
            }
        }

        return structureMap;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Defaults to creating a {@link GenericGatewayController}.
     */
    @Override
    public IGatewayController createGatewayController(String unitID, IGatewayControllerDelegate delegate)
    {
        return new GenericGatewayController(getFamily(), getType(), unitID, delegate);
    }

    @Override
    public boolean isShareable()
    {
        return false;
    }

    @Override
    public void shutdownPlugin()
    {
    }

    @Override
    public void deleteDevice(String unitID)
    {
    }
}
