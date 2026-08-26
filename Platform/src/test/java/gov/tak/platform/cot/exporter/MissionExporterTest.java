package gov.tak.platform.cot.exporter;

import com.atakmap.comms.NetConnectString;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionBuilder;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ACotEventExporter;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.platform.cot.detail.CotDetailManager;
import gov.tak.test.utils.ExporterTestUtils;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.core.persist.MapObjectPersist;
import gov.takx.dataservices.persist.MissionMapObject;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.Test;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_AUTHOR_UID;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_NAME;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_SERVER;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_TOOL;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_TYPE;
import static gov.tak.api.cot.detail.DetailConstants.MISSION;
import static gov.tak.api.cot.detail.DetailConstants.VALUE_TYPE_INVITE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate {@link MissionExporter} functionality.
 *
 * @since 5.0
 */
public class MissionExporterTest extends AContainerTest
{
    private static final String TOOL_NAME = "sample-tool";

    private final MissionExporter missionExporter = new MissionExporter();

    @SuppressWarnings("unused")
    @Mocked(parentTypes = IMapObjectPersist.class)
    private MapObjectPersist mapObjectPersist;

    @SuppressWarnings("unused")
    @Mocked(parentTypes = IMapObjectManager.class)
    private MapObjectManager mapObjectManager;

    protected MissionExporterTest()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(getBean(MapObjectManager.class)));
    }

    @Test
    public void test_mission_export()
    {
        final Mission mission = new MissionBuilder("sample-mission")
                .withCreatorUid("sample-creator")
                .build();

        final NetConnectString netConnect = new NetConnectString("proto", "host", 1234);
        final MissionMapObject missionMapObject = new MissionMapObject(mission.name(), mission.creatorUid(), netConnect);
        missionMapObject.setTool(TOOL_NAME);

        // Now convert the Map Object into a CoT event
        getBean(CotDetailManager.class).startService();
        final long now = CoordinatedTime.currentTimeMillis();
        final CotEvent event = missionExporter.export(missionMapObject);
        assertThat(event).isNotNull();
        getBean(CotDetailManager.class).stopService();

        // We want the common event value validations, but not the location ones, as the mission invite events
        // have bogus locations by design across TAK products. So we'll just flub the location.
        missionMapObject.setLocation(new GeoPoint(0, 0), 0, true);
        event.setPoint(new GeoPoint(0, 0));
        ExporterTestUtils.verifyCommonEventValues(event, now, missionMapObject, ACotEventExporter.ONE_DAY_SECONDS * 1000L, missionMapObject.getMetaString("how", "?"));

        final CotDetail detail = event.getDetail();
        assertThat(detail).isNotNull();
        assertThat(detail.getChildren()).hasSize(3); // archive, contact, and mission
        CotDetail missionDetail = detail.getChildrenByName(MISSION).get(0);
        assertThat(missionDetail.getAttribute(ATTR_AUTHOR_UID)).isEqualTo(mission.creatorUid());
        assertThat(missionDetail.getAttribute(ATTR_NAME)).isEqualTo(mission.name());
        assertThat(missionDetail.getAttribute(ATTR_SERVER)).isEqualTo(netConnect.toString());
        assertThat(missionDetail.getAttribute(ATTR_TOOL)).isEqualTo(TOOL_NAME);
        assertThat(missionDetail.getAttribute(ATTR_TYPE)).isEqualTo(VALUE_TYPE_INVITE);
    }
}
