package gov.takx.commons.swing;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A swing component that handles laying out standard Ok/Cancel/Apply/Delete buttons as well as rudimentary validation for a
 * set of field components. This allows consistent operation for the button bar across different forms. Other components
 * interact with this by adding listeners and validators which are called when the user interacts with this panel.
 * <p>
 * Different listeners are called based on which button is clicked:
 * <ul>
 *     <li>Ok:
 *         <ol>
 *             <li>{@link Validator}</li>
 *             <li>{@link ApplyListener}</li>
 *             <li>{@link CloseListener}</li>
 *         </ol>
 *     </li>
 *     <li>Cancel:
 *          <ol>
 *            <li>{@link CloseListener}</li>
 *      </ol>
 *     </li>
 *      <li>Apply:
 *          <ol>
 *              <li>{@link Validator}</li>
 *              <li>{@link ApplyListener}</li>
 *          </ol>
 *     </li>
 * </ul>
 * To listen for events on a specific button you should place listeners directly on the button component.
 *
 * @since 5.7 (Moved from Client)
 */
@IncubatingApi(since = "5.7")
public class FormButtonBar extends JPanel
{
    private final Map<JComponent, Validator> validators = new HashMap<>();

    /**
     * This flag controls whether the entire form is validated when a single field changes. Full form validation is
     * initially disabled so that users can enter data in individual fields without being flooded by errors from
     * other fields. It is enabled once the user attempts to click Ok/Apply at which point the entire form is revalidated 
     * when fields change.
     */
    private boolean fullValidationEnabled;

    private boolean closeWindow = true;
    private boolean clean = true;
    private String error;

    /**
     * Construct a new instance set and set clean state to {@code true}.
     */
    public FormButtonBar()
    {
        this(true);
    }

    /**
     * Construct a new instance set and set clean state.
     *
     * @param clean the desired clean state
     */
    public FormButtonBar(boolean clean)
    {
        initComponents();
        // Allows us to adjust the layout when consumers change the visibility of buttons
        var buttonColumnHider = new ButtonColumnHider();
        applyButton.addComponentListener(buttonColumnHider);
        okButton.addComponentListener(buttonColumnHider);
        cancelButton.addComponentListener(buttonColumnHider);
        deleteButton.addComponentListener(buttonColumnHider);
        setPanelClean(clean);
    }

    /**
     * See {@link #setPanelClean(boolean)}.
     */
    public void setPanelDirty()
    {
        setPanelClean(false);
    }

    /**
     * See {@link #setPanelClean(boolean)}.
     */
    public void setPanelClean()
    {
        setPanelClean(true);
    }

    /**
     * Sets the clean state. A clean state of {@code true} means that apply listeners will not be called and the Apply button
     * will be disabled. The Ok button is always enabled as it also closes the dialog.
     *
     * @param clean the desired clean state, {@code true} if clean, otherwise {@code false}
     */
    public void setPanelClean(boolean clean)
    {
        this.clean = clean;
        applyButton.setEnabled(!clean);
    }

    /**
     * Attach a generic component by adding a validator and a {@link ListSelectionListener}.
     *
     * @param component the component
     * @param listenerConsumer the consumer for a list selection listener that will trigger validation
     * @param validator an optional validator for the component
     */
    public void attachFieldListSelectionListener(@Nonnull JComponent component, @Nonnull Consumer<ListSelectionListener> listenerConsumer, @Nullable Validator validator)
    {
        if (validator != null) validators.put(component, validator);
        listenerConsumer.accept(event -> validateForm(component, event));
    }

    /**
     * Attach a generic component by adding a validator and an {@link IPanelStateListener}.
     *
     * @param component the component
     * @param listenerConsumer the consumer for a panel state listener that will trigger validation
     * @param validator an optional validator for the component
     */
    public void attachPanelStateListener(@Nonnull JComponent component, @Nonnull Consumer<IPanelStateListener> listenerConsumer, @Nullable Validator validator)
    {
        if (validator != null) validators.put(component, validator);
        listenerConsumer.accept(new IPanelStateListener()
        {
            @Override
            public void setPanelDirty()
            {
                validateForm(component, new ChangeEvent(component));
            }

            @Override
            public void setPanelClean()
            {
            }
        });
    }

    /**
     * Attach a generic component by adding a validator and an {@link ActionListener}.
     *
     * @param component the component
     * @param listenerConsumer the consumer for an action listener that will trigger validation
     * @param validator an optional validator for the component
     */
    public void attachFieldActionListener(@Nonnull JComponent component, @Nonnull Consumer<ActionListener> listenerConsumer, @Nullable Validator validator)
    {
        if (validator != null)
        {
            validators.put(component, validator);
        }
        listenerConsumer.accept(event -> validateForm(component, event));
    }

