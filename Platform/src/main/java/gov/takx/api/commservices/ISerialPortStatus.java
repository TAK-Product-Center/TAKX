package gov.takx.api.commservices;

/**
 * Configuration data for a single serial port.
 *
 * @since 1.9
 */
public interface ISerialPortStatus
{
    String getCommName();

    int getBaudRate();

    boolean isPortOpen();

    boolean isEchoingData();

//    void setBaudRate(int rate);
}
