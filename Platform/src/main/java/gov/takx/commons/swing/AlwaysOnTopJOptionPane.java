package gov.takx.commons.swing;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import java.awt.Component;
import java.lang.invoke.MethodHandles;

/**
 * This class offers a number of utility methods for constructing or showing JOptionPanes and ensuring that
 * they are "always on top" of all other components.
 *
 */
public final class AlwaysOnTopJOptionPane
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static boolean headless = false;    // Are we running headless (no UI)
    private static boolean underTest = false;

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private AlwaysOnTopJOptionPane()
    {
    }

    /**
     * Set if we are running headless (without a UI).  If this is true, then we won't actually put up
     * message dialogs - we'll just log them.
     *
     * @param headless new headless setting
     */
    public static void setHeadless(boolean headless, boolean underTest)
    {
        AlwaysOnTopJOptionPane.headless = headless;
        AlwaysOnTopJOptionPane.underTest = underTest;
    }

    /**
     * Brings up a dialog with the options <i>Yes</i>, <i>No</i> and <i>Cancel</i>; with the title, <b>Select an
     * Option</b>.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null},
     *                        or if the {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used
     * @param message         the {@code Object} to display
     * @return an integer indicating the option selected by the user
     */
    public static int showConfirmDialog(Component parentComponent, Object message)
    {
        return showConfirmDialog(parentComponent, message, null, JOptionPane.YES_NO_CANCEL_OPTION);
    }

    /**
     * Brings up a dialog where the number of choices is determined by the {@code optionType} parameter.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null},
     *                        or if the {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @param optionType      an int designating the options available on the dialog: {@code YES_NO_OPTION},
     *                        {@code YES_NO_CANCEL_OPTION}, or {@code OK_CANCEL_OPTION}
     * @return an integer indicating the option selected by the user
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType)
    {
        return showConfirmDialog(parentComponent, message, title, JOptionPane.QUESTION_MESSAGE, optionType);
    }

    /**
     * Brings up a dialog where the number of choices is determined by the {@code optionType} parameter, where the
     * {@code messageType} parameter determines the icon to display. The {@code messageType} parameter is
     * primarily used to supply a default icon from the Look and Feel.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null},
     *                        or if the {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @param optionType      an int designating the options available on the dialog: {@code YES_NO_OPTION},
     *                        {@code YES_NO_CANCEL_OPTION}, or {@code OK_CANCEL_OPTION}
     * @param messageType     an integer designating the kind of message this is; primarily used to determine the icon from
     *                        the pluggable Look and Feel: {@code ERROR_MESSAGE}, {@code INFORMATION_MESSAGE},
     *                        {@code WARNING_MESSAGE}, {@code QUESTION_MESSAGE}, or {@code PLAIN_MESSAGE}
     * @return an integer indicating the option selected by the user
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, int optionType)
    {
        return showConfirmDialog(parentComponent, message, title, messageType, optionType, null);
    }

    /**
     * Brings up a dialog with a specified icon, where the number of choices is determined by the
     * {@code optionType} parameter. The {@code messageType} parameter is primarily used to supply a default
     * icon from the look and feel.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null},
     *                        or if the {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @param optionType      an int designating the options available on the dialog: {@code YES_NO_OPTION},
     *                        {@code YES_NO_CANCEL_OPTION}, or {@code OK_CANCEL_OPTION}
     * @param messageType     an integer designating the kind of message this is; primarily used to determine the icon from
     *                        the pluggable Look and Feel: {@code ERROR_MESSAGE}, {@code INFORMATION_MESSAGE},
     *                        {@code WARNING_MESSAGE}, {@code QUESTION_MESSAGE}, or {@code PLAIN_MESSAGE}
     * @param icon            the icon to display in the dialog
     * @return an integer indicating the option selected by the user
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, int optionType, Icon icon)
    {
        return showOptionDialog(parentComponent, message, title, messageType, optionType, icon, null, null);
    }

    /**
     * Shows a question-message dialog requesting input from the user. The
     * dialog uses the default frame, which usually means it is centered on
     * the screen.
     *
     * @param message the {@code Object} to display
     * @return string entered by user, may be {@code null} if user canceled the dialog
     */
    @Nullable
    public static String showInputDialog(Object message)
    {
        return showInputDialog(message, null);
    }

    /**
     * Shows a question-message dialog requesting input from the user. The
     * dialog uses the default frame, which usually means it is centered on
     * the screen.
     *
     * @param message the {@code Object} to display
     * @param title   title for the dialog
     * @return string entered by user, may be {@code null} if user canceled the dialog
     */
    @Nullable
    public static String showInputDialog(Object message, String title)
    {
        return showInputDialog(message, title, null);
    }

    /**
     * Shows a question-message dialog requesting input from the user. The
     * dialog uses the default frame, which usually means it is centered on
     * the screen.
     *
     * @param message               the {@code Object} to display
     * @param title                 title for the dialog
     * @param initialSelectionValue the value used to initialize the input field
     * @return string entered by user, may be {@code null} if user canceled the dialog
     */
    @Nullable
    public static String showInputDialog(Object message, String title, Object initialSelectionValue)
    {
        return (String) showInputDialog(null, message, title, JOptionPane.QUESTION_MESSAGE, null, null, initialSelectionValue);
    }

    /**
     * Fully configured JOptionPane.
     *
     * @param parentComponent       determines the {@code Frame} in which the dialog is displayed
     * @param message               the {@code Object} to display
     * @param title                 the title string for the dialog
     * @param messageType           the type of message to be displayed
     * @param icon                  an icon to display in the dialog
     * @param selectionValues       an array of {@code Object}s that gives the possible selections
     * @param initialSelectionValue the value used to initialize the input field
     * @return user's input, or {@code null} meaning the user canceled the input
     * @see JOptionPane#showInputDialog(java.awt.Component, Object, String, int, javax.swing.Icon, Object[], Object)
     */
    @Nullable
    public static Object showInputDialog(Component parentComponent,
                                         Object message, String title, int messageType, Icon icon,
                                         Object[] selectionValues, Object initialSelectionValue)
    {

        JOptionPane pane = new JOptionPane(message, messageType, JOptionPane.OK_CANCEL_OPTION, icon, null, null);

        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        pane.setComponentOrientation(((parentComponent == null)
                ? JFrame.getFrames()[0] // Our application frame
                : parentComponent).getComponentOrientation());

        showDialog(parentComponent, pane, title);
        Object value = pane.getInputValue();

        if (value == JOptionPane.UNINITIALIZED_VALUE)
        {
            return null;
        }
        return value;
    }

    /**
     * Brings up a dialog that displays a message using a default
     * icon determined by the {@code messageType} parameter.
     *
     * @param message     the {@code Object} to display
     * @param title       the title string for the dialog
     * @param messageType the type of message to be displayed
     * @see JOptionPane#showMessageDialog(java.awt.Component, Object, String, int)
     */
    public static void showMessageDialog(Object message, String title, int messageType)
    {
        showMessageDialog(null, message, title, messageType, null);
    }

    /**
     * Brings up a dialog that displays a message using a default
     * icon determined by the {@code messageType} parameter.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @param messageType     the type of message to be displayed
     * @see JOptionPane#showMessageDialog(java.awt.Component, Object, String, int)
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType)
    {
        showMessageDialog(parentComponent, message, title, messageType, null);
    }

    /**
     * Brings up a dialog displaying a message, specifying all parameters.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @param messageType     the type of message to be displayed
     * @param icon            an icon to display in the dialog
     * @see JOptionPane#showMessageDialog(java.awt.Component, Object, String, int, javax.swing.Icon)
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType, Icon icon)
    {
        if (headless || underTest)
        {
            logger.warn("{}: {}", title, message);
        } else
        {
            showOptionDialog(parentComponent, message, title, messageType, JOptionPane.DEFAULT_OPTION, icon, null, null);
        }
    }

    /**
     * Show an error message dialog.
     *
     * @param message The message to display
     * @param title   Title for the dialog
     */
    public static void showErrorDialog(Object message, String title)
    {
        showErrorDialog(null, message, title);
    }

    /**
     * Show an error message dialog.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed
     * @param message         The message to display
     * @param title           Title for the dialog
     * @since 1.4
     */
    public static void showErrorDialog(Component parentComponent, Object message, String title)
    {
        showMessageDialog(parentComponent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Show an error dialog later on the EDT.
     *
     * @param message The message to display
     * @param title   Title for the dialog
     */
    public static void showErrorDialogLater(final Object message, final String title)
    {
        showErrorDialogLater(null, message, title);
    }

    /**
     * Show an error dialog later on the EDT.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed
     * @param message         The message to display
     * @param title           Title for the dialog
     * @since 1.4
     */
    public static void showErrorDialogLater(final Component parentComponent, final Object message, final String title)
    {
        SwingUtilities.invokeLater(() -> showMessageDialog(parentComponent, message, title, JOptionPane.ERROR_MESSAGE));
    }

    /**
     * Show an option dialog with the default parent component.
     *
     * @param message      the {@code Object} to display
     * @param title        the title string for the dialog
     * @param messageType  the type of message to be displayed
     * @param optionType   an integer designating the options available on the dialog
     * @param icon         an icon to display in the dialog
     * @param options      an array of objects indicating the possible choices the user can make
     * @param initialValue the object that represents the default selection for the dialog
     * @return an integer indicating the option chosen by the user, or {@code CLOSED_OPTION} if the user closed the dialog
     */
    public static int showOptionDialog(Object message, String title, int messageType, int optionType,
                                       Icon icon, Object[] options, Object initialValue)
    {
        return showOptionDialog(null, message, title, messageType, optionType, icon, options, initialValue);
    }

    /**
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @param messageType     the type of message to be displayed
     * @param optionType      an integer designating the options available on the dialog
     * @param icon            an icon to display in the dialog
     * @param options         an array of objects indicating the possible choices the user can make
     * @param initialValue    the object that represents the default selection for the dialog
     * @return an integer indicating the option chosen by the user, or {@code CLOSED_OPTION} if the user closed the dialog
     * @see JOptionPane#showOptionDialog(java.awt.Component, Object, String, int, int, javax.swing.Icon, Object[], Object)
     */
    public static int showOptionDialog(Component parentComponent,
                                       Object message, String title, int messageType, int optionType,
                                       Icon icon, Object[] options, Object initialValue)
    {
        if (headless || underTest)
        {
            logger.warn("{}: {}", title, message);
            return JOptionPane.CLOSED_OPTION;
        } else
        {
            return internalShowOptionDialog(parentComponent, message, title, messageType, optionType, icon, options, initialValue);
        }
    }

    /**
     * Internal method to actually show the dialog.  Note that this method is annotated with {@code InvokeAndWaitOnEDT}
     * so that non-EDT threads can make use of the main methods here without regard to being on the EDT or not.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @param messageType     the type of message to be displayed
     * @param optionType      an integer designating the options available on the dialog
     * @param icon            an icon to display in the dialog
     * @param options         an array of objects indicating the possible choices the user can make
     * @param initialValue    the object that represents the default selection for the dialog
     * @return an integer indicating the option chosen by the user, or {@code CLOSED_OPTION} if the user closed the dialog
     * @see JOptionPane#showOptionDialog(java.awt.Component, Object, String, int, int, javax.swing.Icon, Object[], Object)
     */
    @InvokeAndWaitOnEDT
    private static int internalShowOptionDialog(Component parentComponent,
                                                Object message, String title, int messageType, int optionType,
                                                Icon icon, Object[] options, Object initialValue)
    {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon, options, initialValue);

        pane.setInitialValue(initialValue);

        final var frames = JFrame.getFrames();
        final var referenceComponent = parentComponent == null
                ? (frames.length > 0 ? frames[0] : null) // Our application frame
                : parentComponent;

        if (referenceComponent != null)
        {
            pane.setComponentOrientation(referenceComponent.getComponentOrientation());
        }

        showDialog(parentComponent, pane, title);
        Object selectedValue = pane.getValue();

        if (selectedValue == null)
        {
            return JOptionPane.CLOSED_OPTION;
        }
        if (options == null)
        {
            if (selectedValue instanceof Integer)
            {
                return (Integer) selectedValue;
            }
            return JOptionPane.CLOSED_OPTION;
        }
        for (int counter = 0, maxCounter = options.length;
                counter < maxCounter; counter++)
        {
            if (options[counter].equals(selectedValue))
            {
                return counter;
            }
        }
        return JOptionPane.CLOSED_OPTION;
    }

    /**
     * Show an existing {@link JOptionPane} with the given title
     *
     * @param pane  JOptionPane to handle
     * @param title Title string for dialog
     */
    public static void showDialog(JOptionPane pane, String title)
    {
        showDialog(null, pane, title);
    }

    /**
     * Show an existing {@link JOptionPane} with the given title
     *
     * @param parentComponent Component to use as parent for dialog
     * @param pane            JOptionPane to handle
     * @param title           Title string for dialog
     */
    public static void showDialog(Component parentComponent, JOptionPane pane, String title)
    {
        if (title == null)
        {
            title = UIManager.getString("OptionPane.inputDialogTitle");
        }

        if (parentComponent == null)
        {
            final var frames = JFrame.getFrames();
            if (frames.length > 0) parentComponent = frames[0]; // Our application frame
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Parent component is {} is showing: {}", parentComponent,
                    parentComponent == null ? null : parentComponent.isShowing());
        }

        JDialog dialog = pane.createDialog(parentComponent, title);

        if (!SystemUtils.IS_OS_WINDOWS)
        {
            logger.debug("Setting the AlwaysOnTop attribute on non-Windows platforms");
            dialog.setAlwaysOnTop(true);
        }

        dialog.setModal(true);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();
    }

    /**
     * Brings up a dialog with the options <i>Yes</i>, <i>Yes to All</i>, <i>No</i>, and <i>No to All</i>.
     *
     * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null},
     *                        or if the {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used
     * @param message         the {@code Object} to display
     * @param title           the title string for the dialog
     * @return an enum indicating the option selected by the user
     */
    public static YesNoToAll showYesNoToAllDialog(Component parentComponent, Object message, String title)
    {
        return YesNoToAll.getYesNoToAll(showOptionDialog(parentComponent, message, title, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, YesNoToAll.getYesNoToAllOptions(), YesNoToAll.getYesNoToAllDefaultOption()));
    }
}
