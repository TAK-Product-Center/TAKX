package gov.takx.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;

/**
 * Simple map to hold argument/value extracted from our command line.
 *
 * @since 2.0.0
 */
@SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
public class StartupArgs extends HashMap<String, String>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String[] originalArgs;

    /**
     * Construct from a list of strings.
     *
     * @param args List of command line arguments
     */
    public StartupArgs(List<String> args)
    {
        this(args.toArray(new String[0]));
    }

    /**
     * Construct by parsing an array of the command line options given.
     *
     * @param args Args to parse
     */
    public StartupArgs(String... args)
    {
        originalArgs = args.clone();

        for (int i = 0; i < args.length; i++)
        {
            final String arg = args[i];

            if (arg.startsWith("-"))
            {
                String value = "";

                final int startIndex = arg.charAt(1) == '-' ? 2 : 1; // Strip up to 2 dashes
                final String argName = arg.substring(startIndex);

                // See if next arg is a value or another option
                if (i < args.length - 1 && !args[i + 1].startsWith("-"))
                {
                    value = args[i + 1];
                    i++; // Skip value we just grabbed
                }

                put(argName, value); // NOPMD
            } else
            {
                logger.warn("Ignored unexpected argument '{}'", arg);
            }
        }
    }

    /**
     * @return The original arguments we were constructed from
     * @since 2.37
     */
    public String[] getOriginalArgs()
    {
        return originalArgs.clone();
    }

    /**
     * Get the provided value for an option (allowing aliases).
     *
     * @param options Option and aliases to look for
     * @return Value or {@code null} if not present
     */
    public String getArgValue(String... options)
    {
        for (String option : options)
        {
            final String value = get(option);
            if (value != null) return value;
        }

        return null;
    }

    /**
     * See if a argument/option is present.
     *
     * @param options option and aliases to look for
     * @return true if found
     */
    public boolean argPresent(String... options)
    {
        for (String option : options)
        {
            if (containsKey(option)) return true;
        }
        return false;
    }
}
