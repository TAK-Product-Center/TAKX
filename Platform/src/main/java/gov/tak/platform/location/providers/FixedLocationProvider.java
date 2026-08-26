package gov.tak.platform.location.providers;

import gov.tak.api.cot.CotConstants;
import gov.tak.api.location.ALocationProvider;
import gov.tak.api.location.ILocationProviderConfiguration;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.lang.invoke.MethodHandles;

/**
 * Location provider that always returns a fixed value, which is configurable.
 *
 * @since 3.0
 */
@ApplicationScoped
public class FixedLocationProvider extends ALocationProvider<FixedLocationProvider.Configuration>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String KEY = "FixedLocationProvider";

    private static final String DISPLAY_NAME = "Fixed Location";

    FixedLocationProvider()
    {
        super(KEY, DISPLAY_NAME, true, new Configuration(null), true);
    }

    @Nullable
    @Override
    public GeoPointMetaData getCurrentLocation()
    {
        return currentConfiguration.location;
    }

    @Override
    public ILocationProviderConfiguration showConfiguration(@Nonnull ILocationProviderConfiguration configuration)
    {
        final String originalLocationString = configuration.getDisplayableString();

        ILocation parsedLocation = null;
        String locationString;

        while ((locationString = getUserEnteredLocation(originalLocationString)) != null)
        {
            // If it can be parsed, then we're good
            parsedLocation = CoordinateSystem.computeLatLonFromString(locationString);

            if (parsedLocation != null) break;

            AlwaysOnTopJOptionPane.showMessageDialog(null,
                    "Could not interpret location '" + locationString + "'",
                    "Invalid Location", JOptionPane.ERROR_MESSAGE);
        }

        return parsedLocation == null ? null : new Configuration(parsedLocation);
    }

    private String getUserEnteredLocation(String locationString)
    {
        return AlwaysOnTopJOptionPane.showInputDialog(
                "Please enter the location in as decimal degrees, DM, DMS, or MGRS.", "Enter Location",
                locationString);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The given string must be parseable by {@link GeoPoint#parseGeoPoint(String)}.  If an elevation is
     * included in the the given {@code configurationString}, then it is assumed to be HAE.
     */
    @Override
    public void configureFrom(@Nonnull String configurationString)
    {
        final Configuration configuration = makeConfigurationFromRestoredState(configurationString);
        if (configuration == null) throw new IllegalArgumentException("Invalid location format: '" + configurationString + "'");

        configure(configuration);
    }

    /**
     * Set our configuration from the given location.
     *
     * @param location New location ot report as our location
     * @since 5.2
     */
    public void configureFrom(GeoPoint location)
    {
        configure(new Configuration(location));
    }

    @Override
    protected Configuration makeConfigurationFromRestoredState(String restoredState)
    {
        final var location = GeoPoint.parseGeoPoint(restoredState);

        if (location == null)
        {
            logger.error("Unable to restore configuration: {}", restoredState);
        } else
        {
            return new Configuration(location);
        }
        return null;
    }

    /**
     * Our configuration, holding a fixed location value.
     * <p>
     * Note: The restorable state string contains the elevation in meters HAE.
     */
    public static final class Configuration implements ILocationProviderConfiguration
    {
        private final GeoPointMetaData location;
        private final String restorableState;

        private Configuration(ILocation location)
        {
            if (location == null)
            {
                this.location = null;
                restorableState = "";
            } else
            {
                final var geoPoint = new GeoPoint(location);
                this.location = new GeoPointMetaData(geoPoint);
                this.location.getMetaData().setAttribute(StandardAttributeNames.HOW, CotConstants.HOW_HUMAN_ENTERED);
                restorableState = this.location.toString();
            }
        }

        @Override
        public boolean isEmpty()
        {
            return location == null;
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
            if (isEmpty())
            {
                return "not configured";
            } else
            {
                final GeoPoint geoPoint = location.get();
                final String elevation = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.getValueAs(AltitudeUnits.class)
                        .format(geoPoint.getElevation(), 2);

                // Show the user MSL
                return String.format("%.5f, %.5f elev %s", geoPoint.getLatitude(), geoPoint.getLongitude(), elevation);
            }
        }
    }
}
