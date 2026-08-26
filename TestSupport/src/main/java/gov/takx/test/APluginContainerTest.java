package gov.takx.test;

/**
 * Abstract base class for a plugin test class that needs a minimally configured TAKX CDI container initialized
 * before the test code runs.  Unlike {@link AContainerTest}, this class ensures the current working directory is used
 * for any files which are created and cleans them up after suite completion.
 * <p>
 * It is assumed that the tests have been run with our AOP agent installed (via -javaagent).
 *
 * @since 2.8
 */
public class APluginContainerTest extends AContainerTest
{
    public APluginContainerTest()
    {
        // Use the current working directory for the program data (will be cleaned up at the end)
        super(System.getProperty("user.dir"));
    }
}
