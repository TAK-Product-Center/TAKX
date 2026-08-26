package gov.takx.api.milsym;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.symbology.Status;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Outlines an assistant for creating a mil-sym graphic. Allows specifying image attributes via a builder pattern.
 *
 * @since 5.1
 */
@NoPluginImplementations
public interface IMilSymIconBuilder
{
    /**
     * Value used by {@link ISymbolTable.Symbol#getPreviewDrawable(int)}'s default implementation.
     * <p/>
     * Used as {@link IMilSymIconBuilder}'s default resolution.
     */
    int DEFAULT_PREVIEW_SIZE = 128;

    /**
     * Value used by {@link ISymbologyProvider#renderSinglePointIcon(String, AttributeSet, ISymbologyProvider.RendererHints)}'s
     * default implementation.
     */
    int DEFAULT_SINGLE_POINT_SIZE = 32;

    /**
     * @return Current symbol of the builder.
     */
    @Nullable
    ISymbolTable.Symbol getSymbol();

    /**
     * @return Current symbol code of the builder.
     */
    @Nullable
    String getCode();

    /**
     * @return Current symbol affiliation of the builder.
     */
    @Nullable
    Affiliation getAffiliation();

    /**
     * @return Current symbol shape of the builder.
     */
    @Nullable
    ShapeType getShapeType();

    /**
     * @return Current symbol resolution of the builder.
     * @see #DEFAULT_PREVIEW_SIZE
     */
    int getResolution();

    /**
     * @return Current attributes of the builder.
     * @since 5.5
     */
    @Nonnull
    AttributeSet getAttributes();

    /**
     * @param code Symbol code to render.
     * @return Builder.
     */
    @Nonnull
    IMilSymIconBuilder setCode(@Nonnull String code);

    /**
     * @param affiliation Affiliation to apply to the symbol.
     * @return Builder.
     */
    @Nonnull
    IMilSymIconBuilder setAffiliation(@Nullable Affiliation affiliation);

    /**
     * @param echelon Echelon amplifier to apply to the symbol.
     * @return Builder.
     * @since 5.2
     * @deprecated since = "5.5", forRemoval = true, removeAt = "5.8"
     * @see #setAmplifier(Amplifier)
     */
    @Nonnull
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    default IMilSymIconBuilder setEchelon(@Nullable Echelon echelon)
    {
        return setAmplifier(Echelon.toAmplifier(echelon));
    }

    /**
     * @param amplifier Amplifier to apply to the symbol.
     * @return Builder.
     * @since 5.5
     */
    @Nonnull
    IMilSymIconBuilder setAmplifier(@Nullable Amplifier amplifier);

    /**
     * @param presence Presence amplifier to apply to the symbol.
     * @return Builder.
     * @since 5.2
     * @deprecated since = "5.5", forRemoval = true, removeAt = "5.8"
     * @see #setStatus(Status)
     */
    @Nonnull
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    default IMilSymIconBuilder setPresence(@Nullable Presence presence)
    {
        return setStatus(Presence.toStatus(presence));
    }

    /**
     * @param status Status to apply to the symbol.
     * @return Builder.
     * @since 5.5
     */
    @Nonnull
    IMilSymIconBuilder setStatus(@Nullable Status status);

    /**
     * @param shapeType Shape type to apply to the symbol.
     * @return Builder
     */
    @Nonnull
    IMilSymIconBuilder setShapeType(@Nullable ShapeType shapeType);

    /**
     * @param resolution Resolution of the image to generate.
     * @return Builder.
     * @see #DEFAULT_PREVIEW_SIZE
     */
    @Nonnull
    IMilSymIconBuilder setResolution(int resolution);

    /**
     * @param scaling Scaling strategy for cases where the width/height are not exactly the same.
     * @return Builder.
     * @since 5.5
     */
    @Nonnull
    IMilSymIconBuilder setResolutionScaling(@Nonnull MilSymIconScaling scaling);

    /**
     * Single point graphics with modifiers generally put text on the left or right of a rendered symbol.
     * The center point represents the center of the symbol within the image, rather than the center of the image itself.
     * <p/>
     * If we consider a generic hostile ground unit, it appears as a red diamond.
     * The center of the symbol and the center of the image are the same and would be where the {@code x} is.
     * <pre>{@code
     *     / \
     *    / x \
     *    \   /
     * }</pre>
     * If you add a modifier like <i>"staff comments"</i> to the symbol it will put those comments in the top right.
     * <br>
     * The center of <i>the symbol</i> would be where the {@code x} is.
     * <br>
     * The center of <i>the image</i> would be where the {@code o} is.
     * <br>
     * <pre>{@code
     *     / \  Example of a long comment
     *    / x \      o
     *    \   /
     * }</pre>
     * The center point being consumed is {@code x}.
     *
     * @param centerPointConsumer Consumer to take in a 2D point representing the <i>"center"</i> of the icon.
     * @return Builder.
     * @since 5.5.1
     */
    @Nonnull
    IMilSymIconBuilder setCenterPointConsumer(@Nullable Consumer<Point> centerPointConsumer);

    /**
     * Add an attribute <i>(such as a symbol modifier)</i> to be passed down to the native renderer.
     *
     * @param name Attribute name.
     * @param value Attribute value.
     * @return Builder.
     * @since 5.5
     */
    @Nonnull
    IMilSymIconBuilder addAttribute(@Nonnull String name, @Nullable String value);

    /**
     * Add all values from the given {@code attributes} to this builder's attributes.
     *
     * @param attributes Attributes to add.
     * @return Builder.
     * @since 5.5
     */
    @Nonnull
    IMilSymIconBuilder addAttributes(@Nullable AttributeSet attributes);

    /**
     * @return Generated image from the symbol definition and other specified attributes.
     */
    @Nonnull
    BufferedImage render();
}
