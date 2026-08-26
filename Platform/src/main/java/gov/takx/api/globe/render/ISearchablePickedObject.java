package gov.takx.api.globe.render;

import gov.nasa.worldwind.geom.Sector;

/**
 * This interface is used to mark a "picked object" that supports a search operation relative to its location.
 * This is used by renderers so that various selection events can be used to determine a collection of associated
 * RDMs.
 *
 * @see gov.nasa.worldwind.event.SelectEvent
 * @since 2.29
 */
public interface ISearchablePickedObject
{
    /**
     * @return The Id of the Map Object associated with the picked object
     */
    int getMapObjectId();

    /**
     * Get the bounding search sector for this picked object.
     *
     * @return Bounding sector for search
     */
    Sector getSearchSector();

    /**
     * Search for all the RDMs that are <em>close</em> to the picked point.  The definition of "near" is up to
     * the renderer that created this picked object.
     *
     * @return Search results
     */
    IRendererSearchResult searchForRdms();
}
