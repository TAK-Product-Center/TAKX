package gov.takx.core.media;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Represents a command to be processed by a media player.
 *
 * @param type The command being issued
 * @param payload A payload appropriate to the type of command.
 * Meaning depends on {@link Type}:
 * <ul>
 *     <li>{@link Type#PAUSE} and {@link Type#PLAY}, no payload, value is {@code null}</li>
 *     <li>{@link Type#SEEK}, type {@link Double}, presentation timestamp to seek to</li>
 * </ul>
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public record MediaCommand(@Nonnull Type type, @Nullable Object payload)
{
    public enum Type
    {
        PLAY,
        PAUSE,
        SEEK,
    }

    /**
     * Construct a command of this {@code Type}, with no additional data.
     *
     * @param type The command being issued.
     * @see Type
     */
    public MediaCommand(@Nonnull Type type)
    {
        this(type, null);
    }

    /**
     * @return configured payload
     */
    @Nonnull
    public Object requirePayload()
    {
        return Objects.requireNonNull(payload, "'payload' is null");
    }
}
