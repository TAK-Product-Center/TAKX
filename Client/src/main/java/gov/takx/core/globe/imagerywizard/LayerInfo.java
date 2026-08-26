/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.Factory;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100CoverageOfferingBrief;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerStyle;
import gov.nasa.worldwind.util.WWUtil;

import java.util.Objects;

/**
 * Meta-data that describes a layer provided by a Web Map Service (WMS) or a coverage provided by a Web Coverage Service
 * (WCS). In this class the word "layer" is applied to both layers retrieved from a WMS, and coverages retrieved from
 * a WCS.
 *
 */
public class LayerInfo extends AVListImpl
{
    private WMSInfo wmsInfo;
    private String layerName;
    private String layerStyles;

    public LayerInfo(WMSInfo wmsInfo, String layerName, String styles)
    {
        this.wmsInfo = wmsInfo;
        this.setLayerName(layerName);
        this.setStyles(styles);

        // override with proxy URL in case of proxy sever layers being installed layers
        if (wmsInfo.getValue(AVKey.GET_CAPABILITIES_URL) != null) this.setValue(AVKey.GET_CAPABILITIES_URL, wmsInfo.getValue(AVKey.GET_CAPABILITIES_URL));
        if (wmsInfo.getValue(AVKey.GET_MAP_URL) != null) this.setValue(AVKey.GET_MAP_URL, wmsInfo.getValue(AVKey.GET_MAP_URL));
    }

    public LayerInfo(WMSInfo wmsInfo, WMSLayerCapabilities layerCaps, WMSLayerStyle styleCaps)
    {
        this.wmsInfo = wmsInfo;

        this.setLayerName(layerCaps.getName());

        if (styleCaps != null) this.setStyles(styleCaps.getName());

        this.setValue(AVKey.SERVICE, wmsInfo.getServerURI());
        this.setValue(AVKey.TITLE, makeTitle(layerCaps, styleCaps));

        // Override with proxy URL in case of user selecting proxy server (or in the prefs file)
        if (wmsInfo.getValue(AVKey.GET_CAPABILITIES_URL) != null) this.setValue(AVKey.GET_CAPABILITIES_URL, wmsInfo.getValue(AVKey.GET_CAPABILITIES_URL));
        if (wmsInfo.getValue(AVKey.GET_MAP_URL) != null) this.setValue(AVKey.GET_MAP_URL, wmsInfo.getValue(AVKey.GET_MAP_URL));
    }

    /**
     * Create layer info for a WCS coverage.
     *
     * @param wmsInfo  Server that provides the coverage.
     * @param coverage Object that describes this coverage.
     * @since 1.43
     */
    public LayerInfo(WMSInfo wmsInfo, WCS100CoverageOfferingBrief coverage)
    {
        this.wmsInfo = wmsInfo;

        this.setLayerName(coverage.getName());

        this.setValue(AVKey.SERVICE, wmsInfo.getServerURI());
        this.setValue(AVKey.TITLE, coverage.getLabel());

        // Override with proxy URL in case of user selecting proxy server (or in the prefs file)
        if (wmsInfo.getValue(AVKey.GET_CAPABILITIES_URL) != null) this.setValue(AVKey.GET_CAPABILITIES_URL, wmsInfo.getValue(AVKey.GET_CAPABILITIES_URL));
        if (wmsInfo.getValue(AVKey.GET_MAP_URL) != null) this.setValue(AVKey.GET_MAP_URL, wmsInfo.getValue(AVKey.GET_MAP_URL));
    }

    public WMSInfo getWmsInfo()
    {
        return this.wmsInfo;
    }

    public String getLayerName()
    {
        return this.layerName;
    }

    public void setLayerName(String name)
    {
        this.layerName = name;
        this.setValue(AVKey.LAYER_NAMES, name);
        this.firePropertyChange(AVKey.LAYER_NAMES, null, name);
    }

    public String getStyles()
    {
        return this.layerStyles;
    }

    public void setStyles(String styles)
    {
        this.layerStyles = styles;
        this.setValue(AVKey.STYLE_NAMES, styles);
        this.firePropertyChange(AVKey.STYLE_NAMES, null, styles);
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LayerInfo that = (LayerInfo) o;

        if (!Objects.equals(this.layerName, that.layerName)) return false;
        if (!Objects.equals(this.layerStyles, that.layerStyles)) return false;
        return Objects.equals(this.wmsInfo, that.wmsInfo);
    }

    public int hashCode()
    {
        int result = this.wmsInfo != null ? this.wmsInfo.hashCode() : 0;
        result = 31 * result + (this.layerName != null ? this.layerName.hashCode() : 0);
        result = 31 * result + (this.layerStyles != null ? this.layerStyles.hashCode() : 0);
        return result;
    }

    /**
     * Create a World Wind Layer for this imagery.
     *
     * @return New layer.
     */
    public Layer createWWLayer()
    {
        Object caps = wmsInfo.getCapabilities();

        Factory factory = (Factory) WorldWind.createConfigurationComponent(AVKey.LAYER_FACTORY);
        Layer layer = (Layer) factory.createFromConfigSource(caps, this);

        // Assign properties to the layer so it can be identified as derived from this LayerInfo.
        this.assignIdentifiableProperties(layer);

        return layer;
    }

    private void assignIdentifiableProperties(AVList avList)
    {
        avList.setValue(AVKey.SERVICE, this.wmsInfo.getServerURI());
        avList.setValue(AVKey.LAYER_NAMES, this.getLayerName());
        avList.setValue(AVKey.STYLE_NAMES, this.getStyles());
    }

    private static String makeTitle(WMSLayerCapabilities layerCaps, WMSLayerStyle styleCaps)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(!WWUtil.isEmpty(layerCaps.getTitle()) ? layerCaps.getTitle() : layerCaps.getName());

        if (styleCaps != null) sb.append(" : ").append(!WWUtil.isEmpty(styleCaps.getTitle()) ? styleCaps.getTitle() : styleCaps.getName());

        return sb.toString();
    }
}
