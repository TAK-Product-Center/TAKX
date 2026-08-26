/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capabilities;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOfferingBrief;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerStyle;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.WWXML;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents capabilities and layers provided by a Web Map Service (WMS) or Web Coverage Service (WCS). The type of
 * service provided by the server is captured by the {@link ServiceType}. Service type may be specified when the
 * object is created, or may be deferred by specifying {@link ServiceType#AUTO}. This allows the caller of this class
 * to try both WMS and WCS and automatically determine which the server supports.
 *
 */
public class WMSInfo extends AVListImpl
{
    /**
     * The type of service provided by this server.
     *
     * @since 1.43
     */
    public enum ServiceType
    {
        /** Automatically the type of service for this server. */
        AUTO,

        /** Connect to a Web Map Service (WMS) on this server. */
        WMS,

        /** Connect to a Web Coverage Service (WCS) on this server. */
        WCS
    }

    /**
     * The type of service provided by this server. The service type may be specified explicitly when this object is
     * created, or may be determined automatically when connection is established. Note that the service type may be
     * changed when {@link #setCapabilities} is called.
     *
     * @since 1.43
     */
    private ServiceType serviceType;

    /**
     * This is the original URI supplied when creating this object. The value stored in {@link #serverURI} may be
     * resolved a more specific representation (e.g. {@code http://www.website.com/} to {@code http://128.1.1.1}), so keep a reference
     * to the original representation to quickly check for duplicates when the user attempts to add additional servers.
     *
     * @since 1.14
     */
    private final String originalURI;

    /**
     * The URI (possibly resolved to the base URI) used internally to find the host WMS server.
     */
    private String serverURI;

    /**
     * The capabilities associated with this server. Will be an instance of either {@link WMSCapabilities} or
     * {@link WCS100Capabilities}, depending on the type of service.
     */
    private Object capabilities;

    /**
     * A list of the layers provided by this server.
     */
    private final List<LayerInfo> layers = new ArrayList<>();

    /**
     * Layers provided by this server that have already been added to the TAKX project.
     */
    private final Set<LayerInfo> installedLayers = new HashSet<>();

    /**
     * Constructor.
     *
     * @param serverURI The WMS server URI.
     */
    public WMSInfo(String serverURI)
    {
        this(serverURI, ServiceType.AUTO);
    }

    /**
     * Create a new server information object.
     *
     * @param serverURI   URI for the server.
     * @param serviceType Service type to use when connecting to this server. May be {@link ServiceType#AUTO}, in which
     *                    case the client will attempt to determine the appropriate service.
     * @since 1.43
     */
    public WMSInfo(String serverURI, ServiceType serviceType)
    {
        originalURI = serverURI;
        this.serverURI = serverURI;
        this.serviceType = serviceType;
        setValue(AVKey.SERVICE, this.serverURI);
    }

    /**
     * Indicates the type of service (e.g. Web Map Service or Web Coverage Service) provided by this server. The service
     * type may be explicitly specified during construction, or may be determined when connection is established, in which
     * case the service type is reported as {@link ServiceType#AUTO} until the actuall service type is determined.
     *
     * @return The type of service provided by this server. Returns ServiceType.AUTO if the service type is
     * unknown.
     * @since 1.43
     */
    public ServiceType getServiceType()
    {
        return serviceType;
    }

    /**
     * Indicates a descriptive name for the map service provided by this server. This name is determined by the service
     * Capabilities document.
     *
     * @return A descriptive name for the service. May return {@code null} if the Capabilities document is not available.
     * @since 1.43
     */
    public String getServiceName()
    {
        if (capabilities instanceof WMSCapabilities)
        {
            return ((WMSCapabilities) capabilities).getServiceInformation().getServiceTitle();
        } else if (capabilities instanceof WCS100Capabilities)
        {
            return ((WCS100Capabilities) capabilities).getService().getName();
        }

        return null;
    }

    /**
     * The original, unresolved URI provided when this object was instantiated. Allows for quick duplicate checks, e.g.
     * if the user tries to create a duplicate of www.website.com after the URL in {@link #serverURI} has been converted
     * to its actual ip address.
     *
     * @return The original URI.
     * @since 1.14
     */
    public String getOriginalURI()
    {
        return originalURI;
    }

    /**
     * The URI for the WMS server. This may either be the original URI provided by the user or the resolved, base URI
     * provided in the capabilities returned by the server.
     *
     * @return The server URI.
     */
    public String getServerURI()
    {
        return serverURI;
    }

    /**
     * Return the object representing the server's capabilities. The type of object returned depends on the type of
     * service provided by this server. The returned object will be an instance of {@link WMSCapabilities} for a Web Map
     * Service, or {@link WCS100Capabilities} for a Web Coverage Service.
     *
     * @return The object representing the server's capabilities. Returns {@code null} if the capabilities document is not
     * available.
     */
    public Object getCapabilities()
    {
        return capabilities;
    }

    /**
     * Set the object representing the WMS server's capabilities.
     *
     * @param capabilities The object representing the WMS server's capabilities.
     */
    public void setCapabilities(WMSCapabilities capabilities)
    {
        this.capabilities = capabilities;
        serviceType = ServiceType.WMS;

        setWMSInfoParams(capabilities);

        setLayers(createWMSLayerInfoList(capabilities));
    }

    /**
     * Sets the capabilities of this server using the capabilities provided by a WCS server. Calling this method
     * sets the service type of this object to {@link ServiceType#WCS}.
     *
     * @param capabilities WCS capabilities object, or {@code null} to clear currently set capabilities.
     * @since 1.43
     */
    public void setCapabilities(WCS100Capabilities capabilities)
    {
        this.capabilities = capabilities;
        serviceType = ServiceType.WCS;

        setWCSInfoParams(capabilities);

        setLayers(createWCSLayerInfoList(capabilities));
    }

    /**
     * Return the list of layers provided by the targeted WMS server.
     *
     * @return The list of layers provided by the targeted WMS server.
     */
    public List<LayerInfo> getLayers()
    {
        return Collections.unmodifiableList(layers);
    }

    /**
     * Set the list of layers provided by the targeted WMS server.
     *
     * @param layers The list of layers provided by the targeted WMS server.
     */
    public void setLayers(Iterable<? extends LayerInfo> layers)
    {
        this.layers.clear();
        if (layers != null)
        {
            for (LayerInfo layerInfo : layers)
            {
                // Sometimes the LayerInfo is duplicated in the list. I have no explanation for this, and it seems
                // like it shouldn't happen, but it does, and the "contains" fixes it.
                if (layerInfo != null && !this.layers.contains(layerInfo)) this.layers.add(layerInfo);
            }
        }
    }

    /**
     * Return the list of the installed layers associated with this WMS server.
     *
     * @return The list of the installed layers associated with this WMS server.
     */
    public Set<LayerInfo> getInstalledLayers()
    {
        return Collections.unmodifiableSet(installedLayers);
    }

    /**
     * Set the list of the layers provided by this WMS server that have been installed.
     *
     * @param installedLayers The list of the layers provided by this WMS server that have been installed.
     */
    public void setInstalledLayers(Iterable<? extends LayerInfo> installedLayers)
    {
        this.installedLayers.clear();
        if (installedLayers != null)
        {
            //update the currently installed layers to installed state
            for (LayerInfo layerInfo : installedLayers)
            {
                if (layerInfo != null) this.installedLayers.add(layerInfo); // mark this layer as installed
            }
        }
    }

    /**
     * Update the WMS info based on the capabilities retrieved from the server.
     *
     * @param capabilities The capabilities retrieved from the WMS server.
     */
    private void setWMSInfoParams(WMSCapabilities capabilities)
    {
        if (capabilities != null)
        {
            String s = capabilities.getRequestURL("GetCapabilities", "http", "get");
            if (s != null)
            {
                s = WWXML.fixGetMapString(s);

                //if the user wants to connect to an https server, but the WWXML.fixGetMapString() method changes this
                //reset the url string to continue to use the "https" prefix
                String oldServerURI = getServerURI();
                if (oldServerURI != null &&
                        oldServerURI.toLowerCase().startsWith("https") &&
                        !s.toLowerCase().startsWith("https"))
                {
                    s = "https" + s.substring(4);
                }

                serverURI = s;
                setValue(AVKey.SERVICE, s);
            }

            s = capabilities.getServiceInformation().getServiceTitle();
            if (WWUtil.isEmpty(s)) s = "No Name";
            setValue(AVKey.TITLE, s);

            s = capabilities.getServiceInformation().getServiceAbstract();
            setValue(InstallConstants.DESCRIPTION, s);
        } else
        {
            setValue(AVKey.TITLE, null);
            setValue(InstallConstants.DESCRIPTION, null);
        }
    }

    /**
     * Update the WCS info based on the capabilities retrieved from the server.
     */
    private void setWCSInfoParams(WCS100Capabilities capabilities)
    {
        if (capabilities != null)
        {
            String s = capabilities.getService().getName();
            if (WWUtil.isEmpty(s)) s = "No Name";
            setValue(AVKey.TITLE, s);

            s = capabilities.getService().getDescription();
            setValue(InstallConstants.DESCRIPTION, s);

            setLayers(createWCSLayerInfoList(capabilities));
        } else
        {
            setValue(AVKey.TITLE, null);
            setValue(InstallConstants.DESCRIPTION, null);
        }
    }

    /**
     * Create a list of layers from the offerings of a WMS server.
     *
     * @return List of layers provided by the server. Returns {@code null} if capabilities is {@code null}.
     * @since 1.43
     */
    private List<LayerInfo> createWMSLayerInfoList(WMSCapabilities caps)
    {
        if (caps == null) return null;

        // Gather up all the named layers and make a LayerInfo for each.
        List<WMSLayerCapabilities> namedLayerCaps = caps.getNamedLayers();
        if (namedLayerCaps == null) return null;

        // Build a list of LayerInfo references from each combination of layer and style.
        List<LayerInfo> layerInfoList = new ArrayList<>();
        for (WMSLayerCapabilities layerCaps : namedLayerCaps)
        {
            Set<WMSLayerStyle> styles = layerCaps.getStyles();
            if (styles == null || styles.isEmpty())
            {
                layerInfoList.add(new LayerInfo(this, layerCaps, null));
            } else
            {
                for (WMSLayerStyle styleCaps : styles) layerInfoList.add(new LayerInfo(this, layerCaps, styleCaps));
            }
        }

        // Determine which LayerInfo references are installed, and mark each with the property INSTALLED.
//        Set<LayerInfo> installedLayerList = wmsInfo.getInstalledLayers();
//        for (LayerInfo layerInfo : layerInfoList)
//        {
//            boolean isInstalled = false;
//            for (LayerInfo installedInfo : installedLayerList)
//            {
//                if (layerInfo.equals(installedInfo))
//                {
//                    isInstalled = true;
//                    break;
//                }
//            }
//            layerInfo.setValue(InstallConstants.INSTALLED_IN_APP, isInstalled);
//        }

        return layerInfoList;
    }

    /**
     * Create a list of layers from the offerings of a WCS server.
     *
     * @return List of layers provided by the server. Returns {@code null} if capabilities is {@code null}.
     * @since 1.43
     */
    private List<LayerInfo> createWCSLayerInfoList(WCS100Capabilities caps)
    {
        if (caps == null) return null;

        List<WCS100CoverageOfferingBrief> coverages = caps.getContentMetadata().getCoverageOfferings();
        if (coverages == null) return null;

        List<LayerInfo> layerInfoList = new ArrayList<>(coverages.size());
        for (WCS100CoverageOfferingBrief coverage : coverages)
        {
            LayerInfo layerInfo = new LayerInfo(this, coverage);
            layerInfoList.add(layerInfo);
        }

        return layerInfoList;
    }
}