    /**
     * Attach a generic component by adding a validator and an {@link ChangeListener}.
     *
     * @param component the component
     * @param listenerConsumer the consumer for a change listener that will trigger validation
     * @param validator an optional validator for the component
     */
    public void attachFieldChangeListener(@Nonnull JComponent component, @Nonnull Consumer<ChangeListener> listenerConsumer, @Nullable Validator validator)
    {
        if (validator != null)
        {
            validators.put(component, validator);
        }
        listenerConsumer.accept(event -> validateForm(component, event));
    }

    /**
     * Attach a {@link JTextField} by adding a validator and {@link javax.swing.event.DocumentListener}.
     *
     * @param textField the text field
     * @param validator the validator for the field
     */
    public void attachField(@Nonnull JTextField textField, @Nullable Validator validator)
    {
        if (validator != null)
        {
            validators.put(textField, validator);
        }
        textField.getDocument().addDocumentListener((CombinedDocumentListener) event -> validateForm(textField, event));
    }

    /**
     * Attach a {@link JComboBox} by adding a validator and an {@link ActionListener}.
     *
     * @param comboBox the combo box
     * @param validator the validator for the field
     */
    public void attachField(@Nonnull JComboBox<?> comboBox, @Nullable Validator validator)
    {
        if (validator != null)
        {
            validators.put(comboBox, validator);
        }
        comboBox.addActionListener(event -> validateForm(comboBox, event));
    }

    /**
     * Attach a {@link JSpinner} by adding a validator and an {@link javax.swing.event.ChangeListener}.
     *
     * @param spinner the spinner
     * @param validator the validator for the field
     */
    public void attachField(@Nonnull JSpinner spinner, @Nullable Validator validator)
    {
        if (validator != null)
        {
            validators.put(spinner, validator);
        }
        spinner.addChangeListener(event -> validateForm(spinner, event));
    }

    /**
     * Attach an {@link AbstractButton} by adding a validator and an {@link ActionListener}.
     *
     * @param button the button
     * @param validator the validator for the field
     */
    public void attachField(@Nonnull AbstractButton button, @Nullable Validator validator)
    {
        if (validator != null)
        {
            validators.put(button, validator);
        }
        button.addActionListener(event -> validateForm(button, event));
    }

    /**
     * Attach an {@link JTable} by adding a validator and an {@link javax.swing.event.ListSelectionListener}.
     *
     * @param table the table
     * @param validator the validator for the component
     */
    public void attachField(@Nonnull JTable table, @Nullable Validator validator)
    {
        if (validator != null)
        {
            validators.put(table, validator);
        }
        table.getSelectionModel().addListSelectionListener(event -> validateForm(table, event));
    }

    /**
     * @param listener the listener to add
     */
    public void addApplyListener(@Nonnull ApplyListener listener)
    {
        listenerList.add(ApplyListener.class, listener);
    }

    /**
     * @param listener the listener to remove
     */
    public void removeApplyListener(@Nonnull ApplyListener listener)
    {
        listenerList.remove(ApplyListener.class, listener);
    }

    /**
     * @param listener the listener to add
     */
    public void addCloseListener(@Nonnull CloseListener listener)
    {
        listenerList.add(CloseListener.class, listener);
    }

    /**
     * @param listener the listener to remove
     */
    public void removeCloseListener(@Nonnull CloseListener listener)
    {
        listenerList.remove(CloseListener.class, listener);
    }

    /**
     * @param listener the listener to add
     */
    public void addDeleteListener(@Nonnull DeleteListener listener)
    {
        listenerList.add(DeleteListener.class, listener);
    }

    /**
     * @param listener the listener to add
     */
    public void removeDeleteListener(@Nonnull DeleteListener listener)
    {
        listenerList.remove(DeleteListener.class, listener);
    }

    /**
     * @param text the error text to set above the button bar, styled red by default
     */
    public void setErrorLabelText(@Nullable String text)
    {
        errorLabel.setText(text != null ? "<html><span color=red>" + text + "</span></html>" : " ");
    }

    /**
     * Sets the error text and adjusts button states. The ok/apply buttons are enabled only if the error text is
     * {@code null}.
     *
     * @param text the error text, see {@link #setErrorLabelText(String)}
     */
    public void setErrorState(@Nullable String text)
    {
        setErrorLabelText(text);
        error = text;
        var error = text != null;
        applyButton.setEnabled(!error);
        okButton.setEnabled(!error);
    }

