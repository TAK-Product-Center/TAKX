package gov.takx.core.globe.tak.features;

import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.platform.globe.MarkerRenderable;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * A {@link IFeatureProvider} for {@link MarkerRenderable} which wraps a {@link Marker}.
 *
 * @since 3.5
 */
@PseudoBean
public final class MarkerFeatureProvider implements IFeatureProvider<MarkerRenderable, MarkerFeatureProvider.MarkerState>
{
    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    private FrameFactory frameFactory;

    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.RENDERABLE_PRIORITY, MarkerRenderable.class, MarkerFeatureProvider::new)
    {
    };

    @Nonnull
    @Override
    public MarkerState createState(MarkerRenderable markerRenderable)
    {
        Marker marker = markerRenderable.getMarker();
        MarkerAttributes attributes = marker.getAttributes();
        int opacity = (int) ((attributes.getMaterial().getDiffuse().getRGB() >>> 24) * attributes.getOpacity());
        int color = (0x00FFFFFF & attributes.getMaterial().getDiffuse().getRGB()) | (opacity << 24);
        float size = 2 * (float) attributes.getMarkerPixels();

        return new MarkerState(marker.getPosition(), color, size);
    }

    @Override
    public int getFeatureCount(@Nonnull MarkerState state)
    {
        return 1;
    }

    @Override
    public boolean isVisible(@Nonnull MarkerState state)
    {
        return true;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull MarkerState state)
    {
        Position position = state.position;
        return new Geometry[]{new Point(position.longitude.degrees, position.latitude.degrees)};
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull MarkerState state)
    {
        var image = frameFactory.createShapeFill(FrameFactory.ShapeType.Ellipse, (int) state.size, (int) state.size, 0, state.color);
        String uri = FeatureProviderUtility.persistImageInVFS(image, projectVFSManager);
        uri = FeatureProviderUtility.sanitizeUri(uri);
        var iconPointStyle = new IconPointStyle(-1, uri, (float) state.size, (float) state.size, 0, 0, 0, 0, 0, true);
        return new Style[]{FeatureUtility.makeNoLabelStyle(iconPointStyle)};
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull MarkerState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(AltitudeMode.ClampToGround, featureCount);
    }

    /**
     * State object for {@link MarkerFeatureProvider}.
     * @since 5.1
     */
    public record MarkerState(Position position, int color, double size)
    {
    }
}
