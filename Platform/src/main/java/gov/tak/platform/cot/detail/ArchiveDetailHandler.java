package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.mapobject.IMapObject;

import static gov.tak.api.cot.detail.DetailConstants.ARCHIVE;
import static gov.takx.api.mapobject.IMarkerMapObject.MARKER_FAMILY;
import static gov.takx.api.mapobject.IMarkerMapObject.MARKER_TYPE;

/**
 * Handle the {@code <archive/>} detail.  The presence of this tag indicates that no stale handling should be
 * applied to the generated marker.  If it is not present, then stale handling should be applied for team member
 * markers and any single point marker (CoT atoms).
 * <p/>
 * The {@code archive} detail instructs other TAK EUDs to also avoid stale handling, and to ensure it is persisted
 * across restarts.  E.g., ATAK will not persist markers with this detail, and they will not be present upon restart.
 *
 * @since 5.4
 */
public class ArchiveDetailHandler extends CotDetailHandler
{
    ArchiveDetailHandler()
    {
        super(ARCHIVE);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        // We add the 'archive' detail if the map item is tagged as archive, or it is not a marker.
        // We do not add the detail if the map item is transient.
        if (!item.isTransient() && (item.hasMetaValue(StandardAttributeNames.ARCHIVE) || isNotAMarker(item)))
        {
            detail.addChild(new CotDetail(ARCHIVE));
            return true;
        }
        return false;
    }

    private static boolean isNotAMarker(MapItem item)
    {
        return item instanceof IMapObject mapObject
                && (!MARKER_FAMILY.equals(mapObject.getFamily()) || !MARKER_TYPE.equals(mapObject.getType()));
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        item.setMetaString(StandardAttributeNames.ARCHIVE, "");
        return ImportResult.SUCCESS;
    }
}
