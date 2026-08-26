package gov.takx.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * This base class offers the core functionality of {@link ApplicationInstance} without any support from the CDI container,
 * thus the name BareApplicationInstance. It is needed in the early parts of the boot process when we need to access the
 * instance number prior to bringing up the CDI container.
 *
 * @since 2.0.0
 */
public class BareApplicationInstance
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String INSTANCE_ERROR_MESSAGE = "Invalid setting for 'instance' [{}], must be a number > 0";

    public static final int DEFAULT_INSTANCE = 0; // The default instance number

    // Multiple application instances can run on the same box. Each instance after the first must have a non-zero
    // instance number set in the properties, like this: -Dinstance=1
    protected static final int instanceNumber;

    static
    {
        // Determine our instance ID. Instance ID's must be numbers > 0.
        final String instance = System.getProperty("instance", null);

        int instanceFromProperty = DEFAULT_INSTANCE;

        if (instance != null)
        {
            try
            {
                instanceFromProperty = Integer.parseInt(instance);
            } catch (NumberFormatException e)
            {
                logger.error(INSTANCE_ERROR_MESSAGE, instance);
            }

            if (instanceFromProperty < 0)
            {
                instanceFromProperty = 0;
                logger.error(INSTANCE_ERROR_MESSAGE, instance);
            } else if (instanceFromProperty == DEFAULT_INSTANCE)
            {
                logger.warn("Did you really mean to specify an 'instance' number of 0? That is the default.");
            }
        }

        instanceNumber = instanceFromProperty;

        // Log the instance number if it is not the default.
        if (instanceNumber > 0) logger.info("TAKX instance: {}", instance);
    }

    /**
     * @return The instance number of this application. The initial/default instance will have an instance number of 0.
     * @since 1.19
     */
    public int getInstanceNumber()
    {
        return instanceNumber;
    }

    /**
     * @return {@code true} if this is the default instance of this application, indicated by a instance number of 0.
     * @since 1.19
     */
    public boolean isDefaultInstance()
    {
        return instanceNumber == DEFAULT_INSTANCE;
    }
}
