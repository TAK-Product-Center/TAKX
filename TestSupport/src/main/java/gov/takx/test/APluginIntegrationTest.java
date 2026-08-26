package gov.takx.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Abstract base class for plugin integration tests.  It provides the basic TAKX startup handling, as well as other
 * setup and cleanup necessary when integration tests are run for plugins.
 * <br><br>
 * <strong>
 * WARNING: DO NOT use this base class for any test class in the TAKX repo. It is strictly for use by plugin projects.
 * Using this as a base class leads to the deletion of the {@code IntegrationTests/ProjectData} directory and that
 * leads to exceptions in subsequent tests.
 * </strong>
 *
 * @since 2.8
 */
@SuppressWarnings("unused") // Used by plugin tests
public class APluginIntegrationTest extends AIntegrationTest
{
    /**
     * Construct an integration test using the simple name of the test subclass as the project name.
     *
     * @param classesUnderTest The list of classes that are under test.  This is used to locate the containing
     *                         jar/directory so all classes included there are scanned for Bean defining Annotations,
     *                         and, thus, are added to the Weld container constructed for the test.
     * @since 2.19
     */
    protected APluginIntegrationTest(Class<?>... classesUnderTest)
    {
        this(null, classesUnderTest);
    }

    /**
     * Construct with the given project name and use the current working directory as the "program root".
     *
     * @param projectName      Name of project to use
     * @param classesUnderTest The list of classes that are under test.  This is used to locate the containing
     *                         jar/directory so all classes included there are scanned for Bean defining Annotations,
     *                         and, thus, are added to the Weld container constructed for the test.
     */
    protected APluginIntegrationTest(String projectName, Class<?>... classesUnderTest)
    {
        // Use the current working directory for the program data (will be cleaned up at the end) and don't use the
        // process manager
        super(projectName, true, classesUnderTest);
    }

    /**
     * Get a File reference to a given resource.  If the resource's URL can't be properly converted to a URI, then
     * a {@code File} instance is created that uses the URL's path.  A {@code null} is returned if the resource
     * can't be located.
     *
     * @param resource Resource to locate
     * @return File referring to that resource, {@code null} if not found
     * @since 2.20
     */
    protected File getFileFromResource(String resource)
    {
        final URL url = getClass().getResource(resource);
        if (url == null) return null;

        try
        {
            return new File(url.toURI());
        } catch (URISyntaxException e)
        {
            return new File(url.getPath());
        }
    }
}
