package gov.takx.api.geofence;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Conditions that trigger an alert with respect to a GeoFence.
 *
 * @since 5.3
 * @see GeoFence
 */
public enum Trigger
{
    /** Alerts only upon entry, transitioning from a location outside the geofence to inside. */
    Entry,

    /** Alerts only upon exit, transitioning from a location inside the geofence to outside. */
    Exit,

    /** Alerts upon entry or exit, transitioning in either direction. */
    Both;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * @param name name to look up
     * @return The {@link Trigger} whose name is {@code name}; defaults to {@link #Entry} if not found
     * @see #valueOf(String)
     */
    public static Trigger forName(@Nonnull String name)
    {
        try
        {
            return valueOf(name);
        } catch (IllegalArgumentException e)
        {
            logger.warn("Invalid GeoFence trigger '{}'; defaulting to '{}'", name, Entry.name());
            return Entry;
        }
    }
}
