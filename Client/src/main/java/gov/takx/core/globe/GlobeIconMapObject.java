package gov.takx.core.globe;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.drag.DragContext;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.icons.IconSetConstants;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ExtendedLocation;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObject.TooltipVisibility;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Representation of a map object's icon on the globe.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeIconMapObject extends AGlobeMapObject implements IGlobeMarkerMapObject, IGlobeInteractable
{
    public static final String SHOW_TOOLTIP = "ShowToolTip";
    public static final String BACKGROUND_IMAGE = "BackgroundImage";
    public static final String BACKGROUND_SCALE = "BackgroundScale";
    public static final String IMAGE_SOURCE = "ImageSource";
    public static final String SIZE = "Size";
    public static final String TOOLTIP_TEXT = "ToolTipText";
    /** @since 5.6 */
    public static final String SELECTED = "Selected";

    private static final String TOOLTIP_ALWAYS_ON = "on";
    private static final String TOOLTIP_DEFAULT = "default";

    @Inject
    private IExtendedGlobeManager globeManager;

    protected final PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);

    protected boolean tooltipVisibilityForced; // True when tooltip visibility is forced to on or off

    private Dimension currentSize;
    private Object imageSource;
    private boolean dragEnabled;
    private Object backgroundImage;
    private double backgroundScale = 1;
    private boolean renderingOnSurface;
    private String tooltipText;
    private boolean showTooltip;
    private boolean labelOnly;
    private boolean alwaysOnTop;

    GlobeIconMapObject(IMapObject mapObject)
    {
        super(mapObject);
    }

    @PostConstruct
    private void postConstruct()
    {
        final var mapObject = getMapObject();

        applyTooltipVisibility(mapObject.getTooltipVisibility());
        mapObject.addPropertyChangeListener(IMapObject.TOOLTIP_VISIBILITY, pce -> applyTooltipVisibility((TooltipVisibility) pce.getNewValue()));

        final String cotType = mapObject.getCotType();

        // For evac markers, we show both an icon and a label
        final boolean isEvacMarker = CotTypes.EVAC_POINT.equals(cotType);

        // For Spot Markers, marked as LABEL, we only display the label and not the icon
        final String iconsetPath = mapObject.getMetaString(StandardAttributeNames.ICONSET_PATH, "");
        labelOnly = CotTypes.SPOT_MARKER.equals(cotType) && iconsetPath.endsWith(IconSetConstants.LABEL);

        if (labelOnly || isEvacMarker)
        {
            tooltipText = mapObject.getTitle();
            showTooltip = true;
            tooltipVisibilityForced = true;
        }

        refreshIcons();
    }

    /**
     * Modify the image as needed, or just return the original if no changes are needed.
     *
     * @param originalImage The original image
     * @return The modified image (maybe the same if no updates were needed)
     */
    protected BufferedImage makeMapObjectImage(@Nonnull BufferedImage originalImage)
    {
        return originalImage;
    }

    @Override
    public void refreshIcons()
    {
        final IMapObject mapObject = getMapObject();

        double iconScale = mapObject.getIconScale();
        double globalIconScale = UserPreferenceEntry.GLOBAL_ICON_SCALE.getDoubleValue();

        // Make sure we have the latest image.
        BufferedImage currentImage = makeMapObjectImage(mapObject.getIconImage());

        final double scaleMultiplier = iconScale * globalIconScale;

        final int scaledWidth = (int) (ImageUtils.DEFAULT_ICON_WIDTH * scaleMultiplier);
        final int scaledHeight = (int) (ImageUtils.DEFAULT_ICON_HEIGHT * scaleMultiplier);
        currentSize = new Dimension(scaledWidth, scaledHeight);

        // Set the image source and then refresh the marker
        setImage(currentImage);
    }

    @Override
    public Dimension getCurrentImageSize()
    {
        return currentSize;
    }

    @Override
    public boolean isRenderingOnSurface()
    {
        return renderingOnSurface;
    }

    @Override
    public void setRenderingOnSurface(boolean renderingOnSurface)
    {
        boolean oldValue = this.renderingOnSurface;
        this.renderingOnSurface = renderingOnSurface;

        // If renderingOnSurface has changed, the location needs to be updated to include/ignore the elevation.
        if (oldValue != renderingOnSurface)
        {
            setLocation(getLocation());
        }
    }

    @Override
    public void setSelected(boolean selected)
    {
        propertyChangeSupport.updateAndFire(SELECTED, isSelected(), selected, () -> super.setSelected(selected));
    }

    @Nonnull
    @Override
    public ILocation getLocation()
    {
        return getMapObject().getLocation();
    }

    @Override
    public String getName()
    {
        return getMapObject().getName();
    }

    @Override
    public boolean isDragEnabled()
    {
        return dragEnabled;
    }

    @Override
    public void setDragEnabled(boolean dragEnabled)
    {
        this.dragEnabled = dragEnabled;
    }

    @Override
    public void drag(DragContext dragContext)
    {
        PickedObject terrainObject = dragContext.getSceneController().getDrawContext().getPickedObjects().getTerrainObject();
        Position pos = terrainObject != null && terrainObject.getPosition() != null
                ? terrainObject.getPosition()
                : dragContext.getSceneController().getView().computePositionFromScreenPoint(dragContext.getPoint().x, dragContext.getPoint().y);

        final var mapObject = getMapObject();
        final var dragState = dragContext.getDragState();

        mapObject.setMetaBoolean(StandardAttributeNames.DRAGGING, !dragState.equals(AVKey.DRAG_ENDED));

        if (pos != null)
        {
            final var location = WWUtils.positionToRaptorLocation(pos);

            // When we start a drag operation, use an ExtendedLocation so it is recorded as an undoable operation
            final var newLocation = AVKey.DRAG_BEGIN.equals(dragState)
                    ? new ExtendedLocation(location, true, false)
                    : location;
            mapObject.getController().sendLocationChangeMessage(newLocation);
        }
    }

    @Override
    public Object getBackgroundImage()
    {
        return backgroundImage;
    }

    @Override
    public void setBackgroundImage(Object o)
    {
        propertyChangeSupport.updateAndFire(BACKGROUND_IMAGE, backgroundImage, o, () -> backgroundImage = o);
    }

    @Override
    public double getBackgroundScale()
    {
        return backgroundScale;
    }

    @Override
    public void setBackgroundScale(double v)
    {
        propertyChangeSupport.updateAndFire(BACKGROUND_SCALE, backgroundScale, v, () -> backgroundScale = v);
    }

    protected void setImage(BufferedImage image)
    {
        setImageSource(image);
    }

    @Override
    public void setLocation(ILocation location)
    {
        // Do nothing for markers, the features monitor the MO location not ours
    }

    public void setImageSource(Object imageSource)
    {
        propertyChangeSupport.updateAndFire(IMAGE_SOURCE, this.imageSource, imageSource, () -> this.imageSource = imageSource);
    }

    @Override
    public Object getImageSource()
    {
        return imageSource;
    }

    @Override
    public String getToolTipText()
    {
        if (tooltipText != null) return tooltipText;

        StringBuilder toolTip = new StringBuilder(getMapObject().getName());

        if (getMapObject() instanceof IIconMapObject imo)
        {
            List<IIconMapObject> containedCIMOs = imo.getContainedMapObjects();
            if (!containedCIMOs.isEmpty())
            {
                toolTip.append(" (");
                boolean firstTime = true;
                for (IIconMapObject it : containedCIMOs)
                {
                    if (!firstTime) toolTip.append(", ");
                    IGlobeMapObject gmo = globeManager.getPrimaryGlobeDelegate().getGlobeMapObjectManager().getGlobeMapObject(it.getKey());
                    if (gmo instanceof IGlobeMarkerMapObject globeMarkerMapObject) toolTip.append(globeMarkerMapObject.getToolTipText());
                    firstTime = false;
                }
                toolTip.append(")");
            }
        }
        return toolTip.toString();
    }

    @Override
    public boolean isViewable()
    {
        return super.isViewable() && (!(getMapObject() instanceof IIconMapObject imo) || !imo.isContained());
    }

    @Override
    public void setToolTipText(String tooltipText)
    {
        propertyChangeSupport.updateAndFire(TOOLTIP_TEXT, this.tooltipText, tooltipText, () -> this.tooltipText = tooltipText);
    }

    @Override
    public boolean isAlwaysOnTop()
    {
        return alwaysOnTop;
    }

    @Override
    public void setAlwaysOnTop(boolean alwaysOnTop)
    {
        this.alwaysOnTop = alwaysOnTop;
    }

    @Override
    public boolean isShowToolTip()
    {
        return showTooltip;
    }

    @Override
    public void setShowToolTip(boolean showTooltip)
    {
        // When tooltip visibility is "forced", we ignore the change.
        if (!tooltipVisibilityForced) internalSetShowTooltip(showTooltip);
    }

    /**
     * Directly set the show tooltip property without checking if visibility is forced.
     * @since 5.6
     */
    private void internalSetShowTooltip(boolean showTooltip)
    {
        propertyChangeSupport.updateAndFire(SHOW_TOOLTIP, this.showTooltip, showTooltip, () -> this.showTooltip = showTooltip);
    }

    /**
     * Update our state based on the Map Object's tooltip visibility setting and preference setting.
     * @since 5.6
     */
    private void applyTooltipVisibility(TooltipVisibility tooltipVisibility)
    {
        if (tooltipVisibility != TooltipVisibility.DEFAULT)
        {
            tooltipVisibilityForced = true;
            internalSetShowTooltip(tooltipVisibility == TooltipVisibility.ON);
        } else
        {
            final String tooltipPref = PreferenceEntry.MAP_OBJECT_TOOLTIPS.getStringValue();
            tooltipVisibilityForced = !TOOLTIP_DEFAULT.equals(tooltipPref);
            internalSetShowTooltip(TOOLTIP_ALWAYS_ON.equals(tooltipPref));
        }
    }

    @Override
    public void recalculateTooltipState()
    {
        applyTooltipVisibility(getMapObject().getTooltipVisibility());
    }

    @Override
    public boolean isTooltipVisibilityForced()
    {
        return tooltipVisibilityForced;
    }

    @Override
    public boolean isLabelOnly()
    {
        return labelOnly;
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties)
    {
        properties.forEach(property -> propertyChangeSupport.addPropertyChangeListener(property, listener));
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties)
    {
        properties.forEach(property -> propertyChangeSupport.removePropertyChangeListener(property, listener));
    }
}
