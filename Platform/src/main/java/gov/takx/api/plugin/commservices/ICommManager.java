package gov.takx.api.plugin.commservices;

/**
 * Comm manager for a comm service/path that allows the closing of the service, and the ability
 * to get an interface for writing data to the service.
 *
 */
public interface ICommManager extends ICommPath
{
    /**
     * Return an interface for writing data out the comm path.
     *
     * @return IWritableComm interface for writing data out the comm path
     */
    IWritableCommPath getWritableCommPath();

    /**
     * Physically close the comm path
     */
    void closeCommPath();

    /**
     * Disables dispatch of incoming data to parsers.  The IGatewayParser.parseBytes() method is suspended until
     * releaseCommPath() is invoked.  All registered ICommListener instances will still receive data.
     */
    void captureCommPath();

    /**
     * Enables dispatch of incoming data to parsers via the IGatewayParser.parseBytes() method
     */
    void releaseCommPath();

    /**
     * Flag that identifies if the comm path is actively dispatching incoming data to parsers.
     *
     * @return true, if comm path is captured, false otherwise.
     */
    boolean isCommPathCaptured();

    /**
     * Restart a ICommManager that is stopped, but was not completely shutdown, such as in the case of
     * a comm path representing a client ethernet connection that was unexpectedly dropped.  If the
     * setKeepAlive method is set to true, the ethernet connection will have a chance to be restarted.
     * Override this method to perform restart logic.
     *
     * @return result of restart request.  true = success.
     * @see #setKeepAlive
     */
    boolean restart();

    /**
     * Only valid for restartable connections. Tells TAKX NOT to destroy the Comm service when the connection
     * is dropped. That way, parsers and framers will still be around, and if the client tries to send
     * data out the (closed) port, TAKX will attempt to reconnect and send the data.
     *
     * @param keepalive true value indicates not to cleanup the ethernet service.
     * @see #restart()
     */
    void setKeepAlive(boolean keepalive);
}
