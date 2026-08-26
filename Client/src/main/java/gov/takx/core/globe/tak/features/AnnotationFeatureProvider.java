package gov.takx.core.globe.tak.features;

import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle.ScrollMode;
import com.atakmap.map.layer.feature.style.LevelOfDetailStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.core.globe.RotatableGlobeAnnotation;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.globe.tak.RenderableAnnotationManager;
import gov.takx.platform.globe.GeometryAnnotation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.awt.Font;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A {@link IFeatureProvider} for {@link Annotation}.
 *
 * @since 3.5
 */
@PseudoBean
public final class AnnotationFeatureProvider implements IFeatureProvider<Annotation, AnnotationFeatureProvider.AnnotationState>
{
    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.RENDERABLE_PRIORITY, Annotation.class, AnnotationFeatureProvider::new)
    {
        @Nullable
        @Override
        protected IFeatureProvider<?, ?> createFeatureProvider(@Nonnull Object object)
        {
            // We can only service labels attached to a geopoint
            Annotation annotation = (Annotation) object;
            if (RenderableAnnotationManager.getPosition(annotation) == null) return null;
            return super.createFeatureProvider(object);
        }
    };

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    private FrameFactory frameFactory;

    @Nonnull
    @Override
    public AnnotationState createState(Annotation annotation)
    {
        final AnnotationAttributes attributes = annotation.getAttributes();
        Position position = RenderableAnnotationManager.getPosition(annotation);
        Geometry geometry = annotation instanceof GeometryAnnotation geometryAnnotation ? geometryAnnotation.getGeometry() : null;
        int width = attributes.getSize().width;
        int height = attributes.getSize().height;
        int textOpacity = (int) ((attributes.getTextColor().getRGB() >>> 24) * attributes.getOpacity());
        int textColor = (0x00FFFFFF & attributes.getTextColor().getRGB()) | (textOpacity << 24);
        int backgroundOpacity = (int) ((attributes.getBackgroundColor().getRGB() >>> 24) * attributes.getOpacity());
        int backgroundColor = (0x00FFFFFF & attributes.getBackgroundColor().getRGB()) | (backgroundOpacity << 24);
        boolean highlighted = attributes.isHighlighted();
        float labelScale = (float) (highlighted ? attributes.getHighlightScale() : attributes.getScale());
        int borderOpacity = (int) ((attributes.getBorderColor().getRGB() >>> 24) * attributes.getOpacity());
        int borderColor = (0x00FFFFFF & attributes.getBorderColor().getRGB()) | (borderOpacity << 24);
        float rotation = annotation instanceof RotatableGlobeAnnotation rotatableAnnotation
                ? (float) rotatableAnnotation.getTextRotationAngle() : 0;
        double minActiveAltitude = Math.max(annotation.getMinActiveAltitude(), IAnnotationMapObject.DEFAULT_MIN_ACTIVE_ALTITUDE);
        double maxActiveAltitude = Math.min(annotation.getMaxActiveAltitude(), IAnnotationMapObject.DEFAULT_MAX_ACTIVE_ALTITUDE);
        boolean isVisible = annotation.getAttributes().isVisible();

        return new AnnotationState(position, geometry, annotation.getText(), attributes.getFont(), width, height, textColor,
                backgroundColor, labelScale, attributes.getDrawOffset(), attributes.getCornerRadius(), FrameFactory.ShapeType.toShapeType(attributes.getFrameShape()),
                attributes.getBorderWidth(), borderColor, attributes.getImageSource(), attributes.getImageOffset(), rotation, minActiveAltitude,
                maxActiveAltitude, isVisible);
    }

    @Override
    public int getFeatureCount(@Nonnull AnnotationState state)
    {
        return 1;
    }

    @Override
    public boolean isVisible(@Nonnull AnnotationState state)
    {
        return state.isVisible;
    }

    @Nonnull
    @Override
    public String[] createNames(@Nonnull AnnotationState state)
    {
        // We want to use the annotation text in case the features renderer
        // decides to fall back to legacy behavior.
        return new String[]{state.text};
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull AnnotationState state)
    {
        Geometry geometry = state.geometry != null ? state.geometry : state.position != null ? new com.atakmap.map.layer.feature.geometry.Point(state.position.longitude.degrees, state.position.latitude.degrees) : null;
        return new Geometry[]{geometry};
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull AnnotationState state)
    {
        int offsetX = state.drawOffset.x;
        int offsetY = state.drawOffset.y;

        // We pass false for renderText since RenderableAnnotationManager will service text rendering via GLLabelManager.
        return createStyle(state, 0, -1, offsetX, offsetY, false, true, true, true);
    }

    public Style[] createStyle(AnnotationState state, int alignX, int alignY, float offsetX, float offsetY, boolean renderText,
                               boolean renderFrame, boolean renderBackgroundImage, boolean iconNorthUp)
    {
        List<Style> styles = new ArrayList<>();
        boolean hasFrame = hasFrame(state);
        boolean hasPredeterminedSize = state.width > 0 && state.height > 0;
        EnumSet<LabelPointStyle.Style> fontStyle = EnumSet.noneOf(LabelPointStyle.Style.class);
        if (state.font.isBold()) fontStyle.add(LabelPointStyle.Style.BOLD);
        if (state.font.isItalic()) fontStyle.add(LabelPointStyle.Style.ITALIC);
        // If the attributes have an image offset, then apply the additional offset
        float imageOffsetX = offsetX + (state.imageOffset != null ? state.imageOffset.x : 0);
        float imageOffsetY = offsetY + (state.imageOffset != null ? state.imageOffset.y : 0);

        if (renderFrame && hasFrame && hasPredeterminedSize)
        {
            BufferedImage image = frameFactory.createShapeFill(state.shapeType, state.width, state.height, state.cornerRadius, state.backgroundColor);
            String uri = FeatureProviderUtility.persistImageInVFS(image, projectVFSManager);
            uri = FeatureProviderUtility.sanitizeUri(uri);
            IconPointStyle iconPointStyle = new IconPointStyle(-1, uri, state.width, state.height, imageOffsetX, imageOffsetY, alignX, alignY, 0, true);
            styles.add(iconPointStyle);

            boolean drawBorder = state.borderWidth <= 0;
            if (drawBorder)
            {
                image = frameFactory.createShapeStroke(state.shapeType, state.width, state.height, state.cornerRadius, state.borderColor, (int) state.borderWidth);
                uri = FeatureProviderUtility.persistImageInVFS(image, projectVFSManager);
                uri = FeatureProviderUtility.sanitizeUri(uri);
                iconPointStyle = new IconPointStyle(-1, uri, state.width, state.height, imageOffsetX, imageOffsetY, alignX, alignY, 0, true);
                styles.add(iconPointStyle);
            }
        }
        if (renderBackgroundImage && state.imageSource != null)
        {
            String uri = (state.imageSource instanceof BufferedImage image) ? FeatureProviderUtility.persistImageInVFS(image, projectVFSManager) : state.imageSource.toString();
            uri = FeatureProviderUtility.sanitizeUri(uri);
            IconPointStyle iconPointStyle = new IconPointStyle(-1, uri, state.width, state.height, imageOffsetX, imageOffsetY, alignX, alignY, 0, iconNorthUp);
            styles.add(iconPointStyle);
        }
        if (renderText)
        {
            int labelFillColor = hasFrame && !hasPredeterminedSize ? state.backgroundColor : 0;
            LabelPointStyle labelStyle = new LabelPointStyle(state.text, state.textColor, labelFillColor, ScrollMode.DEFAULT, state.font.getName(), state.font.getSize(), fontStyle, offsetX, offsetY, alignX, alignY, state.rotation, false, 14d, state.labelScale);
            styles.add(labelStyle);
        } else
        {
            // Add two invisible points to prevent automatic adding of label
            styles.add(FeatureUtility.EMPTY_POINT_STYLE);
            styles.add(FeatureUtility.EMPTY_POINT_STYLE);
        }
        Style[] styleResult = {new CompositeStyle(styles.toArray(new Style[0]))};

        FeatureUtility.applyAnnotationAltitudeAsLod(state.minActiveAltitude, state.maxActiveAltitude, (minLod, maxLod) -> styleResult[0] = new LevelOfDetailStyle(styleResult[0], minLod, maxLod));

        return new Style[]{styleResult[0]};
    }

    private boolean hasFrame(AnnotationState state)
    {
        return state.shapeType != FrameFactory.ShapeType.None;
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull AnnotationState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(state.position.getAltitude() == 0 ? AltitudeMode.ClampToGround : AltitudeMode.Absolute, featureCount);
    }

    /**
     * State object for {@link AnnotationFeatureProvider}.
     * @since 5.1
     */
    public record AnnotationState(Position position, Geometry geometry, String text, Font font, int width, int height, int textColor, int backgroundColor, float labelScale, Point drawOffset, int cornerRadius, FrameFactory.ShapeType shapeType, double borderWidth, int borderColor, Object imageSource, Point imageOffset, float rotation, double minActiveAltitude, double maxActiveAltitude, boolean isVisible)
    {
    }
}
