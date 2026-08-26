package gov.takx.dataservices.persist;

import com.atakmap.comms.NetConnectString;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IMissionMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import jakarta.annotation.Nonnull;

import static gov.tak.api.cot.CotConstants.UNKNOWN_VALUE;
import static gov.tak.api.cot.CotTypes.MISSION;
import static gov.tak.api.util.StandardAttributeNames.HOW;
import static gov.takx.platform.KeyUtils.MISSION_FAMILY;

/**
 * Map object for a mission.
 * <p/>
 * Intended strictly as an intermediate to facilitate CoT exporting.
 *
 * @since 5.0
 */
@Internal
public class MissionMapObject extends MapObject implements IMissionMapObject
{
    private static final String HOW_CONTENT = "h-g-i-g-o";
    private static final String DEFAULT_TOOL = "public";
    private String creatorUid;
    private NetConnectString netConnect;
    private String tool = DEFAULT_TOOL;

    /**
     * No-arg constructor for Hibernate.
     */
    protected MissionMapObject()
    {
        super(MapObjectSpec.builder(MISSION_FAMILY, MISSION_TYPE).build(), null);

        configure();
    }

    public MissionMapObject(@Nonnull String name, @Nonnull String creatorUid, @Nonnull NetConnectString netConnect)
    {
        super(MapObjectSpec.builder(MISSION_FAMILY, MISSION_TYPE).build(), null);

        this.creatorUid = creatorUid;
        this.netConnect = netConnect;

        configure();
        setName(name);
    }

    private void configure()
    {
        setCotType(MISSION);
        setMetaString(HOW, HOW_CONTENT);
        setLocation(new GeoPoint(0, 0, UNKNOWN_VALUE, UNKNOWN_VALUE, UNKNOWN_VALUE));
    }

    @Nonnull
    @Override
    public String getCreatorUid()
    {
        return creatorUid;
    }

    @Override
    public void setCreatorUid(@Nonnull String creatorUid)
    {
        this.creatorUid = creatorUid;
    }

    @Nonnull
    @Override
    public NetConnectString getNetConnect()
    {
        return netConnect;
    }

    @Override
    public void setNetConnect(@Nonnull String host, int port, @Nonnull String scheme)
    {
        setNetConnect(new NetConnectString(scheme, host, port));
    }

    @Override
    public void setNetConnect(@Nonnull NetConnectString netConnect)
    {
        this.netConnect = netConnect;
    }

    @Nonnull
    @Override
    public String getMissionName()
    {
        return getName();
    }

    @Nonnull
    @Override
    public String getTool()
    {
        return tool;
    }

    @Override
    public void setTool(@Nonnull String tool)
    {
        this.tool = tool;
    }
}
