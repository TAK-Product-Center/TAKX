package gov.takx.platform.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommIn;
import gov.takx.api.commservices.ICommOut;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.commservices.ICommServiceTerminationManager;
import gov.takx.api.platform.IParser;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.api.plugin.commservices.ICommPath;
import gov.takx.api.plugin.commservices.IWritableCommPath;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides the set of common operations across all comm service concrete implementations. Extend this class
 * rather than implementing ICommService and TAKX will provide most of the logic of adding/removing listeners
 * and the like. The only 2 required methods to implement are {@link #openResources()} and
 * {@link #closeResources(boolean)}.
 *
 * @since 2.0.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class ACommService implements ICommService
{
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected boolean isCaptured = false;
    protected String name;
    protected CommServiceType serviceType;
    protected boolean isRunning = false;
    protected ICommIn commIn;
    protected ICommOut commOut;
    protected Map<String, Serializable> properties = new HashMap<>();
    protected List<ICommListener> pendingListeners = new ArrayList<>();

    protected boolean keepAlive = false;

    @Inject
    protected ICommServiceTerminationManager terminationManager;

    protected ACommService(CommServiceType serviceType)
    {
        this.serviceType = serviceType;
    }

    /**
     * Return the name of the comm port.  In the case of ethernet ports should be something along the lines
     * of ipaddress + ":" + port.  Serial ports will be "COM1", "COM2", etc.  In the case of a custom comm
     * service type, the name should be explicitly set.  Otherwise the name will be pulled from the
     * properties map from the key {@link CommServiceConstants#PORT_NAME}
     *
     * @return String comm port name
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Add a listener to this comm service (CommIn). Listener will usually be something along the lines of an
     * implementation of {@code IConnection} which is responsible for opening ports and then listening to the ports for
     * data or port closure.
     *
     * @param listener ICommListener.
     */
    @Override
    public void addCommPathListener(@Nonnull ICommListener listener)
    {
        if (commIn != null)
        {
            commIn.registerCommListener(listener);
        } else
        {
            logger.info("Queueing listener for future assignment to a commIn that is presently null");
            pendingListeners.add(listener);
        }
    }

    /**
     * Remove the listener to the CommIn
     *
     * @param listener ICommListener
     */
    @Override
    public void removeCommPathListener(@Nonnull ICommListener listener)
    {
        if (commIn != null) commIn.removeCommListener(listener);
        pendingListeners.remove(listener);
    }

    @Override
    public void captureCommPath()
    {
        isCaptured = true;
    }

    @Override
    public void releaseCommPath()
    {
        isCaptured = false;
    }

    @Override
    public boolean isCommPathCaptured()
    {
        return isCaptured;
    }

    @Override
    public CommServiceType getType()
    {
        return serviceType;
    }

    /**
     * Return whether or not the comm path is open. In the case of Serial Ports, the service can be running
     * while the serial port is actually closed. Override {@code isCommServiceOpen} to return
     * a value other than whether the service is running.
     *
     * @return {@code true} if port is open, {@code false} if closed.
     * @see #isCommServiceOpen
     */
    @Override
    public boolean isCommOpen()
    {
        return isCommServiceOpen();
    }

    @Override
    public Map<String, Serializable> getProperties()
    {
        return properties;
    }

    /**
     * These are the properties that are available to the plugin developer using ICommPath.  Basically contains
     * the type (see CommServiceType enum), format (as supplied by plugin developer), ipaddress (if applicable)
     * and port (port number if applicable, or the name of the port in the case of a serial port).
     *
     * @return map of properties.
     * @see ICommPath
     * @see CommServiceConstants
     */
    @Override
    public Map<String, String> getCommPathProperties()
    {
        Map<String, String> propertyMap = new HashMap<>();

        propertyMap.put(ICommPath.TYPE, serviceType.toString());
        if (properties != null)
        {
            propertyMap.put(ICommPath.FORMAT, (String) properties.get(CommServiceConstants.PORT_FORMAT.toString()));
            String name = (String) properties.get(CommServiceConstants.PORT_NAME.toString());
            if (name.contains(":"))
            {
                String[] partsIsParts = name.split(":");
                propertyMap.put(ICommPath.IPADDRESS, partsIsParts[0]);
                propertyMap.put(ICommPath.PORT, partsIsParts[1]);
            } else
            {
                propertyMap.put(ICommPath.PORT, name);
            }
        }

        return propertyMap;
    }

    /**
     * Start a comm service by opening its resources.  If the attempt to open
     * resources fails, then stop the comm service.
     *
     * @param properties mapped key,value pairs of properties for the port
     * @return {@code false} on error, {@code true} on success
     */
    @Override
    public boolean startCommService(Map<String, Serializable> properties)
    {
        if (null != properties)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Assigning properties (size: {})", properties.size());
            }

            // Shallow copy of the properties
            for (String key : properties.keySet())
            {
                if (this.properties == null) this.properties = new HashMap<>();
                this.properties.put(key, properties.get(key));
            }
        }

        isRunning = openResources();

        if (!isRunning)
        {
            stopCommService(false);
        } else if (!pendingListeners.isEmpty())
        {
            // Some connections do not leverage commIn and therefore as long as there are not pending
            // listeners, there is no need to fail on a null commIn here
            Objects.requireNonNull(commIn, "The commIn parameter must be non-null at this point in order " +
                    "to register listeners. It is typically initialized in openResources().");

            for (ICommListener listener : pendingListeners)
            {
                commIn.registerCommListener(listener);
            }
        }

        return isRunning;
    }

    @Override
    public boolean restart()
    {
        return false;
    }

    /**
     * Stop the comm service.
     *
     * @param wasExpected {@code true} if manually stopped, {@code false} if connection was dropped or on error.
     */
    @Override
    public void stopCommService(boolean wasExpected)
    {
        isRunning = false;
        if (properties != null)
        {
            properties.clear();
            properties = null;
        }

        // NOTE: Threading issues on shutdown may cause commIn to go null rapidly therefore we need to individually
        // verify null status on these commands
        if (commIn != null)
        {
            commIn.stopCommIn(wasExpected);
        }

        if (commIn != null)
        {
            commIn.shutdownCommIn(wasExpected);
        }

        commIn = null;

        closeResources(wasExpected);

        if (commOut != null)
        {
            commOut.stopCommOut();
            commOut = null;
        }

        if (logger.isInfoEnabled()) logger.info("StopCommService for: {}", getName());
    }

    @Override
    public void setKeepAlive(boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }

    @Override
    public void setParsers(List<? extends IParser> parserList)
    {
        if (commIn != null)
        {
            commIn.setParsers(parserList);
        } else
        {
            if (CommServiceType.NONE != serviceType)
            {
                logger.warn("Trying to set parsers to a commIn that is null");
            }
        }
    }

    @Override
    public void commInterrupted()
    {
        terminationManager.terminateCommService(this);
        stopCommService(false);
    }

    @Override
    public void closeCommPath()
    {
        logger.debug("Closing comm path...");

        terminationManager.terminateCommService(this);
        stopCommService(true);
    }

    @Override
    public IWritableCommPath getWritableCommPath()
    {
        return commOut;
    }

    @Override
    public void cleanup()
    {
    }

    /**
     * Returns the value of keepAlive, whether or not to perform restart logic if a connection is dropped by calling
     * the {@link #restart()} method.
     */
    public boolean isKeepAlive()
    {
        return keepAlive;
    }

    /**
     * Return open status of comm service.  Override this method if a comm service can be running, but not
     * open, such as in the case of a serial port.
     *
     * @return boolean value of comm service/port being open.
     */
    protected boolean isCommServiceOpen()
    {
        return isRunning;
    }

    /**
     * Provides concrete implementations the opportunity to establish ICommIn and ICommOut.
     *
     * @return true, if the resources were opened successfully.  In the event false is returned,
     * a call to closeResources() will be made to ensure that all resources were properly closed.
     */
    protected abstract boolean openResources();

    /**
     * Provides concrete implementations the opportunity to close any resources they opened.
     *
     * @param wasExpected false if service was closed unexpectedly, connection dropped.  true otherwise
     */
    protected abstract void closeResources(boolean wasExpected);
}
