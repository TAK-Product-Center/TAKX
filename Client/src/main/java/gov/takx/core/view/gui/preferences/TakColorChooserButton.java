package gov.takx.core.view.gui.preferences;

import gov.tak.api.icons.TakColor;
import gov.takx.core.view.gui.pointdropper.ColorSelector;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

/**
 * A button that will show a popup with a specified set of {@link TakColor colors}, allowing the user to select one.
 * Once the user clicks on a color, a handler is invoked with the selected color.  If the user clicks anywhere outside
 * the color selector popup, the popup is dismissed.
 * <p>
 * The button will render a small square icon of the currently selected color.
 *
 * @since 5.4
 */
public class TakColorChooserButton extends JButton
{
    private static final int ICON_SIZE = 16;
    private static final Dimension BUTTON_SIZE = new Dimension(ICON_SIZE * 2, ICON_SIZE * 2);

    private final ColorSelector colorSelector;
    private final JFrame chooserFrame;
    private boolean lostFocusToSelf;

    /**
     * @param palette List of colors to offer
     * @param colorSelectionHandler Handler to invoke when the user selects a color from the popup
     * @since 5.4 (removed initialColor parameter in 5.6)
     */
    public TakColorChooserButton(@Nonnull List<TakColor> palette, @Nonnull Consumer<TakColor> colorSelectionHandler)
    {
        colorSelector = new ColorSelector(0, 5, palette, color -> {
            colorSelectionHandler.accept(color);
            setColor(color);
            toggleChooser();
        });

        chooserFrame = new ColorChooserFrame(colorSelector);

        setFocusable(false);
        setBackground(null);
        setPreferredSize(BUTTON_SIZE);

        addActionListener(e -> open());
    }

    /**
     * Set the icon on this button to be a square of the given color.
     *
     * @param color the color to select
     * @since 5.4 (made public and changed type of color parameter from {@link String} in 5.6)
     */
    public void setColor(TakColor color)
    {
        setToolTipText(color.colorName);
        final var colorSquare = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        final var g = colorSquare.createGraphics();
        g.setPaint(color.getColor());
        g.fillRect(0, 0, colorSquare.getWidth(), colorSquare.getHeight());
        setIcon(new ImageIcon(colorSquare));
        g.dispose();
    }

    /**
     * Open the color chooser.  If the chooser is already visible, or it was previously dismissed by a focus loss,
     * then the operation is skipped.
     */
    public void open()
    {
        if (!lostFocusToSelf && !chooserFrame.isVisible()) toggleChooser();
        lostFocusToSelf = false;
    }

    private void toggleChooser()
    {
        lostFocusToSelf = false;

        if (chooserFrame.isVisible())
        {
            chooserFrame.setVisible(false);
        } else
        {
            final var location = getLocationOnScreen();
            chooserFrame.setLocation(location.x - colorSelector.getWidth() + getWidth(), location.y + getHeight());
            chooserFrame.pack();
            chooserFrame.setVisible(true);
            chooserFrame.requestFocus();
            chooserFrame.addWindowFocusListener(new WindowAdapter()
            {
                @Override
                public void windowLostFocus(WindowEvent e)
                {
                    lostFocusToSelf = clickedOnSelf();
                    chooserFrame.setVisible(false);
                    chooserFrame.removeWindowFocusListener(this);
                }
            });
        }
    }

    /**
     * @return {@code true} if the loss of focus was caused by a mouse event while within the bounds of this editor
     *         or its containing parent (typically a tile in a preferences page).
     */
    private boolean clickedOnSelf()
    {
        final var parent = getParent();
        final var location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, parent);
        return parent.contains(location);
    }

    private static class ColorChooserFrame extends JFrame
    {
        ColorChooserFrame(ColorSelector colorSelector)
        {
            setType(Window.Type.UTILITY);
            setUndecorated(true);
            add(colorSelector);
            pack();
        }
    }
}
