package gov.takx.core.view.gui.themes;

import javax.swing.UIManager;

import java.awt.Color;

/**
 * Identifies the custom colors used in the TAK themes.
 *
 * @since 3.4
 * @see <a href="https://www.figma.com/proto/HUi456qQX23W7wOGooH104/TAK-Designs---External-(10%2F4%2F2020)?node-id=53%3A73&scaling=min-zoom&page-id=46%3A0&starting-point-node-id=55%3A6312">
 *     Draft TAK Design Guide</a>
 */
public enum TakThemeColor
{
    RED("Tak.red", Color.RED),
    ORANGE("Tak.orange", Color.ORANGE),
    YELLOW("Tak.yellow", Color.YELLOW),
    GREEN("Tak.green", Color.GREEN),
    BLUE("Tak.blue", Color.BLUE),
    PURPLE("Tak.purple", new Color(0xA250E2));

    private final String propertyKey;
    private final Color fallbackColor;

    /**
     * @param propertyKey The property key associated with the color in {@link javax.swing.UIManager}
     * @param fallbackColor The fallback color if no color is defined for the property key
     */
    TakThemeColor(String propertyKey, Color fallbackColor)
    {
        this.propertyKey = propertyKey;
        this.fallbackColor = fallbackColor;
    }

    /**
     * @return The color stored in {@link UIManager} for the color property or the {@code fallbackColor} if the property
     * is not found.
     */
    public Color getColor()
    {
        final Color color = UIManager.getColor(propertyKey);
        return color == null ? fallbackColor : color;
    }
}
