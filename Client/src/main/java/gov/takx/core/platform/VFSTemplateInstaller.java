package gov.takx.core.platform;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.filesystem.ITemplateFileSystem;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.ITemplateResourceManager;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.core.filesystem.TemplateFileSystem;
import gov.takx.platform.KeyUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

import static gov.takx.api.filesystem.VfsConstants.APP_NAMESPACE;
import static gov.takx.api.filesystem.VfsConstants.CONNECTION_NAMESPACE;
import static gov.takx.api.filesystem.VfsConstants.DEVICE_NAMESPACE;
import static gov.takx.api.filesystem.VfsConstants.IMPORT_EXPORT_NAMESPACE;

/**
 * Utility class used to install VFS Templates into a TAKX installation.
 */
@PseudoBean
public class VFSTemplateInstaller extends AVFSFileInstaller<ITemplateResourceManager>
{
    private ITemplateFileSystem vfs;

    /**
     * @param pluginDefinition Definition of Plugin to manage
     */
    public VFSTemplateInstaller(IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(pluginDefinition);
    }

    @Override
    void validateVFS()
    {
        vfs = getVFS();
    }

    private ITemplateFileSystem getVFS()
    {
        final Class<? extends IRaptorPlugin> pluginClass = pluginDefinition.getPluginClass();

        if (IDevicePlugin.class.isAssignableFrom(pluginClass))
        {
            return TemplateFileSystem.getFileSystem(DEVICE_NAMESPACE, KeyUtils.getUniqueKey(pluginDefinition.getFamily(), pluginDefinition.getType()));
        } else if (IConnectionPlugin.class.isAssignableFrom(pluginClass))
        {
            return TemplateFileSystem.getFileSystem(CONNECTION_NAMESPACE, pluginDefinition.getUniqueKey());
        } else if (IAppPlugin.class.isAssignableFrom(pluginClass))
        {
            return TemplateFileSystem.getFileSystem(APP_NAMESPACE, pluginDefinition.getUniqueKey());
        } else if (IImportExportPlugin.class.isAssignableFrom(pluginClass))
        {
            return TemplateFileSystem.getFileSystem(IMPORT_EXPORT_NAMESPACE, pluginDefinition.getUniqueKey());
        } else
        {
            throw new RuntimeException("Unknown plugin type for VFS template installer: " + pluginClass.getName());
        }
    }

    @Override
    File getRootDir()
    {
        return vfs.getRootDir();
    }

    @Override
    String getURIRoot()
    {
        return getManager().getRoot();
    }

    @Override
    List<URL> getResourceList()
    {
        return getManager().getTemplatesForInstallation();
    }
}
