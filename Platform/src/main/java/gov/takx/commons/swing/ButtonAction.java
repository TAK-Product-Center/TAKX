package gov.takx.commons.swing;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 * Action for representing a button.
 *
 * @since 3.3
 */
public class ButtonAction extends AbstractAction
{
    /**
     * Action for representing a separator in a {@link SplitButton}.
     */
    public static final ButtonAction SEPARATOR = new ButtonAction("separator", null);

    private final String name;
    private final Icon icon;
    private final String tooltip;
    private final Consumer<ActionEvent> consumer;

    /**
     * Constructor for a button with a name (but no icon or tooltip).
     *
     * @param name The button name
     * @param consumer The consumer to be called when the button is clicked
     */
    public ButtonAction(String name, Consumer<ActionEvent> consumer)
    {
        this(name, null, consumer);
    }

    /**
     * Constructor for a button with a name and tooltip (but no icon).
     *
     * @param name The button name
     * @param tooltip The tooltip to display on hover
     * @param consumer The consumer to be called when the button is clicked
     */
    public ButtonAction(String name, String tooltip, Consumer<ActionEvent> consumer)
    {
        this(name, null, tooltip, consumer);
    }

    /**
     * Constructor for a button with a name, icon, and tooltip.
     *
     * @param name The button name
     * @param icon The button icon
     * @param tooltip The tooltip to display on hover
     * @param consumer The consumer to be called when the button is clicked
     */
    public ButtonAction(String name, Icon icon, String tooltip, Consumer<ActionEvent> consumer)
    {
        super(name, icon);
        this.name = name;
        this.icon = icon;
        this.tooltip = tooltip;
        this.consumer = consumer;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        consumer.accept(e);
    }

    @Override
    public Object getValue(String key)
    {
        return switch (key)
        {
            case Action.NAME -> name;
            case Action.SMALL_ICON, Action.LARGE_ICON_KEY -> icon;
            case Action.SHORT_DESCRIPTION -> tooltip;
            default -> super.getValue(key);
        };
    }
}
