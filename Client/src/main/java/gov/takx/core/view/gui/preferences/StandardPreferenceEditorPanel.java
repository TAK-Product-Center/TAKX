package gov.takx.core.view.gui.preferences;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.preferences.IPreferenceEntry;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import java.util.function.Consumer;

/**
 * A preference editor panel with the "standard" handling. When the user clicks on the title/description panel, the edit
 * control panel is made visible for the user to interact with. If the user clicks cancel, then the edit control is
 * hidden, and no change is made to the preference value. If the user clicks Ok, then the edit control is hidden, and
 * the preference is updated with the value obtained from the edit control.
 *
 * @since 5.4
 */
public class StandardPreferenceEditorPanel extends APreferenceEditorPanel
{
    private final Consumer<IPreferenceEntry> valueDisplayUpdater;
    private boolean editorShowing;

    /**
     * @param parent Parent scroll pane that will contain this panel
     * @param preferenceEntry Preference entry to edit
     * @param editorComponent Component to use for editing the preference value
     * @param valueDisplayComponent Component to display the current preference value
     * @param valueDisplayUpdater Consumer to invoke to update the {@code valueDisplayComponent}
     */
    StandardPreferenceEditorPanel(@Nonnull JScrollPane parent, @Nonnull IPreferenceEntry preferenceEntry, @Nonnull IEditorComponent editorComponent,
                                  @Nonnull JComponent valueDisplayComponent, @Nonnull Consumer<IPreferenceEntry> valueDisplayUpdater)
    {
        super(parent, preferenceEntry, editorComponent);

        setOkHandler(this::commit);
        setCancelHandler(this::cancel);
        setEditorVisible(false); // Initially not displayed

        this.valueDisplayUpdater = valueDisplayUpdater;

        setValueDisplayComponent(valueDisplayComponent);
        updateDisplayValue();
    }

    @Override
    protected void handleMainPanelClick()
    {
        if (!editorShowing) setEditorVisible(true);
    }

    /**
     * @param visible Show the edit panel if {@code true}, hide it if {@code false}
     */
    private void setEditorVisible(boolean visible)
    {
        if (visible) editorComponent.updateFromPreference(preferenceEntry); // Get the current value

        // The value display is the opposite of the editor, when the editor is visible, we hide the value display
        valueDisplayHolder.setVisible(!visible);

        editorPanel.setVisible(visible);
        if (visible) editorComponent.getComponent().requestFocus();

        // When the value display is hidden, the description will expand to fill that empty space.
        // In order to make it shrink again when the value is made visible, we need to reset its size.
        if (!visible) resetDescriptionSize();

        editorShowing = visible;
    }

    private void commit()
    {
        // Some preference changes can invoke listeners that take time to process (think network connection changes).
        // So get the update off the EDT.
        doInBackground(() -> editorComponent.applyToPreference(preferenceEntry))
                .thenRun(this::updateDisplayValue);

        setEditorVisible(false);
    }

    private void cancel()
    {
        setEditorVisible(false);
    }

    @InvokeOnEDT
    @Override
    protected void updateDisplayValue()
    {
        valueDisplayUpdater.accept(preferenceEntry);
        resetDescriptionSize();
    }
}
