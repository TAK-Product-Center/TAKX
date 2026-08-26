package gov.takx.api.globe;

import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 */
@NoPluginImplementations
@SuppressWarnings("UnusedDeclaration")
public interface IStateIcon
{
    /**
     * Set the image to display providing the visual indication of state by using a
     * URL that represents an image resource.
     * <p/>
     * NOTE: If the state icon is displaying text prior to this call, the text
     * will be <b>removed</b> and the image will be displayed <i>in its stead</i>.
     * You can not display both text and an image within a single state icon.
     *
     * @param imagePathURL path to visual state indicator resource
     */
    void setImage(@Nonnull URL imagePathURL);

    /**
     * Set the image to display providing the visual indication of state.
     * <p/>
     * NOTE: If the state icon is displaying text prior to this call, the text
     * will be <b>removed</b> and the image will be displayed <i>in its stead</i>.
     * You can not display both text and an image within a single state icon.
     *
     * @param image visual state indicator resource
     */
    void setImage(BufferedImage image);

    /**
     * @return {@code true} if this state icon is an image (vs text)
     * @since 5.7
     */
    boolean isImage();

    /**
     * Set the state icon to display text.
     * <p/>
     * NOTE: If the state icon is displaying an image prior to this call, the image
     * will be <b>removed</b> and the text will be displayed <i>in its stead</i>.
     * You can not display both text and an image within a single state icon.
     *
     * @param text The text to display within the state icon.
     */
    void setLabel(String text);

    /**
     * Is this particular icon visible (i.e. drawn) on the globe?
     *
     * @return true if the icon will be drawn; false otherwise
     */
    boolean isVisible();

    /**
     * Set the visibility state (i.e. drawn or not) of the icon on the globe.
     *
     * @param visible set true to have the icon drawn;
     *                set false to keep the icon from being drawn
     */
    void setVisible(boolean visible);

    /**
     * Set whether a text state icon will have the text wrapped to stay within the bounds of the outer rectangle
     * of all the state icons.  With no text wrapping, the text will extend as far as needed to render.
     * Text wrapping is on by default.
     *
     * @param wrapText Pass {@code true} to wrap the text, {@code false} otherwise
     */
    void setWrapText(boolean wrapText);

    /**
     * @return Whether text wrapping is enabled.
     */
    boolean isWrapText();

    /**
     * @param font the font to use for text state icons
     * @since 5.7
     */
    void setFont(@Nonnull Font font);

    /**
     * @return the font in use for text state icons
     * @since 5.7
     */
    @Nonnull
    Font getFont();
}
