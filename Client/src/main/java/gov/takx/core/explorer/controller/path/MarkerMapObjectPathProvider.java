package gov.takx.core.explorer.controller.path;

import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.explorer.ExplorerUtils;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.platform.utils.AffiliationUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Implementation of {@link IExplorablePathProvider} that handles any {@link IMapObjectExplorable} that
 * contains a {@link IMarkerMapObject} map object.
 *
 * @since 5.3
 */
public class MarkerMapObjectPathProvider implements IExplorablePathProvider
{
    @Override
    @Nullable
    public List<String> createDefaultPath(@Nonnull IExplorable explorable)
    {
        if (explorable instanceof IMapObjectExplorable mapObjectExplorable && mapObjectExplorable.getMapObject() instanceof IPointMapObject mapObject)
        {
            final String groupName = mapObject.getGroupName();

            if (mapObject instanceof IconMapObject)
            {
                if (groupName.contains("2525") || mapObject.getDisplayableType().contains("2525"))
                {
                    if (AffiliationUtils.AffiliationFilter.FRIENDLY.test(mapObject)) return ExplorerPathConstants.COT2525C_FRIENDLY_PATH;
                    if (AffiliationUtils.AffiliationFilter.HOSTILE.test(mapObject)) return ExplorerPathConstants.COT2525C_HOSTILE_PATH;
                    if (AffiliationUtils.AffiliationFilter.NEUTRAL.test(mapObject)) return ExplorerPathConstants.COT2525C_NEUTRAL_PATH;
                    if (AffiliationUtils.AffiliationFilter.UNKNOWN.test(mapObject)) return ExplorerPathConstants.COT2525C_UNKNOWN_PATH;
                    return ExplorerPathConstants.COT2525C_OTHER_PATH;
                } else
                {
                    return MapObjectGroupNames.DEVICES_GROUP.equalsIgnoreCase(groupName)
                            ? ExplorerUtils.extendPath(ExplorerPathConstants.DEVICES_PATH, mapObject.getType())
                            : ExplorerUtils.extendPath(ExplorerPathConstants.MARKERS_ICONS_PATH, groupName);
                }
            } else
            {
                return mapObject.hasMetaValue(StandardAttributeNames.TEAM) && mapObject.hasMetaValue(StandardAttributeNames.ATAK_ROLE_TYPE)
                        ? ExplorerPathConstants.MARKERS_CONTACTS_PATH
                        : ExplorerUtils.extendPath(ExplorerPathConstants.MARKERS_OTHER_PATH, groupName);
            }
        } else
        {
            return null;
        }
    }
}