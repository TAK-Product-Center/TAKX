package gov.takx.core.media;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Represents a command to be processed by a media player
 *
 * @since 2.8
 */
public class MediaCommand
{
    public enum Type
    {
        PLAY,
        PAUSE,
        SEEK,
        START_CAPTURE,
        STOP_CAPTURE
    }

    private final Type type;
    private final Object payload;

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
     * Construct a command of this {@code Type} with an appropriate payload.
     *
     * @param type    The command being issued.
     * @param payload A payload appropriate to the type of command.
     * @see Type
     * @since 5.1.0
     */
    public MediaCommand(@Nonnull Type type, @Nullable Object payload)
    {
        this.type = type;
        this.payload = payload;
    }

    public Type getType()
    {
        return type;
    }

    /**
     * Meaning depends on {@link Type}:
     * <ul>
     *     <li>{@link Type#SEEK}, timestamp to seek to</li>
     *     <li>{@link Type#START_CAPTURE}, the path to save the video</li>
     *     <li>{@link Type#STOP_CAPTURE}, key of registered packet listener (the capture path), see {@link MediaProducer#addPacketListener(String, PacketListener)}</li>
     * </ul>
     *
     * @return The command payload
     */
    @Nullable
    public Object getPayload()
    {
        return payload;
    }

    /**
     * @return configured payload
     * @see #getPayload()
     * @since 5.3
     */
    @Nonnull
    public Object requirePayload()
    {
        return Objects.requireNonNull(payload, "'payload' is null");
    }

    @Override
    public String toString()
    {
        return "MediaCommand{" +
                "type=" + type +
                ", payload=" + payload +
                '}';
    }
}
