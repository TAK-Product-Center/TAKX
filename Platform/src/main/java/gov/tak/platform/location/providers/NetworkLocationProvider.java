package gov.tak.platform.location.providers;

import com.atakmap.commoncommo.GenericDataListener;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.location.ALocationProvider;
import gov.tak.api.location.ILocationProviderConfiguration;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static gov.tak.api.cot.detail.DetailConstants.PRECISION_LOCATION;
import static gov.tak.api.cot.detail.DetailConstants.TRACK;
import static gov.tak.api.maps.coords.GeoPointMetaData.ALTITUDE_SOURCE;
import static gov.tak.api.maps.coords.GeoPointMetaData.GEOPOINT_SOURCE;
import static gov.tak.api.maps.coords.GeoPointMetaData.UNKNOWN;

/**
 * Location provider that connects to common commo on a configured port.
 * Listens for CoT encoded location updates.
 *
 * @since 3.0
 */
@ApplicationScoped
public class NetworkLocationProvider extends ALocationProvider<NetworkLocationProvider.Configuration> implements GenericDataListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String KEY = "NetworkLocationProvider";
    private static final String DISPLAY_NAME = "Network Location";

    private static final String UDP_URI_BASE = "udp://0.0.0.0:";
    private static final int DEFAULT_PORT = 4349;

    private final ICommonCommoManager commonCommoManager;

    private GeoPointMetaData currentLocation;

    @Inject
    public NetworkLocationProvider(ICommonCommoManager commonCommoManager)
    {
        super(KEY, DISPLAY_NAME, true, new Configuration(DEFAULT_PORT));

        this.commonCommoManager = commonCommoManager;
    }

    @Nullable
    @Override
    protected NetworkLocationProvider.Configuration makeConfigurationFromRestoredState(String restoredState)
    {
        final int port = Integer.parseInt(restoredState);

        if (port <= 0)
        {
            logger.error("Unable to restore configuration: {}", restoredState);
        } else
        {
            return new Configuration(port);
        }
        return null;
    }

    @Override
    public GeoPointMetaData getCurrentLocation() throws IllegalStateException
    {
        return currentLocation;
    }

    @Override
    public ILocationProviderConfiguration showConfiguration(@Nonnull ILocationProviderConfiguration configuration)
    {
        int port = -1;

        final String originalPortString = configuration.getDisplayableString();
        String portString;
        while ((portString = getListenPort(originalPortString)) != null)
        {
            // If it can be parsed, then we're good
            try
            {
                port = Integer.parseInt(portString);

                if (port <= 0 || port > 65535)
                {
                    AlwaysOnTopJOptionPane.showMessageDialog(null,
                            "Port number must be greater than 0 and less than 65535",
                            "Invalid Port", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
                break;
            } catch (NumberFormatException e)
            {
                AlwaysOnTopJOptionPane.showMessageDialog(null,
                        "Not a valid port number",
                        "Invalid Port", JOptionPane.ERROR_MESSAGE);
            }
        }

        return port <= 0 ? null : new Configuration(port);
    }

    private String getListenPort(String portString)
    {
        return AlwaysOnTopJOptionPane.showInputDialog(
                "Please enter UDP port number to listen for CoT or NMEA location updates\nDefault " + DEFAULT_PORT,
                "Enter Port", portString);
    }

    @Override
    protected void onStart()
    {
        commonCommoManager.addInput(URI.create(UDP_URI_BASE + currentConfiguration.port), true);
        commonCommoManager.addGenericDataListener(this);
    }

    @Override
    protected void onStop()
    {
        commonCommoManager.removeInput(URI.create(UDP_URI_BASE + currentConfiguration.port));
        commonCommoManager.removeGenericDataListener(this);
    }

    @Override
    public void genericDataReceived(byte[] bytes, String remoteEndpointId)
    {
        if (!remoteEndpointId.startsWith("*:" + currentConfiguration.port + ":")) return;

        final String message = new String(bytes, StandardCharsets.UTF_8).trim();

        try
        {
            if (message.charAt(0) != '<') // Ignore any non-xml smelling message we get (like NMEA)
            {
                logger.debug("Ignoring message: {}", message);
                return;
            }

            logger.debug("Parse: {}", message);

            final CotEvent event = CotEvent.parse(message);

            if (event.isValid())
            {
                final GeoPointMetaData oldLocation = currentLocation;

                currentLocation = parseCotEvent(event);

                fireLocationChanged(oldLocation, currentLocation);
            }
        } catch (Exception e)
        {
            logger.error("Error parsing generic data from commo: {}", message, e);
        }
    }

    /**
     * Pull the location and optional track (speed and bearing) data from the CoT event and populate a new
     * {@link GeoPointMetaData} instance from it.
     *
     * @param cotEvent Event to process
     * @return newly created GeoPointMetaData
     */
    private GeoPointMetaData parseCotEvent(CotEvent cotEvent)
    {
        final GeoPointMetaData location = new GeoPointMetaData(new GeoPoint(cotEvent.getPoint()));
        final AttributeSet metaData = location.getMetaData();
        final CotDetail detail = cotEvent.getDetail();

        final CotDetail precisionLocation = detail.getFirstChildByName(PRECISION_LOCATION);
        if (precisionLocation != null)
        {
            location.setGeoPointSource(precisionLocation.getAttribute(GEOPOINT_SOURCE, UNKNOWN));
            location.setAltitudeSource(precisionLocation.getAttribute(ALTITUDE_SOURCE, UNKNOWN));
        }

        final CotDetail track = detail.getFirstChildByName(TRACK);
        if (track != null)
        {
            final String speed = track.getAttribute(DetailConstants.ATTR_SPEED);
            if (speed != null) metaData.setAttribute(StandardAttributeNames.SPEED, Double.parseDouble(speed));

            final String bearing = track.getAttribute(DetailConstants.ATTR_COURSE);
            if (bearing != null) metaData.setAttribute(StandardAttributeNames.BEARING, Double.parseDouble(bearing));
        }

        metaData.setAttribute(StandardAttributeNames.HOW, cotEvent.getHow());

        return location;
    }

    public static final class Configuration implements ILocationProviderConfiguration
    {
        private final int port;
        private final String displayableString;
        private final String restorableState;

        private Configuration(int port)
        {
            if (port <= 0)
            {
                this.port = -1;
                displayableString = "not configured";
                restorableState = "";
            } else
            {
                this.port = port;
                restorableState = displayableString = Integer.toString(port);
            }
        }

        @Override
        public boolean isEmpty()
        {
            return port == -1;
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
    }
}
