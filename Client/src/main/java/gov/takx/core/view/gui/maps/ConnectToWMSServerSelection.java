package gov.takx.core.view.gui.maps;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capabilities;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.wms.CapabilitiesRequest;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.globe.imagerywizard.AVTable;
import gov.takx.core.globe.imagerywizard.FileStoreUtil;
import gov.takx.core.globe.imagerywizard.InstallConstants;
import gov.takx.core.globe.imagerywizard.LayerInfo;
import gov.takx.core.globe.imagerywizard.WMSInfo;
import gov.takx.core.globe.imagerywizard.WMSValidator;
import gov.takx.core.globe.layers.worldwind.ApplicationHttpRetriever;
import gov.takx.core.security.CertUtils;
import gov.takx.core.security.ICertificateInstallerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

/**
 */

/**
 * This class is a refactor of the old WMSSelectionPage class
 */
public class ConnectToWMSServerSelection implements ICertificateInstallerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String WMS_ERROR = "wmsError";

    private final WMSValidator validator;
    private volatile boolean isDead = false;
    private final Object updateLock = new Object();
    private final ConnectToWMSServerDialog owner;
    private final JList<WMSInfo> urlList;
    private final DefaultListModel<WMSInfo> urlListModel;
    private final Map<WMSInfo, Thread> serverMap = new ConcurrentHashMap<>();

    public ConnectToWMSServerSelection(ConnectToWMSServerDialog owner, WMSValidator validator,
                                       Iterable<? extends WMSInfo> servers, JList<WMSInfo> urlList)
    {
        this.owner = owner;
        this.validator = validator;
        this.urlList = urlList;
        urlListModel = (DefaultListModel<WMSInfo>) urlList.getModel();

        List<WMSInfo> serversCopy = new ArrayList<>();
        if (servers != null)
        {
            for (WMSInfo wmsInfo : servers)
            {
                if (wmsInfo != null)
                {
                    serversCopy.add(wmsInfo);
                }
            }
        }
        setListValues(serversCopy);
        refreshServerInfo();
    }

    public WMSValidator getValidator()
    {
        return validator;
    }

    public WMSInfo getSelectedServer()
    {
        return urlList.getSelectedValue();
    }

    public WMSInfo getValidSelectedServer()
    {
        WMSInfo info = getSelectedServer();
        if (info != null && info.getValue(InstallConstants.STATUS) == InstallConstants.OK)
        {
            return info;
        }
        return null;
    }

    public void refreshServerInfo()
    {
        updateServers(getListValues(), true);
    }

    public List<LayerInfo> getSelectedServerLayers()
    {
        WMSInfo selected = getValidSelectedServer();
        if (selected != null)
        {
            return selected.getLayers();
        }
        return null;
    }

    /**
     * Notification that a certificate installation has finished for the specified host, with the corresponding result.
     * If it was successful, set the HttpsURLConnection to use the socket factory that we have specified that
     * WW use.  For some reason, the WMS connections through WW do not look at this AVKey when making the
     * connection, so we have to do it manually.
     *
     * @param result RESULT of attempt to install cert.
     * @param host   host installing cert for.
     * @see ICertificateInstallerListener.RESULTS;
     */
    @Override
    public void certificateInstalled(RESULTS result, String host)
    {
        logger.debug("Installed cert for host {} result = {}", host, result);
        if (result == RESULTS.SUCCESS)
        {
            SSLContext sslContext = (SSLContext) WorldWind.getValue(AVKey.HTTP_SSL_CONTEXT);
            if (sslContext != null)
            {
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                final WMSInfo wmsInfo = getWMSInfoFromHostname(host);
                if (wmsInfo != null)
                {
                    new Thread(() -> {
                        // Servers are still updating... need to let the full update finish before calling
                        // update on the server that we just installed the cert for.
                        while (isServersUpdating())
                        {
                            Thread.yield();
                        }
                        logger.debug("Calling update on {}", wmsInfo);
                        wmsInfo.setValue(InstallConstants.STATUS, InstallConstants.WAITING);
                        doUpdateServer(wmsInfo, false, true);
                        owner.refreshComponents();
                    }).start();
                }
            }
        }
    }

    /**
     * Get the WMSInfo object that corresponds to a hostname.
     *
     * @param host hostname string
     * @return WMSInfo object, if it exists in the model.
     */
    private WMSInfo getWMSInfoFromHostname(String host)
    {
        Enumeration<WMSInfo> e = urlListModel.elements();
        while (e.hasMoreElements())
        {
            WMSInfo wmsInfo = e.nextElement();
            try
            {
                URI uri = new URI(wmsInfo.getServerURI());
                URL url = uri.toURL();
                if (host.equals(url.getHost()))
                {
                    return wmsInfo;
                }
            } catch (URISyntaxException e1)
            {
                logger.error("Error with URI Syntax", e1);
            } catch (MalformedURLException e1)
            {
                logger.error("Malformed URL", e1);
            }
        }
        return null;
    }

    private Iterable<WMSInfo> getListValues()
    {
        List<WMSInfo> servers = new ArrayList<>();
        Enumeration<WMSInfo> e = urlListModel.elements();
        while (e.hasMoreElements())
        {
            servers.add(e.nextElement());
        }
        return servers;
    }

    private void setListValues(List<WMSInfo> servers)
    {
        String[] sortKeys = {AVKey.SERVICE, AVKey.TITLE, AVKey.STYLE_NAMES};
        AVTable.sortValues(servers, sortKeys);
        urlListModel.removeAllElements();
        for (WMSInfo info : servers)
        {
            urlListModel.addElement(info);
        }
    }

    private synchronized boolean isServersUpdating()
    {
        // If there is anything in the serverMap then the servers are still updating
        return !serverMap.isEmpty();
    }

    private void updateServers(final Iterable<WMSInfo> servers, final boolean updateCapabilities)
    {
        synchronized (updateLock)
        {
            ForkJoinPool.commonPool().execute(() -> doUpdateServers(servers, updateCapabilities));
        }
    }

    private void doUpdateServers(Iterable<WMSInfo> servers, final boolean updateCapabilities)
    {
        // Mark each server info as waiting.
        for (WMSInfo server : servers)
        {
            server.setValue(InstallConstants.STATUS, InstallConstants.WAITING);
        }

        // Repaint the server table.
        owner.refreshComponents();

        // Update the status of each server.
        for (final WMSInfo server : servers)
        {
            if (Thread.interrupted())
            {
                break;
            }

            if (serverMap.containsKey(server)) continue;

            ServerUpdater runnable = new ServerUpdater(this, server, updateCapabilities);
            Thread t = new Thread(runnable);
            serverMap.put(server, t);
            t.start();
        }
    }

    class ServerUpdater implements Runnable
    {
        ConnectToWMSServerSelection selection;
        WMSInfo server;
        boolean updateCapabilities;

        public ServerUpdater(ConnectToWMSServerSelection selection, WMSInfo server, boolean updateCapabilities)
        {
            this.selection = selection;
            this.server = server;
            this.updateCapabilities = updateCapabilities;
        }

        @Override
        public void run()
        {
            logger.debug("Updating Map Server: {}, updating capabilities: {}", server, updateCapabilities);

            // Update the server status.
            doUpdateServer(server, updateCapabilities, true);
            if (!selection.isDead)
            {
                owner.syncMapToURLList();
                owner.refreshComponents();
            }
            serverMap.remove(server);

            if (logger.isDebugEnabled())
            {
                logger.debug("Updated server: {}, layers: {}", server, server.getLayers());
            }
        }
    }

    /**
     * Add layers specified within a WMS URL.  Creates the WMSInfo object, connects to the server and downloads the
     * list of available layers.  The URL is parsed for specified layers and these are chosen from the available
     * server layers.
     *
     * @param url WMS URL with one or more specified layers
     * @return whether or not there were any valid layer names in the URL
     */
    public boolean addLayersFromURL(URL url)
    {
        //create the WMSInfo object
        String s = url.toString();
        s = WWXML.fixGetMapString(s);
        final WMSInfo wmsInfo = new WMSInfo(s);

        // in the case of a proxy server, set to override with the proxy url.
        wmsInfo.setValue(AVKey.GET_CAPABILITIES_URL, s);
        wmsInfo.setValue(AVKey.GET_MAP_URL, s);

        //parse the layer names from the WMS URL
        final String[] layerNames = FileStoreUtil.getLayerNames(url);
        if (layerNames == null) return false;

        // get the available layers from the WMS server
        synchronized (updateLock)
        {
            ForkJoinPool.commonPool().execute(() -> {
                doUpdateServer(wmsInfo, true, true);

                //validate the formats available for the layers
                if (validator.validate(wmsInfo).equals(InstallConstants.OK))
                {
                    //get all layers
                    List<LayerInfo> allLayer = wmsInfo.getLayers();
                    Set<LayerInfo> matchingLayers = new HashSet<>();
                    //get all layers
                    for (LayerInfo layer : allLayer)
                    {
                        String name = layer.getLayerName();
                        if (name != null)
                        {
                            //match available layers to the parsed layer names from the URL
                            for (String n : layerNames)
                            {
                                if (name.equals(n))
                                {
                                    matchingLayers.add(layer);
                                }
                            }
                        }
                    }

                    Set<String> missingLayerNames = getMissingLayerNames(layerNames, matchingLayers);
                    if (!missingLayerNames.isEmpty())
                    {
                        showMissingWMSLayersError();
                    }

                    //ask the owner to complete the layer adding process
                    owner.addLayers(new ArrayList<>(matchingLayers));
                } else
                {
                    showMissingWMSLayersError();
                }
            });
        }
        return true;
    }

    public void cancelServerUpdate()
    {
        if (!isServersUpdating())
        {
            return;
        }

        isDead = true;
    }

    private void showMissingWMSLayersError()
    {
        //the available formats are not valid, so show a message dialog on the EDT
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(JFrame.getFrames()[0],
                "Unable add WMS URL layer(s)."));
    }

    private Set<String> getMissingLayerNames(String[] layerNames, Set<LayerInfo> matchedLayers)
    {
        Set<String> missingLayerNames = new HashSet<>();
        Set<String> matchedLayerNames = new HashSet<>();
        for (LayerInfo info : matchedLayers)
        {
            matchedLayerNames.add(info.getLayerName());
        }

        for (String layerName : layerNames)
        {
            if (!matchedLayerNames.contains(layerName))
            {
                missingLayerNames.add(layerName);
            }
        }
        return missingLayerNames;
    }

    private void doUpdateServer(final WMSInfo wmsInfo, boolean updateCapabilities, boolean firstRun)
    {
        wmsInfo.setValue(WMS_ERROR, "");
        try
        {
            if (wmsInfo.getCapabilities() == null || updateCapabilities)
            {
                updateCapabilities(wmsInfo);
            } else
            {
                wmsInfo.setValue(InstallConstants.STATUS, InstallConstants.OK);
            }
        } catch (IllegalArgumentException e)
        {
            logger.warn("Exception connecting to server {}", wmsInfo.getServerURI(), e);

            final String targetUrl = wmsInfo.getServerURI();

            int ret = AlwaysOnTopJOptionPane.showConfirmDialog(owner,
                    "<html><center>The provided URL appears to be invalid:<br>" +
                            targetUrl + "<br><br><b><font color=red>Do you want to remove this URL?" +
                            "</font></b></center></html>",
                    "Invalid URL",
                    JOptionPane.YES_NO_OPTION);
            if (logger.isDebugEnabled()) logger.debug("Invalid WMS URL: {}", targetUrl);

            if (ret == JOptionPane.YES_OPTION)
            {
                owner.removeWmsServer(wmsInfo);
            }
        } catch (final Exception exception)
        {
            URL url;
            try
            {
                url = new URL(wmsInfo.getServerURI());
            } catch (MalformedURLException e)
            {
                logger.warn("Error: ", e);
                url = null;
            }

            // TODO: This logic doesn't really look correct and needs to be tested with a server that requires PKI.
            //  display the user confirmation pop up if the permitted socket factory is enabled in user preferences
            //  and this is the first caught exception from attempting to connect to this https server
            if (url != null)
            {
                if (isHandshakeException(exception) && firstRun)
                {
                    try
                    {
                        logger.debug("About to install cert for {}", url.getHost());
                        CertUtils.installCert(url.getHost(), this);
                    } catch (Exception e)
                    {
                        logger.error("Exception trying to install cert for {}", url.getHost());
                    }
                } else if (firstRun)
                {
                    CertUtils.enablePKIKeyUse();
                    doUpdateServer(wmsInfo, updateCapabilities, false);
                } else if (isHandshakeException(exception))
                {
                    // TODO:  Maybe we want to display the PKI picking dialog here instead.
                    AlwaysOnTopJOptionPane.showErrorDialogLater("Could not load your PKI user certificate for: \n\n" +
                            wmsInfo.getServerURI() + "\n\nEither select the correct key or install your PKI cert through " +
                            "Internet Explorer and restart", "PKI Error");
                }
            }

            wmsInfo.setValue(WMS_ERROR, "Server is currently unreachable.");

            wmsInfo.setCapabilities((WMSCapabilities) null);
            wmsInfo.setLayers(null);

            if (exception instanceof IOException)
            {
                wmsInfo.setValue(InstallConstants.STATUS, InstallConstants.CONNECTION_ERROR);
            } else if (exception instanceof SAXException)
            {
                wmsInfo.setValue(InstallConstants.STATUS, InstallConstants.CONNECTION_ERROR);
            } else
            {
                wmsInfo.setValue(InstallConstants.STATUS, InstallConstants.UNKNOWN_ERROR);
            }
        }
    }

    /**
     * Update the capabilities document for a server. This method handles both WMS and WCS services. If the service type
     * is "auto" then this method will try to connect to a WCS service, and fall back to WMS if that fails.
     *
     * @param wmsInfo Server for which to fetch capabilities.
     */
    private void updateCapabilities(WMSInfo wmsInfo) throws Exception
    {
        final String serverURI = wmsInfo.getServerURI();
        switch (wmsInfo.getServiceType())
        {
            case AUTO ->
            {
                logger.debug("Determining service type for: {}", serverURI);
                String result = updateWCS(wmsInfo);
                if (!InstallConstants.OK.equals(result))
                {
                    logger.debug("Unable to connect using WCS: {}. Trying WMS...", serverURI);
                    updateWMS(wmsInfo);
                }
            }
            case WCS ->
            {
                logger.debug("Connecting to WCS server: {}", serverURI);
                updateWCS(wmsInfo);
            }
            case WMS ->
            {
                logger.debug("Connecting to WMS server: {}", serverURI);
                updateWMS(wmsInfo);
            }
            default -> logger.warn("Unexpected service type: {}", wmsInfo.getServiceType());
        }
    }

    /**
     * Update capabilities for a WMS server.
     *
     * @param wmsInfo Server for which to fetch capabilities.
     * @return a string indicating the result of the operation. Returns {@link InstallConstants#OK} if the update
     * succeeds.
     */
    private String updateWMS(WMSInfo wmsInfo) throws Exception
    {
        URI uri = new URI(wmsInfo.getServerURI());

        CapabilitiesRequest request = new CapabilitiesRequest(uri);
        URLRetriever retriever = ApplicationHttpRetriever.createRetriever(request.getUri().toURL(), null);
        retriever.call();
        WMSCapabilities caps = new WMSCapabilities(retriever.getBuffer());
        caps.parse();

        logger.info("Connected to WMS server: {}", uri);
        wmsInfo.setCapabilities(caps);

        String result = getValidator().validate(wmsInfo);
        wmsInfo.setValue(InstallConstants.STATUS, result);

        return result;
    }

    /**
     * Update capabilities for a WCS server.
     *
     * @param wmsInfo Server for which to fetch capabilities.
     * @return a string indicating the result of the operation. Returns {@link InstallConstants#OK} if the update
     * succeeds.
     */
    private String updateWCS(WMSInfo wmsInfo) throws Exception
    {
        URI uri = new URI(wmsInfo.getServerURI());

        CapabilitiesRequest request = new CapabilitiesRequest(uri, "WCS");
        request.setVersion("1.0.0");

        URLRetriever retriever = ApplicationHttpRetriever.createRetriever(request.getUri().toURL(), null);
        retriever.call();

        WCS100Capabilities caps = new WCS100Capabilities(retriever.getBuffer());
        caps.parse();

        if (caps.getService() == null)
        {
            caps = null;
        }

        if (logger.isInfoEnabled() && caps != null)
        {
            logger.info("Connected to WCS server: {}", uri);
        }
        wmsInfo.setCapabilities(caps);

        String result = getValidator().validate(wmsInfo);
        wmsInfo.setValue(InstallConstants.STATUS, result);

        return result;
    }

    private boolean isHandshakeException(Exception exception)
    {
        return exception instanceof SSLHandshakeException || exception.getCause() instanceof SSLHandshakeException;
    }
}
