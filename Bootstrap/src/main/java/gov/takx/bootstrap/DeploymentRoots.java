package gov.takx.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class to locate "deployment roots" for CDI scanning.
 * <p/>
 * Developers note: this code was moved out of {@link ApplicationModuleFinder} because instantiating that class
 * <em>partially</em> initializes JBoss Modules.  It causes its URL stream handler factory to get installed, but then
 * it doesn't perform the service loader scan to find our {@link ApplicationUrlStreamHandlerFactory}, so normal
 * registration of our URL handlers would fail.  With this extraction, our test infrastructure can install its
 * own handler via {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)} and then add our custom URL handlers.
 *
 * @since 5.2 (code moved here from {@link ApplicationModuleFinder}
 */
public class DeploymentRoots
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Canonical class names for each of our deployment roots
    private static final List<String> CLIENT_ROOT_CLASS_NAMES = List.of(
            "gov.takx.core.launcher.ApplicationLauncher", // Client
            "gov.takx.api.cdi.InstanceManager"            // Platform
    );

    /**
     * Get the collection of deployment roots to be scanned for CDI managed beans for the given
     * {@code testRootClassNames} AND the TAKX Client module.
     *
     * @param testRootClassNames Names of classes to use to locate any test roots
     * @return The deployment roots for the client (used to build the Weld deployment)
     */
    public static Set<File> getClientDeploymentRoots(Collection<String> testRootClassNames)
    {
        // In order to support integration tests for plugin developers, we need to be fairly clever in
        // how we locate the deployment roots. We can't assume the typical source tree layout.
        // We have to use class files to get the code source location.

        // We use a "canonical" class from each jar we need.
        // I'm not thrilled by having to use a hard-coded class name from each jar, but I can't find a better
        // way to do this.

        final Set<File> roots = getDeploymentRoots(CLIENT_ROOT_CLASS_NAMES);

        // To allow other modules outside the TAKX core to run integration tests with full CDI support, we let them
        // pass in a list of additional classes for any test roots that need to be added

        roots.addAll(getDeploymentRoots(testRootClassNames));

        logger.debug("Deployment roots: {}", roots);

        return roots;
    }

    /**
     * Convert a set of class names into "deployment roots" (jars or directories) to be scanned for CDI managed beans.
     *
     * @param testRootClassNames Names of classes to use to locate deployment roots
     * @return The deployment roots for the given class names
     */
    public static Set<File> getDeploymentRoots(Collection<String> testRootClassNames)
    {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        return testRootClassNames.stream()
                .map(className -> getDeploymentRoot(className, loader))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Get the File (jar or directory) that represents the deployment root containing the named class.
     *
     * @param className Name of class to use to locate the root
     * @param loader    Class loader to use
     * @return deployment root
     */
    private static File getDeploymentRoot(String className, ClassLoader loader)
    {
        try
        {
            final var cls = loader.loadClass(className);
            String codeSourcePath = cls.getProtectionDomain().getCodeSource().getLocation().getPath();

            // Need some special handling when the code source path specifies a jar file ending with "!/"
            if (codeSourcePath.startsWith("file:") && codeSourcePath.endsWith("!/"))
            {
                codeSourcePath = codeSourcePath.substring("file:".length(), codeSourcePath.length() - 2);
            }

            // File URLs apparently do not have '+' signs encoded, but URLDecoder.decode will assume they are an
            // encoded space, so replace all '+' with '%2B' (the URL encoding for a plus sign), ensuring that
            // URLDecoder treats them as literal '+' when decoding.
            codeSourcePath = codeSourcePath.replaceAll("\\+", "%2B");
            codeSourcePath = URLDecoder.decode(codeSourcePath, StandardCharsets.UTF_8);

            return new File(codeSourcePath).getAbsoluteFile();
        } catch (ClassNotFoundException e)
        {
            // This is a slight hack to avoid spitting out errors in cases where it is expected that we won't
            // be able to resolve a test root. I don't have time to implement a generalized mechanism (and,
            // in fact, the whole mechanism for declaring test roots needs to be reworked since it is
            // dependent on test discovery order). So, for now, just emit the error as a debug message
            // if it is the specific root causing the problem.

            final String message = "Failed to find source location for class: " + className + " using " + loader;

            if (className.startsWith("gov.takx.test"))
            {
                logger.debug(message, e);
            } else
            {
                logger.error(message, e);
            }
        }

        return null;
    }

    private DeploymentRoots()
    {
    }
}
