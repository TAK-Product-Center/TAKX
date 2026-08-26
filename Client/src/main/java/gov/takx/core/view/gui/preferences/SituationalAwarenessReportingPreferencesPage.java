package gov.takx.core.view.gui.preferences;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.platform.preferences.PreferenceEntry;

import java.util.List;

import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_CONSTANT_INTERVAL_RELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_CONSTANT_INTERVAL_UNRELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_RELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_UNRELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_RELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_UNRELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_RELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_UNRELIABLE;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_REPORT_NO_LOCATION;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_SEND_LOCATION;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_STRATEGY;

/**
 * Preferences related to SA reporting.
 * @since 5.4
 */
public class SituationalAwarenessReportingPreferencesPage extends APreferencesPage
{
    private static final String DYNAMIC = "Dynamic";
    private static final String CONSTANT = "Constant";
    private static final List<String> STRATEGIES = List.of(CONSTANT, DYNAMIC);

    // Preferences that should be shown for Constant reporting
    private static final List<PreferenceEntry> CONSTANT_REPORTING_PREFERENCES = List.of(
            SA_REPORTING_CONSTANT_INTERVAL_RELIABLE,
            SA_REPORTING_CONSTANT_INTERVAL_UNRELIABLE);

    // Preferences that should be shown for Dynamic reporting
    private static final List<PreferenceEntry> DYNAMIC_REPORTING_PREFERENCES = List.of(
            SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_RELIABLE,
            SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_UNRELIABLE,
            SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_RELIABLE,
            SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_UNRELIABLE,
            SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_RELIABLE,
            SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_UNRELIABLE);

    private final IPreferenceMonitor<String> reportingStrategyMonitor = SA_REPORTING_STRATEGY.newStringMonitor(this::updateForStrategy);

    SituationalAwarenessReportingPreferencesPage()
    {
        super("Reporting Preferences");

        addCheckboxEditor(SA_REPORTING_SEND_LOCATION);
        addEditor(SA_REPORTING_STRATEGY, PreferencesComponentFactory.makeComboBox(STRATEGIES));
        addEditor(SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_RELIABLE, PreferencesComponentFactory.makeIntSpinnerEditor());
        addEditor(SA_REPORTING_DYNAMIC_INTERVAL_MAXIMUM_UNRELIABLE, PreferencesComponentFactory.makeIntSpinnerEditor());
        addEditor(SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_RELIABLE, PreferencesComponentFactory.makeIntSpinnerEditor());
        addEditor(SA_REPORTING_DYNAMIC_INTERVAL_MINIMUM_UNRELIABLE, PreferencesComponentFactory.makeIntSpinnerEditor());
        addEditor(SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_RELIABLE, PreferencesComponentFactory.makeIntSpinnerEditor());
        addEditor(SA_REPORTING_DYNAMIC_INTERVAL_STATIONARY_UNRELIABLE, PreferencesComponentFactory.makeIntSpinnerEditor());
        addEditor(SA_REPORTING_CONSTANT_INTERVAL_RELIABLE, PreferencesComponentFactory.makeIntSpinnerEditor());
        addEditor(SA_REPORTING_CONSTANT_INTERVAL_UNRELIABLE, PreferencesComponentFactory.makeIntSpinnerEditor());
        addCheckboxEditor(SA_REPORTING_REPORT_NO_LOCATION);

        updateForStrategy(SA_REPORTING_STRATEGY.getStringValue());
    }

    /**
     * Update the visible panels based on the given {@code strategy}.
     */
    @InvokeOnEDT
    @SuppressWarnings("DataFlowIssue")
    private void updateForStrategy(String strategy)
    {
        final boolean isConstantStrategy = CONSTANT.equals(strategy);

        CONSTANT_REPORTING_PREFERENCES.forEach(pref -> getEditorPanel(pref).getParent().setVisible(isConstantStrategy));
        DYNAMIC_REPORTING_PREFERENCES.forEach(pref -> getEditorPanel(pref).getParent().setVisible(!isConstantStrategy));
    }

    @Override
    public void releaseResources()
    {
        reportingStrategyMonitor.dispose();
        super.releaseResources();
    }
}
