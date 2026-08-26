package gov.takx.core.globe.tak.features;

import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle.ScrollMode;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.math.MathUtils;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.globe.IStateIconManager;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.core.globe.GlobePlaybackIconMapObject;
import gov.takx.core.globe.GlobeStateIcon;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.globe.tak.features.AnnotationFeatureProvider.AnnotationState;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.platform.globe.CardinalPoint;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.UIManager;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A {@link IFeatureProvider} for {@link IGlobeMarkerMapObject}.
 *
 * @since 3.5
 */
@PseudoBean
public final class GlobeMarkerFeatureProvider implements IFeatureProvider<IGlobeMarkerMapObject, GlobeMarkerFeatureProvider.GlobeMarkerState>
{
    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.GLOBE_MAP_OBJECT_PRIORITY, IGlobeMarkerMapObject.class, GlobeMarkerFeatureProvider::new)
    {
        @Nullable
        @Override
        protected IFeatureProvider<?, ?> createFeatureProvider(@Nonnull Object object)
        {
            final IGlobeMarkerMapObject globeMarkerMapObject = (IGlobeMarkerMapObject) object;
            if (globeMarkerMapObject.getMapObject() == null) return null;

            return new GlobeMarkerFeatureProvider();
        }
    };

    private static final double DEFAULT_HIGHLIGHT_SCALE = 1.3;
    private static final AnnotationFeatureProvider ANNOTATION_FEATURE_PROVIDER = new AnnotationFeatureProvider();
    private static final String TRACK_HEADING_ARROW_URL = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, "track_heading_arrow.png").toString();
    private static final String OUTLINE_ICON = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, "outline.png").toString();
    // The outline icon is 50x50 and intended to overlay a 32x32 icon
    private static final float OUTLINE_SCALE = 50f / 32f;

    private static final Font TOOLTIP_FONT = UIManager.getFont("ToolTip.font");
    private static final String TOOLTIP_FONT_NAME = TOOLTIP_FONT.getName();
    private static final int TOOLTIP_FONT_SIZE = TOOLTIP_FONT.getSize();
    private static final int TOOLTIP_COLOR = UIManager.getColor("ToolTip.foreground").getRGB();
    private static final int TOOLTIP_BG_COLOR = UIManager.getColor("ToolTip.background").getRGB();
    private static final EnumSet<LabelPointStyle.Style> TOOLTIP_FONT_STYLE = EnumSet.noneOf(LabelPointStyle.Style.class);

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    private IMilStd2525ServiceProvider milStd2525ServiceProvider;

    @Nonnull
    @Override
    public GlobeMarkerState createState(IGlobeMarkerMapObject globeMarkerMapObject)
    {
        IMapObject mapObject = globeMarkerMapObject.getMapObject();

        boolean isClampToGround = isClampToGround(globeMarkerMapObject);
        Dimension markerSize = globeMarkerMapObject.getCurrentImageSize();
        double scale = mapObject.getIconScale();
        if (globeMarkerMapObject.isHighlighted()) scale *= DEFAULT_HIGHLIGHT_SCALE;
        int style = mapObject instanceof Marker marker ? marker.getStyle() : 0;
        final boolean renderWithHeadingIndicator = MathUtils.hasBits(style, Marker.STYLE_ROTATE_HEADING_MASK) && !MathUtils.hasBits(style, Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);
        final boolean renderRotatedMarker = MathUtils.hasBits(style, Marker.STYLE_ROTATE_HEADING_MASK) && MathUtils.hasBits(style, Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);
        int alignY = 0;
        if (mapObject instanceof IconMapObject iconMapObject && iconMapObject.getTrackStyleDefinition() != null &&
                iconMapObject.getTrackStyleDefinition().hasDefinedGlyphVerticalAlignment())
        {
            alignY = switch (iconMapObject.getTrackStyleDefinition().getGlyphVerticalAlignment())
            {
                case Top -> -1;
                case Middle -> 0;
                case Bottom -> 1;
            };
        }

        float iconWidth;
        float iconHeight;
        float iconOffsetX;
        float iconOffsetY;
        boolean has2525Symbol = milStd2525ServiceProvider.get2525ServiceForMapObject(mapObject).hasSymbolCode(mapObject)
                // This check for the 'adapt icon' flag acts to verify the 2525 icon adapter is what specified the MO's icon image.
                // If the user defines a custom image the flag will be set to 'false' and we will NOT want to treat it specially like other 2525 icons.
                && (mapObject.getMetaBoolean(StandardAttributeNames.ADAPT_MARKER_ICON, true) || mapObject.getMetaBoolean(StandardAttributeNames.MIL_SYM_ADAPTING, false));
        if (has2525Symbol)
        {
            // If we have 2525 symbols on this marker we will not use the marker size, but the symbol's rendered image size.
            // We assume that the 2525 icon adapter has set the image beforehand and is configured to create
            // reasonably sized images (32px) for displaying on the globe.
            BufferedImage iconImage = mapObject.getIconImage();
            iconWidth = (float) (iconImage.getWidth() * scale);
            iconHeight = (float) (iconImage.getHeight() * scale);
            var attributeSet = mapObject.getAttributeSet();
            int cx = attributeSet.getIntAttribute(StandardAttributeNames.MIL_SYM_CENTER_X, -1);
            int cy = attributeSet.getIntAttribute(StandardAttributeNames.MIL_SYM_CENTER_Y, -1);
            if (cx > 0 && cy > 0)
            {
                iconOffsetX = (float) (iconWidth / 2F - (cx * scale));
                iconOffsetY = (float) -(iconHeight / 2F - (cy * scale));
            } else
            {
                iconOffsetX = 0;
                iconOffsetY = 0;
            }
        } else
        {
            // Otherwise we will use the marker's size for displaying the specified image source.
            // This is generally 32px coming from GlobeIconMapObject.DEFAULT_BASE_SIZE.
            iconWidth = (float) (markerSize.width * scale);
            iconHeight = (float) (markerSize.height * scale);
            iconOffsetX = 0;
            iconOffsetY = 0;
        }

        float backgroundScale = (float) globeMarkerMapObject.getBackgroundScale();
        float backgroundIconWidth = markerSize.width * backgroundScale;
        float backgroundIconHeight = markerSize.height * backgroundScale;
        Object backgroundImage = globeMarkerMapObject.getBackgroundImage();
        Object imageSource = globeMarkerMapObject.getImageSource();

        boolean hasHeading = mapObject.hasHeading();
        double heading = hasHeading ? mapObject.getHeading() : 0;

        var stateIconStates = createStateIconStates(globeMarkerMapObject, iconWidth, iconHeight, alignY);
        var tooltipState = createTooltipState(globeMarkerMapObject);
        var selectedIconState = createSelectedIconState(globeMarkerMapObject, iconWidth, iconHeight, has2525Symbol);

        return new GlobeMarkerState(globeMarkerMapObject.getLocation(), isClampToGround, shouldShowMarker(globeMarkerMapObject),
                scale, renderWithHeadingIndicator, renderRotatedMarker, alignY, iconWidth, iconHeight, iconOffsetX, iconOffsetY,
                imageSource, backgroundImage, backgroundIconWidth, backgroundIconHeight, backgroundScale, hasHeading, heading,
                stateIconStates, tooltipState, globeMarkerMapObject.isLabelOnly(), selectedIconState);
    }

    /**
     * Determine if the marker should be visible.  Special handling to hide a "live" icon when
     * {@link PreferenceEntry#PLAYBACK_SHOW_LIVE_MARKERS} is {@code false}.
     * @since 5.7
     */
    private boolean shouldShowMarker(IGlobeMarkerMapObject globeMarkerMapObject)
    {
        if (!globeMarkerMapObject.isViewable()) return false;

        if (!(globeMarkerMapObject instanceof GlobePlaybackIconMapObject)) // This detects the "live" marker
        {
            return !globeMarkerMapObject.getMapObject().inPlaybackMode()
                    || PreferenceEntry.PLAYBACK_SHOW_LIVE_MARKERS.getBooleanValue();
        }
        return true;
    }

    @Override
    public int getFeatureCount(@Nonnull GlobeMarkerState state)
    {
        return 1;
    }

    @Override
    public boolean isVisible(@Nonnull GlobeMarkerState state)
    {
        return state.isVisible;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull GlobeMarkerState state)
    {
        final ILocation location = state.location;
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();
        final double hae = state.isClampToGround
                ? -1000 // Arbitrary value to ensure the points snap to the surface regardless of the terrain used/not used
                : location.getElevation() + ElevationManager.getGeoidHeight(latitude, longitude);
        return new Geometry[]{new Point(longitude, latitude, hae)};
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull GlobeMarkerState state)
    {
        List<Style> styles = new ArrayList<>();
        float iconStateWidth = state.iconWidth;
        float iconStateHeight = state.iconHeight;
        float offsetX = state.offsetX;
        float offsetY = state.offsetY;
        String uri;

        if (state.isLabelOnly)
        {
            uri = ""; // Don't show the icon if label only
        } else if (state.imageSource instanceof BufferedImage iconImage)
        {
            if (state.backgroundImage instanceof BufferedImage backgroundIconImage)
            {
                final float backgroundIconWidth = state.backgroundIconWidth;
                final float backgroundIconHeight = state.backgroundIconHeight;

                iconStateWidth = Math.max(state.iconWidth, backgroundIconWidth);
                iconStateHeight = Math.max(state.iconHeight, backgroundIconHeight);

                var compositeImage = new BufferedImage((int) iconStateWidth, (int) iconStateHeight, BufferedImage.TYPE_4BYTE_ABGR);
                var g2d = compositeImage.createGraphics();
                g2d.drawImage(backgroundIconImage, (int) (iconStateWidth - backgroundIconWidth) / 2, (int) (iconStateHeight - backgroundIconHeight) / 2, (int) backgroundIconWidth, (int) backgroundIconHeight, null);
                g2d.drawImage(iconImage, (int) (iconStateWidth - state.iconWidth) / 2, (int) (iconStateHeight - state.iconHeight) / 2, (int) state.iconWidth, (int) state.iconHeight, null);
                iconImage = compositeImage;

                // Adjust offset of top/bottom vertical aligned markers to account for additional area added by background image
                final float max = Math.max(0, (backgroundIconHeight - state.iconHeight) / 2);
                if (state.alignY == -1) offsetY = -max;
                if (state.alignY == 1) offsetY = max;
            }
            uri = FeatureProviderUtility.persistImageInVFS(iconImage, projectVFSManager);
        } else if (state.imageSource instanceof String)
        {
            uri = state.imageSource.toString();
        } else
        {
            uri = "";
        }

        uri = FeatureProviderUtility.sanitizeUri(uri);

        // TME uses degrees counter-clockwise from North for heading, we use clockwise, so invert it.
        double heading = -state.heading;
        if (state.renderWithHeadingIndicator && state.hasHeading)
        {
            var iconHeadingPointStyle = new IconPointStyle(-1, TRACK_HEADING_ARROW_URL, 64f, 64f, 0, 0, (float) heading, true);
            styles.add(iconHeadingPointStyle);
        }

        var iconPointStyle = new IconPointStyle(-1, uri, iconStateWidth, iconStateHeight, offsetX, offsetY, 0, state.alignY, state.renderRotatedMarker ? (float) heading : 0, state.renderRotatedMarker);
        styles.add(iconPointStyle);
        styles.add(FeatureUtility.EMPTY_POINT_STYLE); // Required to prevent automatic adding of label

        if (state.selectedIconState != null)
        {
            // We use an offset of 0,0 for the outline icon because the only current use of pixel offsets are for positioning the 2525 icon to be centered over the geopoint
            var outlineIconPointStyle = new IconPointStyle(-1, OUTLINE_ICON, state.selectedIconState.iconWidth * OUTLINE_SCALE, state.selectedIconState.iconHeight * OUTLINE_SCALE, 0, 0, 0, state.alignY, 0, false);
            styles.add(outlineIconPointStyle);
        }

        state.stateIconStates.forEach(s -> {
            Style style = ANNOTATION_FEATURE_PROVIDER.createStyle(s.state, s.alignX, s.alignY, s.offsetX, s.offsetY, s.isText, s.isText, s.renderBackgroundImage, false)[0];
            if (style != null) styles.add(style);
        });

        if (state.tooltipState != null)
        {
            float labelOffsetY = state.isLabelOnly ? 0 : 0 - iconStateHeight / 2;
            int alignY = state.isLabelOnly ? 0 : 1;
            styles.add(new LabelPointStyle(state.tooltipState.text, TOOLTIP_COLOR, TOOLTIP_BG_COLOR, ScrollMode.DEFAULT, TOOLTIP_FONT_NAME, TOOLTIP_FONT_SIZE,
                    TOOLTIP_FONT_STYLE, 0, labelOffsetY, 0, alignY, 0f, false, 14d, 1f));
        }

        var compositePointStyle = new CompositeStyle(styles.toArray(new Style[0]));
        return new Style[]{compositePointStyle};
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull GlobeMarkerState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(state.isClampToGround ? AltitudeMode.ClampToGround : AltitudeMode.Absolute, featureCount);
    }

    private List<StateIconState> createStateIconStates(IGlobeMarkerMapObject globeMarkerMapObject, float iconWidth, float iconHeight, int imageAlignY)
    {
        final List<StateIconState> states = new ArrayList<>();

        final var stateIconManager = getStateIconManager(globeMarkerMapObject);
        if (stateIconManager != null && stateIconManager.needsUpdate())
        {
            stateIconManager.updateVisuals();

            float halfWidth = iconWidth / 2;
            float halfHeight = iconHeight / 2;
            float imageOffsetY = imageAlignY == -1 ? iconHeight / 2f : imageAlignY == 1 ? -iconHeight / 2f : 0;

            for (CardinalPoint position : stateIconManager.findPositions(false))
            {
                final var stateIcon = (GlobeStateIcon) stateIconManager.getStateIcon(position);

                if (!stateIcon.isVisible()) continue; // Skip if not visible

                int alignX = switch (position)
                {
                    case WEST, NORTH_WEST, SOUTH_WEST -> -1;
                    case NORTH, SOUTH -> 0;
                    case EAST, NORTH_EAST, SOUTH_EAST -> 1;
                };
                int alignY = switch (position)
                {
                    case NORTH, NORTH_WEST, NORTH_EAST -> -1;
                    case EAST, WEST -> 0;
                    case SOUTH, SOUTH_WEST, SOUTH_EAST -> 1;
                };
                float offsetX = switch (position)
                {
                    case WEST, NORTH_WEST, SOUTH_WEST -> -halfWidth;
                    case NORTH, SOUTH -> 0;
                    case EAST, NORTH_EAST, SOUTH_EAST -> halfWidth;
                };
                float offsetY = switch (position)
                {
                    case NORTH, NORTH_WEST, NORTH_EAST -> halfHeight;
                    case EAST, WEST -> 0;
                    case SOUTH, SOUTH_WEST, SOUTH_EAST -> -halfHeight;
                } + imageOffsetY;

                final double spacing = stateIconManager.getSpacing();
                offsetY = (float) (offsetY * spacing);
                offsetX = (float) (offsetX * spacing);

                // We want to replicate the behavior of GlobeStateIcon$StateIconAnnotation here.
                // If the annotation has an image, then we don't want to render label frame and text.
                final var state = ANNOTATION_FEATURE_PROVIDER.createState(stateIcon.getAnnotation());
                states.add(new StateIconState(state, alignX, alignY, offsetX, offsetY, !stateIcon.isImage(), true));
            }
        }
        return states;
    }

    private TooltipState createTooltipState(IGlobeMarkerMapObject globeMarkerMapObject)
    {
        // If the tooltips visibility is being forced, then obey that
        final boolean showTooltip = globeMarkerMapObject.isTooltipVisibilityForced()
                ? globeMarkerMapObject.isShowToolTip()
                : globeMarkerMapObject.isHighlighted() || globeMarkerMapObject.isShowToolTip();

        return showTooltip ? new TooltipState(globeMarkerMapObject.getToolTipText()) : null;
    }

    private SelectedIconState createSelectedIconState(IGlobeMarkerMapObject globeMarkerMapObject, double iconWidth, double iconHeight, boolean has2525Symbol)
    {
        // Use the markerSize for 2525 objects so we use the base icon
        // size not the rendered icon size which will include modifier labels
        Dimension markerSize = globeMarkerMapObject.getCurrentImageSize();
        double highlightScale = globeMarkerMapObject.isHighlighted() ? DEFAULT_HIGHLIGHT_SCALE : 1;
        double selectedIconWidth = has2525Symbol ? markerSize.width * highlightScale : iconWidth;
        double selectedIconHeight = has2525Symbol ? markerSize.height * highlightScale : iconHeight;
        return globeMarkerMapObject.isSelected()
                ? new SelectedIconState((float) selectedIconWidth, (float) selectedIconHeight)
                : null;
    }

    private IStateIconManager getStateIconManager(IGlobeMarkerMapObject globeMarkerMapObject)
    {
        if (!(globeMarkerMapObject.getMapObject() instanceof IIconMapObject iconMapObject)) return null;
        final var controller = iconMapObject.getController();
        return globeMarkerMapObject instanceof GlobePlaybackIconMapObject
                ? controller.getPlaybackStateIconManager()
                : controller.getStateIconManager();
    }

    private boolean isClampToGround(IGlobeMarkerMapObject globeMarkerMapObject)
    {
        IMapObject mapObject = globeMarkerMapObject.getMapObject();
        boolean snapIconToSurface = mapObject.supportsSnapIconToSurface() && mapObject.isSnapIconToSurface();
        return globeMarkerMapObject.isRenderingOnSurface() || snapIconToSurface;
    }

    /**
     * State object for icon state properties in {@link GlobeMarkerState}.
     * @since 5.1
     */
    private record StateIconState(AnnotationState state, int alignX, int alignY, float offsetX, float offsetY, boolean isText, boolean renderBackgroundImage)
    {
    }

    /**
     * State object for tooltip properties in {@link GlobeMarkerState}.
     * @since 5.1
     */
    private record TooltipState(String text)
    {
    }

    /**
     * State object for selected icon properties in {@link GlobeMarkerState}.
     * @since 5.6
     */
    private record SelectedIconState(float iconWidth, float iconHeight)
    {
    }

    /**
     * State object for {@link GlobeMarkerFeatureProvider}.
     * @since 5.1 (signature change in 5.5.1)
     */
    public record GlobeMarkerState(ILocation location, boolean isClampToGround, boolean isVisible, double scale, boolean renderWithHeadingIndicator,
            boolean renderRotatedMarker, int alignY, float iconWidth, float iconHeight, float offsetX, float offsetY, Object imageSource,
            Object backgroundImage, float backgroundIconWidth, float backgroundIconHeight, float backgroundIconScale, boolean hasHeading,
            double heading, List<StateIconState> stateIconStates, TooltipState tooltipState, boolean isLabelOnly,
            SelectedIconState selectedIconState)
    {
    }
}
