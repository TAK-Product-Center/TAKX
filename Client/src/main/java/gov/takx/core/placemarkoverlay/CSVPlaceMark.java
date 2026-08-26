package gov.takx.core.placemarkoverlay;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.WWTexture;
import gov.nasa.worldwind.util.WWUtil;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.placemarkoverlay.gui.PlacemarkOverlayPropertiesPanel;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class CSVPlaceMark extends PointPlacemark
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private PlacemarkOverlayManager placemarkOverlayManager;

    private final List<PropertyChangeListener> propertyChangeListeners;
    private final Map<String, String> csvMap;
    private final PlacemarkOverlay parent;
    private boolean selected;
    private WWTexture selectedTexture;
    public static final String COMMENTS = "Comments";

    public CSVPlaceMark(PlacemarkOverlay parent,
                        Position position,
                        Map<String, String> csvMap)
    {
        super(position);
        propertyChangeListeners = new ArrayList<>();
        this.parent = parent;
        this.csvMap = csvMap;
    }

    public void updateValue(String key, String value)
    {
        String oldValue = csvMap.get(key);

        if (!equal(oldValue, value))
        {
            csvMap.put(key, value);
            boolean isDisplayNameChange = key.equals(parent.getDisplayNameHeader());

            if (key.equals(parent.getLatitudeHeader()) || key.equals(parent.getLongitudeHeader()))
            {
                final String coordString = csvMap.get(getLatColumn()) + "," + csvMap.get(getLonColumn());
                ILocation location = CoordinateSystem.computeLatLonFromString(coordString);
                if (location != null)
                {
                    setPosition(Position.fromDegrees(location.getLatitude(), location.getLongitude()));
                } else
                {
                    logger.warn("Unparsable location string: {}", coordString);
                }
            } else
            {
                Collection<String> tooltipColumns = parent.getTooltipColumns();

                if (isDisplayNameChange || (tooltipColumns != null && tooltipColumns.contains(key)))
                {
                    String newTooltip = PlacemarkOverlayUtils.generateTooltip(getDisplayName(), tooltipColumns, csvMap);
                    setValue(AVKey.ROLLOVER_TEXT, newTooltip);
                    setValue(AVKey.HOVER_TEXT, newTooltip);
                }
            }

            // Special handling for display name changes.
            if (isDisplayNameChange)
            {
                labelText = value;
                propertyChanged(IMapObject.NAME, oldValue, value);
            } else
            {
                propertyChanged(parent.getDisplayNameHeader(), oldValue, value);
            }
        }
    }

    public String getDisplayName()
    {
        return csvMap.get(parent.getDisplayNameHeader());
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener propertyChangeListener)
    {
        propertyChangeListeners.add(propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener propertyChangeListener)
    {
        propertyChangeListeners.remove(propertyChangeListener);
    }

    public String getLatColumn()
    {
        return parent.getLatitudeHeader();
    }

    public String getLonColumn()
    {
        return parent.getLongitudeHeader();
    }

    public String getImageColumn()
    {
        return parent.getImageColumn();
    }

    public String addImageColumn()
            throws IOException
    {
        // Add the column to the layer.
        return parent.addImageColumn();
    }

    public Map<String, String> getCSVMap()
    {
        return csvMap;
    }

    public List<String> getHeaders()
    {
        return parent.getHeaders();
    }

    public String getDirectory()
    {
        return parent.getDirectory();
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    public PlacemarkOverlayPropertiesPanel getPropertiesPanel()
    {
        PlacemarkOverlayPropertiesPanel propertiesPanel = placemarkOverlayManager.getPropertiesPanel();
        propertiesPanel.setSelectedCSVPlacemark(this, parent.isEditable());
        return propertiesPanel;
    }

    /**
     * This is a hack to show a selected pin texture.  A different texture is returned depending on whether this point
     * placemark is currently selected
     *
     * @param attrs attributes for this point placemark
     * @return the current texture
     */
    @Override
    protected WWTexture chooseTexture(PointPlacemarkAttributes attrs)
    {
        if (!WWUtil.isEmpty(attrs.getImageAddress()))
        {
            if (!selected)
            {
                WWTexture texture = textures.get(attrs.getImageAddress());
                if (texture != null)
                {
                    return texture;
                }

                URL localUrl = WorldWind.getDataFileStore().requestFile(attrs.getImageAddress());
                if (localUrl != null)
                {
                    texture = new BasicWWTexture(localUrl);
                    textures.put(attrs.getImageAddress(), texture);
                    return texture;
                }
            } else
            {
                //get/create a selected (outlined) version of this texture
                if (selectedTexture != null)
                {
                    return selectedTexture;
                }

                URL localUrl = WorldWind.getDataFileStore().requestFile(attrs.getImageAddress());

                if (localUrl != null)
                {
                    BufferedImage image = ImageUtils.getImage(localUrl, true);
                    //noinspection ConstantConditions - will be the image at the URL or the default
                    BufferedImage selectedImage = GraphicsUtils.createSelectedImage(image);
                    selectedTexture = new BasicWWTexture(selectedImage);

                    return selectedTexture;
                }
            }
        }

        return super.chooseTexture(attrs);
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }

    /**
     * Notify listeners that one of this placemark's values has changed.
     *
     * @param id       The value's id.
     * @param oldValue The old value.
     * @param newValue The new value.
     */
    private void propertyChanged(String id, String oldValue, String newValue)
    {
        PropertyChangeEvent evt = new PropertyChangeEvent(this, id, oldValue, newValue);

        for (PropertyChangeListener pcl : propertyChangeListeners)
        {
            pcl.propertyChange(evt);
        }
    }

    /**
     * Determine if two, potentially {@code null}, Strings are equal.
     *
     * @param str1 String 1.
     * @param str2 String 2.
     * @return {@code true} iff the Strings are equal.
     */
    private static boolean equal(String str1, String str2)
    {
        return (str1 == null ? str2 == null : str1.equals(str2));
    }
}
