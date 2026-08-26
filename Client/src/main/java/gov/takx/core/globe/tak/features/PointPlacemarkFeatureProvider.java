package gov.takx.core.globe.tak.features;

import android.net.Uri;
import com.atakmap.map.elevation.ElevationManager;
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
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.tak.api.engine.math.PointD;
import gov.tak.platform.graphics.Color;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.tak.RenderableManager;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.BiConsumer;

/**
 * A {@link IFeatureProvider} for {@link PointPlacemark}.
 *
 * @since 3.5
 */
@PseudoBean
public final class PointPlacemarkFeatureProvider implements IFeatureProvider<PointPlacemark, PointPlacemarkFeatureProvider.PointPlacemarkState>
{
    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.RENDERABLE_PRIORITY, PointPlacemark.class, PointPlacemarkFeatureProvider::new)
    {
    };

    private static final int FEATURE_COUNT = 2;

    private static final int DEFAULT_IMAGE_COLOR = Color.WHITE;
    private static final int DEFAULT_LABEL_COLOR = Color.WHITE;
    private static final int DEFAULT_LINE_COLOR = Color.WHITE;
    private static final int TRANSPARENT_COLOR = 0;

    static
    {
        PointPlacemark.makeOrderedRenderableFactory = new OrderedRenderableFactory();
    }

    @PseudoBean
    private static class OrderedRenderableFactory implements BiConsumer<PointPlacemark, DrawContext>
    {
        @Inject
        RenderableManager renderableManager;

        @Override
        public void accept(PointPlacemark pointPlacemark, DrawContext drawContext)
        {
            renderableManager.offerRenderable(pointPlacemark, drawContext);
        }
    }

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Nonnull
    @Override
    public PointPlacemarkState createState(PointPlacemark pointPlacemark)
    {
        var attributes = getAttributes(pointPlacemark);
        var defaultAttributes = pointPlacemark.getDefaultAttributes();
        int imageColor = attributes.getImageColor() != null ? attributes.getImageColor().getRGB() : DEFAULT_IMAGE_COLOR;
        String imageAddress = attributes.getImageAddress() != null ? attributes.getImageAddress() : defaultAttributes.getImageAddress();
        PointD imageSize = getImageSize(pointPlacemark, attributes);
        final Offset imageAttrOffset = attributes.getImageOffset() != null ? attributes.getImageOffset() : defaultAttributes.getImageOffset();
        Point2D.Double imageOffset = imageAttrOffset.computeOffset(imageSize.x, imageSize.y, null, null);
        float rotation = attributes.getHeading() != null ? attributes.getHeading().floatValue() : 0f;
        boolean drawLabel = !StringUtils.isEmpty(pointPlacemark.getLabelText()) && attributes.isDrawLabel();
        String labelText = drawLabel ? pointPlacemark.getLabelText() : "";
        int textColor = drawLabel
                ? attributes.getLabelColor() != null ? attributes.getLabelColor().getRGB() : DEFAULT_LABEL_COLOR
                : TRANSPARENT_COLOR;
        int textSize = attributes.getLabelFont() != null ? attributes.getLabelFont().getSize() : PointPlacemarkAttributes.DEFAULT_LABEL_FONT.getSize();
        float labelScale = attributes.getLabelScale() != null ? attributes.getLabelScale().floatValue() : defaultAttributes.getLabelScale().floatValue();
        final Offset labelAttrOffset = attributes.getLabelOffset() != null ? attributes.getLabelOffset() : defaultAttributes.getLabelOffset();
        Point2D.Double labelOffset = labelAttrOffset.computeOffset((float) imageSize.x, (float) imageSize.y, null, null);
        int lineColor = attributes.getLineColor() != null ? attributes.getLineColor().getRGB() : DEFAULT_LINE_COLOR;
        float lineWidth = attributes.getLineWidth() != null ? attributes.getLineWidth().floatValue() : 0f;

        return new PointPlacemarkState(pointPlacemark.getPosition(), pointPlacemark.isVisible(), imageColor, attributes.getImage(),
                imageAddress, imageSize, imageOffset, rotation, drawLabel, labelText, textColor, textSize, labelScale, labelOffset,
                lineColor, lineWidth, pointPlacemark.getAltitudeMode());
    }

    @Override
    public int getFeatureCount(@Nonnull PointPlacemarkState state)
    {
        return FEATURE_COUNT;
    }

    @Override
    public boolean isVisible(@Nonnull PointPlacemarkState state)
    {
        return state.isVisible;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull PointPlacemarkState state)
    {
        final Position position = state.position;
        double hae = position.elevation + ElevationManager.getGeoidHeight(position.latitude.degrees, position.longitude.degrees);
        Point point = new Point(position.longitude.degrees, position.latitude.degrees, hae);
        LineString line = new LineString(3);
        line.addPoint(position.longitude.degrees, position.latitude.degrees, hae);
        line.addPoint(position.longitude.degrees, position.latitude.degrees, 0);

        return new Geometry[]{point, line};
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull PointPlacemarkState state)
    {
        String imageUri;
        if (state.image != null)
        {
            imageUri = FeatureProviderUtility.persistImageInVFS(state.image, projectVFSManager);
        } else
        {
            if (new File(state.imageAddress).exists())
            {
                imageUri = "file://" + state.imageAddress;
            } else if (WorldWind.class.getResource("/" + state.imageAddress) != null)
            {
                imageUri = "ww:/" + state.imageAddress;
            } else
            {
                imageUri = Uri.parse(state.imageAddress).toString();
            }
        }
        imageUri = FeatureProviderUtility.sanitizeUri(imageUri);
        var iconPointStyle = new IconPointStyle(state.imageColor, imageUri, (float) state.imageSize.x, (float) state.imageSize.y, (float) state.imageOffset.x, (float) state.imageOffset.y, -1, 1, state.rotation, true);
        var labelPointStyle = new LabelPointStyle(state.labelText, state.textColor, 0, ScrollMode.DEFAULT, state.textSize, (float) state.labelOffset.x, (float) state.labelOffset.y, 1, 1, state.rotation, false, 14d, state.labelScale);
        var compositePointStyle = new CompositeStyle(new Style[]{iconPointStyle, labelPointStyle});

        var lineStyle = new BasicStrokeStyle(state.lineColor, state.lineWidth);

        return new Style[]{compositePointStyle, lineStyle};
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull PointPlacemarkState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArrayForWWMode(state.altitudeMode, featureCount);
    }

    private static PointPlacemarkAttributes getAttributes(PointPlacemark pointPlacemark)
    {
        var attributes = pointPlacemark.isHighlighted() ? pointPlacemark.getHighlightAttributes() : pointPlacemark.getAttributes();
        return attributes != null ? attributes : pointPlacemark.getDefaultAttributes();
    }

    private static PointD getImageSize(PointPlacemark pointPlacemark, PointPlacemarkAttributes attributes)
    {
        PointPlacemark.OrderedPlacemark orderedPlacemark = (PointPlacemark.OrderedPlacemark) pointPlacemark.getValue(PointPlacemark.OrderedPlacemark.class.getName());
        double globalScale = UserPreferenceEntry.GLOBAL_ICON_SCALE.getDoubleValue();
        if (orderedPlacemark != null)
        {
            Rectangle imageBounds = orderedPlacemark.getImageBounds();
            return new PointD(imageBounds.getWidth() * globalScale, imageBounds.getHeight() * globalScale, 0);
        } else if (attributes.getImage() != null)
        {
            BufferedImage image = attributes.getImage();
            double imageScale = attributes.getScale() != null ? attributes.getScale() : pointPlacemark.getDefaultAttributes().getScale();
            imageScale *= globalScale;
            return new PointD(image.getWidth() * imageScale, image.getHeight() * imageScale, 0);
        } else
        {
            return new PointD(32.0 * globalScale, 32.0 * globalScale, 0);
        }
    }

    /**
     * State object for {@link PointPlacemarkFeatureProvider}.
     * @since 5.1
     */
    public record PointPlacemarkState(Position position, boolean isVisible, int imageColor, BufferedImage image, String imageAddress, PointD imageSize,
            Point2D.Double imageOffset, float rotation, boolean drawLabel, String labelText, int textColor, int textSize, float labelScale, Point2D.Double labelOffset,
            int lineColor, float lineWidth, int altitudeMode)
    {
    }
}
