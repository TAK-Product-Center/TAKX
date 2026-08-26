package gov.takx.core.preferences;

import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.annotation.ToolPreference;
import gov.takx.api.annotation.ToolPreferenceType;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.api.preferences.tool.HiddenPreference;
import gov.takx.api.preferences.tool.IToolPreference;
import gov.takx.api.preferences.tool.TextPreference;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;

import java.util.List;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link ToolPreferenceManager}.
 *
 * @since 5.0
 */
public class ToolPreferenceManagerTest extends AIntegrationTest
{
    private static final String DEFAULT_TOOL_UID = "toolUid";
    private static final String DEFAULT_TOOL_NAME = "Tool Name";
    private static final String DEFAULT_DESCRIPTION = "description";
    private static final String DEFAULT_PREFERENCE_ID = "prefId";
    private static final String DEFAULT_TITLE = "title";
    private static final String DEFAULT_VALUE = "default value";

    private static final String LONG_PREFERENCE = "longPreference";
    private static final String BOOLEAN_PREFERENCE = "booleanPreference";

    private static final TextPreference DEFAULT_PREFERENCE = new TextPreference(DEFAULT_PREFERENCE_ID, DEFAULT_TITLE,
            DEFAULT_DESCRIPTION, DEFAULT_VALUE);

    @Mocked
    private ImageIcon toolIcon;

    private ToolPreferenceManager manager;

    @BeforeEach
    void beforeEach()
    {
        manager = getBean(ToolPreferenceManager.class);
    }

    @AfterEach
    void afterEach()
    {
        stopService(ToolPreferenceManager.class);
    }

    @Test
    public void addTool_addsPreferenceEntryAndNode()
    {
        manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon, List.of(DEFAULT_PREFERENCE));

