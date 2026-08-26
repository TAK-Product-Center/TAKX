package gov.tak.api.comms.takserver.mission.call.sync;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.UploadedResourceResponse;
import gov.tak.api.comms.takserver.mission.feign.FeignSyncApi;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Map;

/**
 * Endpoint caller to upload a file to enterprise sync.
 *
 * @since 3.5
 */
public class UploadResource extends AbstractEndpointCaller<UploadedResourceResponse>
        implements IClientAssociatedEndpointCaller<UploadedResourceResponse>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("altitude", -1),
            paramOptional("latitude", 0),
            paramOptional("longitude", 0),
            paramOptional("keywords", ""),
            paramOptional("expiration", -1),
            paramOptional("name", ""),
            paramOptional("tool", "public"),
            paramOptional("creatoruid", ""));

    /**
     * @param api API reference.
     * @param fileContent Raw file contents to upload.
     */
    public UploadResource(@Nonnull FeignSyncApi api, @Nonnull byte[] fileContent)
    {
        this(api, "application/octet-stream", fileContent);
    }

    /**
     * @param api API reference.
     * @param contentType Mime type of file contents.
     * @param fileContent Raw file contents to upload.
     * @since 5.3
     */
    public UploadResource(@Nonnull FeignSyncApi api, @Nonnull String contentType, @Nonnull byte[] fileContent)
    {
        super((headers, params) -> api.upload(headers, params, contentType, fileContent), PARAM_MAP);
    }

    /**
     * Sets the altitude of the file.
     * Generally the latitude and longitude should also be set.
     *
     * @param altitude Altitude in meters.
     *
     * @return Self.
     */
    @Nonnull
    public UploadResource withAltitudeMeters(double altitude)
    {
        // Strip near-zero values
        if (altitude < 1 && altitude >= -1)
        {
            return unsetParam("altitude");
        }

        return setParam("altitude", (int) altitude);
    }

    /**
     * Sets the latitude/longitude of the file.
     *
     * @param latitude Latitude of the file.
     * @param longitude Longitude of the file.
     *
     * @return Self.
     */
    @Nonnull
    public UploadResource withLocation(double latitude, double longitude)
    {
        // Invalid locations will strip the location
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180)
        {
            return unsetParam("latitude")
                    .unsetParam("longitude");
        }

        return setParam("latitude", latitude)
                .setParam("longitude", longitude);
    }

    /**
     * Sets the keywords of the file to indicate it's a data package.
     *
     * @return Self.
     */
    @Nonnull
    public UploadResource withDataPackageKeywords()
    {
        return withKeywords("missionpackage");
    }

    /**
     * Sets the keywords of the file.
     *
     * @param keywords Collection of keywords.
     *
     * @return Self.
     */
    @Nonnull
    public UploadResource withKeywords(@Nonnull Collection<String> keywords)
    {
        return withKeywords(String.join(",", keywords));
    }

    /**
     * Sets the keywords of the file.
     *
     * @param keywords Keywords separated by commas.
     *
     * @return Self.
     */
    @Nonnull
    public UploadResource withKeywords(@Nonnull String keywords)
    {
        return setParam("keywords", keywords);
    }

    /**
     * Sets an expiration timestamp on the file, after which it will be automatically deleted.
     *
     * @param expiration Timestamp in epoch millis for the file to delete itself.
     * Using {@code System.currentTimeMillis() + 1000} would specify an expiration time of one second from now.
     *
     * @return Self.
     */
    @Nonnull
    public UploadResource withExpirationMillis(long expiration)
    {
        return setParam("expiration", expiration);
    }

    /**
     * Sets the name of the file.
     *
     * @param name Name of the file.
     *
     * @return Self.
     */
    @Nonnull
    public UploadResource withName(@Nonnull String name)
    {
        return setParam("name", name);
    }

    /**
     * Sets the tool name of the file.
     *
     * @param tool Name of the tool responsible for creating the file.
     *
     * @return Self.
     */
    @Nonnull
    public UploadResource withTool(@Nonnull String tool)
    {
        return setParam("tool", tool);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatoruid";
    }
}
