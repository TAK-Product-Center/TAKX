package gov.takx.core.commservices;

import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Generic Comm Path Service
 *
 */
class FileCommService extends ACommService
{
    private final InputStream inputStream;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected FileCommService(@Nonnull Map<String, Serializable> properties, InputStream inputStream)
    {
        super(CommServiceType.FILE);

        if (!properties.containsKey(CommServiceConstants.PORT_NAME.toString()))
        {
            throw new IllegalStateException("Properties must contain a CommServiceConstants.PORT_NAME property");
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Assigning properties (size: {})", properties.size());
        }

        //
        // Shallow copy of the properties
        for (String key : properties.keySet())
        {
            this.properties.put(key, properties.get(key));
        }

        name = (String) properties.get(CommServiceConstants.PORT_NAME.toString());

        this.inputStream = inputStream;
    }

    @Override
    public boolean isCommOpen()
    {
        return true;
    }

    @Override
    public boolean startCommService(Map<String, Serializable> properties)
    {
        logger.debug("Establishing comm in and comm out");

        commIn = new FileCommIn(this, name);
        commOut = new CommOut(name);
        return true;
    }

    @Override
    public boolean restart()
    {
        logger.debug("Starting file parsing...");

        isRunning = openResources();
        ((FileCommIn) commIn).restart();

        if (!isRunning)
        {
            stopCommService(false);
        }

        return isRunning;
    }

    public boolean openResources()
    {
        commIn.startCommIn(inputStream);

        logger.debug("File Comm Path {} only supports input stream and does not support output.", name);

        commOut.startCommOut(new NullOutputStream());

        return true;
    }

    protected void closeResources(boolean wasExpected)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("closing file resources...");
        }

        if (null != commIn)
        {
            commIn.stopCommIn(wasExpected);
        }
    }
}
