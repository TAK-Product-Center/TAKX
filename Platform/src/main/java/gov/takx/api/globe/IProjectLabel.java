package gov.takx.api.globe;

import java.awt.Color;

/**
 * Project label object.
 *
 * @since 1.4
 */
public interface IProjectLabel
{
    /**
     * Default label color.
     */
    Color DEFAULT_COLOR = Color.YELLOW;

    /**
     * Default label size.
     */
    int DEFAULT_SIZE = 24;

    /**
     * Get the label color.
     *
     * @return Label Color
     */
    Color getColor();

    /**
     * Set the label color.
     *
     * @param color Color
     */
    void setColor(Color color);

    /**
     * Get the label.
     *
     * @return Label
     */
    String getLabel();

    /**
     * Set the label.
     *
     * @param label Label
     */
    void setLabel(String label);

    /**
     * Get the font size.
     *
     * @return Font size
     */
    int getSize();

    /**
     * Set the font size.
     *
     * @param size Font size
     */
    void setSize(int size);

    /**
     * Is the label visible.  NOTE:  Label may not show up on the globe if the user has hidden the entire project
     * label layer.
     *
     * @return true, if visible
     */
    boolean isVisible();

    /**
     * Set label visibility.  NOTE:  Label may not show up on the globe if the user has hidden the entire project
     * label layer.
     *
     * @param visible true, to display the label
     */
    void setVisible(boolean visible);
}
