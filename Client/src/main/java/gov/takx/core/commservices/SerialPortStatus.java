package gov.takx.core.commservices;

import gov.takx.api.commservices.ISerialPortStatus;

import java.io.Serializable;

/**
 */
public class SerialPortStatus implements ISerialPortStatus, Serializable
{
    private final String commName;
    private final boolean portOpen;
    private final int baudRate;
    private final boolean echoData;

    public SerialPortStatus(String commName, int baudRate, boolean portOpen, boolean echoData)
    {
        this.commName = commName;
        this.portOpen = portOpen;
        this.baudRate = baudRate;
        this.echoData = echoData;
    }

    @Override
    public String getCommName()
    {
        return commName;
    }

    @Override
    public boolean isPortOpen()
    {
        return portOpen;
    }

    @Override
    public boolean isEchoingData()
    {
        return echoData;
    }

    @Override
    public int getBaudRate()
    {
        return baudRate;
    }

    @Override
    public String toString()
    {
        return "SerialPortStatus{" +
                "commName='" + commName + '\'' +
                ", portOpen=" + portOpen +
                ", baudRate=" + baudRate +
                ", echoData=" + echoData +
                '}';
    }
}
