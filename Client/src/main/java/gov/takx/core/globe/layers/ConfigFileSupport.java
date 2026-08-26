package gov.takx.core.globe.layers;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Support methods for working with XML config files tht define layers and elevation models.
 *
 */
@PseudoBean
public class ConfigFileSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String LAYER_CONFIG_DIR = "WWXMLConfigurations";
    private static final String VFS_TAG = ":VFS:";

    @Inject
    @VFSManagerScope(VFSScope.Global)
    private IVirtualFileSystemManager globalVFSManager;

    /**
     * Import the given configuration file into the VFS dir for layer configurations.  Ensure that we don't overwrite
     * an existing config file by generating a unique name upon import.
     *
     * @param configFile The configuration file to import (copy into VFS)
     * @return The name of the file generated in the VFS layer config directory.  The returned name has a special
     * tag prefixed to it so we can later identify it as a VFS relative path.
     * @throws java.io.IOException On an error copying the file
     */
    public String importXMLConfigFile(File configFile) throws IOException
    {
        File target = FileUtils.getUniqueFileName(getVFS().getRootDir(), configFile);
        FileUtils.copyProgress(configFile, target, null);

        return VFS_TAG + target.getName();
    }

    /**
     * Get the "real" file path to load for a given config file.  The config file may have a VFS_TAG prefix, in which
     * case we convert the path to the absolute path to the file in VFS.  Otherwise, we return the given file path
     * as is.
     *
     * @param configFile The config file path to process
     * @return The real file path to use
     */
    public String getRealFileToLoad(String configFile)
    {
        // If the file name starts with our special tag, then remove it and generate the full path to the file
        // in the VFS.

        String realFileToLoad = configFile; // Default to using path "as is"

        if (configFile.startsWith(VFS_TAG))
        {
            FilePath vfsPath = new FilePath(configFile.substring(VFS_TAG.length()));
            realFileToLoad = vfsPath.toSystemPath(getVFS().getRootDir());
        }

        logger.debug("Config File '{}' will use real path '{}'", configFile, realFileToLoad);

        return realFileToLoad;
    }

    /**
     * @return The VirtualFileSystem we are using to store our configuration files
     */
    private IVirtualFileSystem getVFS()
    {
        return globalVFSManager.getVirtualFileSystem(LAYER_CONFIG_DIR);
    }
}
