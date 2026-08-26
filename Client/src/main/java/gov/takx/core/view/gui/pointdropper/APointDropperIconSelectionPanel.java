package gov.takx.core.view.gui.pointdropper;

import gov.takx.commons.swing.DeselectableButtonGroup;
import gov.takx.commons.swing.WrapLayout;
import gov.takx.core.view.gui.pointdropper.PointDropperPanel.PointDropperSelection;
import jakarta.annotation.Nonnull;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

/**
 * Abstract base class for an icon selection panel.
 *
 * @since 3.0
 */
public abstract class APointDropperIconSelectionPanel extends JPanel
{
    private static final Border UNSELECTED_BORDER = new JToggleButton().getBorder();

    // A border with the same dimensions as the default so the icons don't resize when we apply it
    private static final Border SELECTED_BORDER = new CompoundBorder(
            new LineBorder(Color.white, 1),
            new EmptyBorder(2, 2, 2, 2));

    protected static final int DEFAULT_WIDTH = 400;

    private final Consumer<PointDropperSelection> selectionConsumer;

    protected final IconButtonGroup iconButtonGroup = new IconButtonGroup();

    /**
     * @param selectionConsumer Consumer to handle the user making an icon selection
     * @since 5.2
     */
    APointDropperIconSelectionPanel(@Nonnull Consumer<PointDropperSelection> selectionConsumer)
    {
        final WrapLayout layout = new WrapLayout(DEFAULT_WIDTH);
        layout.setHgap(10); // Give a little more horizontal spacing

        setLayout(layout);

        this.selectionConsumer = selectionConsumer;
    }

    /**
     * Add the given button to the button group
     * @since 5.2
     */
    protected void addButtonToGroup(JToggleButton button)
    {
        // Set the border of the button according to its selected state to give the user an indication of which icon is active.
        button.addItemListener(e -> button.setBorder(e.getStateChange() == ItemEvent.SELECTED ? SELECTED_BORDER : UNSELECTED_BORDER));

        iconButtonGroup.add(button);
    }

    /**
     * Clears the toggle state of all icon buttons.
     */
    public void clearSelection()
    {
        iconButtonGroup.clearSelection();
    }

    /**
     * Pass an "empty" selection to our consumer.
     * @since 5.3
     */
    protected void applyEmptySelection()
    {
        applySelection(new PointDropperSelection(null, null, null));
    }

    /**
     * Pass the selection info to the selection consumer.
     * @since 5.3
     */
    void applySelection(@Nonnull PointDropperSelection selection)
    {
        selectionConsumer.accept(selection);
    }

    protected static class IconButtonGroup extends DeselectableButtonGroup
    {
        /**
         * Removes all buttons from the group.
         */
        public void removeAll()
        {
            clearSelection();
            buttons.clear();
        }
    }
}
