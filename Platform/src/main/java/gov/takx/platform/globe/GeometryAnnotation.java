package gov.takx.platform.globe;

import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.AbstractAnnotation;
import gov.nasa.worldwind.render.DrawContext;
import jakarta.annotation.Nonnull;

import java.awt.Rectangle;

/**
 * An {@link gov.nasa.worldwind.render.Annotation} that has a {@link Geometry} associated with it for location.
 * @since 5.1
 */
public class GeometryAnnotation extends AbstractAnnotation
{
    private Geometry geometry;
    private Position centerPosition;

    /**
     * @param geometry the geometry associated with the label
     */
    public GeometryAnnotation(@Nonnull Geometry geometry)
    {
        this.geometry = geometry;
        centerPosition = getCenterPosition(geometry.getEnvelope());
    }

    /**
     * Sets the geometry associated with the label.
     * @param geometry the geometry
     */
    public void setGeometry(@Nonnull Geometry geometry)
    {
        this.geometry = geometry;
        centerPosition = getCenterPosition(geometry.getEnvelope());
    }

    /**
     * Gets the geometry assocaited with the label.
     * @return the geometry
     */
    @Nonnull
    public Geometry getGeometry()
    {
        return geometry;
    }

    /**
     * Gets the center position of the geometry's bounding box.
     * @return the center position
     */
    @Nonnull
    public Position getCenterPosition()
    {
        return centerPosition;
    }

    @Override
    protected void doRenderNow(DrawContext dc)
    {
        // nothing to render for WW
    }

    @Override
    protected Rectangle computeBounds(DrawContext dc)
    {
        Envelope envelope = geometry.getEnvelope();
        return new Rectangle((int) envelope.minX, (int) envelope.maxY, (int) (envelope.maxX - envelope.minX), (int) (envelope.maxY - envelope.minY));
    }

    private static Position getCenterPosition(Envelope envelope)
    {
        double latDeltaDiv2 = (envelope.maxY - envelope.minY) / 2.0;
        double lonDeltaDiv2 = (envelope.maxX - envelope.minX) / 2.0;
        return Position.fromDegrees(envelope.minY + latDeltaDiv2, envelope.minX + lonDeltaDiv2, envelope.maxZ);
    }
}
