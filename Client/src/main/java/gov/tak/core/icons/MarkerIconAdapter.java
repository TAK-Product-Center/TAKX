package gov.tak.core.icons;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.Role;
import gov.tak.api.icons.IMarkerIconAdapterRegistry;
import gov.tak.api.icons.IconAdapter;
import gov.tak.api.icons.PredefinedCotIconAdapter;
import gov.tak.api.icons.RoleIconColorizer;
import gov.tak.api.icons.TeamColorMapper;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.cot.CotMarkerRefresher;
import gov.tak.core.comms.roles.RolesManager;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static gov.tak.api.util.StandardAttributeNames.ICONSET_PATH;
import static gov.tak.api.util.StandardAttributeNames.READINESS;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Service responsible for "adapting" a Marker's CoT type to an appropriate icon.
 * Plugins can register as additional adapters, and they are given priority over the default handling provided here.
 *
 * @since 3.0
 */
@ApplicationScoped
public class MarkerIconAdapter implements IMarkerIconAdapterRegistry, IconAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Cache of colorized icons
    private static final Map<String, BufferedImage> COLORIZED_ICONS = new ConcurrentHashMap<>();

    // List of Attributes to watch (they affect team icon adaptation)
    private static final List<String> ATTRS_TO_WATCH = List.of(
            StandardAttributeNames.IMPORTING, StandardAttributeNames.ATAK_ROLE_TYPE,
            StandardAttributeNames.TEAM, StandardAttributeNames.HOW, StandardAttributeNames.USE_COT_TYPE_ICON);

    private final IPreferenceMonitor<Boolean> useCotTypeIcon = PreferenceEntry.USE_COT_TYPE.newBooleanMonitor(this::cotIconPropertyChanged);
    private final Map<String, PropertyListener> listeners = new ConcurrentHashMap<>();
    private final List<IconAdapter> iconAdapters = new CopyOnWriteArrayList<>();
    private final PredefinedCotIconAdapter predefinedCotIconAdapter;
    private final Mil2525IconAdapter mil2525IconAdapter;
    private final RolesManager rolesManager;
    private final IMapObjectManager mapObjectManager;
    private final boolean underTest;

    private boolean projectFinishedOpening;

    @Inject
    MarkerIconAdapter(PredefinedCotIconAdapter predefinedCotIconAdapter, Mil2525IconAdapter mil2525IconAdapter,
                      RolesManager rolesManager, IMapObjectManager mapObjectManager, @PlatformUnderTest boolean underTest)
    {
        this.predefinedCotIconAdapter = predefinedCotIconAdapter;
        this.mil2525IconAdapter = mil2525IconAdapter;
        this.rolesManager = rolesManager;
        this.underTest = underTest;
        this.mapObjectManager = mapObjectManager;
    }

    /**
     * Stop this service, unregister and release all listeners and adapters.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        dispose();
    }

    private void cotIconPropertyChanged(boolean ignored)
    {
        List<? extends IMapObject> mapObjects = mapObjectManager.getMapObjects().stream().toList();

        for (IMapObject mapObject : mapObjects)
        {
            if (!StringUtils.isEmpty(mapObject.getMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, ""), true) &&
                    mapObject instanceof Marker marker)
            {
                marker.setMetaString(StandardAttributeNames.LAST_TEAM_ICON_AND_COLOR, ""); // needed to allow toggle back to skittle icon
                adapt(marker);
                if (marker.getMetaBoolean(StandardAttributeNames.STALE, false)) CotMarkerRefresher.colorStaleIcon(marker);
            }
        }
    }

    private void projectOpened(@Observes ProjectOpened ignored)
    {
        projectFinishedOpening = true;
    }

    private void projectClosing(@Observes ProjectClosing ignored)
    {
        projectFinishedOpening = false;
    }

    @Override
    public void addIconAdapter(IconAdapter iconAdapter)
    {
        iconAdapters.add(iconAdapter);
    }

    @Override
    public void removeIconAdapter(IconAdapter iconAdapter)
    {
        iconAdapters.remove(iconAdapter);
    }

    @Override
    public boolean adapt(@Nonnull Marker marker)
    {
        // ignore if not opened
        if (!underTest && !projectFinishedOpening) return false;

        final boolean adaptMarkerIcon = marker.getMetaBoolean(StandardAttributeNames.ADAPT_MARKER_ICON, true);
        final String cotType = marker.getCotType();

        logger.debug("adapt {}, type {}, adapt_marker_icon {}", marker, cotType, adaptMarkerIcon);

        // If the marker is tagged with adapt_marker_icon set false, then skip this process
        if (!adaptMarkerIcon) return false;

        // For now, we don't adapt markers that don't have a CoT type, unless they have a user icon set or mil-sym defined
        if (CotTypes.EMPTY.equals(cotType)
                && StringUtils.isEmpty(marker.getMetaString(ICONSET_PATH, ""))
                && StringUtils.isEmpty(marker.getAttributeSet().getStringAttribute(StandardAttributeNames.MIL_SYM, "")))
        {
            return false;
        }

        if (doAdapt(marker, this::applyAdapters)) return true;
        if (doAdapt(marker, this::adaptByReadiness)) return true;
        if (doAdapt(marker, this::adaptByTeam)) return true;
        if (doAdapt(marker, predefinedCotIconAdapter::adapt)) return true;
        if (doAdapt(marker, mil2525IconAdapter::adapt)) return true;

        return doAdapt(marker, this::adaptAsUnknown);
    }

    /**
     * Attempt to perform an adaptation using the given adapter method.
     * If the adapter succeeded, then we reset {@link StandardAttributeNames#ADAPT_MARKER_ICON} so we don't prevent
     * future adaptation (e.g., if the CoT type is changed).
     *
     * @param marker Marker to process
     * @param adapter Adapter method to invoke
     * @return true if the marker was processed and an icon was supplied by the adapter
     * @since 5.1
     */
    private boolean doAdapt(Marker marker, Function<Marker, Boolean> adapter)
    {
        final boolean adapted = adapter.apply(marker);
        if (adapted) marker.setMetaBoolean(StandardAttributeNames.ADAPT_MARKER_ICON, true);

        return adapted;
    }

    /**
     * Apply registered adapters.
     *
     * @param marker Marker to adapt
     * @return true if the marker was processed and an icon was supplied by any of the adapters
     */
    private boolean applyAdapters(Marker marker)
    {
        for (IconAdapter iconAdapter : iconAdapters)
        {
            try
            {
                if (iconAdapter.adapt(marker)) return true;
            } catch (Exception e)
            {
                logger.error("Registered adapter failed: {}", iconAdapter, e);
            }
        }

        return false;
    }

    /**
     * If 'readiness' has been set on the marker, apply an appropriate icon.
     *
     * @param marker Marker to adapt
     * @return true if the marker was processed and an icon was supplied
     */
    private boolean adaptByReadiness(Marker marker)
    {
        final String type = marker.getCotType();

        if (marker.hasMetaValue(READINESS) && !marker.getMetaBoolean(READINESS, false))
        {
            String iconName = "damaged.png"; // Friendly by default

            if (CotTypes.isHostile(type))
            {
                iconName = "damaged_hostile.png";
            } else if (CotTypes.isNeutral(type))
            {
                iconName = "damaged_neutral.png";
            } else if (CotTypes.isUnknown(type))
            {
                iconName = "damaged_unknown.png";
            }

            marker.setIconImage(UIResources.getUri(TAK_ASSETS_ICONS, iconName));

            return true;
        }

        return false;
    }

    /**
     * If a 'team' role has been set on the marker, apply an appropriate icon.
     *
     * @param marker Marker to adapt
     * @return true if the marker was processed and an icon was supplied
     */
    private boolean adaptByTeam(Marker marker)
    {
        boolean useTypeIcon = marker.getMetaBoolean(StandardAttributeNames.USE_COT_TYPE_ICON, false) || useCotTypeIcon.getCurrentValue();
        if (marker.hasMetaValue(StandardAttributeNames.TEAM) && !useTypeIcon)
        {
            final String colorName = marker.getMetaString(StandardAttributeNames.TEAM, "white");
            final int color = TeamColorMapper.teamToColor(colorName);

            if (logger.isDebugEnabled()) logger.debug("adapt TC: {} ({}) on {}", colorName, Integer.toHexString(color), marker);

            // Only update the icon on the marker if the role or color has changed
            final String iconPath = rolesManager.getIconPathForRole(marker);

            final String customRoleName = marker.getMetaString(StandardAttributeNames.CUSTOM_ROLE, "");
            String roleNameToUse;
            Role role;
            if (!customRoleName.isBlank())
            {
                roleNameToUse = customRoleName;
                String customRoleAbbreviation = marker.getMetaString(StandardAttributeNames.CUSTOM_ROLE_ABBREVIATION, "");
                role = rolesManager.getRole(roleNameToUse, customRoleAbbreviation);
            } else
            {
                roleNameToUse = marker.getMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, "");
                List<Role> roleList = rolesManager.search(roleNameToUse);
                role = !roleList.isEmpty() ? roleList.get(0) : null;
            }

            final BufferedImage iconImage = role == null ? null : GraphicsUtils.copyImage(rolesManager.createIcon(role, iconPath));
            final String iconAndColor = iconPath + (roleNameToUse.isEmpty() ? "" : ":" + roleNameToUse.toLowerCase(Locale.ROOT)) + ":" + color;
            final String lastIconAndColor = marker.getMetaString(StandardAttributeNames.LAST_TEAM_ICON_AND_COLOR, "");

            if (!lastIconAndColor.equals(iconAndColor))
            {
                marker.setMetaString(StandardAttributeNames.LAST_TEAM_ICON_AND_COLOR, iconAndColor);
                marker.setMetaString(StandardAttributeNames.TEAM_ICON_ASSIGNED, iconPath);

                final BufferedImage coloredImage = COLORIZED_ICONS.computeIfAbsent(iconAndColor,
                        k -> {
                            Color alphaColor = new Color(color, true);
                            return iconImage != null ? ImageUtils.colorize(iconImage, alphaColor) : RoleIconColorizer.colorize(iconPath, alphaColor);
                        });

                marker.setExternalIconImage(coloredImage);
            }

            return true;
        } else
        {
            // Clear state since this marker is no longer part of a team
            marker.setMetaString(StandardAttributeNames.LAST_TEAM_ICON_AND_COLOR, null);
        }

        return false;
    }

    /**
     * Apply a default "unknown" icon, based on the marker affiliation.
     *
     * @param marker Marker to adapt
     * @return true
     */
    private boolean adaptAsUnknown(Marker marker)
    {
        marker.setIconImage(CotIconResolver.getUnknownCotIconUri(marker.getCotType()));
        return true;
    }

    /**
     * Adapt if the marker is NOT actively being imported
     */
    private void adaptIfNotImporting(Marker marker)
    {
        final boolean importing = marker.getMetaBoolean(StandardAttributeNames.IMPORTING, false);
        logger.debug("adaptIfNotImporting {} : {}", importing, marker);
        if (!importing) adapt(marker);
    }

    /**
     * Notified when a new map object is added.  Start watching for changes of the CoT type and attributes.
     */
    @VisibleForTesting
    void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        // We only care about Markers
        final IMapObject mapObject = event.getMapObject();
        if (mapObject instanceof Marker marker)
        {
            listeners.put(marker.getKey(), new PropertyListener(marker));
            adaptIfNotImporting(marker);
        }
    }

    /**
     * Notified when a map object is removed. Stop watching for changes.
     */
    @VisibleForTesting
    void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final IMapObject mapObject = event.getMapObject();
        if (mapObject instanceof Marker)
        {
            final PropertyListener listener = listeners.remove(mapObject.getKey());
            if (listener != null) listener.dispose();
        }
    }

    @Override
    public void dispose()
    {
        listeners.values().forEach(PropertyListener::dispose);
        listeners.clear();

        iconAdapters.forEach(IconAdapter::dispose);
        iconAdapters.clear();
    }

    /**
     * Property change listener to handle a single Marker, watching properties and attributes that affect how
     * a marker is adapted.
     */
    private class PropertyListener implements PropertyChangeListener
    {
        private final Marker marker;

        private PropertyListener(Marker marker)
        {
            this.marker = marker;

            marker.addPropertyChangeListener(IMapObject.COT_TYPE, this);

            final AttributeSet attributeSet = marker.getAttributeSet();
            ATTRS_TO_WATCH.forEach(attr -> attributeSet.addPropertyChangeListener(attr, this));
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            adaptIfNotImporting(marker);
        }

        private void dispose()
        {
            marker.removePropertyChangeListener(IMapObject.COT_TYPE, this);

            final AttributeSet attributeSet = marker.getAttributeSet();
            ATTRS_TO_WATCH.forEach(attr -> attributeSet.removePropertyChangeListener(attr, this));
        }
    }
}
