package gov.takx.commons.swing;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory to produce a specialized {@link javax.swing.Popup} that displays a message and a set of action buttons.
 *
 * @since 5.3
 * @see #yesNo(String, Component, Runnable)
 * @see #yesNo(String, Component, Runnable, Runnable)
 * @see #builder(String, Component)
 * @see Builder
 */
@IncubatingApi(since = "5.3")
public class ActionPopupFactory
{
    private static final PopupFactory popupFactory = PopupFactory.getSharedInstance();

    /**
     * Empty runnable for actions that need to do nothing.
     */
    public static final Runnable DO_NOTHING = () -> {
    };

    /**
     * Create a popup presenting the given {@code message} and a pair of Yes/No buttons. The "No" button will
     * do nothing other than hide the popup.
     *
     * @param message The message for the popup
     * @param relativeTo The component relative to which the popup should be displayed. It will be displayed below
     *                   the given component, with right edges aligned.
     * @param yesHandler Handler to invoke if the user clicks the "Yes" button
     * @return The {@link Popup}.  Call {@link Popup#show()} to display it.
     */
    public static Popup yesNo(@Nonnull String message, @Nonnull Component relativeTo, @Nonnull Runnable yesHandler)
    {
        return yesNo(message, relativeTo, yesHandler, DO_NOTHING);
    }

    /**
     * Create a popup presenting the given {@code message} and a pair of Yes/No buttons.
     *
     * @param message The message for the popup
     * @param relativeTo The component relative to which the popup should be displayed. It will be displayed below
     *                   the given component, with right edges aligned.
     * @param yesHandler Handler to invoke if the user clicks the "Yes" button
     * @param noHandler Handler to invoke if the user clicks the "No" button
     * @return The {@link Popup}.  Call {@link Popup#show()} to display it.
     */
    public static Popup yesNo(@Nonnull String message, @Nonnull Component relativeTo, @Nonnull Runnable yesHandler, @Nonnull Runnable noHandler)
    {
        return builder(message, relativeTo)
                .action("Yes", yesHandler)
                .action("No", noHandler)
                .build();
    }

    /**
     * Create a popup presenting the given {@code message}.  Actions must be added using {@link Builder#action(String, Runnable)}.
     *
     * @param message The message for the popup
     * @param relativeTo The component relative to which the popup should be displayed.  It will be displayed below
     *                   the given component, with right edges aligned.
     * @return new builder
     * @see Builder#action(String, Runnable)
     */
    public static Builder builder(@Nonnull String message, @Nonnull Component relativeTo)
    {
        return new Builder().message(message).relativeTo(relativeTo);
    }

    /**
     * Builder for configuring and creating a {@link Popup} with a message and a set of action buttons.
     */
    @IncubatingApi(since = "5.3")
    public static class Builder
    {
        private final List<ActionButton> actionButtons = new ArrayList<>();
        private String message;
        private Component relativeTo;

        /**
         * @param message The message to display
         */
        @Nonnull
        public Builder message(@Nonnull String message)
        {
            this.message = message;
            return this;
        }

        /**
         * @param relativeTo The component relative to which the popup should be displayed.  It will be displayed below
         *                   the given component, with right edges aligned.
         */
        @Nonnull
        public Builder relativeTo(@Nonnull Component relativeTo)
        {
            this.relativeTo = relativeTo;
            return this;
        }

        /**
         * Adds an action button to the popup.  Buttons will be laid out horizontally in the order in which they are added.
         * 
         * @param text The button text
         * @param handler The handler to invoke if the user clicks the button created for this action
         * @see #DO_NOTHING
         */
        @Nonnull
        public Builder action(@Nonnull String text, @Nonnull Runnable handler)
        {
            return action(text, "", handler);
        }

        /**
         * Adds an action button to the popup.  Buttons will be laid out horizontally in the order in which they are added.
         * 
         * @param text The button text
         * @param tooltip Tooltip text for button
         * @param handler The handler to invoke if the user clicks the button created for this action
         * @see #DO_NOTHING
         */
        @Nonnull
        public Builder action(@Nonnull String text, @Nonnull String tooltip, @Nonnull Runnable handler)
        {
            actionButtons.add(new ActionButton(text, tooltip, handler));
            return this;
        }

        /**
         * @return The {@link Popup}.  Call {@link Popup#show()} to display it.
         */
        @Nonnull
        public Popup build()
        {
            final var popupPanel = new PopupPanel(message, actionButtons);

            // Position the popup with its top just below, and its right edge aligned with, 'relativeTo' 
            final var point = relativeTo.getLocationOnScreen();
            final var panelSize = popupPanel.getPreferredSize();
            final int x = point.x - panelSize.width + relativeTo.getWidth();
            final int y = point.y + relativeTo.getHeight();

            final var popup = popupFactory.getPopup(relativeTo, popupPanel, x, y);
            popupPanel.configureAutoHide(popup);
            return popup;
        }
    }

