package gov.tak.api.comms.takserver.mission.call.sync;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.feign.FeignSyncApi;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to search for files on the server through a variety of filter options.
 *
 * @since 3.5
 */
public class SearchResources extends AbstractEndpointCaller<List<Resource>>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("box", ""),
            paramOptional("circle", ""),
            paramOptional("minAltitude", ""),
            paramOptional("maxAltitude", ""),
            paramOptional("startTime", ""),
            paramOptional("endTime", ""),
            paramOptional("filename", ""),
            paramOptional("name", ""),
            paramOptional("keyword", ""),
            paramOptional("mimetype", ""),
            paramOptional("uid", ""),
            paramOptional("hash", ""),
            paramOptional("mission", ""),
            paramOptional("tool", ""));

    /**
     * @param api API reference.
     */
    public SearchResources(@Nonnull FeignSyncApi api)
    {
        super(api::search, PARAM_MAP);
    }

    /**
     * Configures parameters to search for mission packages.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withMissionPackageFiltering()
    {
        return withKeywordRegexes(List.of("missionpackage"));
    }

    /**
     * Adds an inclusive area constraint to the search.
     *
     * @param lat1 First corner Y coordinate <i>(latitude)</i>.
     * @param lon1 First corner X coordinate <i>(longitude)</i>.
     * @param lat2 Second corner Y coordinate <i>(latitude)</i>.
     * @param lon2 Second corner X coordinate <i>(longitude)</i>.
     *
     * @return Self
     */
    @Nonnull
    public SearchResources withBox(double lat1, double lon1, double lat2, double lon2)
    {
        return withBox(String.format("%f %f %f %f", lat1, lon1, lat2, lon2));
    }

    /**
     * Adds an inclusive area constraint to the search.
     *
     * @param box String representation of a box.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withBox(@Nonnull String box)
    {
        return setParam("box", box);
    }

    /**
     * Adds an inclusive area constraint to the search.
     *
     * @param lat Circle center Y coordinate <i>(latitude)</i>
     * @param lon Circle center X coordinate <i>(longitude)</i>
     * @param radius Radius of circle in meters.
     *
     * @return Self
     */
    @Nonnull
    public SearchResources withCircle(double lat, double lon, double radius)
    {
        // Format from 'KmlUtils.parseSpatialCoordinates(...)' from 'takserver-war' project
        // Splits values based on: text.replaceAll(",", " ").split("\\s+")
        // Circle takes 3 values: lat,lon,radius
        return withCircle(String.format("%f %f %f", lat, lon, radius));
    }

    /**
     * Adds an inclusive area constraint to the search.
     *
     * @param circle String representation of a circle.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withCircle(@Nonnull String circle)
    {
        return setParam("circle", circle);
    }

    /**
     * Adds a minimum altitude constraint to the search.
     *
     * @param minAltitude Minimum allowed altitude of associated resources, in meters.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withMinAltitude(double minAltitude)
    {
        return setParam("minAltitude", minAltitude);
    }

    /**
     * Adds a maximum altitude constraint to the search.
     *
     * @param maxAltitude Maximum allowed altitude of associated resources, in meters.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withMaxAltitude(double maxAltitude)
    {
        return setParam("maxAltitude", maxAltitude);
    }

    /**
     * Adds a start time constraint to the search.
     *
     * @param startTime Start time cutoff.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withStartTime(@Nonnull Instant startTime)
    {
        return setParam("startTime", startTime);
    }

    /**
     * Adds an end time constraint to the search.
     *
     * @param endTime End time cutoff.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withEndTime(@Nonnull Instant endTime)
    {
        return setParam("endTime", endTime);
    }

    /**
     * Adds a name matching constraint to the search.
     *
     * @param filename Regular expression to match against file names. Pattern must not exceed 256 characters.
     *
     * @return Self.
     *
     * @see #withNameRegex(String) Used for matching against the resource name, rather than the associated file name.
     */
    @Nonnull
    public SearchResources withFileNameRegex(@Nonnull String filename)
    {
        return setParam("filename", filename);
    }

    /**
     * Adds a name matching constraint to the search.
     *
     * @param name Regular expression to match against resource names. Pattern must not exceed 256 characters.
     *
     * @return Self.
     *
     * @see #withFileNameRegex(String) Used for matching against the original file names associated with the resource.
     */
    @Nonnull
    public SearchResources withNameRegex(@Nonnull String name)
    {
        return setParam("name", name);
    }

    /**
     * Adds a keyword matching constraint to the search.
     *
     * @param keywords List of regular expression to match against keywords. Pattern must not exceed 256 characters.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withKeywordRegexes(@Nonnull List<String> keywords)
    {
        return setParam("keyword", keywords);
    }

    /**
     * Adds a mime-type matching constraint to the search.
     *
     * @param mimetype Regular expression to match against mime-types. Pattern must not exceed 256 characters.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withMimeTypeRegex(@Nonnull String mimetype)
    {
        return setParam("mimetype", mimetype);
    }

    /**
     * Adds an uid matching constraint to the search.
     *
     * @param uid Regular expression to match against uids. Pattern must not exceed 256 characters.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withUidRegex(@Nonnull String uid)
    {
        return setParam("uid", uid);
    }

    /**
     * Adds a sha256 hash matching constraint to the search.
     *
     * @param hash Regular expression to match against SHA256 hashes. Pattern must not exceed 256 characters.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withHashRegex(@Nonnull String hash)
    {
        return setParam("hash", hash);
    }

    /**
     * Adds a mission name matching constraint to the search.
     *
     * @param mission Regular expression to match against mission names. Pattern must not exceed 256 characters.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withMissionNameRegex(@Nonnull String mission)
    {
        return setParam("mission", mission);
    }

    /**
     * Adds a tool name matching constraint to the search.
     *
     * @param tool Regular expression to match against tool names. Pattern must not exceed 256 characters.
     *
     * @return Self.
     */
    @Nonnull
    public SearchResources withToolRegex(@Nonnull String tool)
    {
        return setParam("tool", tool);
    }
}
