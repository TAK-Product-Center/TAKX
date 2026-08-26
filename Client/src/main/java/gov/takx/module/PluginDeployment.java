package gov.takx.module;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.bootstrap.Bootstrap;
import gov.takx.bootstrap.FlavorUtils;
import gov.takx.bootstrap.JarVerifier;
import gov.takx.bootstrap.Verification;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.attachments.AbstractAttachable;
import gov.takx.platform.cdi.AnnotationIndexer;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.modules.NativeLibraryResourceLoader;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static gov.takx.bootstrap.FlavorConstants.CIV;
import static gov.takx.bootstrap.FlavorConstants.GOV;
import static gov.takx.bootstrap.FlavorConstants.MIL;

/**
 * This class represents a deployment based on a single plugin jar file.  It is responsible for mounting the
 * jar in VFS as needed, discovering sub-jars, and managing the jar scanner that actually locates plugin declarations.
 *
 * @since 2.0.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PluginDeployment extends AbstractAttachable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // List of app flavors that GOV flavored plugins can run in
    private static final List<String> GOV_COMPATIBLE_APP_FLAVORS = List.of(CIV, GOV, MIL);

    private static final String LIB = "lib";

    // Jar names to test to warn developers about including our distro jars in their plugin's "lib/" directory
    private static final List<String> DISTRO_JAR_PREFIXES = Arrays.asList("platform-", "client-", "bootstrap-");

    // Jar names to test to avoid including in their plugin's "lib/" directory
    private static final List<String> EVIL_JAR_PREFIXES = List.of(
            "aspectj",
            "log4j",
            "slf4j",
            "jboss-annotations-api",
            "jboss-modules",
            "jakarta.annotation-api-",
            "jakarta.el-api-",
            "jakarta.enterprise.cdi-api-",
            "jakarta.enterprise.lang-model-",
            "jakarta.inject-api-",
            "jakarta.interceptor-api-",
            "jakarta.persistence-api-",
            "jakarta.transaction-api-",
            "jakarta.xml.bind-api-",
            "weld-",
            "worldwind-",
            "worldwindx-");

    private final String productFlavor = PreferenceEntry.PRODUCT_FLAVOR.getStringValue();

    private final VirtualFile pluginFile;
    private final File jarPhysicalFile;
    private final String moduleIdentifier;
    private final boolean stubDeployment;
    private Closeable mountHandle;

    @Inject
    private IProgramEnvironment programEnvironment;

    /**
     * Construct a deployment from the specified virtual file.
     *
     * @param pluginFile       The virtual file referring to the jar file to be processed
     * @param tempFileProvider VFS temp file provider
     */
    public PluginDeployment(VirtualFile pluginFile, TempFileProvider tempFileProvider) throws IOException
    {
        stubDeployment = false;

        this.pluginFile = pluginFile;

        jarPhysicalFile = pluginFile.getPhysicalFile(); // Save the location of the original jar file
        moduleIdentifier = generateModuleId(pluginFile.getName());

        putAttachment(DeploymentAttachments.MODULE_ID, moduleIdentifier);
        putAttachment(DeploymentAttachments.TEMP_FILE_PROVIDER, tempFileProvider);

        final JarFile jarFile = new JarFile(jarPhysicalFile);
        putAttachment(DeploymentAttachments.JAR_FILE, jarFile);

        // Does this jar contain a "lib" subdirectory?
        final boolean nestedResources = jarFile.getEntry(LIB) != null;
        putAttachment(DeploymentAttachments.HAS_NESTED_RESOURCES, nestedResources);

        if (logger.isDebugEnabled())
        {
            logger.debug("Deployment of: {} exploded={} moduleId={}", jarPhysicalFile.getName(), nestedResources, moduleIdentifier);
        }
    }

    /**
     * Constructor used to generate a deployment for use in the unit/integration tests.  Those tests
     * don't use the module system, so there's no Jboss VFS usage, and we need some special handling to stub out
     * some handling.
     *
     * @param jarPhysicalFile The jar on which to base the deployment
     */
    public PluginDeployment(File jarPhysicalFile) throws IOException
    {
        stubDeployment = false;

        logger.debug("Make test deployment on: {}", jarPhysicalFile);

        pluginFile = null;
        this.jarPhysicalFile = jarPhysicalFile;

        moduleIdentifier = generateModuleId(jarPhysicalFile.getName());
        putAttachment(DeploymentAttachments.MODULE_ID, moduleIdentifier);

        putAttachment(DeploymentAttachments.JAR_FILE, new JarFile(jarPhysicalFile));
        putAttachment(DeploymentAttachments.HAS_NESTED_RESOURCES, false); // No nested jars while testing
    }

    /**
     * Constructor used to generate a deployment for use in the unit/integration tests using a specified class
     * (meaning there's no jar file from which to build a deployment).
     *
     * @param pluginFile       Virtual file representing this plugin
     * @param pluginDefinition Plugin definition upon which to create a deployment
     * @since 2.18
     */
    PluginDeployment(VirtualFile pluginFile, IPluginDefinition<?> pluginDefinition)
    {
        final Class<? extends IRaptorPlugin> pluginClass = pluginDefinition.getPluginClass();

        logger.debug("Make test deployment on: {}", pluginClass);

        stubDeployment = true;

        File physicalFile;
        try
        {
            physicalFile = pluginFile.getPhysicalFile();
        } catch (IOException e)
        {
            logger.error("Failed to construct stub VFS file for: {}", pluginClass);
            physicalFile = null;
        }

        this.pluginFile = pluginFile;
        jarPhysicalFile = physicalFile;

        moduleIdentifier = pluginClass.getName();
        putAttachment(DeploymentAttachments.MODULE_ID, moduleIdentifier);

        putAttachment(DeploymentAttachments.DEPLOYABLE, false);
        putAttachment(DeploymentAttachments.HAS_NESTED_RESOURCES, false); // No nested jars while testing
        putAttachment(DeploymentAttachments.CONTAINER, WeldContainer.instance("TEST"));

        addToAttachmentList(DeploymentAttachments.PLUGIN_DEFINITIONS, pluginDefinition);
    }

    /**
     * Perform our post-construct operations.  If we are a "jar of jars" then process the lib directory.
     */
    @PostConstruct
    private void postConstruct()
    {
        if (!stubDeployment) // Avoid all of this if this is a test stub
        {
            final String basename = FileUtils.getNameWithoutExtension(jarPhysicalFile);
            Index index = new AnnotationIndexer(programEnvironment.getProjectDataDirectory(), basename, Collections.singletonList(jarPhysicalFile)).readIndex();

            putAttachment(DeploymentAttachments.INDEX, index);
        }
    }

    /**
     * @return Module identifier for this deployment
     * @since 2.35
     */
    public String getModuleIdentifier()
    {
        return moduleIdentifier;
    }

    /**
     * Construct the module name from the plugin jar file name.  Spaces are illegal in module ids, so fix them.
     * <p>
     * Example: {@code plugin.foobar-1.0.0.jar} becomes {@code plugin.foobar-1.0.0}
     *
     * @param jarName Name of jar we are processing
     * @return Generated module identifier
     */
    private String generateModuleId(String jarName)
    {
        // Strip the extension and use that to generate the module id
        return FileUtils.getNameWithoutExtension(jarName).replace(' ', '_').replace(':', '_');
    }

    /**
     * Initiate the scanning of the deployment's jar file to locate plugin declarations.
     */
    public void prepareDeployment() throws IOException
    {
        putAttachment(DeploymentAttachments.DEPLOYABLE, true); // Unless a failure is detected along the way

        verifyJar();

        if (getAttachment(DeploymentAttachments.DEPLOYABLE))
        {
            // If it has nested resources, then mount the jar file as a node in the VFS as an expanded zip archive
            // and process all the nested resources.
            if (getAttachment(DeploymentAttachments.HAS_NESTED_RESOURCES)) processNestedResources();

            new PluginJarScanner(this).scan(); // Get it scanned
        }
    }

    /**
     * @return The name of the jar we are handling
     */
    public String getName()
    {
        return jarPhysicalFile.getName();
    }

    /**
     * @return The actual file for the jar we are processing, irrespective of any VFS manipulation
     */
    public File getJarPhysicalFile()
    {
        return jarPhysicalFile;
    }

    /**
     * If this is a jar of jars, then return the physical mount point for this jar. If this jar has not been
     * mounted, then return {@code null}.
     *
     * @return Mount point if "jar of jars" or {@code null}
     */
    public File getMountPoint()
    {
        if (pluginFile != null && pluginFile.isDirectory())
        {
            try
            {
                return pluginFile.getPhysicalFile();
            } catch (IOException e)
            {
                logger.error("Failed to obtain jar mount point: {}", pluginFile, e);
            }
        }

        return null;
    }

    /**
     * Determines the verification and compatibility status of the plugin. The plugin flavor, if present, must abide by
     * the compatibility matrix implemented in {@link #checkFlavorCompatibility(String, String)}.
     * 
     * @since 5.2 (renamed from validateMilFlavor in 5.5)
     */
    private void verifyJar() throws IOException
    {
        try (var jarFile = new JarFile(jarPhysicalFile, true))
        {
            Verification verification = JarVerifier.verify(jarFile);
            String flavor = FlavorUtils.getFlavor(jarFile);
            boolean compatible = checkFlavorCompatibility(productFlavor, flavor);
            logger.debug("Plugin jar [{}] verification: {} flavor: {} compatible: {}", jarPhysicalFile.getCanonicalPath(), verification, flavor, compatible);
            // TODO DCW TAKX-3900 For now allow unverified plugins in civ/gov, but eventually need to enforce it
            boolean deployable = compatible && (Bootstrap.isDeveloperMode() || FlavorUtils.isCiv(flavor) || GOV.equalsIgnoreCase(flavor) || verification != Verification.None);
            putAttachment(DeploymentAttachments.DEPLOYABLE, deployable);
            putAttachment(DeploymentAttachments.VERIFICATION, verification);
        }
    }

    /**
     * Process any nested resources in this deployment.  This handles nested jar files, native libraries, and classes.
     */
    private void processNestedResources() throws IOException
    {
        mountHandle = VFS.mountZipExpanded(pluginFile, pluginFile, getAttachment(DeploymentAttachments.TEMP_FILE_PROVIDER));

        final VirtualFile libVF = pluginFile.getChild(LIB); // lib directory (for "jar of jars", and native libs)

        // If there is a native library subdirectory, then add a resource loader for it to the deployment
        if (libVF.getChild(Module.getPlatformIdentifier()).exists())
        {
            addToAttachmentList(DeploymentAttachments.RESOURCE_LOADERS, new NativeLibraryResourceLoader(libVF.getPhysicalFile()));

            logger.debug("Added NATIVE resource loader to deployment");
        }

        // Find all the jar files within the lib directory
        VfsFileFinder.JAR_FINDER.find(libVF).forEach(this::addSubJar);

        // If any sub jars were actually found (the lib directory wasn't empty), then add the dependency
        if (!getAttachmentList(DeploymentAttachments.SUB_JARS).isEmpty())
        {
            // Generate the library module Id since it does have sub-jars
            putAttachment(DeploymentAttachments.LIB_MODULE_ID, "private-" + moduleIdentifier + "-libs");
        }
    }

    /**
     * Inspect a given virtual file and add it to the list of sub jars if it passes inspection.
     *
     * @param jar Jar to inspect
     */
    private void addSubJar(VirtualFile jar)
    {
        if (logger.isDebugEnabled()) logger.debug("Sub-jar: {}", jar.getName());

        try
        {
            final JarFile jarFile = new JarFile(jar.getPhysicalFile());

            if (isDistroJar(jarFile))
            {
                VFSUtils.safeClose(jarFile);

                logger.error("""
                        Plugin jar [{}] contains a TAKX distributed jar file in '/lib': {}
                        You should not include jar files from the TAKX distribution in your plugin's jar file.
                        These classes will be ignored, but you should fix your build to avoid including them in the plugin jar file.
                        """, jarPhysicalFile.getName(), jar.getName());
            } else if (isEvilJar(jarFile))
            {
                VFSUtils.safeClose(jarFile);

                logger.error("""
                        Plugin jar [{}] contains a known problematic jar file in '/lib': {}
                        This jar is known to cause problems with plugin execution when in your plugin's jar file.
                        These classes will be ignored, but you should fix your build to avoid including them in the plugin jar file.
                        """, jarPhysicalFile.getName(), jar.getName());
            } else
            {
                addToAttachmentList(DeploymentAttachments.SUB_JARS, jarFile);
            }
        } catch (IOException e)
        {
            // Convert into a DeploymentException so this method can be used in a stream handler
            throw new DeploymentException(String.format("Failed to process sub-jar: %s/lib/%s", jarPhysicalFile.getName(), jar.getName()), e);
        }
    }

    /**
     * Determine if the given jar file is one of those that we distribute.  We want to warn the developer that they
     * may have messed up their build by including our jars in their plugin jar.  We already avoid importing any
     * classes from such a nested jar due to how we define the local dependency, but we want to issue an error
     * to the log so the developer can get a direct warning about the problem.
     * <p>
     * We test not only the name of the jar, we also test the "Built-By" attribute in the manifest to ensure
     * it is one of ours and not some other random jar that is just named like ours (such as Apache commons).
     *
     * @param jarFile The jar file to test
     * @return {@code true} if this appears to be one of our distributed jar files
     */
    private boolean isDistroJar(JarFile jarFile)
    {
        final String jarName = new File(jarFile.getName()).getName(); // Get last component of name

        // If it looks like one of ours, then dig deeper
        if (StringUtils.startsWithAny(jarName, DISTRO_JAR_PREFIXES))
        {
            // There are a number of typical jars that have names like ours (the JNA platform, Apache commons, etc.)
            // So, look in the manifest and see if it was built by TAKX. If so, then it's definitely one of ours.
            try
            {
                final Manifest mf = jarFile.getManifest();
                final String builtBy = mf != null ? mf.getMainAttributes().getValue("Built-By") : null;

                if ("TAKX".equalsIgnoreCase(builtBy)) return true;
            } catch (IOException e)
            {
                logger.error("Failed to test jar: {}", jarFile.getName(), e);

                // In case of an error (which will probably lead to more later), err on the side of lenience
                // and return false.
            }
        }

        return false;
    }

    /**
     * We know that certain jar files will cause problems if they are included in the /lib directory.  For example,
     * the jboss annotations will cause things like @PostConstruct to fail.
     *
     * @param jarFile The jar file to test
     * @return {@code true} if it is one of the known evil jars
     */
    private boolean isEvilJar(JarFile jarFile)
    {
        final String jarName = new File(jarFile.getName()).getName(); // Get last component of name

        return StringUtils.startsWithAny(jarName, EVIL_JAR_PREFIXES);
    }

    /**
     * Do our best to close/cleanup our resources and VFS usage.
     */
    public void cleanup()
    {
        logger.debug("Closing: {}", this);

        for (JarFile subJarFile : getAttachmentList(DeploymentAttachments.SUB_JARS))
        {
            if (logger.isDebugEnabled()) logger.debug("  close sub-jar: {}", subJarFile.getName());
            VFSUtils.safeClose(subJarFile);
        }

        VFSUtils.safeClose(mountHandle);
    }

    /**
     * This method implements the TAK flavor compatibility matrix. The general rules are:
     * <p/>
     * <ul>
     *     <li>{@code CIV} plugins can run in any client.</li>
     *     <li>{@code GOV} plugins can run in {@code CIV}, {@code GOV}, and {@code MIL} clients.</li>
     *     <li>All other plugin flavors can only run in a matching client.</li>
     * </ul>
     *
     * @param appFlavor the app flavor id
     * @param pluginFlavor the plugin flavor id
     * @return {@code true} if the plugin is compatible, otherwise {@code false}
     * @since 5.5
     */
    private static boolean checkFlavorCompatibility(String appFlavor, String pluginFlavor)
    {
        if (FlavorUtils.isCiv(pluginFlavor))
        {
            return true;
        } else if (pluginFlavor.equals(GOV))
        {
            return GOV_COMPATIBLE_APP_FLAVORS.contains(appFlavor);
        } else
        {
            return appFlavor.equals(pluginFlavor);
        }
    }

    @Override
    public String toString()
    {
        return "PluginDeployment{" +
                "jarPhysicalFile=" + jarPhysicalFile +
                '}';
    }
}
