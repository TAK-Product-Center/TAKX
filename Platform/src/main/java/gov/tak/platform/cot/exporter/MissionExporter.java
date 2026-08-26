package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ACotEventExporter;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMissionMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.MissionMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_AUTHOR_UID;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_NAME;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_SERVER;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_TOOL;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_TYPE;
import static gov.tak.api.cot.detail.DetailConstants.MISSION;
import static gov.tak.api.cot.detail.DetailConstants.VALUE_TYPE_INVITE;

/**
 * Exporter handling {@link MissionMapObject}.
 * This creates a CoT event that will prompt a receiving user to join the given mission.
 *
 * @since 5.0
 */
public class MissionExporter extends ACotEventExporter
{
    @Override
    public Class<? extends MapObject> getType()
    {
        return MissionMapObject.class;
    }

    @Nullable
    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        final IMissionMapObject missionMapObject = (IMissionMapObject) mapObject;

        final CotEvent event = createBasicEvent(mapObject, ONE_DAY_SECONDS);
        final CotDetail detail = event.getDetail();
        final CotDetail missionDetail = detail.addChild(new CotDetail(MISSION));
        missionDetail.setAttribute(ATTR_AUTHOR_UID, missionMapObject.getCreatorUid());
        missionDetail.setAttribute(ATTR_SERVER, missionMapObject.getNetConnect().toString());
        missionDetail.setAttribute(ATTR_TYPE, VALUE_TYPE_INVITE);
        missionDetail.setAttribute(ATTR_TOOL, missionMapObject.getTool());
        missionDetail.setAttribute(ATTR_NAME, missionMapObject.getMissionName());

        return event;
    }
}
