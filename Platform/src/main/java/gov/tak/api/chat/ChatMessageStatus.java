package gov.tak.api.chat;

import gov.tak.api.cot.CotTypes;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ImageIcon;

/**
 * {@link IChatMessage} statuses, ordered by the stages of message transmission. The status of the most recently sent
 * {@link IChatMessage} will be used to determine the state of the last block of sent messages.
 *
 * @since 3.4
 */
public enum ChatMessageStatus
{
    none(null),
    sent(getIcon("dots_circle.png")),
    delivered(getIcon("check_circle_outline.png")),
    read(getIcon("check_circle.png"));

    @Nullable
    private final ImageIcon icon;

    ChatMessageStatus(@Nullable ImageIcon icon)
    {
        this.icon = icon;
    }

    /**
     * @param cotType The CoT message type String
     * @return The ConversationStatus that matches the type, or {@link ChatMessageStatus#none} if the type is
     * unrecognized.
     */
    @Nonnull
    public static ChatMessageStatus parseCotType(@Nonnull String cotType)
    {
        return switch (cotType)
        {
            case CotTypes.FREE_TEXT_DELIVERED -> delivered;
            case CotTypes.FREE_TEXT_READ -> read;
            default -> none;
        };
    }

    /**
     * @return The icon used to represent the status
     */
    @Nullable
    public ImageIcon getIcon()
    {
        return icon;
    }

    /**
     * Compare this {@link ChatMessageStatus} with another and return {@code true} if this one is greater, i.e. it
     * represents a later transmission stage.
     *
     * @param other The other status.
     * @return {@code true} if this status is greater, {@code false} otherwise.
     */
    public boolean isGreater(@Nonnull ChatMessageStatus other)
    {
        return ordinal() > other.ordinal();
    }

    /**
     * @param fileName The resource file name.
     * @return The sized icon.
     */
    @Nonnull
    private static ImageIcon getIcon(@Nonnull String fileName)
    {
        //noinspection ConstantConditions - won't be null since the default will be returned
        return UIResources.getSizedIcon(ResourceDirectory.PLATFORM_ICONS, fileName, UIResources.STANDARD_IMAGE_SIZE, false);
    }
}
