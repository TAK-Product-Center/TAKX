package gov.takx.core.globe.tak.features;

import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle.ScrollMode;
import com.atakmap.map.layer.feature.style.Style;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.GlobeRouteMapObject;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.globe.tak.editor.IControlPoint;
import gov.takx.dataservices.shapes.Route;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.awt.image.BufferedImage;
import java.util.List;

import static gov.takx.api.shapes.IRoute.DISPLAY_NAME;

/**
 * A {@link IFeatureProvider} for {@link GlobeRouteMapObject}.
 *
 * @since 5.5
 */
@PseudoBean
public final class GlobeRouteFeatureProvider implements IFeatureProvider<GlobeRouteMapObject, GlobeRouteFeatureProvider.GlobeRouteState>
{
    private static String waypointIconUrl;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    private FrameFactory frameFactory;

    @Nonnull
    @Override
    public GlobeRouteState createState(GlobeRouteMapObject globeRouteMapObject)
    {
        Route route = globeRouteMapObject.getShapeModel();
        return new GlobeRouteState(route.getVertices(), route.isVisible(), route.getLineColor().getRGB(), route.getLineWidth(), route.getWaypoints());
    }

    @Override
    public int getFeatureCount(@Nonnull GlobeRouteState state)
    {
        return state.waypoints.size() + 1;
    }

    @Override
    public boolean isVisible(@Nonnull GlobeRouteState state)
    {
        return state.isVisible;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull GlobeRouteState state)
    {
        int featureCount = getFeatureCount(state);
        Geometry[] geometries = new Geometry[featureCount];
        int index = 0;
        for (GeoPointMetaData waypoint : state.waypoints)
        {
            Point point = new Point(waypoint.get().getLongitude(), waypoint.get().getLatitude(), 0);
            geometries[index++] = point;
        }
        LineString lineString = new LineString(3);
        state.vertices.forEach(g -> lineString.addPoint(g.getLongitude(), g.getLatitude(), g.getAltitude()));
        geometries[index++] = lineString;
        return geometries;
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull GlobeRouteState state)
    {
        int featureCount = getFeatureCount(state);
        Style[] styles = new Style[featureCount];
        int index = 0;
        String iconUri = getWaypointIconUrl(frameFactory, projectVFSManager);
        for (GeoPointMetaData waypoint : state.waypoints)
        {
            IconPointStyle pointStyle = new IconPointStyle(-1, iconUri);
            String labelText = waypoint.getMetaData().getStringAttribute(DISPLAY_NAME, "");

            Style labelStyle;
            if (StringUtils.isEmpty(labelText))
            {
                labelStyle = FeatureUtility.EMPTY_POINT_STYLE;
            } else
            {
                labelStyle = new LabelPointStyle(labelText, -1, 0, ScrollMode.DEFAULT, 14, -32, 32, 1, 1, 0, false, 14d, 1f);
            }
            styles[index++] = new CompositeStyle(new Style[]{pointStyle, labelStyle});
        }
        BasicStrokeStyle lineStyle = new BasicStrokeStyle(state.lineColor, (float) state.lineWidth);
        styles[index++] = lineStyle;
        return styles;
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull GlobeRouteState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(AltitudeMode.ClampToGround, featureCount);
    }

    private static synchronized String getWaypointIconUrl(FrameFactory frameFactory, IVirtualFileSystemManager projectVFSManager)
    {
        if (waypointIconUrl == null)
        {
            int size = IControlPoint.DEFAULT_POINT_SIZE / 4;
            BufferedImage image = frameFactory.createShapeFill(FrameFactory.ShapeType.Ellipse, size, size, 0, -1);
            // Get the image persisted in VFS
            final IVirtualFileSystem vfs = projectVFSManager.getVirtualFileSystem(IMapObject.MAP_OBJECT_ICONS_DIR);
            String uri = ImageUtils.writeImageToCanonicalFile(vfs.getRootDir(), image).toString();
            waypointIconUrl = FeatureUtility.sanitizeUri(uri);
        }
        return waypointIconUrl;
    }

    /**
     * State object for {@link GlobeRouteFeatureProvider}.
     */
    public record GlobeRouteState(List<ILocation> vertices, boolean isVisible, int lineColor, double lineWidth, List<GeoPointMetaData> waypoints)
    {
    }
}
