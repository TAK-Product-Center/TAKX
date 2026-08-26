package gov.takx.api.geofence;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.MapItem;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.platform.utils.AffiliationUtils;
import gov.takx.platform.utils.AffiliationUtils.AffiliationFilter;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.function.Predicate;

/**
 * Defines the types of map items that are monitored by a given GeoFence.  Map items are matched according to their
 * Affiliation, CoT type, and other metadata attached to them.
 *
 * @see GeoFence
 * @see Affiliation
 * @see AffiliationUtils
 * @since 5.3
 */
public enum MonitoredType
{
    /**
     * Matches the self marker, or any marker having a friendly, neutral, hostile, or unknown affiliation.
     */
    All(AffiliationFilter.FRIENDLY
            .or(AffiliationFilter.HOSTILE)
            .or(AffiliationFilter.NEUTRAL)
            .or(AffiliationFilter.UNKNOWN)),

    /**
     * Matches the self marker, or any marker having a friendly affiliation and has a metadata value for {@link StandardAttributeNames#TEAM};
     */
    TAKUsers(mapItem -> CotTypes.SELF.equals(mapItem.getCotType()) ||
            AffiliationFilter.FRIENDLY.and(m -> m.hasMetaValue(StandardAttributeNames.TEAM)).test(mapItem)),

    /**
     * Matches the self marker, or any marker having a friendly affiliation.
     */
    Friendly(AffiliationFilter.FRIENDLY),

    /**
     * Matches any marker having a hostile affiliation.
     */
    Hostile(AffiliationFilter.HOSTILE),

    /**
     * Matches any marker, matching is further constrained to a list of selected map items, but that matching is handled elsewhere.
     */
    Custom(mapItem -> true);

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Predicate<MapItem> filter;

    MonitoredType(Predicate<MapItem> filter)
    {
        this.filter = filter;
    }

    /**
     * Determine if a given map item matches this type, according to CoT type and other metadata.
     *
     * @param item Map item to test
     * @return {@code true} if it matches this types configuration
     */
    public boolean matches(@Nonnull MapItem item)
    {
        return filter.test(item);
    }

    /**
     * @param name name to look up
     * @return The {@link MonitoredType} whose name is {@code name}; defaults to {@link #All} if not found
     * @see #valueOf(String)
     */
    public static MonitoredType forName(@Nonnull String name)
    {
        try
        {
            return valueOf(name);
        } catch (IllegalArgumentException e)
        {
            logger.warn("Invalid GeoFence Monitored Type '{}'; defaulting to '{}'", name, All.name());
            return All;
        }
    }
}
