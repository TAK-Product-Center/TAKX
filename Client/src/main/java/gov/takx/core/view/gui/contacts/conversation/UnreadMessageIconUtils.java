package gov.takx.core.view.gui.contacts.conversation;

import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageLabeler;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static gov.takx.platform.ui.FontUtils.DEFAULT_FONT;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Updates {@link JLabel labels} to display (or hide) an unread message count icon. The font, icon size and icon text
 * are all managed by this class. {@link JLabel Labels} must have a preferred size set for calls to
 * {@link #updateLabel(JLabel, int)} work properly.
 *
 * @since 5.2
 */
public class UnreadMessageIconUtils
{
    /**
     * The max number of messages to show on an "unread messages" label being displayed to the user.
     */
    public static final int MESSAGE_COUNT_LABEL_THRESHOLD = 99;
    private static final Font SINGLE_DIGIT_FONT = DEFAULT_FONT.deriveFont(DEFAULT_FONT.getSize() + 1f);
    private static final Font DOUBLE_DIGIT_FONT = DEFAULT_FONT.deriveFont(DEFAULT_FONT.getSize() - 1f);
    private static final Font TRIPLE_DIGIT_FONT = DEFAULT_FONT.deriveFont(DEFAULT_FONT.getSize() - 3f);
    private static final Dimension BACKGROUND_IMAGE_DIMENSION = new Dimension(19, 19);
    public static final BufferedImage NOTIFICATION_DOT =
            UIResources.getSizedImage(UIResources.getUrl(PLATFORM_ICONS, "notification_dot.png"), BACKGROUND_IMAGE_DIMENSION);
    private static final String ICON_FILE = "notification_dot.png";
    private static final URL ICON_URL = UIResources.getUrl(PLATFORM_ICONS, ICON_FILE);

    private static final Font defaultNumMessagesLabelFont = new JLabel().getFont();
    private static final Font smallerNumMessagesLabelFont =
            defaultNumMessagesLabelFont.deriveFont(defaultNumMessagesLabelFont.getSize() - 2f);
    // Map from an icon size to its corresponding icon
    private static final Map<Integer, ImageIcon> sizeToIconMap = new HashMap<>();

    private UnreadMessageIconUtils()
    {
    }

    /**
     * Sets (or clears) the unread message count displayed by the provided label.
     *
     * @param iconLabel The label to update; the preferred size <em>must</em> be set
     * @param unreadMessageCount The unread message count
     */
    public static void updateLabel(@Nonnull JLabel iconLabel, int unreadMessageCount)
    {
        if (unreadMessageCount <= 0)
        {
            iconLabel.setVisible(false);
            return;
        }

        final boolean overThreshold = unreadMessageCount > MESSAGE_COUNT_LABEL_THRESHOLD;

        iconLabel.setText(overThreshold ? MESSAGE_COUNT_LABEL_THRESHOLD + "+" : String.valueOf(unreadMessageCount));
        iconLabel.setIcon(sizeToIconMap.computeIfAbsent(getIconSize(iconLabel), UnreadMessageIconUtils::createIcon));
        iconLabel.setFont(overThreshold ? smallerNumMessagesLabelFont : defaultNumMessagesLabelFont);
        iconLabel.setVisible(true);
    }

    /**
     * @param buttonImage The button image
     * @param countImage The unread message count image
     * @return the button image overlaid with the unread message count image
     */
    public static BufferedImage createOverlaidImage(@Nonnull BufferedImage buttonImage, @Nonnull BufferedImage countImage)
    {
        return GraphicsUtils.overlayImage(buttonImage, countImage, buttonImage.getWidth() - countImage.getWidth(), 0);
    }

    /**
     * @param count The unread message count
     * @return the {@link #NOTIFICATION_DOT} with the specified count
     */
    public static BufferedImage createCountImage(int count)
    {
        final boolean lessThanTen = count < 10;
        final boolean lessThanOneHundred = count < 100;
        final int verticalOffset = lessThanOneHundred ? 1 : 2;
        final int horizontalOffset = count != 1 && lessThanTen || !lessThanOneHundred ? 0 : -1;
        final Font font = lessThanTen ? SINGLE_DIGIT_FONT : lessThanOneHundred ? DOUBLE_DIGIT_FONT : TRIPLE_DIGIT_FONT;
        final String countText = count > UnreadMessageIconUtils.MESSAGE_COUNT_LABEL_THRESHOLD ? UnreadMessageIconUtils.MESSAGE_COUNT_LABEL_THRESHOLD + "+" : String.valueOf(count);

        return ImageLabeler.getLabeledImage(NOTIFICATION_DOT, countText, ImageLabeler.CENTER,
                ImageLabeler.JUSTIFY_CENTER, verticalOffset, horizontalOffset, font);
    }

    /**
     * Calculates the icon size based on a label's dimensions.
     *
     * @param label The label that will display the icon
     * @return The icon size
     */
    private static int getIconSize(@Nonnull JLabel label)
    {
        return label.getPreferredSize().height - 1;
    }

    /**
     * @param size The icon size
     * @return The unread message icon scaled to the specified size
     */
    private static ImageIcon createIcon(int size)
    {
        return UIResources.getSizedIcon(ICON_URL, new Dimension(size, size));
    }
}
