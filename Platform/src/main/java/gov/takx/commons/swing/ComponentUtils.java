package gov.takx.commons.swing;

import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSpinner;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;

/**
 * Miscellaneous static Component utilities.
 *
 */
public final class ComponentUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private ComponentUtils()
    {
    }

    /**
     * Center the component on the screen that TAKX is being displayed in. TAKX window could be on an extended
     * desktop on a second screen.
     *
     * @param component component to center.
     */
    public static void centerComponentOnScreen(Component component)
    {
        // Get the component size
        Dimension prefSize = component.getPreferredSize();

        // From the frame and then the bounds of the device the frame is on
        Frame frame = JFrame.getFrames()[0];
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();

        // Parent size is going to be the size of the screen that the TAKX window is in
        Dimension parentSize = new Dimension(bounds.width, bounds.height);

        // Initial location will basically be the upper left coordinate of the screen
        Point parentLocation = new Point((int) bounds.getX(), (int) bounds.getY());

        // Calculate x and y based on the upper left coordinates of the screen, screen size and size of the component
        int x = parentLocation.x + (parentSize.width - prefSize.width) / 2;
        int y = parentLocation.y + (parentSize.height - prefSize.height) / 2;
        component.setLocation(x, y);

        if (logger.isDebugEnabled())
        {
            logger.debug("Parent location = {}, {}", parentLocation.x, parentLocation.y);
            logger.debug("Parent size = {}, {}", parentSize.width, parentSize.height);
            logger.debug("prefSize = {}, {}", prefSize.width, prefSize.height);
            logger.debug("Set Location {}, {}", x, y);
        }
    }

    public static Dimension getActiveMonitorScreenSize()
    {
        Frame frame = JFrame.getFrames()[0];
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();
        return new Dimension(bounds.width, bounds.height);
    }

    public static Dimension getComponentPackedSize(Component component)
    {
        JFrame myFrame = new JFrame();
        // don't use the default BorderLayout or you won't get the natural size
        myFrame.getContentPane().setLayout(new FlowLayout());
        myFrame.getContentPane().add(component);
        myFrame.pack();

        Rectangle r = component.getBounds();
        myFrame.dispose();

        return new Dimension((int) r.getWidth(), (int) r.getHeight());
    }

    /**
     * Commits the spinner values, or makes sure that the spinner values are up to date with text a user might have
     * entered in without clicking on the up/down spinners. There are times when the model differs from what is in the
     * text box.
     *
     * @param spinner spinner control for which to commit the value
     */
    public static void commitSpinner(JSpinner spinner)
    {
        try
        {
            spinner.commitEdit();
        } catch (ParseException pe)
        {
            // Edited value is invalid, spinner.getValue() will return
            // the last valid value, you could revert the spinner to show that:
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor)
            {
                ((JSpinner.DefaultEditor) editor).getTextField().setValue(spinner.getValue());
            }
        }
    }

    /**
     * Scan the component hierarchy and remove the buttons we find. The only button that returns {@code null} for the
     * action command and the text will be the "X" button, which will be removed.
     *
     * @param comp Component to process descendents of
     */
    public static void removeMinMaxClose(Component comp)
    {
        if (comp instanceof AbstractButton button)
        {
            // Hack to remove the "X" button. The only button in the dialog where both the action and text are empty.
            if (StringUtils.isEmpty(button.getActionCommand()) && StringUtils.isEmpty(button.getText()))
            {
                comp.setVisible(false);
            }
        }

        if (comp instanceof Container)
        {
            for (Component component : ((Container) comp).getComponents())
            {
                removeMinMaxClose(component);
            }
        }
    }

    /**
     * @param component Component to find center point of.
     * @param screenSpace {@code true} to yield the screen-space center point.
     *                    {@code false} to yield the relative component center point in its parent.
     * @return The center point of the given component.
     * @since 5.6
     */
    @Nonnull
    public static Point getCenterPoint(@Nonnull Component component, boolean screenSpace)
    {
        var location = screenSpace ? component.getLocationOnScreen() : component.getLocation();
        location = new Point(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
        return location;
    }
}
