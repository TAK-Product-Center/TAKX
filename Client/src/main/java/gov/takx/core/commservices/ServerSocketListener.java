package gov.takx.core.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.commservices.ICommServiceTerminationManager;
import gov.takx.api.commservices.IServerSocketListener;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.api.plugin.commservices.ICommPath;
import gov.takx.api.plugin.commservices.ICommSocketListener;
import gov.takx.api.timer.ITimerService;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens on a specific port for client requests to connect.  Accepts the connection and starts
 * ServerSocketCommService objects for each.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class ServerSocketListener implements Runnable, IServerSocketListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long MAX_IDLE_TIME = 120_000;

    private final int port;
    private final InetAddress address;

    private ICommSocketListener listener;
    private Map<String, ICommService> services = new ConcurrentHashMap<>();

    private final String commFormat;
    private final ServerSocket serverSocket;
    private final CommServiceType commServiceType;
    private boolean closeExpected;
    private TimerTask checkIdleTask;

    @Inject
    private ICommServiceTerminationManager terminationManager;

    @Inject
    private ICommServiceManager manager;

    @Inject
    private ITimerService timerService;

    /**
     * Construct the Server Socket by injecting port name, address and listener and starting the thread.
     *
     * @param serverSocket    the server socket with which to accept connections
     * @param listener        listener to be notified of clients that are accepted
     * @param commFormat      specific format of data expected on client sockets
     * @param commServiceType Our comm service type (SSL or ordinary socket)
     */
    protected ServerSocketListener(ServerSocket serverSocket, ICommSocketListener listener, String commFormat, CommServiceType commServiceType)
    {
        this.serverSocket = serverSocket;
        address = serverSocket.getInetAddress();
        port = serverSocket.getLocalPort();
        this.commFormat = commFormat != null ? commFormat : "";
        this.listener = listener;
        this.commServiceType = commServiceType;
    }

    @PostConstruct
    private void postConstruct()
    {
        // start listening for clients.
        Thread thisThread = new Thread(this);
        thisThread.setName("Server Socket Listener: " + address + ":" + port);
        thisThread.start();
    }

    /**
     * Return the name of the Server, which could just be a port number, but will also include
     * the ip address if bound to a specific port.
     *
     * @return name of server
     */
    @Override
    public String getName()
    {
        if (address != null)
        {
            return address + "/" + port;
        } else
        {
            return ((Integer) port).toString();
        }
    }

    /**
     * Stop the listener by closing the server and no longer listening on the port.  Should cause a breakout
     * from the run method.
     */
    @Override
    public void stopSocketListener()
    {
        if (serverSocket != null)
        {
            try
            {
                // First remove the listener from any comm services so it won't be notified of the
                // unexpected comm path closed for the server or comm path stopped for any current clients. If we don't
                // remove the listener here, plugins will be notified even though they just called
                // removeServerSocketListener() on ICommServiceDelegate, which triggers this method to be called.
                if (services != null) services.values().forEach(service -> service.removeCommPathListener(listener));

                // May not seem like the most sense to close the socket to get the accept() call to unblock,
                // but it does seem to be the most recommended solution.
                closeExpected = true;
                serverSocket.close();
            } catch (IOException e)
            {
                logger.error("IOException closing server socket", e);
            }
        }
    }

    @Override
    public void addCommPathListener(@Nonnull ICommListener listener)
    {
        logger.error("Adding a listener not supported for ServerSocketListener");
    }

    @Override
    public void removeCommPathListener(@Nonnull ICommListener listener)
    {
        logger.error("Removing a listener not supported for ServerSocketListener");
    }

    @Override
    public Map<String, String> getCommPathProperties()
    {
        Map<String, String> propertyMap = new HashMap<>();

        propertyMap.put(ICommPath.TYPE, commServiceType.toString());
        propertyMap.put(ICommPath.FORMAT, commFormat);
        if (address != null)
        {
            propertyMap.put(ICommPath.IPADDRESS, address.toString());
        }
        propertyMap.put(ICommPath.PORT, Integer.toString(port));

        return propertyMap;
    }

    @SuppressWarnings({"InfiniteLoopStatement"})
    @Override
    public void run()
    {
        try
        {
            startIdleTask();

            while (true)
            {
                logger.debug("Waiting for connections...");

                final Socket socket = serverSocket.accept();

                final InetAddress clientInetAddress = socket.getInetAddress();

                // Construct the "client address" string which includes the IP address and remote port number
                // We need to port number in this string since if we want to support connections from multiple
                // endpoints on the same host (same host address).

                String clientAddress = clientInetAddress.getHostAddress() + ":" + socket.getPort();

                // Ensure address doesn't begin with "/"
                if (clientAddress.startsWith("/"))
                {
                    clientAddress = clientAddress.substring(1);
                }

                logger.debug("Accepting connection from {}", clientAddress);

                final String sourceName = clientAddress + ":" + port;

                final Map<String, Serializable> props = new HashMap<>();
                props.put(CommServiceConstants.RUNNING.toString(), true);
                props.put(CommServiceConstants.PORT_TYPE.toString(), commServiceType.toString());
                props.put(CommServiceConstants.PORT_NAME.toString(), Integer.toString(port));
                props.put(CommServiceConstants.PORT_FORMAT.toString(), commFormat);
                props.put(CommServiceConstants.PORT_ADDRESS.toString(), clientAddress);

                // See if there is an existing service of the same name in our list of accepted clients.  We
                // know that the names must be unique and it is just possible that the service of the
                // same port name has not been completely cleaned up before the new one has been accepted.
                // Remove the old one from the local list.
                ICommService existingService = services.get(sourceName);
                if (existingService != null)
                {
                    logger.debug("Existing comm service:  {}", sourceName);
                    services.remove(sourceName);
                }

                // In an effort to reuse ServerSocketCommService(s), if the registry/manager already
                // has an instance of the service by the same name, we will check to see if it is closed,
                // and simply reuse it with the newly accepted socket.  If it is still open, we will have to
                // create a new ServerSocketCommService (not ideal).
                boolean newServerSocket = false;
                ServerSocketCommService serverSocketService =
                        (ServerSocketCommService) manager.getCommService(commServiceType, sourceName);
                if (serverSocketService == null)
                {
                    logger.debug("Creating new ServerSocketCommService");
                    serverSocketService = new ServerSocketCommService(socket, listener, commServiceType);
                    newServerSocket = true;
                } else
                {
                    // If we are accepting socket connections faster than we get notification that the previous one
                    // has been shutdown, the previous one will appear to still be open.  Since it can't REALLY
                    // be open, we know it will be notified of the close soon, and we can remove it from the
                    // registry, and create a new one.  Can't just use the old one since it will soon be closed
                    // if we try.  Only really an issue when we are getting more than 10 connections per second
                    // on the same ip address / port.
                    if (serverSocketService.isCommOpen())
                    {
                        terminationManager.terminateCommService(serverSocketService);
                        serverSocketService = new ServerSocketCommService(socket, listener, commServiceType);
                        newServerSocket = true;
                    } else  // else, we can reuse the old one.
                    {
                        serverSocketService.setSocket(socket);
                        logger.debug("Using OLD ServerSocketCommService");
                    }
                }

                boolean isRunning = serverSocketService.startCommService(props);

                services.put(sourceName, serverSocketService);

                // Start the ICommIn thread
                if (isRunning) serverSocketService.startCommIn();

                if (terminationManager != null)
                {
                    if (newServerSocket)
                    {
                        manager.addCommService(serverSocketService);
                    } else
                    {
                        serverSocketService.restart();
                    }
                }
            }
        } catch (BindException e)
        {
            logger.error("Error binding server to port {}", port, e);

            if (listener != null) listener.commPathClosed(this, false);
        } catch (IOException e)
        {
            if (closeExpected)
            {
                logger.debug("Expected close on port {}", port);
            } else
            {
                logger.warn("Error on port {}; closed unexpectedly ", port, e);
            }

            if (listener != null) listener.commPathClosed(this, closeExpected);
        }

        // Tell the registry (CommServicesManager) to remove stop any of the clients that were accepted on this port
        if (services != null)
        {
            ICommService[] commServices = services.values().toArray(new ICommService[0]);
            for (ICommService service : commServices)
            {
                if (terminationManager != null)
                {
                    terminationManager.terminateCommService(service);
                    service.stopCommService(true);
                }
            }
            services.clear();
            services = null;
        }

        stopIdleTask();
        listener = null;
        terminationManager = null;
    }

    private void startIdleTask()
    {
        checkIdleTask = new CheckIdleTask();
        timerService.scheduleAtFixedRate(checkIdleTask, 0, 10 * 1000);
    }

    private void stopIdleTask()
    {
        if (checkIdleTask != null) checkIdleTask.cancel();
        checkIdleTask = null;
    }

    /**
     * Timer to remove idle clients that were accepted for this server, but have closed and have not re-opened after
     * an interval.  The though is, if there is a client that continually connects and disconnects, we want to
     * be able to re-use the previous comm service.  If it has been idle for a reasonable amount of time, it's
     * probably not coming back.
     */
    private class CheckIdleTask extends TimerTask
    {
        @Override
        public void run()
        {
            List<String> sourceNamesToRemove = new ArrayList<>();

            ICommService[] commServices = services.values().toArray(new ICommService[0]);
            for (ICommService service : commServices)
            {
                ServerSocketCommService sscService = (ServerSocketCommService) service;
                long idleTime = sscService.getIdleTime();
                // Prune any socket services that have been around for a long time and whose keep alive is false
                if (idleTime > 0 && !sscService.isKeepAlive())
                {
                    if (System.currentTimeMillis() - idleTime > MAX_IDLE_TIME)
                    {
                        // We want to clean up this thread...
                        sscService.shutdownIdleService();
                        terminationManager.terminateCommService(sscService);
                        sourceNamesToRemove.add(sscService.getName());
                    }
                }
            }

            sourceNamesToRemove.forEach(services::remove);
        }
    }
}
