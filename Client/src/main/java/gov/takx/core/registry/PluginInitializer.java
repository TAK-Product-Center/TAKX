package gov.takx.core.registry;

import gov.takx.api.plugin.IGlobalResourceManager;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.ITemplateResourceManager;
import gov.takx.core.platform.AVFSFileInstaller;
import gov.takx.core.platform.VFSGlobalFileInstaller;
import gov.takx.core.platform.VFSTemplateInstaller;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for handling the initialization of a plugin instance.  It handles processing any
 * template/VFS/resource managers.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class PluginInitializer
{
    /**
     * Initialize the given plugin.  Handle file/template resources.
     *
     * @param pluginDefinition Definition of plugin to initialize (only instantiated if needed)
     */
    public void initialize(IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        collectInstallers(pluginDefinition).forEach(AVFSFileInstaller::verifyInstallation);
    }

    /**
     * Create a list of all the file/template installers needed to handle the given plugin, based upon the
     * interfaces that it implements.
     *
     * @param pluginDefinition Definition of plugin to process
     * @return List of installers, may be empty
     */
    private List<AVFSFileInstaller<?>> collectInstallers(IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        final Class<? extends IRaptorPlugin> pluginClass = pluginDefinition.getPluginClass();
        final List<AVFSFileInstaller<?>> installers = new ArrayList<>();

        if (ITemplateResourceManager.class.isAssignableFrom(pluginClass))
        {
            installers.add(new VFSTemplateInstaller(pluginDefinition));
        }

        if (IGlobalResourceManager.class.isAssignableFrom(pluginClass))
        {
            installers.add(new VFSGlobalFileInstaller(pluginDefinition));
        }

        return installers;
    }
}
