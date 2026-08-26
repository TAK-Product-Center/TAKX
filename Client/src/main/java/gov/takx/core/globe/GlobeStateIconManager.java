package gov.takx.core.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObjectManager;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.globe.IStateIcon;
import gov.takx.api.globe.IStateIconManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.globe.CardinalPoint;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeStateIconManager implements IStateIconManager
{
    private static final double DEFAULT_STATE_ICON_SCALE = 2.0 / 3.0; // Size of state icons relative to the map object icon.

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    @Inject
    private IExtendedGlobeManager globeManager;

    private IGlobeMapObjectManager globeMapObjectManager;

    // Individual state icons by compass position (8 values: N, S, E, W, NE, NW, SE, SW)
    private final Map<CardinalPoint, GlobeStateIcon> icons = Collections.synchronizedMap(new EnumMap<>(CardinalPoint.class));

    private final IMapObject mapObject; // The map object we're handling
    private final boolean forPlayback; // True if we're handling a playback marker

    private double scale = DEFAULT_STATE_ICON_SCALE; // The scale of the state icons relative to the primary icon.
    private double spacing = 1; // How far the state icon will be placed away from the MO icon
    private boolean visible = true;
    private boolean updatingVisuals;

    /**
     * @param mapObject associated map object
     * @param forPlayback Pass {@code true} if this manager is handling a playback marker
     */
    public GlobeStateIconManager(IMapObject mapObject, boolean forPlayback)
    {
        this.mapObject = mapObject;
        this.forPlayback = forPlayback;
    }

    @PostConstruct
    private void postConstruct()
    {
        globeMapObjectManager = globeManager.getPrimaryGlobeDelegate().getGlobeMapObjectManager();
    }

    @Override
    public boolean hasStateIconAtPosition(CardinalPoint point)
    {
        return icons.containsKey(point);
    }

    @Override
    public IStateIcon getStateIcon(CardinalPoint point)
    {
        return icons.get(point);
    }

    @Override
    public IStateIcon addStateIcon(CardinalPoint point, @Nonnull BufferedImage image)
    {
        return addStateIcon(point, new GlobeStateIcon(image, getStateIconSize(), this::notifyStateChanged));
    }

    @Override
    public IStateIcon addStateIcon(CardinalPoint point, @Nonnull URL imagePathURL)
    {
        return addStateIcon(point, new GlobeStateIcon(imagePathURL, getStateIconSize(), this::notifyStateChanged));
    }

    @Override
    public IStateIcon addStateIcon(CardinalPoint point, String text)
    {
        return addStateIcon(point, new GlobeStateIcon(text, getStateIconSize(), this::notifyStateChanged));
    }

    @Override
    public IStateIcon removeStateIcon(CardinalPoint point)
    {
        final var stateIcon = icons.remove(point);
        notifyStateChanged();
        return stateIcon;
    }

    @Override
    public void removeAllStateIcons()
    {
        icons.clear();
        notifyStateChanged();
    }

    @Override
    public List<CardinalPoint> findPositions(boolean open)
    {
        // iterate over all positions and store based on open parameter
        return Arrays.stream(CardinalPoint.values())
                .filter(point -> open ^ icons.containsKey(point))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible != this.visible)
        {
            this.visible = visible;
            notifyStateChanged();
        }
    }

    @Override
    public boolean needsUpdate()
    {
        return visible || mapObject.isVisible();
    }

    @Override
    public void setScale(double scale)
    {
        if (Double.compare(scale, this.scale) != 0)
        {
            this.scale = Math.max(scale, 0.1);
            notifyStateChanged();
        }
    }

    @Override
    public void setSpacing(double spacing) throws IllegalArgumentException
    {
        if (spacing <= 0) throw new IllegalArgumentException("Spacing must be > 0");

        if (Double.compare(spacing, this.spacing) != 0)
        {
            this.spacing = spacing;
            notifyStateChanged();
        }
    }

    @Override
    public double getSpacing()
    {
        return spacing;
    }

    @Override
    public void updateVisuals()
    {
        setVisible(mapObject.isVisible());

        final int stateIconSize = getStateIconSize();

        updatingVisuals = true;
        synchronized (icons)
        {
            icons.values().stream().filter(Objects::nonNull).forEach(icon -> icon.setCurrentSize(stateIconSize));
        }
        updatingVisuals = false;
    }

    /**
     * Add the given state icon to our map using the given cardinal point, and send a state change notification.
     *
     * @param stateIcon the state icon
     * @param point Cardinal point at which to add the icon
     * @return created state icon
     * @since 3.5 (signature change in 5.7)
     */
    private IStateIcon addStateIcon(CardinalPoint point, GlobeStateIcon stateIcon)
    {
        icons.put(point, stateIcon);
        notifyStateChanged();
        return stateIcon;
    }

    /**
     * Handle a change in our state or one of our associated state icons.
     * @since 3.5
     */
    private void notifyStateChanged()
    {
        if (!updatingVisuals) uiEventBus.post(new StateIconsChanged(getGlobeMapObject()));
    }

    /**
     * Compute the size that a state icon should be relative to the map object icon.
     *
     * @return The size that a state icon should be.
     */
    private int getStateIconSize()
    {
        final var globeMapObject = getGlobeMapObject();

        // If we don't yet have a Globe icon, then use a default size
        int imageHeight = globeMapObject != null
                ? (int) globeMapObject.getCurrentImageSize().getHeight()
                : ImageUtils.DEFAULT_ICON_HEIGHT;

        return (int) Math.round(imageHeight * scale);
    }

    /**
     * Get the Globe map object.  Since GMOs for playback are recreated every time the playback state transitions from
     * live to playback, we can't cache this reference.
     *
     * @since 5.7
     */
    private IGlobeMarkerMapObject getGlobeMapObject()
    {
        return (IGlobeMarkerMapObject) globeMapObjectManager.getGlobeMapObject(mapObject.getKey(), forPlayback);
    }
}