        final IPreferenceEntry preferenceEntry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, DEFAULT_PREFERENCE_ID);
        assertThat(preferenceEntry).isNotNull();

        final Preferences toolPreferencesNode = manager.getPreferences().node(DEFAULT_TOOL_UID);
        assertThat(toolPreferencesNode).isNotNull();
    }

    @Test
    public void stopService_clearsState()
    {
        manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon, List.of(DEFAULT_PREFERENCE));

        List<IPreferenceEntry> preferenceEntries = manager.getToolPreferenceEntries(DEFAULT_TOOL_UID);
        assertThat(preferenceEntries).isNotNull();

        stopService(ToolPreferenceManager.class);

        preferenceEntries = manager.getToolPreferenceEntries(DEFAULT_TOOL_UID);
        assertThat(preferenceEntries).isNull();
    }

    @SuppressLogging(ToolPreferenceManager.class)
    @Test
    public void addTool_whenToolAlreadyAdded_throwsException()
    {
        manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon, List.of(DEFAULT_PREFERENCE));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon, List.of(DEFAULT_PREFERENCE)));

    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void removeTool_doesNotDeleteTool() throws BackingStoreException
    {
        manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon, List.of(DEFAULT_PREFERENCE));

        // Give it a value
        var preferenceEntry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, DEFAULT_PREFERENCE_ID);
        preferenceEntry.put("testing");

        manager.removeTool(DEFAULT_TOOL_UID);

        // IPreferenceEntry should be gone
        preferenceEntry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, DEFAULT_PREFERENCE_ID);
        assertThat(preferenceEntry).isNull();

        // The preference node should still exist
        assertThat(manager.getPreferences().nodeExists(DEFAULT_TOOL_UID)).isTrue();
    }

    @Test
    public void deleteTool_removesAndDeletesTool() throws BackingStoreException
    {
        manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon, List.of(DEFAULT_PREFERENCE));
        manager.deleteTool(DEFAULT_TOOL_UID);

        // IPreferenceEntry should be gone
        final IPreferenceEntry preferenceEntry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, DEFAULT_PREFERENCE_ID);
        assertThat(preferenceEntry).isNull();

        // The preference node should be gone
        assertThat(manager.getPreferences().nodeExists(DEFAULT_TOOL_UID)).isFalse();
    }

    @Test
    public void getToolPreferenceEntries_returnsAllToolPreferenceEntries()
    {
        final List<IToolPreference> preferences =
                List.of(createToolPreference("1"), createToolPreference("2"), createToolPreference("3"));

        manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon, preferences);

        final List<IPreferenceEntry> toolPreferenceEntries = manager.getToolPreferenceEntries(DEFAULT_TOOL_UID);
        assertThat(toolPreferenceEntries).isNotNull();
        assertThat(toolPreferenceEntries.size()).isEqualTo(preferences.size());
    }

    @Test
    public void getToolPreferenceEntry_returnsCorrectEntry()
    {
        final List<IToolPreference> preferences =
                List.of(createToolPreference("1"), createToolPreference("2"), createToolPreference("3"));
        manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon, preferences);

        IPreferenceEntry entry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, "1");
        assertThat(entry).isNotNull();
        assertThat(entry.getKey()).isEqualTo(KeyUtils.getUniqueKey(DEFAULT_TOOL_UID, "1"));

        entry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, "2");
        assertThat(entry).isNotNull();
        assertThat(entry.getKey()).isEqualTo(KeyUtils.getUniqueKey(DEFAULT_TOOL_UID, "2"));

        entry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, "3");
        assertThat(entry).isNotNull();
        assertThat(entry.getKey()).isEqualTo(KeyUtils.getUniqueKey(DEFAULT_TOOL_UID, "3"));
    }

    @Test
    public void modifyingPreferenceEntry_callsCorrectListeners(@Mocked(allowCaching = false) IPreferenceEntryChangeListener allToolsListener,
                                                               @Mocked(allowCaching = false) IPreferenceEntryChangeListener tool1Listener,
                                                               @Mocked(allowCaching = false) IPreferenceEntryChangeListener tool1Pref1Listener)
    {
        final String tool1Uid = "1";
        final String tool2Uid = "2";

        // Add listener to manager
        manager.addPreferenceEntryChangeListener(allToolsListener);

        // Add tool with tool-specific listener
        manager.addTool(tool1Uid, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon,
                List.of(createToolPreference("1"), createToolPreference("2")),
                tool1Listener);

        // Add second tool w/o a tool-specific listener
        manager.addTool(tool2Uid, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon,
                List.of(createToolPreference("1")));

        final IPreferenceEntry tool1Pref1Entry = manager.getToolPreferenceEntry(tool1Uid, "1");
        final IPreferenceEntry tool1Pref2Entry = manager.getToolPreferenceEntry(tool1Uid, "2");
        final IPreferenceEntry tool2Pref1Entry = manager.getToolPreferenceEntry(tool2Uid, "1");

        assertThat(tool1Pref1Entry).isNotNull();
        assertThat(tool1Pref2Entry).isNotNull();
        assertThat(tool2Pref1Entry).isNotNull();

        // Add preference-specific listener
        tool1Pref1Entry.addPreferenceEntryChangeListener(tool1Pref1Listener);

        int allToolsListenerTimes = 0;
        int tool1ListenerTimes = 0;
        int tool1Pref1ListenerTimes = 0;

        // Make sure not notifications have been issued
        verifyChangeCount(allToolsListener, allToolsListenerTimes);
        verifyChangeCount(tool1Listener, tool1ListenerTimes);
        verifyChangeCount(tool1Pref1Listener, tool1Pref1ListenerTimes);

        // Change tool 1, preference 1
        tool1Pref1Entry.put("x");
        allToolsListenerTimes++;
        tool1ListenerTimes++;
        tool1Pref1ListenerTimes++;

        // Make sure all listeners were notified
        verifyChangeCount(allToolsListener, allToolsListenerTimes);
        verifyChangeCount(tool1Listener, tool1ListenerTimes);
        verifyChangeCount(tool1Pref1Listener, tool1Pref1ListenerTimes);

        // Change tool 1, preference 2
        tool1Pref2Entry.put("y");
        allToolsListenerTimes++;
        tool1ListenerTimes++;

        // Make sure only the manager and tool listeners were notified
        verifyChangeCount(allToolsListener, allToolsListenerTimes);
        verifyChangeCount(tool1Listener, tool1ListenerTimes);
        verifyChangeCount(tool1Pref1Listener, tool1Pref1ListenerTimes);

        // Change tool 2, preference 1
        tool2Pref1Entry.put("z");
        allToolsListenerTimes++;

        // Make sure only the manager listener was notified
        verifyChangeCount(allToolsListener, allToolsListenerTimes);
        verifyChangeCount(tool1Listener, tool1ListenerTimes);
        verifyChangeCount(tool1Pref1Listener, tool1Pref1ListenerTimes);
    }

    private void verifyChangeCount(IPreferenceEntryChangeListener mockedPreferenceEntryChangeListener, int times)
    {
        verify(mockedPreferenceEntryChangeListener, times(times)).preferenceEntryChanged(any());
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void toolPreferenceValues_whenChanged_areObserved()
    {
        // All CDI bindings should work prior to actually creating the tool preferences
        final var verifier = new ToolCdiVerifier();

        manager.addTool(DEFAULT_TOOL_UID, DEFAULT_TOOL_NAME, DEFAULT_DESCRIPTION, toolIcon,
                List.of(new HiddenPreference(LONG_PREFERENCE, "0"),
                        new HiddenPreference(BOOLEAN_PREFERENCE, "false")));

        final var booleanPreferenceEntry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, BOOLEAN_PREFERENCE);
        final var longPreferenceEntry = manager.getToolPreferenceEntry(DEFAULT_TOOL_UID, LONG_PREFERENCE);

        for (int i = 1; i < 5; i++)
        {
            longPreferenceEntry.put(i);
            assertThat(verifier.longChangeCount).isEqualTo(i);
            assertThat(verifier.lastLongValue).isEqualTo(i);
            assertThat(verifier.longSupplier.get()).isEqualTo(i);

            assertThat(verifier.booleanChangeCount).isEqualTo(0); // should not change
        }

        verifier.booleanChangeCount = verifier.longChangeCount = 0;

        for (int i = 1; i < 5; i++)
        {
            final boolean newValue = !booleanPreferenceEntry.getBooleanValue();
            booleanPreferenceEntry.put(newValue);
            assertThat(verifier.booleanChangeCount).isEqualTo(i);
            assertThat(verifier.lastBooleanValue).isEqualTo(newValue);
            assertThat(verifier.booleanSupplier.get()).isEqualTo(newValue);

            assertThat(verifier.longChangeCount).isEqualTo(0); // should not change
        }
    }

    private IToolPreference createToolPreference(String uid)
    {
        return new TextPreference(uid, DEFAULT_TITLE, DEFAULT_DESCRIPTION, DEFAULT_VALUE);
    }

    /**
     * Harness to verify that value supplier injection and event observation work for tool preferences.
     * @since 5.5.1
     */
    @PseudoBean
    private static class ToolCdiVerifier
    {
        @Inject
        @ToolPreference(toolUid = DEFAULT_TOOL_UID, preferenceId = BOOLEAN_PREFERENCE)
        private Supplier<Boolean> booleanSupplier;

        @Inject
        @ToolPreference(toolUid = DEFAULT_TOOL_UID, preferenceId = LONG_PREFERENCE)
        private Supplier<Long> longSupplier;

        private int booleanChangeCount;
        private boolean lastBooleanValue;
        private int longChangeCount;
        private long lastLongValue = -1;

        private void onBooleanPreferenceChange(@Observes @ToolPreferenceType(toolUid = DEFAULT_TOOL_UID, preferenceId = BOOLEAN_PREFERENCE) PreferenceEntryChange change)
        {
            booleanChangeCount += 1;
            lastBooleanValue = change.getBooleanValue();
        }

        private void onLongPreferenceChange(@Observes @ToolPreferenceType(toolUid = DEFAULT_TOOL_UID, preferenceId = LONG_PREFERENCE) PreferenceEntryChange change)
        {
            longChangeCount += 1;
            lastLongValue = change.getLongValue();
        }
    }
}