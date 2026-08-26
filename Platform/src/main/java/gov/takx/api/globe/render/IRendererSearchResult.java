package gov.takx.api.globe.render;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;

import java.util.List;
import java.util.Map;

/**
 * The result of a search on a renderer. A renderer query finds map objects and RDMs within a geographic boundary,
 * as determined by the renderer (e.g., it may use a scaled area based on view, a fixed region, etc.).
 *
 * @since 2.29
 */
public interface IRendererSearchResult
{
    /**
     * Get the total number of map points that met the search criteria. This may be different than the number of
     * points included in the result.
     *
     * @return The number of map points that met the search criteria.
     */
    long getTotalMapPointCount();

    /**
     * The number of map points returned in the result. This number may be less than the total number of map
     * points that matched the search criteria because there is a limit to how many points will be returned,
     * due to memory constraints.
     *
     * @return The number of map points included in the search result.
     */
    int getMapPointCount();

    /**
     * Get the data, organized by map object.
     *
     * @return Map relating map objects to data points that met the search criteria.
     */
    Map<IMapObject, List<IRaptorDataMessage>> getData();

    /**
     * @return The bounding sector of the search
     */
    Sector getSearchSector();
}
