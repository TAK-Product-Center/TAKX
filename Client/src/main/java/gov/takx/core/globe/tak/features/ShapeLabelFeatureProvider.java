package gov.takx.core.globe.tak.features;

import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;

/**
 * A {@link IFeatureProvider} for shape labels associated with {@link IGlobeShapeMapObject}.
 *
 * @since 5.0
 */
public final class ShapeLabelFeatureProvider extends AShapeLabelFeatureProvider<IGlobeShapeMapObject>
{
    @Nonnull
    @Override
    public ShapeLabelState createState(IGlobeShapeMapObject globeShapeMapObject)
    {
        final var shape = globeShapeMapObject.requireShapeModel();
        final var location = shape.getLocation();
        final double altitude = shape.isExtruded() ? shape.getVerticalHeight() : 0;
        boolean isVisible = globeShapeMapObject.isViewable() && UserPreferenceEntry.SHOW_SHAPE_LABELS.getBooleanValue();
        return new ShapeLabelState(location, shape.getName(), altitude, 0, 0, 0, -1, isVisible, 0, false);
    }
}
