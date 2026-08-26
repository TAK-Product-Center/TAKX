package gov.takx.api.preferences.tool;

import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.awt.Image;

/**
 * Defines a boolean {@link gov.takx.api.preferences.IPreferenceEntry} that will be controlled by a JToggleButton. The
 * preference entry's boolean value will correspond to the button's selected state.
 *
 * @since 5.0
 */
public class ToggleButtonPreference extends AToolPreference
{
    private static final int ICON_SIZE = ImageUtils.MEDIUM_ICON_HEIGHT;

    private final String text;
    private final Icon icon;

    /**
     * @param id The preference id. This id will be combined with the tool uid to form a unique preference entry id.
     *           See {@link IToolPreference#getId()}.
     * @param title The title that will be displayed in the tool preference list
     * @param description The description that will be displayed in the tool preference list
     * @param defaultValue The initial selection value to use if no value has been stored
     * @param text The text to display on the toggle button
     * @param icon The icon to display on the button
     */
    public ToggleButtonPreference(@Nonnull String id, @Nonnull String title, @Nonnull String description,
                                  boolean defaultValue, @Nonnull String text, @Nullable ImageIcon icon)
    {
        super(id, title, description, defaultValue);
        this.text = text;

        this.icon = icon != null
                ? new ImageIcon(icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH))
                : null;
    }

    /**
     * @return The button text
     */
    public String getText()
    {
        return text;
    }

    /**
     * @return The button icon; may be {@code null}
     */
    @Nullable
    public Icon getIcon()
    {
        return icon;
    }
}