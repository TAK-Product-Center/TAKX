package gov.takx.api.mapobject;

import com.atakmap.comms.NetConnectString;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Contract for a map object defining a mission.
 * <p/>
 * Intended strictly as an intermediate to facilitate CoT exporting.
 *
 * @since 5.0
 */
@Internal
@NoPluginImplementations
public interface IMissionMapObject extends IMapObject
{
    String MISSION_TYPE = "Mission";

    /**
     * @return The uid of the mission's creator.
     */
    @Nonnull
    String getCreatorUid();

    /**
     * @param creatorUid The uid of the mission's creator.
     */
    void setCreatorUid(@Nonnull String creatorUid);

    /**
     * @return The net-connect string representation of the TAK Server the mission belongs to.
     */
    @Nonnull
    NetConnectString getNetConnect();

    /**
     * Sets the net-connect string of the TAK Server the mission belongs to from the net-connect components.
     *
     * @param host TAK Server host.
     * @param port TAK Server port.
     * @param scheme TAK Server connection scheme.
     */
    void setNetConnect(@Nonnull String host, int port, @Nonnull String scheme);

    /**
     * @param netConnect The net-connect string representation of the TAK Server the mission belongs to.
     */
    void setNetConnect(@Nonnull NetConnectString netConnect);

    /**
     * @return The net-connect string representation of the TAK Server the mission belongs to.
     */
    @Nonnull
    String getMissionName();

    /**
     * @return The tool name responsible for creating this map object. Defaults to {@code public}.
     */
    @Nonnull
    String getTool();

    /**
     * @param tool The tool name responsible for creating this map object.
     */
    void setTool(@Nonnull String tool);
}
