package gov.tak.platform.location.providers;

import gov.tak.api.location.ALocationProvider;
import gov.tak.api.location.ILocationProviderConfiguration;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static gov.tak.api.cot.CotConstants.HOW_MACHINE_GENERATED_PREFIX;
import static gov.tak.api.util.StandardAttributeNames.HOW;

/**
 * Location provider that returns the location of a selected Map Object as the current location.
 *
 * @since 3.0
 */
@ApplicationScoped
public class MapObjectTrackingLocationProvider extends ALocationProvider<MapObjectTrackingLocationProvider.Configuration>
        implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String KEY = "MapObjectTrackingLocationProvider";
    private static final String DISPLAY_NAME = "Tracking Map Object";

    private static final String LOST_MO_FORMAT = "Tracked MapObject removed [%s] : Will now report No Location";
    private static final String RESTORED_MO_FORMAT = "Tracked MapObject restored: [%s] : Will resume tracking";
    private static final List<String> LOCATION_PROPERTIES = List.of(IMapObject.LOCATION, IMapObject.HEADING, IMapObject.SPEED);

    private final IMapObjectManager mapObjectManager;
    private final IUserProxy userProxy;

    private GeoPointMetaData lastMetaData;

    // If the MO we were tracking is removed, this records it
    private IMapObject lostMapObject;

    @Inject
    MapObjectTrackingLocationProvider(IMapObjectManager mapObjectManager, IUserProxy userProxy)
    {
        super(KEY, DISPLAY_NAME, true, new Configuration(null));

        this.mapObjectManager = mapObjectManager;
        this.userProxy = userProxy;
    }

    @Nullable
    @Override
    public GeoPointMetaData getCurrentLocation() throws IllegalStateException
    {
        return currentConfiguration.getGeoPointMetaData();
    }

    @Override
    public ILocationProviderConfiguration showConfiguration(@Nonnull ILocationProviderConfiguration configuration)
    {
        final MapObjectSelector[] mapObjects = mapObjectManager.getMapObjectControllers().stream()
                .map(IMapObjectController::getMapObject)
                .map(MapObjectSelector::new)
                .toArray(MapObjectSelector[]::new);

        final MapObjectSelector currentSelector = configuration.isEmpty()
                ? null
                : new MapObjectSelector(((Configuration) configuration).mapObject);

        final MapObjectSelector selection = (MapObjectSelector) AlwaysOnTopJOptionPane.showInputDialog(null,
                "Please select a Map Object to track", "Select Map Object to Track",
                JOptionPane.QUESTION_MESSAGE, null, mapObjects, currentSelector);

        return selection == null ? null : new Configuration(selection.mapObject);
    }

    @Override
    protected Configuration makeConfigurationFromRestoredState(String restoredState)
    {
        final var mapObject = mapObjectManager.getMapObject(restoredState);
        if (mapObject != null)
        {
            return new Configuration(mapObject);
        } else
        {
            logger.error("Unable to find tracked Map Object: {}", restoredState);
        }
        return null;
    }

    @Override
    public void configure(@Nonnull ILocationProviderConfiguration configuration)
    {
        // Stop listening if we have a configured Map Object
        if (!currentConfiguration.isEmpty())
        {
            currentConfiguration.mapObject.removePropertyChangeListener(LOCATION_PROPERTIES, this);
        }

        super.configure(configuration);

        // Start listening if we now have a configured Map Object
        if (!currentConfiguration.isEmpty())
        {
            currentConfiguration.mapObject.addPropertyChangeListener(LOCATION_PROPERTIES, this);
        }

        lostMapObject = null; // We've been reconfigured, so forget about any "lost" MO
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        GeoPointMetaData metadata = getConfiguration().getGeoPointMetaData();
        fireLocationChanged(lastMetaData, metadata);
        lastMetaData = metadata;
    }

    /**
     * If the MO being removed is the one we're tracking, issue a warning and revert to reporting "no location".
     */
    @SuppressWarnings("unused")
    private void onMapObjectRemoved(@Observes(notifyObserver = Reception.IF_EXISTS) MapObjectRemoved event)
    {
        final IMapObject mapObject = event.getMapObject();

        if (mapObject.equals(currentConfiguration.mapObject))
        {
            userProxy.reportWarning("Lost Map Object", String.format(LOST_MO_FORMAT, mapObject.getName()));

            configure(new Configuration(null)); // So we'll report no location

            lostMapObject = mapObject; // Track this in case it comes back
        }
    }

    /**
     * If the MO being added is the one we lost, resume tracking it.
     */
    @SuppressWarnings("unused")
    private void onMapObjectAdded(@Observes(notifyObserver = Reception.IF_EXISTS) MapObjectAdded event)
    {
        final IMapObject mapObject = event.getMapObject();

        if (mapObject.equals(lostMapObject))
        {
            // It has come back, so we can start tracking it again
            userProxy.reportWarning("Map Object Restored", String.format(RESTORED_MO_FORMAT, mapObject.getName()));

            configure(new Configuration(mapObject));
            lostMapObject = null;
        }
    }

    /**
     * Holds a configuration, a selected Map Object, which may be {@code null}.
     */
    public static final class Configuration implements ILocationProviderConfiguration
    {
        private final IMapObject mapObject;
        private final String displayableString;
        private final String restorableState;

        private Configuration(IMapObject mapObject)
        {
            this.mapObject = mapObject;

            if (mapObject == null)
            {
                displayableString = "not configured";
                restorableState = "";
            } else
            {
                displayableString = mapObject.getName();
                restorableState = mapObject.getKey();
            }
        }

        @Nonnull
        @Override
        public String getRestorableState()
        {
            return restorableState;
        }

        @Nonnull
        @Override
        public String getDisplayableString()
        {
            return displayableString;
        }

        @Override
        public boolean isEmpty()
        {
            return mapObject == null;
        }

        /**
         * @return GeoPointMetaData from the MapObject, or {@code null} when {@link #isEmpty()}
         */
        GeoPointMetaData getGeoPointMetaData()
        {
            if (isEmpty()) return null;

            final var point = mapObject.getGeoPointMetaData();
            final var metaData = point.getMetaData();
            if (!metaData.containsAttribute(HOW)) metaData.setAttribute(HOW, HOW_MACHINE_GENERATED_PREFIX);
            return point;
        }
    }

    /**
     * Holder class to provide the MO name as the display string in a combobox.
     */
    private static final class MapObjectSelector
    {
        private final IMapObject mapObject;
        private final String name;

        private MapObjectSelector(IMapObject mapObject)
        {
            this.mapObject = mapObject;
            name = mapObject.getName();
        }

        @Override
        public String toString()
        {
            return name;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return mapObject.equals(((MapObjectSelector) o).mapObject);
        }

        @Override
        public int hashCode()
        {
            return mapObject.hashCode();
        }
    }
}