    /**
     * Defines an action button to place in the popup.
     *
     * @param text Button text
     * @param tooltip Tooltip text for button
     * @param handler Action to invoke when the button is clicked
     */
    private record ActionButton(@Nonnull String text, @Nonnull String tooltip, @Nonnull Runnable handler)
    {
    }

    /**
     * The panel that will be shown in the popup.  It has the message on the top, and buttons on the bottom,
     * right aligned.
     */
    private static class PopupPanel extends JPanel
    {
        private static final CellConstraints CC = new CellConstraints();
        private static final Insets MARGIN = new Insets(2, 5, 2, 5);
        private static final CompoundBorder BORDER = new CompoundBorder(
                new LineBorder(Color.white, 2, true),
                Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));

        private final WrappingLabel messageLabel = new WrappingLabel();
        private final JPanel buttonPanel = new JPanel();
        private final List<JButton> buttons = new ArrayList<>();

        private PopupPanel(String message, List<ActionButton> actionButtons)
        {
            initComponents(message, actionButtons.size());
            createButtons(actionButtons);
            revalidate();
        }

        /**
         * Create and buttons from the given {@link ActionButton} elements.  The button text is made slightly larger
         * than the default font.
         */
        private void createButtons(List<ActionButton> actionButtons)
        {
            int col = 2;

            for (var actionButton : actionButtons)
            {
                final var button = new JButton(actionButton.text);
                button.setMargin(MARGIN);
                button.setFont(button.getFont().deriveFont(button.getFont().getSize() + 1f));
                button.setToolTipText(actionButton.tooltip);
                button.addActionListener(e -> actionButton.handler.run());

                buttons.add(button);
                buttonPanel.add(button, CC.xy(col, 1));
                col += 2;
            }
        }

        /**
         * Configure things so that the popup will be automatically hidden if the user clicks any of the action buttons
         * or outside the bounds of this panel.
         *
         * @param popup The popup to hide
         */
        private void configureAutoHide(Popup popup)
        {
            // Hide if any button is clicked
            buttons.forEach(b -> b.addActionListener(e -> popup.hide()));

            // Add/remove mouse event listener when this panel is shown/hidden
            addAncestorListener(new MouseListenerInstaller(this, popup));
        }

        private void initComponents(String message, int numButtons)
        {
            setName("popupPanel");
            setBorder(BORDER);
            setLayout(new FormLayout("default", "default, $ugap, default"));

            messageLabel.setName("messageLabel");
            messageLabel.setText(message);
            add(messageLabel, CC.xy(1, 1));

            buttonPanel.setName("buttonPanel");
            final String buttonPanelColumnSpec = String.format("$glue, %d*(default, $lcgap)", numButtons);
            buttonPanel.setLayout(new FormLayout(buttonPanelColumnSpec, "default"));
            add(buttonPanel, CC.xy(1, 3));
        }

        /**
         * Listens for ancestor events so we can know when the popup is shown by the caller.
         * When shown, add an AWT event listener to hide the popup if the user clicks outside a given panel.
         */
        private static class MouseListenerInstaller implements AncestorListener
        {
            private final AutoHideMouseEventListener mouseEventListener;

            private MouseListenerInstaller(JPanel panel, Popup popup)
            {
                mouseEventListener = new AutoHideMouseEventListener(panel, popup);
            }

            @Override
            public void ancestorAdded(AncestorEvent event)
            {
                Toolkit.getDefaultToolkit().addAWTEventListener(mouseEventListener, AWTEvent.MOUSE_EVENT_MASK);
            }

            @Override
            public void ancestorRemoved(AncestorEvent event)
            {
                Toolkit.getDefaultToolkit().removeAWTEventListener(mouseEventListener);
            }

            @Override
            public void ancestorMoved(AncestorEvent event)
            {
                // ignore
            }
        }

        /**
         * Event listener to hide a popup when the mouse is clicked anywhere outside the bounds of a given panel.
         */
        private record AutoHideMouseEventListener(JPanel panel, Popup popup) implements AWTEventListener
        {
            @Override
            public void eventDispatched(AWTEvent event)
            {
                final int id = event.getID();
                if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_CLICKED || id == MouseEvent.MOUSE_DRAGGED)
                {
                    final Point point = ((MouseEvent) event).getLocationOnScreen();
                    SwingUtilities.convertPointFromScreen(point, panel);
                    if (!panel.contains(point)) popup.hide();
                }
            }
        }
    }

    // No instances
    private ActionPopupFactory()
    {
    }
}
