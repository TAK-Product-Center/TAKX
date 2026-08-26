package gov.takx.platform.globe.renderer;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.globe.render.IRendererSearchResult;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Basic implementation of {@link IRendererSearchResult}.
 *
 * @since 2.29
 */
public class RendererSearchResult implements IRendererSearchResult
{
    private final long totalMapPointCount;
    private int mapPointCount = 0;
    private final Map<IMapObject, List<IRaptorDataMessage>> dataMap;
    private final Sector searchSector;

    /**
     * Create result.
     *
     * @param totalMapPointCount Total number of map points that met the search criteria.
     * @param searchSector       Bounding sector for the search
     * @param dataMap            Map of messages per map object
     */
    public RendererSearchResult(long totalMapPointCount, Sector searchSector, Map<IMapObject, List<IRaptorDataMessage>> dataMap)
    {
        this.totalMapPointCount = totalMapPointCount;
        this.searchSector = searchSector;
        this.dataMap = dataMap;

        for (List<IRaptorDataMessage> list : dataMap.values())
        {
            mapPointCount += list.size();
        }
    }

    @Override
    public long getTotalMapPointCount()
    {
        return totalMapPointCount;
    }

    @Override
    public int getMapPointCount()
    {
        return mapPointCount;
    }

    @Override
    public Map<IMapObject, List<IRaptorDataMessage>> getData()
    {
        return Collections.unmodifiableMap(dataMap);
    }

    @Override
    public Sector getSearchSector()
    {
        return searchSector;
    }
}
