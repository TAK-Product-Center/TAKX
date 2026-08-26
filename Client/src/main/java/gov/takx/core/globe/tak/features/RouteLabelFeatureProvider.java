package gov.takx.core.globe.tak.features;

import gov.takx.core.globe.GlobeRouteMapObject;
import gov.takx.dataservices.shapes.Route;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;

/**
 * A {@link IFeatureProvider} for shape labels associated with {@link GlobeRouteMapObject}.
 *
 * @since 5.5
 */
public final class RouteLabelFeatureProvider extends AShapeLabelFeatureProvider<GlobeRouteMapObject>
{
    @Nonnull
    @Override
    public ShapeLabelState createState(GlobeRouteMapObject globeShapeMapObject)
    {
        Route shape = globeShapeMapObject.requireShapeModel();
        final var location = !shape.getVertices().isEmpty() ? shape.getVertices().get(0) : shape.getLocation();
        final double altitude = shape.isExtruded() ? shape.getVerticalHeight() : 0;
        boolean isVisible = globeShapeMapObject.isViewable() && UserPreferenceEntry.SHOW_SHAPE_LABELS.getBooleanValue();
        return new ShapeLabelState(location, shape.getName(), altitude, 0, -24, 0, 0, isVisible, 0, false);
    }
}
