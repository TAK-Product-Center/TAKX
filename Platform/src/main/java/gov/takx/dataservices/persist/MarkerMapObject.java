package gov.takx.dataservices.persist;

import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.commons.swing.ImageUtils;
import jakarta.persistence.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A geo-referenced marker supporting a custom icon, text, and styling.
 *
 * @since 3.0
 */
@Entity
public class MarkerMapObject extends PointMapObject implements IMarkerMapObject
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private int style = 0;

    MarkerMapObject()
    {
    }

    public MarkerMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        // By default markers render with a heading indicator
        setStyle(getStyle() | Marker.STYLE_ROTATE_HEADING_MASK);
        setStyle(getStyle() & ~Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);
    }

    @Override
    public int getStyle()
    {
        return style;
    }

    /**
     * Sets the style of the Marker as defined by the STYLE values in the IMarker interface. After
     * this has been invoked the style listeners are notified.
     *
     * @param style a STYLE flag from the IMarker interface.
     */
    @Override
    public void setStyle(int style)
    {
        propertyChangeSupport.updateAndFire(STYLE, this.style, style, () -> this.style = style);
    }

    @Override
    public void setTitle(String title)
    {
        if (title == null) title = "";

        final String name = getName();

        if (!Objects.equals(title, name))
        {
            // XXX - Yuck. Stupid exception for CASEVAC markers
            if (Objects.equals(name, getMetaString(StandardAttributeNames.CALLSIGN, "")))
            {
                setMetaString(StandardAttributeNames.CALLSIGN, title);
            }

            setName(title);
            super.setTitle(title);
        }
    }

    /**
     * @return true since markers support snap to surface
     */
    @Override
    public boolean supportsSnapIconToSurface()
    {
        return true;
    }

    @Override
    public boolean isPreviewImageAvailable()
    {
        return getExternalFile() != null;
    }

    @Override
    public String getPreviewDescription()
    {
        return getTitle();
    }

    @Override
    public File getExternalFile()
    {
        return getAttachments().stream().filter(ImageUtils::isImageTypeReadable).findFirst().orElse(null);
    }
}
