package gov.takx.api.commservices;

import gov.takx.api.platform.IParser;
import gov.takx.api.plugin.commservices.ICommManager;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Comm service interface implemented by all comm services.  Methods that are exposed through the API are
 * defined in ICommManager.  Highly recommended to extend {@link ACommService}
 * rather than implementing this class.
 *
 */
public interface ICommService extends ICommManager
{
    /**
     * Return the type of comm service
     *
     * @return the comm service type
     * @see CommServiceType
     */
    CommServiceType getType();

    /**
     * return the property map for the comm service.
     *
     * @return property map
     */
    Map<String, Serializable> getProperties();

    /**
     * Start a comm service with specified properties
     *
     * @param properties mapped key,value pairs of properties for the port
     * @return false on error, true on success
     */
    boolean startCommService(Map<String, Serializable> properties);

    /**
     * Stop the comm service.
     *
     * @param wasExpected boolean value true if manually stopped, false if connection was dropped or on error.
     */
    void stopCommService(boolean wasExpected);

    /**
     * Return whether or not the comm path is open.  Not always the same as whether or not the service
     * is running, as a serial comm service could be open, but the port could be closed.
     *
     * @return boolean true/false, port open/closed.
     */
    boolean isCommOpen();

    /**
     * Inject parsers into the CommIn.  Parsers are created by the Device Gateway when it receives a notification
     * that there is a new comm service.
     *
     * @param parserList list of gateway parsers
     */
    void setParsers(List<? extends IParser> parserList);

    /**
     * Method for CommIn implementations to let the service know that it was interrupted unexpectedly.
     */
    void commInterrupted();

    /**
     * Called when comm service is removed from manager.  Allows for any additional cleanup.
     */
    void cleanup();
}
