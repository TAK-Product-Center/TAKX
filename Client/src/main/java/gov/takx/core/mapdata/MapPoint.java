package gov.takx.core.mapdata;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.core.globe.IGlobeInteractable;
import gov.takx.core.globe.render.GlobeMarkerAttributes;
import gov.takx.core.persist.MapPointPersist;
import gov.takx.platform.tracks.VerticalAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.lang.invoke.MethodHandles;

/**
 * This is a Data Transfer Object for managing the subset of data we need from {@link IRaptorDataMessage}s to support
 * a data model of the messages/track points associated with a map object.  Its purpose is to reduce the
 * overhead of dealing with RDM's and how we render them on the globe.  By using a DTO, we avoid the size
 * of an RDM and the hibernate session overhead with managing persistent entities.
 * <p/>
 * This object also includes GUI support methods for rendering the point as a marker on the globe.  Normally,
 * there would be a full separation between the model (basic map point data) and the view (marker rendering).
 * But our target platform is seriously memory and CPU constrained, so a combination object is warranted.
 */
public class MapPoint extends TrackPointIds implements Marker, IGlobeInteractable, Comparable<MapPoint>, OrderedRenderable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final boolean hasElevation;
    protected final long time;
    protected final Position position;
    protected final Angle heading;
    protected final Angle pitch;
    protected final Angle roll;
    protected boolean showToolTip;
    protected GlobeMarkerAttributes attributes;
    protected double searchRadius;
    protected Vec4 surfacePoint;
    protected Integer color;

    /**
     * Placemark to render map point glyph.
     */
    private PointPlacemark glyph;

    /**
     * AVKey used to indicate that a PointPlacemark is associate with a MapPoint marker.
     * The value will be a reference to this MapPoint object.
     */
    public static final String AVKEY_ASSOCIATED_MARKER = "AssociatedMarker";

    /**
     * Constructor used by hibernate.  Note that the argument order and type has to match the select
     * statement in {@link MapPointPersist}.
     *
     * @param rdmId        ID of RDM
     * @param moId         ID of MO associated with RDM
     * @param latitude     Latitude from RDM
     * @param longitude    Longitude from RDM
     * @param elevation    Elevation from RDM
     * @param heading      Heading from RDM
     * @param hasElevation Does the RDM have an elevation value
     * @param hasHeading   Does the RDM have a heading value
     * @param hasTime      Does the RDM have a device time value
     * @param deviceTime   Time set by the device
     * @param receiveTime  Time set as received
     */
    public MapPoint(int rdmId, int moId, double latitude, double longitude, double elevation, double heading,
                    boolean hasElevation, boolean hasHeading, boolean hasTime, long deviceTime, long receiveTime)
    {
        super(rdmId, moId);
        this.hasElevation = hasElevation;

        this.heading = hasHeading ? Angle.fromDegrees(heading) : null;

        // Our time is the "relevant" time from the RDM.   If it has a device time, then we use that, otherwise
        // use receive time.
        time = hasTime ? deviceTime : receiveTime;

        // For rendering performance, combine the lat/lon/elev into a Position (which is what WW needs to render)
        position = Position.fromDegrees(latitude, longitude, elevation);

        // TODO: need to retrieve pitch and roll from database
        pitch = null;
        roll = null;
    }

    /**
     * Construct from a {@link IRaptorDataMessage} - extract the interesting bits.
     *
     * @param rdm Message to abstract
     */
    public MapPoint(IRaptorDataMessage rdm)
    {
        super(rdm.getIdAsInt(), rdm.getMapObject().getId());
        hasElevation = rdm.hasElevation();

        // Our time is the "relevant" time from the RDM.
        time = rdm.getRelevantTime();

        position = Position.fromDegrees(rdm.getLatitude(), rdm.getLongitude(), rdm.getElevation());

        heading = rdm.hasHeading() ? Angle.fromDegrees(rdm.getHeading()) : null;

        boolean has3dOrientation = rdm.has3dOrientation();
        pitch = has3dOrientation ? Angle.fromDegrees(rdm.getPitch()) : null;
        roll = has3dOrientation ? Angle.fromDegrees(rdm.getRoll()) : null;
    }

    /**
     * Constructor used by derived classes.
     *
     * @param pos        Position of the map point
     * @param moId       Map object ID
     * @param latestTime The relevant time to use for this marker
     */
    protected MapPoint(Position pos, int moId, long latestTime)
    {
        super(0, moId);
        position = pos;

        // Everything else gets a default value
        hasElevation = false;
        time = latestTime == 0 ? System.currentTimeMillis() : latestTime; // Use "now" if time is 0
        heading = null;
        pitch = null;
        roll = null;
    }

    public int getMoId()
    {
        return moId;
    }

    public double getLatitude()
    {
        return position.latitude.degrees;
    }

    public double getLongitude()
    {
        return position.longitude.degrees;
    }

    public double getElevation()
    {
        return position.elevation;
    }

    public boolean hasElevation()
    {
        return hasElevation;
    }

    public boolean hasHeading()
    {
        return heading != null;
    }

    public long getTime()
    {
        return time;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof MapPoint && rdmId == ((MapPoint) obj).rdmId;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this object.
     */
    @Override
    public int compareTo(MapPoint o)
    {
        return Integer.compare(rdmId, o.rdmId);
    }

    /**
     * @return hashcode as determined by that of our RDMs ID, this is consistent with our equals implementation
     */
    @Override
    public int hashCode()
    {
        return rdmId;
    }

    @Override
    public String toString()
    {
        return "[MapPoint: id=" + rdmId + ", moId=" + moId + ", pos=" + position + ']';
    }

    // Marker and GlobeInteractable methods

    @Override
    public Position getPosition()
    {
        return position;
    }

    @Override
    public void setPosition(Position position)
    {
        // We are an immutable object (for all non-UI related data)
        logger.error("Illegal call to setPosition: {}", position);
    }

    @Override
    public Angle getHeading()
    {
        return heading;
    }

    @Override
    public void setHeading(Angle heading)
    {
        // We are an immutable object (for all non-UI related data)
        logger.error("Illegal call to setHeading: {}", heading);
    }

    @Override
    public Angle getPitch()
    {
        return pitch;
    }

    @Override
    public void setPitch(Angle angle)
    {
        // We are an immutable object (for all non-UI related data)
        logger.error("Illegal call to setPitch: {}", angle);
    }

    @Override
    public Angle getRoll()
    {
        return roll;
    }

    @Override
    public void setRoll(Angle angle)
    {
        // We are an immutable object (for all non-UI related data)
        logger.error("Illegal call to setRoll: {}", angle);
    }

    @Override
    public GlobeMarkerAttributes getAttributes()
    {
        return attributes;
    }

    @Override
    public void setAttributes(MarkerAttributes attributes)
    {
        if (attributes instanceof GlobeMarkerAttributes)
        {
            this.attributes = (GlobeMarkerAttributes) attributes;
        } else
        {
            throw new IllegalArgumentException("Attributes must be GlobeMarkerAttributes");
        }
    }

    /**
     * Get the point color.
     * @return the point color
     * @since 5.6.1
     */
    public int getColor()
    {
        return color != null ? color : getAttributes().getColor().getRGB();
    }

    /**
     * Set the point color.
     * @param color the point color
     * @since 5.6.1
     */
    public void setColor(Integer color)
    {
        this.color = color;
    }

    @Override
    public void render(DrawContext dc, Vec4 point, double radius)
    {
        if (attributes.shouldRenderMarker())
        {
            attributes.getShape(dc).render(dc, this, point, radius);
        }
    }

    @Override
    public void render(DrawContext dc, Vec4 point, double radius, boolean isRelative)
    {
        if (attributes.shouldRenderMarker())
        {
            attributes.getShape(dc).render(dc, this, point, radius, isRelative);
        }
    }

    private boolean isSnappedToSurface()
    {
        final ITrackStylingContext tsc = attributes == null ? null : attributes.getTrackStylingContext();
        return tsc != null && tsc.isSnappedToSurface();
    }

    /**
     * Get the position to use as a reference point for globe-dragging.  If the renderer is going to draw this point
     * at the surface, then we need to return a surface point here so that dragging will feel correct.
     *
     * @return The geodetic position at which the marker will be drawn, which may be different from the position
     * of the corresponding RDM.
     */
    public Position getReferencePosition()
    {
        // Note also that we don't do this snapping for getPosition(), for several reasons: we want to display the
        // elevation of the RDM in the panel at the bottom of the window, we don't want to do any allocation
        // in getPosition() because it is called many times per rendering cycle, and we don't want to cache a copy of
        // the snapped position because there can be a great many MapPoint objects in existence at any given time.
        //
        // So we use a separate method and calculate it as requested, which should not be very often - currently it's
        // only used by GlobeViewInputHandler when the globe is dragged with the mouse.

        return isSnappedToSurface() ? new Position(position, 0.0) : position;
    }

    public void renderGlyph(DrawContext dc)
    {
        if (glyph != null)
        {
            glyph.render(dc);
        }
    }

    /**
     * Prepare to use a glyph.  Construct the glyph on demand and use the given attributes when doing so.
     * This method is called on every render cycle.
     *
     * @param dc                 Drawing context
     * @param attrs              Attributes to use for the rendering of the glyph/glyph
     * @param placemarkAlignment Vertical alignment to use for placemark
     * @param verticalOffset     Vertical offset to apply to glyph after alignment positioning
     * @param markerSize         Geographic size of the marker (in meters)
     */
    public void useGlyph(DrawContext dc, PointPlacemarkAttributes attrs, VerticalAlignment placemarkAlignment,
                         double verticalOffset, double markerSize)
    {
        // If we don't have a glyph prepared, then create one now.
        // If we already have a glyph, and the glyph image address is changing, then re-set the attributes.

        if (glyph == null)
        {
            glyph = new PointPlacemark(position);

            glyph.setAttributes(attrs);
            glyph.setAltitudeMode(WorldWind.ABSOLUTE);
        } else
        {
            final String imageAddress = glyph.getAttributes().getImageAddress();

            if (imageAddress == null || !imageAddress.equals(attrs.getImageAddress()))
            {
                glyph.setAttributes(attrs); // Glyph image address is changing, update the attributes
            }
        }

        // Adjust the elevation of the glyph to match the vertical alignment. Do this each frame to ensure that the
        // glyph keeps in sync with where the marker is actually rendered, which might change as more accurate
        // terrain data is loaded.
        Position markerPosition = dc.getGlobe().computePositionFromPoint(surfacePoint);
        Position glyphPosition = markerPosition; // Same as marker unless adjusted

        if (placemarkAlignment == VerticalAlignment.Top || placemarkAlignment == VerticalAlignment.Bottom)
        {
            int sign = (placemarkAlignment == VerticalAlignment.Top) ? 1 : -1;

            // The height of the marker depends on its shape.  Cube, cone, and cylinder are all 2 times as tall as
            // they are wide (which is determined by the marker size value).

            final String shapeType = attributes.getShapeType();
            double height = markerSize;

            if (!shapeType.equals(BasicMarkerShape.SPHERE) && !shapeType.equals(BasicMarkerShape.ORIENTED_SPHERE))
            {
                height *= 2;
            }

            glyphPosition = new Position(markerPosition, markerPosition.elevation + (sign * height) + verticalOffset);
        } else if (verticalOffset != 0.0)
        {
            glyphPosition = new Position(markerPosition, markerPosition.elevation + verticalOffset);
        }

        glyph.setPosition(glyphPosition);

        // Ensure that the attributes for this map point are applied to the glyph we created
        attributes.applyMapPointForGlyph(this);

        // Add the AVKey entry to associate the placemark with this map point
        glyph.setValue(AVKEY_ASSOCIATED_MARKER, this);
    }

    /**
     * Clear out the glyph.  No glyph will be rendered for this map point after this method is called.
     */
    public void clearGlyph()
    {
        glyph = null;
    }

    @Override
    public boolean isShowToolTip()
    {
        return showToolTip;
    }

    @Override
    public void setShowToolTip(boolean showToolTip)
    {
        this.showToolTip = showToolTip;
    }

    @Override
    public boolean isHighlighted()
    {
        return attributes.isHighlighted(this);
    }

    public boolean isSecondaryHighlighted()
    {
        return attributes.isSecondaryHighlighted(moId);
    }

    @Override
    public void setHighlighted(boolean highlighted)
    {
        if (highlighted)
        {
            GlobeMarkerAttributes.setHighlighted(this, moId);
        } else
        {
            GlobeMarkerAttributes.setHighlighted(null, null);
        }
    }

    /**
     * Set the search radius for this point.  This value will vary as the rendering perspective (zoom, pitch, etc.)
     * changes.  It will be set during the rendering cycle.  The search radius is used when a user double-clicks
     * on a globe marker and we do a search for all map points "near" the selected point.
     *
     * @param searchRadius search radius for this point
     */
    public void setSearchRadius(double searchRadius)
    {
        this.searchRadius = searchRadius;
    }

    /**
     * Get the search sector for this group.
     *
     * @return Search sector
     */
    public Sector getSearchSector(Globe globe)
    {
        return Sector.boundingSector(globe, position, searchRadius);
    }

    public Vec4 getSurfacePoint()
    {
        return surfacePoint;
    }

    public void setSurfacePoint(Vec4 surfacePoint)
    {
        this.surfacePoint = surfacePoint;
    }

    @Override
    public double getDistanceFromEye()
    {
        return 0;
    }

    @Override
    public void pick(DrawContext drawContext, Point point)
    {
        //
    }

    @Override
    public void render(DrawContext drawContext)
    {
        //
    }
}
