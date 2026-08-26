package gov.takx.core.view.gui.preferences;

import gov.tak.api.icons.TakColor;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.preferences.IPreferenceEntry;
import jakarta.annotation.Nonnull;

import javax.swing.JScrollPane;

import java.util.List;

/**
 * A preference editor panel for {@link TakColor} preferences.
 *
 * @since 5.6
 */
public class TakColorChooserPreferenceEditorPanel extends APreferenceEditorPanel
{
    private final TakColorChooserButton chooserButton;

    /**
     * Create a new editor panel for the given parent and preference, and allow users to select from the given color List.
     *
     * @param parent Parent scroll pane that will contain this panel
     * @param preferenceEntry Preference entry to edit
     * @param colors the List of colors to allow the user to select from
     */
    public TakColorChooserPreferenceEditorPanel(@Nonnull JScrollPane parent, @Nonnull IPreferenceEntry preferenceEntry, @Nonnull List<TakColor> colors)
    {
        super(parent, preferenceEntry);
        chooserButton = new TakColorChooserButton(colors, this::handleColorSelected);
        chooserButton.setFocusable(false);
        setValueDisplayComponent(chooserButton);
        editorPanel.setVisible(false);
        updateDisplayValue();
    }

    private void handleColorSelected(@Nonnull TakColor color)
    {
        preferenceEntry.putAndSave(color.name());
    }

    @Override
    protected void handleMainPanelClick()
    {
        chooserButton.open();
    }

    @InvokeOnEDT
    @Override
    protected void updateDisplayValue()
    {
        chooserButton.setColor(preferenceEntry.getValueAs(TakColor.class));
    }
}