    /**
     * @return {@code true} if the apply button should be visible, otherwise {@code false}
     */
    public boolean isApplyButtonVisible()
    {
        return applyButton.isVisible();
    }

    /**
     * @param visible {@code true} if the apply button should be visible, otherwise {@code false}
     */
    public void setApplyButtonVisible(boolean visible)
    {
        applyButton.setVisible(visible);
    }

    /**
     * @return the current error text
     */
    @Nullable
    public String getError()
    {
        return error;
    }

    /**
     * @return {@code true} if the current error text is not {@code null}, otherwise {@code false}
     */
    public boolean isError()
    {
        return error != null;
    }

    /**
     * "Clean" implies that no fields have been changed since having been attached to this instance.
     *
     * @return {@code true} if the button bar is in clean state, otherwise {@code false}
     */
    public boolean isClean()
    {
        return clean;
    }

    private void closeWindow(@Nonnull ActionEvent ignored)
    {
        if (closeWindow)
        {
            var window = SwingUtilities.getWindowAncestor(this);
            if (window != null)
            {
                window.dispose();
                window.setVisible(false);
            }
        }
    }

    private String getFieldError(@Nonnull JComponent component, @Nonnull Object event)
    {
        return validators.containsKey(component) ? validators.get(component).validate(event) : null;
    }

    private String getFormError(@Nonnull Object event)
    {
        String firstErrorText = null;
        for (var component : validators.keySet())
        {
            firstErrorText = getFieldError(component, event);
            if (firstErrorText != null)
            {
                break;
            }
        }
        return firstErrorText;
    }

    private boolean validateForm(@Nullable JComponent checkFirst, @Nonnull Object event)
    {
        setPanelDirty();
        String error = null;
        if (checkFirst != null)
        {
            error = getFieldError(checkFirst, event);
        }
        if (fullValidationEnabled && error == null)
        {
            error = getFormError(event);
        }
        setErrorState(error);
        return error == null;
    }

    private void deleteButtonActionPerformed(ActionEvent e)
    {
        var event = new DeleteActionEvent(e);
        // If any delete listeners are successful then close the window
        if (Stream.of(listenerList.getListeners(DeleteListener.class))
                .map(l -> {
                    l.delete(event);
                    return event.successful;
                })
                .reduce(Boolean::logicalOr).orElse(false))
        {
            close(e);
        }
    }

    private void okButtonActionPerformed(ActionEvent e)
    {
        apply(e);
        if (!isError()) close(e);
    }

    private void cancelButtonActionPerformed(ActionEvent e)
    {
        close(e);
    }

    private void applyButtonActionPerformed(ActionEvent e)
    {
        apply(e);
    }

    private void apply(ActionEvent e)
    {
        if (!clean)
        {
            fullValidationEnabled = true;
            if (!validateForm(null, e))
            {
                return;
            }
            List.of(listenerList.getListeners(ApplyListener.class)).forEach(l -> l.apply(e));
            setPanelClean();
        }
    }

    private void close(ActionEvent e)
    {
        List.of(listenerList.getListeners(CloseListener.class)).forEach(l -> l.close(e));
        closeWindow(e);
    }

    /**
     * @return {@code true} if the error label should is visible, otherwise {@code false}
     */
    public boolean isErrorLabelVisible()
    {
        return errorLabel.isVisible();
    }

    /**
     * @param visible {@code true} if the error label should be visible, otherwise {@code false}
     */
    public void setErrorLabelVisible(boolean visible)
    {
        if (errorLabel.isVisible() != visible)
        {
            errorLabel.setVisible(visible);
            FormLayoutUtils.hideOrRestoreRow(errorLabel, !visible);
        }
    }

    /**
     * @return {@code true} if the delete button is visible, otherwise {@code false}
     */
    public boolean isDeleteButtonVisible()
    {
        return deleteButton.isVisible();
    }

    /**
     * @param visible {@code true} if the delete button should be visible, otherwise {@code false}
     */
    public void setDeleteButtonVisible(boolean visible)
    {
        deleteButton.setVisible(visible);
    }

    /**
     * @return {@code true} if the window ancestor should be closed after cancel is clicked, otherwise {@code false}
     */
    public boolean isCloseWindow()
    {
        return closeWindow;
    }

    /**
     * @param closeWindow {@code true} if the window ancestor should be closed after cancel is clicked, otherwise
     * {@code false}
     */
    public void setCloseWindow(boolean closeWindow)
    {
        this.closeWindow = closeWindow;
    }

    /**
     * @return the error text component
     */
    @Nonnull
    public JLabel getErrorLabel()
    {
        return errorLabel;
    }

    /**
     * @return the Ok button component
     */
    @Nonnull
    public JButton getOkButton()
    {
        return okButton;
    }

