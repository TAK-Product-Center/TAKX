package gov.takx.api.media.stream;

import jakarta.annotation.Nonnull;

import java.awt.Dimension;

/**
 * Parameters controlling video frame decoding.
 *
 * @param imageOutputSize The image size to produce from decoded video frames
 * @since 5.8
 */
public record VideoStreamConfiguration(@Nonnull Dimension imageOutputSize) implements IStreamConfiguration
{
}
