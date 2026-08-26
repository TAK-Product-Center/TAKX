package gov.takx.core.platform;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.plugin.IGlobalResourceManager;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.commons.file.FilePath;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.List;

import static gov.takx.api.filesystem.VfsConstants.FILES_DIR;

/**
 * This class is responsible for copying plugin resources into the Global VFS.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class VFSGlobalFileInstaller extends AVFSFileInstaller<IGlobalResourceManager>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    @VFSManagerScope(VFSScope.Global)
    private IVirtualFileSystemManager mgr;

    private File rootDir;

    /**
     * @param pluginDefinition Plugin definition from which we are extracting resources
     */
    public VFSGlobalFileInstaller(IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(pluginDefinition);
    }

    @Override
    void validateVFS()
    {
        final FilePath path = new FilePath(FILES_DIR, pluginDefinition.getFamily(), pluginDefinition.getType());

        rootDir = mgr.getRoot(path.toString());

        if (!rootDir.exists() && !rootDir.mkdirs()) logger.error("Failed to create dir: {}", rootDir);
    }

    @Override
    File getRootDir()
    {
        return rootDir;
    }

    @Override
    String getURIRoot()
    {
        return getManager().getGlobalRoot();
    }

    @Override
    List<URL> getResourceList()
    {
        return getManager().getGlobalFilesForInstallation();
    }
}
