package gov.takx.test.utils;

/**
 * Static utility methods for use by the persistence tests.
 *
 */
public class PersistTestUtils
{
    /**
     * @return Class.method of our caller's caller
     */
    public static String getCaller()
    {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Need to go up 3 to get to our caller's caller
        return stackTrace.length > 3
                ? stackTrace[3].getClassName() + "." + stackTrace[3].getMethodName()
                : "<unknown>";
    }
}
