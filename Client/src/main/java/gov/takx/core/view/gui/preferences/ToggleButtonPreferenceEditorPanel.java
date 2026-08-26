package gov.takx.core.view.gui.preferences;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.commons.swing.MouseClickedListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import java.awt.event.ItemEvent;
import java.util.function.Consumer;

/**
 * Preference editor panel to control a boolean preference value.  A toggle button/checkbox is displayed to the right
 * of the title/description, and it can be directly changed.  No subordinate editor panel is used, and clicking anywhere
 * in the title/description panel will toggle the button state.
 *
 * @since 5.4
 */
public class ToggleButtonPreferenceEditorPanel extends APreferenceEditorPanel
{
    private final JToggleButton toggleButton;

    /**
     * Factory method to produce an instance using a {@link JCheckBox}.
     *
     * @param parent Parent scroll pane that will contain this panel
     * @param preferenceEntry Preference entry to edit
     * @param actionHandler Optional consumer to invoke when the button state changes
     * @return configured editor panel
     */
    public static ToggleButtonPreferenceEditorPanel usingCheckBox(@Nonnull JScrollPane parent, @Nonnull IPreferenceEntry preferenceEntry,
                                                                  @Nullable Consumer<Boolean> actionHandler)
    {
        return new ToggleButtonPreferenceEditorPanel(parent, preferenceEntry, new JCheckBox((String) null, preferenceEntry.getBooleanValue()), actionHandler);
    }

    /**
     * Factory method to produce an instance using a {@link JToggleButton}.
     * 
     * @param text Button text
     * @param icon Button icon
     * @param parent Parent scroll pane that will contain this panel
     * @param preferenceEntry Preference entry to edit
     * @param actionHandler Optional consumer to invoke when the button state changes
     * @return configured editor panel
     */
    public static ToggleButtonPreferenceEditorPanel usingToggleButton(@Nullable String text, @Nullable Icon icon,
                                                                      @Nonnull JScrollPane parent, @Nonnull IPreferenceEntry preferenceEntry,
                                                                      @Nullable Consumer<Boolean> actionHandler)
    {
        return new ToggleButtonPreferenceEditorPanel(parent, preferenceEntry, new JToggleButton(text, icon, preferenceEntry.getBooleanValue()), actionHandler);
    }

    private ToggleButtonPreferenceEditorPanel(@Nonnull JScrollPane parent, @Nonnull IPreferenceEntry preferenceEntry,
                                              @Nonnull JToggleButton toggleButton, @Nullable Consumer<Boolean> actionHandler)
    {
        super(parent, preferenceEntry);

        this.toggleButton = toggleButton;
        toggleButton.setFocusable(false);

        toggleButton.addMouseListener((MouseClickedListener) this::mainPanelMouseClicked);
        if (actionHandler != null) toggleButton.addItemListener(e -> actionHandler.accept(e.getStateChange() == ItemEvent.SELECTED));

        editorPanel.setVisible(false); // We don't use an editor panel

        setValueDisplayComponent(toggleButton);
        updateDisplayValue();
    }

    @Override
    protected void handleMainPanelClick()
    {
        // Some preference changes can invoke listeners that take time to process (think network connection changes).
        // So get the update off the EDT.
        doInBackground(() -> preferenceEntry.putAndSave(!preferenceEntry.getBooleanValue()))
                .thenRun(this::updateDisplayValue);
    }

    @InvokeOnEDT
    @Override
    protected void updateDisplayValue()
    {
        toggleButton.setSelected(preferenceEntry.getBooleanValue());
    }
}
