package gov.takx.platform.ui;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.awt.Font;
import java.lang.invoke.MethodHandles;

/**
 * Utility methods and constants related to fonts.
 *
 * @since 3.3
 */
public class FontUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final Font DEFAULT_FONT;
    public static final int DEFAULT_FONT_SIZE;

    static
    {
        Font defaultFont = UIManager.getFont("defaultFont");

        // When running headless, this value will never be set (and it doesn't really matter since we are headless), so
        // take advantage of the fact that providing a null arg to Font.decode() returns the default dialog font.
        if (defaultFont == null)
        {
            defaultFont = Font.decode(null);
        }

        DEFAULT_FONT = defaultFont;
        DEFAULT_FONT_SIZE = DEFAULT_FONT.getSize();
    }

    /**
     * Updates the font for the titled border on the provided component with the relative size change.
     *
     * @param component The component for which to update the titled border font
     * @param relativeSize The relative size change for the font (e.g. +2)
     */
    public static void updateTitledBorderFont(@Nonnull JComponent component, float relativeSize)
    {
        final TitledBorder titledBorder = getTitledBorder(component);
        if (titledBorder == null) return;

        final Font titleFont = titledBorder.getTitleFont();
        final Font derivedFont = titleFont.deriveFont(titleFont.getSize() + relativeSize);
        titledBorder.setTitleFont(derivedFont);
    }

    /**
     * Updates the font for the titled border on the provided component with the style change.
     *
     * @param component The component for which to update the titled border font
     * @param relativeStyle The relative change in style (e.g. {@link Font#BOLD})
     */
    public static void updateTitledBorderFont(@Nonnull JComponent component, int relativeStyle)
    {
        final TitledBorder titledBorder = getTitledBorder(component);
        if (titledBorder == null) return;

        final Font titleFont = titledBorder.getTitleFont();
        final Font derivedFont = titleFont.deriveFont(titleFont.getStyle() | relativeStyle);
        titledBorder.setTitleFont(derivedFont);
    }

    /**
     * Updates the font for the titled border on the provided component with the relative size and style change.
     *
     * @param component The component for which to update the titled border font
     * @param relativeSize The relative size change for the font (e.g. +2)
     * @param relativeStyle The relative change in style (e.g. {@link Font#BOLD})
     */
    @SuppressWarnings("unused") // Included for completeness to align with Font.deriveFont() overloads
    public static void updateTitledBorderFont(@Nonnull JComponent component, float relativeSize, int relativeStyle)
    {
        final TitledBorder titledBorder = getTitledBorder(component);
        if (titledBorder == null) return;

        final Font titleFont = titledBorder.getTitleFont();
        final Font derivedFont = titleFont.deriveFont(titleFont.getStyle() | relativeStyle, titleFont.getSize() + relativeSize);
        titledBorder.setTitleFont(derivedFont);
    }

    /**
     * Gets the titled border for the provided component.
     *
     * @param component The component for which to get the border
     * @return The TitledBorder or {@code null} if the component doesn't have one.
     */
    private static TitledBorder getTitledBorder(@Nonnull JComponent component)
    {
        final Border border = component.getBorder();
        if (!(border instanceof TitledBorder titledBorder))
        {
            logger.warn("Component {} does not have a titled border", component);
            return null;
        }

        return titledBorder;
    }

    /**
     * Style the font of the given component, optionally bold and with the specified font size adjustment.
     * 
     * @param component Component to adjust
     * @param bold Pass {@code true} to make the font bold
     * @param fontAdjustment The font size adjustment
     * @return The given {@code component}
     * @since 5.4
     */
    public static <T extends JComponent> T styleFont(@Nonnull T component, boolean bold, float fontAdjustment)
    {
        final var font = component.getFont();
        component.setFont(font.deriveFont(bold ? font.getStyle() | Font.BOLD : font.getStyle(), font.getSize() + fontAdjustment));
        return component;
    }

    private FontUtils()
    {
    }
}
