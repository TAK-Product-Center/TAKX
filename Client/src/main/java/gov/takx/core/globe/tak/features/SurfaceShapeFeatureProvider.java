package gov.takx.core.globe.tak.features;

import android.graphics.Color;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceEllipse;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolyline;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.shapes.IPolygon;
import gov.takx.core.globe.GlobeShapeMapObject;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link IFeatureProvider} for {@link SurfaceShape}.
 *
 * @since 3.5
 */
public final class SurfaceShapeFeatureProvider extends AShapeFeatureProvider<SurfaceShape>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.RENDERABLE_PRIORITY, SurfaceShape.class)
    {
        @Override
        protected IFeatureProvider<?, ?> createFeatureProvider(@Nonnull Object object)
        {
            final SurfaceShape surfaceShape = (SurfaceShape) object;

            // TODO TME can't render surface images yet, so avoid creating a feature provider in that case

            if ((surfaceShape.getAttributes() == null || surfaceShape.getAttributes().getImageSource() == null) &&
                    (surfaceShape.getHighlightAttributes() == null || surfaceShape.getHighlightAttributes().getImageSource() == null))
            {
                return new SurfaceShapeFeatureProvider();
            }
            return null;
        }
    };

    // Attributes to use if none are supplied by a given surface object
    private static final ShapeAttributes DEFAULT_SHAPE_ATTRIBUTES = new BasicShapeAttributes();
    private static final Earth EARTH = new Earth();

    @Nonnull
    @Override
    public ShapeState createState(SurfaceShape surfaceShape)
    {
        ShapeGeometryState shapeGeometryState;
        if (surfaceShape instanceof SurfacePolyline surfacePolyline)
        {
            shapeGeometryState = new PolylineGeometryState(WWUtils.latLonIterableToILocations(surfacePolyline.getLocations()), surfacePolyline.isClosed());
        } else if (surfaceShape instanceof SurfacePolygon surfacePolygon)
        {
            List<List<ILocation>> polygonHoles = getPolygonHoles(surfacePolygon);
            shapeGeometryState = new PolygonGeometryState(WWUtils.latLonIterableToILocations(surfacePolygon.getLocations()), polygonHoles);
        } else if (surfaceShape instanceof SurfaceEllipse surfaceEllipse)
        {
            shapeGeometryState = new EllipseGeometryState(WWUtils.latLonToRaptorLocation(surfaceEllipse.getCenter()), surfaceEllipse.getHeading().degrees, surfaceEllipse.getMajorRadius(), surfaceEllipse.getMinorRadius(), 1);
        } else
        {
            shapeGeometryState = new PolygonGeometryState(WWUtils.latLonIterableToILocations(surfaceShape.getLocations(EARTH)), Collections.emptyList());
        }

        ShapeAttributes shapeAttributes = getShapeAttributes(surfaceShape);
        int interiorColor = shapeAttributes.isDrawInterior() ? getDiffuseColor(shapeAttributes.getInteriorMaterial(), shapeAttributes.getInteriorOpacity()) : 0;
        int outlineColor = shapeAttributes.isDrawOutline() ? getDiffuseColor(shapeAttributes.getOutlineMaterial(), shapeAttributes.getOutlineOpacity()) : 0;

        return new ShapeState(shapeGeometryState, surfaceShape.isVisible(), interiorColor, outlineColor, shapeAttributes.getOutlineWidth(),
                shapeAttributes.getOutlineStippleFactor(), shapeAttributes.getOutlineStipplePattern(), null, AltitudeMode.ClampToGround, 0);
    }

    private ShapeAttributes getShapeAttributes(SurfaceShape surfaceShape)
    {
        final boolean highlighted = surfaceShape.isHighlighted();
        ShapeAttributes shapeAttributes = highlighted ? surfaceShape.getHighlightAttributes() : surfaceShape.getAttributes();

        // The default shape attributes are often null; if that is the case, create them here.
        if (shapeAttributes == null)
        {
            logger.debug("No {} ShapeAttributes defined for {}; using defaults", highlighted ? "highlight" : "normal", surfaceShape);

            shapeAttributes = DEFAULT_SHAPE_ATTRIBUTES;
            if (highlighted)
            {
                surfaceShape.setHighlightAttributes(shapeAttributes);
            } else
            {
                surfaceShape.setAttributes(shapeAttributes);
            }
        }
        return shapeAttributes;
    }

    /**
     * @return Color made from WW Material and given opacity
     */
    private int getDiffuseColor(Material material, double opacity)
    {
        final int intOpacity = (int) (opacity * 255);
        final java.awt.Color diffuse = material.getDiffuse();
        return Color.argb(intOpacity, diffuse.getRed(), diffuse.getGreen(), diffuse.getBlue());
    }

    /**
     * Extract polygon hole information from {@link SurfacePolygon} as a list of ILocation lists.
     * @param surfacePolygon the surface polygon
     * @return a list of ILocation lists
     */
    @Nonnull
    private List<List<ILocation>> getPolygonHoles(@Nonnull SurfacePolygon surfacePolygon)
    {
        List<List<ILocation>> polygonHoles = new ArrayList<>();
        if (surfacePolygon.getValue(IGlobeMapObject.class.getName()) instanceof GlobeShapeMapObject globeShapeMapObject &&
                globeShapeMapObject.getShapeModel() instanceof IPolygon wwPolygon)
        {
            for (IPolygon hole : wwPolygon.getHoles())
            {
                if (hole.getVertices().isEmpty()) continue;
                polygonHoles.add(new ArrayList<>(hole.getVertices()));
            }
        }
        return polygonHoles;
    }
}
