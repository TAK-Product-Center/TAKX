package gov.takx.core.globe.render;

import gov.nasa.worldwind.avlist.AVKey;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.core.mapdata.MapPointGroup;

/**
 * This class holds/manages the marker attributes for a map point group marker.  Each instance is shared
 * by every group object that has the same styling context.
 *
 * @since 1.14
 */
public class GlobeMarkerGroupAttributes extends GlobeMarkerAttributes
{
    /**
     * Construct on the given map Object.  Our rendering attributes are pulled from the TrackStylingContext provided,
     * which may be specific to an RDM for the MapObject or just the MapObject if no RDM-level TSD was assigned.
     *
     * @param mapObject Map Object we are handling
     * @param tsc       The Track Styling Context associated with this MO
     */
    public GlobeMarkerGroupAttributes(IMapObject mapObject, ITrackStylingContext tsc)
    {
        super(mapObject, tsc);
    }

    /**
     * Determine if the given map point group is the primary selected object.
     * <p/>
     * Overridden to avoid comparison of RDM ID since a group doesn't have a single id
     *
     * @param mapPoint Map point to test
     * @return true if highlighted, false otherwise
     */
    @Override
    public boolean isHighlighted(MapPoint mapPoint)
    {
        return highlightedMapPoint != null && highlightedMapPoint == mapPoint;
    }

    /**
     * Overridden to turn off heading linkage.
     */
    @Override
    protected void setGlyphPropertiesFromTSD()
    {
        super.setGlyphPropertiesFromTSD();

        // Now turn off heading linkage since it makes no sense on a group
        linkGlyphToHeading = false;
        glyphAttributes.setHeading(0.0); // North heading if not linked
    }

    /**
     * Overridden
     *
     * @param mapPoint map point being rendered
     */
    @Override
    protected void setAttributesForBucketMode(MapPoint mapPoint)
    {
        // No bucket mode for groups, just use the basic marker shape and color
        setMarkerShapeFromTSD();
        setColor(stylingContext.getTrackShapeConstantColor());
    }

    /**
     * Overridden to always return 3 pixels larger than a non-grouped marker with the same TSD.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    protected double determineMarkerSize(MapPoint mapPoint)
    {
        return super.determineMarkerSize(mapPoint) + 3;
    }

    /**
     * Setup any tool tip text that should be displayed for this map point.  Set the tool tip text to show the count.
     *
     * @param mapPoint Map point being processed
     */
    @Override
    protected void setupTooltip(MapPoint mapPoint)
    {
        MapPointGroup mpg = (MapPointGroup) mapPoint;
        mpg.setValue(AVKey.HOVER_TEXT, "Contains " + mpg.getCount() + " track points");
    }
}
