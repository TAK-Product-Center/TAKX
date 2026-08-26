package gov.takx.api.milsym;

import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;

/**
 * Outlines a cache for MIL-STD-2525 graphics built by a {@link IMilStd2525Service} instance.
 *
 * @since 5.1
 */
@NoPluginImplementations
public interface IMilSymIconCache
{
    /**
     * @param code Symbol code to look up a cached image of.
     * @param resolution Desired image resolution.
     * @param scaling Desired image scaling.
     *
     * @return The cached image instance at the requested resolution.
     * {@code null} if the cache does not have the image.
     * @since 5.5.1
     */
    @Nullable
    BufferedImage lookup(@Nonnull String code, int resolution, @Nonnull MilSymIconScaling scaling);

    /**
     * @param code Symbol code to register with.
     * @param resolution Resolution of image.
     * @param scaling Scaling scheme of image.
     * @param image Rendered symbol.
     * @since 5.5.1
     */
    void put(@Nonnull String code, int resolution, @Nonnull MilSymIconScaling scaling, @Nonnull BufferedImage image);

    /**
     * @param code Symbol code to register as a failure,
     */
    void setFailure(@Nonnull String code);

    /**
     * @param code Code to look up.
     *
     * @return {@code true} if the code has been rendered <i>(successfully or unsuccessfully)</i> previously.
     * {@code false} indicates the code has never been attempted to be rendered.
     */
    boolean isCached(@Nullable String code);

    /**
     * @param code Code to look up.
     *
     * @return {@code true} if the code has failed to render previously.
     */
    boolean isFailure(@Nullable String code);

    /**
     * @param code Code to look up.
     *
     * @return {@code true} if the code has succeeded to render previously.
     */
    boolean isSuccess(@Nullable String code);

    /**
     * @return Instance of the unknown image to be used as a fallback for failures.
     */
    @Nonnull
    BufferedImage getUnknownImage();
}
