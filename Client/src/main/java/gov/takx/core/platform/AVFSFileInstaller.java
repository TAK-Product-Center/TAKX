package gov.takx.core.platform;

import com.google.common.io.Resources;
import gov.takx.api.plugin.IFileResourceManager;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility class used to install VFS Templates into a TAKX installation.
 */
public abstract class AVFSFileInstaller<M extends IFileResourceManager>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final IPluginDefinition<? extends IRaptorPlugin> pluginDefinition;

    /**
     * @param pluginDefinition Plugin definition from which we are extracting resources
     */
    protected AVFSFileInstaller(IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        this.pluginDefinition = pluginDefinition;
    }

    protected M getManager()
    {
        //noinspection unchecked
        return (M) pluginDefinition.getInstance();
    }

    abstract void validateVFS();

    abstract File getRootDir();

    abstract String getURIRoot();

    abstract List<URL> getResourceList();

    /**
     * Verify that the templates have been installed into TAKX.
     */
    public void verifyInstallation()
    {
        final List<URL> resourceList = getResourceList();

        if (logger.isDebugEnabled()) logger.debug("Resource List Size: {}", resourceList.size());

        if (!resourceList.isEmpty()) installResources(resourceList);
    }

    /**
     * Validate the VFS and install to the given resources.
     *
     * @param resourceList List of resources to process
     * @since 3.0
     */
    private void installResources(List<URL> resourceList)
    {
        final String rootToStripOff = getURIRoot();

        // Ensure we can access the corresponding template VFS.  If we can't, this
        // will throw an UnsupportedOperationException for the developer to deal with
        // during their unit testing
        validateVFS();

        // Iterate over each of the resources to install inside of the template VFS
        for (URL rez : resourceList)
        {
            if (logger.isDebugEnabled()) logger.debug("Does resource exist? {}", null != rez);

            if (null != rez)
            {
                //
                // We build the relative VFS root directory dynamically for each URL, so we reset
                // our rootDir to be the appropriate template VFS root before processing the current URL
                File rootDir = getRootDir();

                //
                // Extract out the actual resource path.  This should be in the following .jar resource bundle format:
                // file:jar:<PATH>!/path/to/resource per the Java jar protocol specification
                String resPath;

                try
                {
                    resPath = URLDecoder.decode(rez.getPath(), "UTF-8");
                } catch (UnsupportedEncodingException uee)
                {
                    logger.error("Error retrieving template file.", uee);
                    resPath = rez.getPath();
                }

                if (logger.isDebugEnabled()) logger.debug("Installing template from {}", resPath);

                //
                // Check to see if we have a resource bundle path separator before continuing
                int pathSep = resPath.indexOf('!') + 1;
                if (pathSep >= 2)
                {
                    //
                    // Extract out the absolute resource path from the jar protocol URL
                    String absolutePath = resPath.substring(pathSep);

                    if (logger.isDebugEnabled()) logger.debug("Absolute Path: {}", absolutePath);

                    int rootSep = absolutePath.indexOf(rootToStripOff);
                    if (rootSep >= 0)
                    {
                        //
                        // Convert it to a relative path based on what the plugin developer requested stripped off
                        String relativePath = absolutePath.substring(rootToStripOff.length() + 1);

                        if (logger.isDebugEnabled()) logger.debug("Relative Path: {}", relativePath);

                        //
                        // Finally, tokenize the path based on the package separator '/'
                        StringTokenizer st = new StringTokenizer(relativePath, "/");

                        while (st.hasMoreTokens())
                        {
                            String token = st.nextToken();

                            if (st.hasMoreTokens())
                            {
                                //
                                // This is a folder name token, not a file name token, since it isn't the last token
                                // Therefore, we need to see if the folder already exists in the VFS template space,
                                // and if not, create it.

                                File folder = new File(rootDir, token);

                                if (!folder.exists())
                                {
                                    if (logger.isDebugEnabled()) logger.debug("Creating new folder named {}", token);

                                    if (!folder.mkdirs()) logger.error("Failed to create: {}", folder);
                                }

                                //
                                // This becomes new root directory of where the resource will ultimately be installed
                                rootDir = folder;
                            } else
                            {
                                //
                                // Time to write the actual file, as this is the last token, and we are assured
                                // that any and all parenting folders properly exist and that rootDir points to
                                // our actual root directory inside the VFS template space.
                                //

                                if (logger.isDebugEnabled()) logger.debug("Writing {} to {}", token, rootDir);
                                File templateFile = new File(rootDir, token);
                                copyFile(templateFile, rez);
                            }
                        }
                    } else
                    {
                        logger.error("Unable to install resource - root path to strip off [{}] didn't appear in the absolute path [{}]", rootToStripOff, absolutePath);
                    }
                } else if (rez.getProtocol().equals("file"))
                {
                    //
                    // Extract out the absolute resource path from the jar protocol URL
                    int rootSep = resPath.indexOf(rootToStripOff);
                    String name = resPath.substring(rootSep + rootToStripOff.length() + 1);
                    File target = new File(rootDir + File.separator + name);
                    copyFile(target, rez);
                } else
                {
                    logger.error("Unable to install resource- does not appear to be a valid .jar resource path as there was no ! detected in the path");
                }
            } else
            {
                logger.warn("Skipping at least one resource as it was a null URL provided by manager = {})", getManager());
            }
        }
    }

    private void copyFile(File templateFile, URL rez)
    {
        // Ensure we only write the file if it doesn't already exist in the VFS space.
        // This check ensures that we don't inadvertently overwrite any existing file
        // that might have been modified upon installation by the installer or by the user.

        if (!templateFile.exists())
        {
            try (OutputStream os = new FileOutputStream(templateFile))
            {
                //noinspection UnstableApiUsage
                Resources.copy(rez, os);
                if (logger.isDebugEnabled()) logger.debug("Wrote VFS template {}", templateFile);
            } catch (IOException e)
            {
                logger.error("Failed to copy template: {}", rez, e);
            }
        }
    }
}
