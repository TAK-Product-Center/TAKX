package gov.takx.api.commservices;

import gov.takx.api.plugin.connection.ICommServiceDelegate;

/**
 * Listener interface to receive updates for analysis of the serial ports.  Implement this interface for
 * the class to receive notifications when calling ICommServiceDelegate.analyzeSerialPorts(listener).
 *
 * @see ICommServiceDelegate
 */
public interface IPortAnalyzerListener
{
    /**
     * Implement this method to get callbacks on a serial port's status
     *
     * @param name   name of serial port (COM1, COM2, etc)
     * @param baud   baud rate
     * @param status string status of port.  In case of valid data, the plugin name.
     * @param valid  whether or not there is valid data.
     */
    void onSetPortStatus(String name, int baud, String status, boolean valid);

    /**
     * Implement to receive an error report running the serial port analyzer.
     *
     * @param error error string
     */
    void onPortAnalyzerError(String error);

    /**
     * Implement to get notification that all ports have been analyzed.
     */
    void onPortAnalyzerDone();
}
