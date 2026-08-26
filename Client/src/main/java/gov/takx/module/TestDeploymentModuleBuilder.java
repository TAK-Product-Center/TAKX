package gov.takx.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * This class prepares the "module" for a plugin deployment while testing.  Since we don't run the tests using
 * actual jboss modules, we need to construct a workable class loader for plugin deployments so the rest of the
 * core can work regardless.
 *
 * @since 2.0.0
 */
public class TestDeploymentModuleBuilder implements IDeploymentModuleBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final PluginDeployment deployment;

    /**
     * Construct a test module builder from the given deployment.
     *
     * @param deployment The plugin deployment to process
     */
    public TestDeploymentModuleBuilder(PluginDeployment deployment)
    {
        this.deployment = deployment;
    }

    @Override
    public void addModuleSpecToLoader()
    {
        // Do nothing, not really using modules
    }

    @Override
    public void build() throws IOException, ClassNotFoundException
    {
        final List<URL> urls = new ArrayList<>(); // URLs to populate the class loader

        if (deployment.getAttachment(DeploymentAttachments.HAS_NESTED_RESOURCES))
        {
            urls.add(deployment.getMountPoint().toURI().toURL()); // Add the location where it was extracted

            // Add in all sub-jars
            for (JarFile jarFile : deployment.getAttachmentList(DeploymentAttachments.SUB_JARS))
            {
                urls.add(makeJarURL(jarFile.getName()));
            }

            // If the deployment has additional resource roots, then add those.

            for (File root : deployment.getAttachmentList(DeploymentAttachments.RESOURCE_ROOTS))
            {
                urls.add(root.toURI().toURL());
            }
        } else
        {
            // Just add in the deployments .jar file
            urls.add(makeJarURL(deployment.getJarPhysicalFile().getAbsolutePath()));
        }

        if (logger.isDebugEnabled()) logger.debug("Module {} URLs={}", deployment.getName(), urls);

        // And construct the class loader from the urls
        final ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]));

        // Add the class loader to the deployment
        deployment.putAttachment(DeploymentAttachments.CLASS_LOADER, classLoader);

        // Ensure that our context class loader is the plugins module loader
        Thread.currentThread().setContextClassLoader(classLoader);

        // And process all the plugins in this deployment
        new PluginJarProcessor(deployment).processPlugins();
    }

    /**
     * Construct a "jar:" scheme URL from the given file path.
     *
     * @param path Full path to jar file
     * @return URL for jar
     */
    private URL makeJarURL(String path) throws MalformedURLException
    {
        return new URL("jar:file:" + path.replace('\\', '/') + "!/"); // URIs actually don't like backslashes
    }

    public String toString()
    {
        return "Test module builder for: " + deployment.getName();
    }
}
