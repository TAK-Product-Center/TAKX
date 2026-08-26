package gov.takx.core.view.gui.preferences;

import gov.takx.platform.toolbar.ToolbarButtonLabelLocation;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

import static gov.takx.platform.preferences.UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UIConfigurationPreferencesPanel}.
 *
 * @since 3.4
 */
public class UIConfigurationPreferencesPanelTest extends AContainerTest
{
    private UIConfigurationPreferencesPanel preferencesPanel;

    @BeforeEach
    public void setupBeforeMethod()
    {
        preferencesPanel = new UIConfigurationPreferencesPanel();
    }

    @Test
    public void testButtonLabelLocationChange()
    {
        final JComboBox<ToolbarButtonLabelLocation> labelLocationComboBox = preferencesPanel.toolbarLabelLocationComboBox;

        // Start off with the default label location
        TOOLBAR_BUTTON_LABEL_LOCATION.putAndSave(ToolbarButtonLabelLocation.BOTTOM.name());
        SwingTestUtils.waitForEdtEvents();

        // Make sure UI reflects preferences
        ToolbarButtonLabelLocation selectedLabelLocation = (ToolbarButtonLabelLocation) labelLocationComboBox.getSelectedItem();
        assertThat(selectedLabelLocation).isEqualTo(ToolbarButtonLabelLocation.BOTTOM);

        // Change the preference and make sure UI is updated
        TOOLBAR_BUTTON_LABEL_LOCATION.putAndSave(ToolbarButtonLabelLocation.RIGHT.name());
        SwingTestUtils.waitForEdtEvents();
        selectedLabelLocation = (ToolbarButtonLabelLocation) labelLocationComboBox.getSelectedItem();
        assertThat(selectedLabelLocation).isEqualTo(ToolbarButtonLabelLocation.RIGHT);

        // Update selected option in UI and make sure preference is updated
        labelLocationComboBox.setSelectedItem(ToolbarButtonLabelLocation.NONE);
        SwingTestUtils.waitForEdtEvents();
        preferencesPanel.applyAction();

        final ToolbarButtonLabelLocation labelLocation = TOOLBAR_BUTTON_LABEL_LOCATION.getValueAs(ToolbarButtonLabelLocation.class);
        assertThat(labelLocation).isEqualTo(ToolbarButtonLabelLocation.NONE);
    }
}