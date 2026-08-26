package gov.takx.api.commservices;

import java.util.Collection;

/**
 * Platform service for querying serial port data, including serial modems.
 *
 * @since 1.9
 */
public interface ISerialPortManager
{
    /**
     * Current listing of installed serial ports.
     *
     * @return Non-null collection of serial port status data
     */
    Collection<ISerialPortStatus> listSerialPorts();
}
