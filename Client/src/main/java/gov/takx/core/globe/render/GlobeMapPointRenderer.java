package gov.takx.core.globe.render;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IMapObjectMarkerList;
import gov.takx.api.globe.render.IRendererSearchResult;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.core.mapdata.MapDataUtils;
import gov.takx.core.mapdata.MapPoint;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for rendering the actual track point map points on the globe.
 * @since 5.1 (renamed from GlobeMarkerRenderer)
 */
@PseudoBean
public class GlobeMapPointRenderer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private MapDataUtils mapDataUtils;

    @Inject
    private IPlaybackManager playbackManager;

    private static final double elevation = .2d;
    private long frameTimeStamp;
    private final List<MapPoint[]> mapPointArrays = new ArrayList<>();
    private long playbackCurrentTime;
    private boolean playbackLive = true;

    private boolean forceSnapToSurface;

    /**
     * Call with {@code true} to force all map points to be snapped to the surface.
     *
     * @param forceSnapToSurface {@code true} to force snap to surface, {@code false} to obey the styling context
     */
    public void setForceSnapToSurface(boolean forceSnapToSurface)
    {
        this.forceSnapToSurface = forceSnapToSurface;
    }

    /**
     * Render the markers in the given lists.
     *
     * @param dc          Drawing context
     * @param markerLists The set of marker lists to render
     */
    public void renderMarkerLists(DrawContext dc, Iterable<? extends IMapObjectMarkerList> markerLists)
    {
        try
        {
            if (dc.getFrameTimeStamp() != frameTimeStamp)
            {
                frameTimeStamp = dc.getFrameTimeStamp();
                playbackCurrentTime = playbackManager.getCurrentPlaybackPosition();
                playbackLive = playbackManager.isLive();

                computeSurfacePoints(dc, markerLists);
            }

            mapPointArrays.forEach(mapPoints -> draw(dc, mapPoints));
        } catch (Exception e)
        {
            logger.error("Failed to render markers", e);
        }
    }

    /**
     * Search for visible data within the given sector.
     *
     * @param searchSector Bounding sector for search
     * @return search results
     * @since 2.29
     */
    IRendererSearchResult searchForRdms(Sector searchSector)
    {
        return mapDataUtils.getViewableMessagesForArea(searchSector);
    }

    /**
     * Compute the point on the surface corresponding to each of the map points that we are rendering.
     * Should only be called once per frame.  The results are placed into the member variable {@code mapPointArrays}.
     *
     * @param dc          Drawing context
     * @param markerLists The set of marker lists to render
     */
    protected void computeSurfacePoints(DrawContext dc, Iterable<? extends IMapObjectMarkerList> markerLists)
    {
        // Determine "now", if we're in playback mode, then use the playback position, otherwise real current time
        final long now = playbackLive ? System.currentTimeMillis() : playbackCurrentTime;

        mapPointArrays.clear();

        for (IMapObjectMarkerList markerList : markerLists)
        {
            final IMapObject mapObject = markerList.getMapObject();
            int mapPointIndex = 0;

            if (mapDataUtils.isMapObjectViewable(mapObject))
            {
                final int listSize = markerList.size();

                if (listSize == 0) continue;

                final var mapPointArray = new MapPoint[listSize];

                // Force snap to surface if the map object has no elevation data
                final boolean mustSnapToSurface = forceSnapToSurface || !mapObject.hasElevation();

                for (MapPoint mapPoint : markerList)
                {
                    // The mapPoint list is dynamic.  If new map points are added while we are executing, then this list
                    // may grow in size.  If our index goes above our allocated size, then break out of the loop.
                    // The new points will get rendered on the next rendering cycle.

                    if (mapPointIndex >= listSize)
                    {
                        logger.debug("mapPoint list grew in size - BREAK");
                        break;
                    }

                    // If the mapPoint is null, add a null reference to the surfacePoints cache array so that it is
                    // the same size as the mapPoint iterator.

                    if (mapPoint == null)
                    {
                        mapPointArray[mapPointIndex++] = null;
                        continue;
                    }

                    final var markerAttributes = mapPoint.getAttributes();
                    if (markerAttributes == null) continue; // Marker has not been prepared for rendering yet

                    final var tsc = markerAttributes.getTrackStylingContext();
                    final long timeThreshold = tsc.getTimeThreshold();

                    // Do not draw mapPoint if we are in playback, and it is newer than the current playback time, or
                    // it is older than the timeThreshold (if any).
                    final long mapPointTime = mapPoint.getTime();
                    final boolean skip = (!playbackLive && mapPointTime > playbackCurrentTime)
                            || (timeThreshold > 0 && (now - timeThreshold > mapPointTime));

                    if (skip)
                    {
                        mapPointArray[mapPointIndex++] = null;
                    } else
                    {
                        // Compute the surface point
                        final boolean isSnapToSurface = mustSnapToSurface || tsc.isSnappedToSurface();
                        mapPointArray[mapPointIndex++] = mapPoint;
                        mapPoint.setSurfacePoint(computeSurfacePoint(dc, mapPoint.getPosition(), isSnapToSurface));
                    }
                }

                // Add the processed map point set to the map to be rendered
                mapPointArrays.add(mapPointArray);
            }
        }
    }

    private void draw(DrawContext dc, MapPoint[] mapPoints)
    {
        try
        {
            for (var mapPoint : mapPoints)
            {
                // A null MapPoint indicates that the map point need not be rendered. Typically, this means the map point
                // is outside the viewing frustum and therefore not visible.
                // If it's attributes are null, it hasn't been prepared for rendering, so skip it.
                if (mapPoint != null && mapPoint.getAttributes() != null) drawMapPoint(dc, mapPoint);
            }
        } catch (Exception e)
        {
            logger.error("Failed to render map points", e);
        }
    }

    private void drawMapPoint(DrawContext dc, MapPoint mapPoint)
    {
        // This method is called to prepare the MapPoint for rendering and submit it to the DrawContext.

        // The map point's size computation depends on the map point's attributes, therefore we must apply the map point
        // list attributes prior to computing the map point's size.
        final var markerAttributes = mapPoint.getAttributes();
        markerAttributes.applyMapPoint(dc, mapPoint);

        Vec4 point = mapPoint.getSurfacePoint();
        if (point == null) point = computeSurfacePoint(dc, mapPoint.getPosition(), true);
        if (point == null) return;

        final double markerSize = computeMarkerSizeInMeters(dc, point, mapPoint);

        mapPoint.setSearchRadius(2 * markerSize);

        // The placemark glyph handles creating its own OrderedRenderable
        markerAttributes.setupGlyph(dc, mapPoint, markerSize);

        mapPoint.renderGlyph(dc);

        dc.addOrderedRenderable(mapPoint);
    }

    protected static Vec4 computeSurfacePoint(DrawContext dc, Position pos, boolean snapToSurface)
    {
        if (!snapToSurface)
        {
            return dc.getGlobe().computePointFromPosition(pos);
        }

        // Compute points that are at the renderer-specified elevation
        final Vec4 point = dc.getSurfaceGeometry().getSurfacePoint(pos.getLatitude(), pos.getLongitude(), elevation);
        if (point != null)
        {
            return point;
        }

        // Point is outside the current sector geometry, so compute it from the globe.
        return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(), elevation);
    }

    /**
     * Determine the size of the marker being drawn in meters.
     *
     * @param dc     Drawing context
     * @param point  point at which the marker is being rendered
     * @param marker The marker object being rendered
     * @return The height of the marker, in meters
     */
    protected double computeMarkerSizeInMeters(DrawContext dc, Vec4 point, Marker marker)
    {
        final View view = dc.getView();
        double d = point.distanceTo3(view.getEyePoint());
        final MarkerAttributes attributes = marker.getAttributes();
        double radius = attributes.getMarkerPixels() * view.computePixelSizeAtDistance(d);

        return Math.max(radius, attributes.getMinMarkerSize());
    }
}
