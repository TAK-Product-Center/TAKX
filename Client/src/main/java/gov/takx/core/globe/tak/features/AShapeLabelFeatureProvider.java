package gov.takx.core.globe.tak.features;

import android.graphics.Color;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.platform.globe.GlobeTextUtils;
import jakarta.annotation.Nonnull;

import java.util.EnumSet;

/**
 * An abstract {@link IFeatureProvider} for shape labels associated with {@link IGlobeMapObject}.
 *
 * @since 5.5
 */
public abstract class AShapeLabelFeatureProvider<T extends IGlobeMapObject> implements IFeatureProvider<T, AShapeLabelFeatureProvider.ShapeLabelState>
{
    private static final EnumSet<LabelPointStyle.Style> SHAPE_LABEL_FONT_STYLE = EnumSet.of(LabelPointStyle.Style.BOLD);

    @Nonnull
    @Override
    public String[] createNames(@Nonnull ShapeLabelState state)
    {
        return state.isVisible ? new String[]{state.text} : new String[0];
    }

    @Override
    public int getFeatureCount(@Nonnull ShapeLabelState state)
    {
        return state.isVisible ? 1 : 0;
    }

    @Override
    public boolean isVisible(@Nonnull ShapeLabelState state)
    {
        return state.isVisible;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull ShapeLabelState state)
    {
        return state.isVisible
                ? new Geometry[]{new Point(state.location.getLongitude(), state.location.getLatitude(), state.altitude)}
                : new Geometry[0];
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull ShapeLabelState state)
    {
        if (!state.isVisible) return new Style[0];
        Style labelStyle = new LabelPointStyle(state.text, Color.WHITE, 0, LabelPointStyle.ScrollMode.DEFAULT,
                GlobeTextUtils.SHAPE_LABEL_FONT.getName(), GlobeTextUtils.SHAPE_LABEL_FONT.getSize(), SHAPE_LABEL_FONT_STYLE, state.offsetX, state.offsetY,
                state.alignX, state.alignY, state.rotation, state.rotationAbsolute, 14d, 1);
        return new Style[]{labelStyle};
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull ShapeLabelState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(AltitudeMode.Relative, featureCount);
    }

    /**
     * State object for {@link AShapeLabelFeatureProvider}.
     */
    public record ShapeLabelState(ILocation location, String text, double altitude, float offsetX, float offsetY, int alignX, int alignY, boolean isVisible, float rotation, boolean rotationAbsolute)
    {
    }
}
