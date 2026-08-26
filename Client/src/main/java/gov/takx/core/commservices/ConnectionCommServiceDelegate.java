package gov.takx.core.commservices;

import gov.takx.api.commservices.ICommService;
import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.commservices.ICommServiceTerminationManager;
import gov.takx.api.commservices.IPortAnalyzerListener;
import gov.takx.api.commservices.ISerialPortStatus;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.api.plugin.commservices.ICommManager;
import gov.takx.api.plugin.commservices.ICommPath;
import gov.takx.api.plugin.commservices.ICommSocketListener;
import gov.takx.api.plugin.connection.ICommServiceDelegate;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;

/**
 */
@ApplicationScoped
public class ConnectionCommServiceDelegate implements ICommServiceDelegate
{
    /**
     * The minimum time we will allow between port scans
     */
    private static final int MIN_SCAN_FREQUENCY = 5_000;

    private volatile SerialPortScanner scanner;

    private final ICommServiceManager commServiceManager;
    private final ICoreExecutorService executorService;

    private volatile long scanInitiated = System.currentTimeMillis();

    @Inject
    ConnectionCommServiceDelegate(ICommServiceManager commServiceManager, ICoreExecutorService executorService)
    {
        this.commServiceManager = commServiceManager;
        this.executorService = executorService;
    }

    @Override
    public ICommServiceTerminationManager getCommServiceTerminationManager()
    {
        // We know that ICommService is only implemented by CommServiceManager, which also implements
        // the ICommServiceTerminationManager interface. The only reason this interface, ICommServiceTerminationManager,
        // is required is to fully support custom comm service type terminations.
        return (ICommServiceTerminationManager) commServiceManager;
    }

    @Override
    public ICommManager createCommPath(@Nonnull String absoluteFilePath, @Nonnull InputStream inputStream) throws IOException
    {
        String extension = FileUtils.getExtension(absoluteFilePath);
        return commServiceManager.createCommPath(absoluteFilePath, inputStream, extension);
    }

    @Override
    public ICommManager createCommPath(@Nonnull String absoluteFilePath, @Nonnull InputStream inputStream, String commFormat) throws IOException
    {
        return commServiceManager.createCommPath(absoluteFilePath, inputStream, commFormat);
    }

    @Override
    public ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo, String commFormat) throws IOException
    {
        return createSerialCommPath(commPathName, baudRate, echo, null, commFormat);
    }

    @Override
    public ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo, String echoPort, String commFormat) throws IOException
    {
        return createSerialCommPath(commPathName, baudRate, echo, echoPort, commFormat, 0);
    }

    @Override
    public ICommManager createSerialCommPath(@Nonnull String commPathName, int baudRate, boolean echo, String echoPort, String commFormat, int timeout) throws IOException
    {
        return commServiceManager.createSerialCommPath(commPathName, baudRate, echo, echoPort, commFormat, timeout);
    }

    @Override
    public ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName) throws IOException
    {
        return createCommPath(commType, commPathName, "");
    }

    @Override
    public ICommManager createCommPath(@Nonnull ICommService commService) throws IOException
    {
        return commServiceManager.createCommPath(commService);
    }

    @Override
    public ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName, String commFormat) throws IOException
    {
        return createCommPath(commType, commPathName, commFormat, -1);
    }

    @Override
    public ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName, String commFormat, int readBufferSize) throws IOException
    {
        return createCommPath(commType, commPathName, commFormat, readBufferSize, null);
    }

    @Override
    public ICommManager createCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName, String commFormat, int readBufferSize, ICommListener commListener) throws IOException
    {
        return commServiceManager.createCommPath(commType, commPathName, (commFormat == null ? "" : commFormat), readBufferSize, commListener);
    }

    @Override
    public ICommPath getExistingCommPath(@Nonnull CommServiceType commType, @Nonnull String commPathName)
    {
        return commServiceManager.getExistingCommPath(commType, commPathName);
    }

    @Override
    public boolean addServerSocketListener(int port, String commFormat)
    {
        return addServerSocketListener(port, null, commFormat);
    }

    @Override
    public boolean addServerSocketListener(int port, ICommSocketListener listener, String commFormat)
    {
        return addServerSocketListener(port, null, listener, commFormat);
    }

    @Override
    public boolean addServerSocketListener(int port, InetAddress bindAddress, ICommSocketListener listener, String commFormat)
    {
        return commServiceManager.openServerSocket(port, bindAddress, listener, commFormat);
    }

    @Override
    public boolean addServerSocketListener(@Nonnull ServerSocket serverSocket, String commFormat)
    {
        return addServerSocketListener(serverSocket, null, commFormat);
    }

    @Override
    public boolean addServerSocketListener(@Nonnull ServerSocket serverSocket, ICommSocketListener listener, String commFormat)
    {
        return commServiceManager.handleServerSocket(serverSocket, listener, commFormat);
    }

    @Override
    public boolean addSSLServerSocketListener(int port, String commFormat)
    {
        return addSSLServerSocketListener(port, null, commFormat);
    }

    @Override
    public boolean addSSLServerSocketListener(int port, ICommSocketListener listener, String commFormat)
    {
        return addSSLServerSocketListener(port, null, listener, commFormat);
    }

    @Override
    public boolean addSSLServerSocketListener(int port, InetAddress bindAddress, ICommSocketListener listener, String commFormat)
    {
        return commServiceManager.openSSLServerSocket(port, bindAddress, listener, commFormat);
    }

    @Override
    public void removeServerSocketListener(int port)
    {
        commServiceManager.closeServerSocket(port, null);
    }

    @Override
    public void removeServerSocketListener(int port, InetAddress bindAddress)
    {
        commServiceManager.closeServerSocket(port, bindAddress);
    }

    @Override
    public ICommPath getServerSocketCommPath(int port)
    {
        return commServiceManager.getServerSocketCommPath(port, null);
    }

    @Override
    public ICommPath getServerSocketCommPath(int port, InetAddress bindAddress)
    {
        return commServiceManager.getServerSocketCommPath(port, bindAddress);
    }

    @Override
    public void analyzeSerialPorts(@Nonnull final IPortAnalyzerListener listener, final int baudRate, final int timeoutSeconds)
    {
        executorService.execute(() -> {
            Thread.currentThread().setName("Serial Port Analyzer");
            try
            {
                if (scanner != null)
                {
                    scanner.haltScan();
                }

                scanner = new SerialPortScanner(listener, baudRate, timeoutSeconds);

                // Ensure that we never run more often than MIN_SCAN_FREQUENCY millis
                final long now = System.currentTimeMillis();
                final long timeSoFar = now - scanInitiated;

                if (timeSoFar < MIN_SCAN_FREQUENCY)
                {
                    cleanSleep(MIN_SCAN_FREQUENCY - timeSoFar); // Wait the remainder of our refresh time
                }

                scanInitiated = System.currentTimeMillis(); // Track when we start
                scanner.start();
            } catch (Exception e)
            {
                listener.onPortAnalyzerError("Error scanning ports: " + e.getMessage());
            }
        });
    }

    /**
     * Sleep for the given time, ignoring interrupted exceptions.
     *
     * @param millis Time to sleep
     */
    private void cleanSleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        } catch (InterruptedException ignored)
        {
        }
    }

    @Override
    public void suspendAnalyzeSerialPorts()
    {
        if (scanner != null)
        {
            scanner.haltScan();
            scanner = null;
        }
    }

    @Override
    public Collection<ISerialPortStatus> listSerialPorts()
    {
        return commServiceManager.getSerialPortCommStatus();
    }
}
