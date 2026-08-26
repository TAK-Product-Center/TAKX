package gov.takx.core.plugin.synthetic;

import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.device.ADevicePlugin;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.plugin.gateway.IGatewayControllerDelegate;
import gov.takx.api.plugin.gateway.IGatewayFramer;
import gov.takx.api.plugin.gateway.IGatewayFramerDelegate;
import gov.takx.api.plugin.gateway.IGatewayParser;
import gov.takx.api.plugin.gateway.IGatewayParserDelegate;
import gov.takx.api.plugin.synthetic.ISyntheticPlugin;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.core.plugin.DefaultDevicePluginController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;

/**
 * Implementation of {@link ISyntheticPlugin}.
 * This is specifically to support "core" defined types, like Map Tools and File.
 *
 * @since 3.0
 */
public class SyntheticPlugin extends ADevicePlugin implements ISyntheticPlugin
{
    private static final String DEFAULT_VERSION = "3.0.0";

    private final Function<String, IDevicePluginController> controllerGenerator;

    /**
     * Construct using a default version and default device plugin controller generator.
     *
     * @param family Plugin family
     * @param type   Plugin type
     * @see DefaultDevicePluginController
     */
    public SyntheticPlugin(@Nonnull String family, @Nonnull String type)
    {
        this(family, type, DEFAULT_VERSION, null);
    }

    /**
     * Construct using the given plugin attributes.
     *
     * @param family              Plugin family
     * @param type                Plugin type
     * @param version             Plugin version
     * @param controllerGenerator Function to generate device plugin controller instances from a unit Id,
     *                            if {@code null} {@link DefaultDevicePluginController} instances are generated
     * @see DefaultDevicePluginController
     */
    public SyntheticPlugin(@Nonnull String family, @Nonnull String type, @Nonnull String version,
                           @Nullable Function<String, IDevicePluginController> controllerGenerator)
    {
        super(new PluginSpecification.Literal(family, type, version), new PluginDescriptor.Literal(type));

        this.controllerGenerator = controllerGenerator != null
                ? controllerGenerator
                : unitId -> new DefaultDevicePluginController(family, type, unitId);
    }

    @Override
    protected IDevicePluginController makeDeviceController(String unitId)
    {
        return controllerGenerator.apply(unitId);
    }

    @Override
    public IGatewayController createGatewayController(String unitID, IGatewayControllerDelegate delegate)
    {
        return null;
    }

    @Override
    public IDataStructureFactory getDataStructureFactory()
    {
        return null;
    }

    @Override
    protected IDataStructureFactory generateDataStructureFactory()
    {
        return null;
    }

    @Override
    public IGatewayParser createGatewayParser(CommServiceType sourceType, String sourceName, String format, IGatewayParserDelegate delegate)
    {
        return null;
    }

    @Override
    public IGatewayFramer createGatewayFramer(CommServiceType sourceType, String sourceName, String format, IGatewayFramerDelegate delegate)
    {
        return null;
    }
}