    /**
     * @return the Cancel button component
     */
    @Nonnull
    public JButton getCancelButton()
    {
        return cancelButton;
    }

    /**
     * @return the Apply button component
     */
    @Nonnull
    public JButton getApplyButton()
    {
        return applyButton;
    }

    /**
     * @return the Data button component
     */
    @Nonnull
    public JButton getDeleteButton()
    {
        return deleteButton;
    }

    /**
     * Listener for "Apply" events.
     * 
     * @since 5.7
     */
    @FunctionalInterface
    @IncubatingApi(since = "5.7")
    public interface ApplyListener extends EventListener
    {
        /**
         * Applies any relevant form field changes.
         *
         * @param actionEvent the event that triggered this call
         */
        void apply(@Nonnull ActionEvent actionEvent);
    }

    /**
     * Listener for "Close" events.
     * 
     * @since 5.7
     */
    @FunctionalInterface
    @IncubatingApi(since = "5.7")
    public interface CloseListener extends EventListener
    {
        /**
         * Called when the form should be closed.
         *
         * @param actionEvent the event that triggered this call
         */
        void close(@Nonnull ActionEvent actionEvent);
    }

    /**
     * Listener for "Delete" events.
     * 
     * @since 5.7
     */
    @FunctionalInterface
    @IncubatingApi(since = "5.7")
    public interface DeleteListener extends EventListener
    {
        /**
         * Delete the entity associated with the form.
         *
         * @param actionEvent the event that triggered this call
         */
        void delete(@Nonnull ActionEvent actionEvent);
    }

    /**
     * Validator called when form validation is triggered.
     * 
     * @since 5.7
     */
    @FunctionalInterface
    @IncubatingApi(since = "5.7")
    public interface Validator extends EventListener
    {
        /**
         * Called when the form needs to be validated.
         *
         * @param actionEvent the event that triggered this call,
         * @return error text, or {@code null} if validation was successful
         */
        @Nullable
        String validate(@Nonnull Object actionEvent);
    }

    /**
     * Extension of ActionEvent that allows a listener to indicate that the delete operation was successful.
     * 
     * @since 5.7
     */
    @IncubatingApi(since = "5.7")
    public static class DeleteActionEvent extends ActionEvent
    {
        private boolean successful;

        /**
         * @param source the source event for the delete action
         */
        public DeleteActionEvent(ActionEvent source)
        {
            super(source.getSource(), source.getID(), source.getActionCommand());
        }

        /**
         * Call to indicate the delete operation was successful
         */
        public void successful()
        {
            successful = true;
        }
    }

    /**
     * This listener hides columns in the layout when button visibility is changed. This allows us to have a clean
     * layout while still being able to hide/show buttons via the {@link java.awt.Component#setVisible(boolean)} method.
     */
    private static class ButtonColumnHider implements ComponentVisibilityListener
    {
        @Override
        public void componentShown(@Nonnull ComponentEvent e)
        {
            FormLayoutUtils.restoreColumn(e.getComponent());
        }

        @Override
        public void componentHidden(@Nonnull ComponentEvent e)
        {
            FormLayoutUtils.hideColumn(e.getComponent());
        }
    }

    @SuppressWarnings({"Convert2MethodRef", "RedundantExplicitVariableType"})
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        errorLabel = new JLabel();
        deleteButton = new JButton();
        okButton = new JButton();
        cancelButton = new JButton();
        applyButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.BUTTON_BAR_GAP_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, 3*($lcgap, default)",
                "default, $rgap, default"));

        //---- errorLabel ----
        errorLabel.setText(" ");
        errorLabel.setName("errorLabel");
        add(errorLabel, cc.xywh(1, 1, 9, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- deleteButton ----
        deleteButton.setText("Delete");
        deleteButton.setName("deleteButton");
        deleteButton.addActionListener(e -> deleteButtonActionPerformed(e));
        add(deleteButton, cc.xy(1, 3));

        //---- okButton ----
        okButton.setText("OK");
        okButton.setName("okButton");
        okButton.addActionListener(e -> okButtonActionPerformed(e));
        add(okButton, cc.xy(5, 3));

        //---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(e -> cancelButtonActionPerformed(e));
        add(cancelButton, cc.xy(7, 3));

        //---- applyButton ----
        applyButton.setText("Apply");
        applyButton.setName("applyButton");
        applyButton.addActionListener(e -> applyButtonActionPerformed(e));
        add(applyButton, cc.xy(9, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel errorLabel;
    private JButton deleteButton;
    private JButton okButton;
    private JButton cancelButton;
    private JButton applyButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
