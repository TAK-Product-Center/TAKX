package gov.takx.core.preferences;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.ToolPreference;
import gov.takx.api.annotation.ToolPreferenceType;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IToolPreferenceManager;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.api.preferences.tool.IToolPreference;
import gov.takx.core.preferences.tool.ToolPreferenceAdded;
import gov.takx.core.preferences.tool.ToolPreferenceRemoved;
import gov.takx.core.view.gui.preferences.tools.ToolPreferenceEntry;
import gov.takx.platform.utils.InjectionUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Manager that allows plugins to add custom preferences to the "Tools" preferences panel.
 * <p>
 * Note that all preference value producers here produce {@link Supplier} instead of the actual type, as documented
 * in {@link IToolPreferenceManager}.
 *
 * @since 5.0
 */
@InjectableService(registerAs = IToolPreferenceManager.class)
@ApplicationScoped
@Typed({ToolPreferenceManager.class, IToolPreferenceManager.class})
public class ToolPreferenceManager extends APreferenceManager implements IToolPreferenceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NODE_KEY = "tool";
    private static final String DEFAULT_PREFERENCES_XML = "conf/DefaultToolPreferences.xml";

    // Map from preference entry to event qualifier
    private static final Map<IPreferenceEntry, ToolPreferenceType> ENTRY_TO_QUALIFIER = new ConcurrentHashMap<>();

    private final Event<ToolPreferenceAdded> toolPreferenceAddedEvent;
    private final Event<ToolPreferenceRemoved> toolPreferenceRemovedEvent;

    private final Map<String, ToolDefinition> toolDefinitionsMap = new ConcurrentHashMap<>();

    /**
     * @param toolUid The tool UID. This id <em>must</em> be unique to the tool. All the tool's preferences will be
     *                added to a preference node with this name. The {@link IToolPreference}s returned by
     *                {@link IPreferenceEntryChangeListener}s keys will have the prefix "[toolUid]/"
     * @param title The title that will be displayed in the tool list and the tool preferences panel
     * @param description The description that will be displayed in the tool list
     * @param icon The tool icon that will be displayed in the tool list and the tool preferences panel
     * @param preferenceEntries The tool preference entries to add to the tool preferences panel
     *
     * @since 5.4
     */
    public record ToolDefinition(@Nonnull String toolUid, @Nonnull String title, @Nonnull String description,
            @Nonnull ImageIcon icon, @Nonnull List<ToolPreferenceEntry> preferenceEntries)
    {
        /**
         * @return the preference entry for the given {@code preferenceId}
         */
        @Nullable
        public ToolPreferenceEntry getPreferenceEntry(@Nonnull String preferenceId)
        {
            return preferenceEntries.stream()
                    .filter(entry -> preferenceId.equals(entry.getPreferenceId()))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Inject
    ToolPreferenceManager(Event<ToolPreferenceAdded> toolPreferenceAddedEvent, Event<ToolPreferenceRemoved> toolPreferenceRemovedEvent)
    {
        super(NODE_KEY, DEFAULT_PREFERENCES_XML);

        this.toolPreferenceAddedEvent = toolPreferenceAddedEvent;
        this.toolPreferenceRemovedEvent = toolPreferenceRemovedEvent;
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices ignore)
    {
        toolDefinitionsMap.clear();
    }

    @Override
    protected File getPreferencesFile()
    {
        return programEnvironment.getInstanceToolPreferencesFile();
    }

    @Override
    public void addTool(@Nonnull String toolUid, @Nonnull String toolName, @Nonnull String description,
                        @Nonnull ImageIcon icon, @Nonnull List<IToolPreference> preferences)
    {

        addTool(toolUid, toolName, description, icon, preferences, null);
    }

    @Override
    public void addTool(@Nonnull String toolUid, @Nonnull String toolName, @Nonnull String description,
                        @Nonnull ImageIcon icon, @Nonnull List<IToolPreference> preferences,
                        @Nullable IPreferenceEntryChangeListener listener)
            throws IllegalArgumentException
    {
        if (toolDefinitionsMap.containsKey(toolUid))
        {
            logger.error("Attempting to add preferences for a tool that already exists. Tool UID: {} Tool name: {}",
                    toolUid, toolName);

            throw new IllegalArgumentException("A tool with the UID " + toolUid + " already exists");
        }

        if (toolUid.length() > Preferences.MAX_KEY_LENGTH)
        {
            throw new IllegalArgumentException("Tool UID [" + toolUid + "] is too long; must be less than " + Preferences.MAX_KEY_LENGTH);
        }

        // Create a ToolPreferenceEntry for each preference.
        final var preferenceEntries = preferences.stream()
                .map(preference -> new ToolPreferenceEntry(toolUid, preference, listener))
                .toList();

        final var toolDefinition = new ToolDefinition(toolUid, toolName, description, icon, preferenceEntries);
        toolDefinitionsMap.put(toolUid, toolDefinition);

        toolPreferenceAddedEvent.fire(new ToolPreferenceAdded(toolDefinition));
    }

    @Override
    public void removeTool(@Nonnull String toolUid)
    {
        final var toolDefinition = toolDefinitionsMap.remove(toolUid);

        if (toolDefinition == null)
        {
            logger.error("Attempting to remove preferences for a tool that does not exist. Tool UID: {}", toolUid);
            return;
        }

        // Remove all preference entry listeners
        toolDefinition.preferenceEntries.forEach(
                preferenceEntry -> preferenceEntry.getChangeListeners()
                        .forEach(preferenceEntry::removePreferenceEntryChangeListener));

        toolPreferenceRemovedEvent.fire(new ToolPreferenceRemoved(toolDefinition));
    }

    @Override
    public void deleteTool(@Nonnull String toolUid)
    {
        if (toolDefinitionsMap.containsKey(toolUid)) removeTool(toolUid);

        try
        {
            final Preferences toolPreferences = getPreferences();
            final String rootPath = toolPreferences.absolutePath(); // rx/tools

            if (rootPath.equals(toolUid)) throw new IllegalArgumentException("The root node cannot be removed");

            Preferences node = toolPreferences.node(toolUid);
            Preferences parentNode = toolPreferences;

            node.removeNode();

            // remove empty parents up to the root
            while (!parentNode.absolutePath().equals(rootPath) && parentNode.childrenNames().length == 0)
            {
                node = parentNode;
                parentNode = node.parent();
                node.removeNode();
            }

            savePreferences();
        } catch (BackingStoreException e)
        {
            logger.error("Unable to delete preference node for {}", toolUid, e);
        }
    }

    @Nullable
    @Override
    public List<IPreferenceEntry> getToolPreferenceEntries(@Nonnull String toolUid)
    {
        final var toolDefinition = toolDefinitionsMap.get(toolUid);
        return toolDefinition != null ? Collections.unmodifiableList(toolDefinition.preferenceEntries) : null;
    }

    @Nullable
    @Override
    public IPreferenceEntry getToolPreferenceEntry(@Nonnull String toolUid, @Nonnull String preferenceId)
    {
        final var toolDefinition = toolDefinitionsMap.get(toolUid);
        return toolDefinition != null ? toolDefinition.getPreferenceEntry(preferenceId) : null;
    }

    /**
     * @return The list of tool definitions that have been added
     */
    public List<ToolDefinition> getToolDefinitions()
    {
        return List.copyOf(toolDefinitionsMap.values());
    }

    /** @since 5.5.1 */
    @Produces
    @Dependent
    @ToolPreference
    public Supplier<IPreferenceEntry> producePreferenceEntry(InjectionPoint injectionPoint)
    {
        return () -> getPreferenceEntryFromInjection(injectionPoint);
    }

    /** @since 5.5.1 */
    @Produces
    @Dependent
    @ToolPreference
    public Supplier<String> produceStringValue(InjectionPoint injectionPoint)
    {
        return () -> getValueForAnnotation(injectionPoint);
    }

    /** @since 5.5.1 */
    @Produces
    @Dependent
    @ToolPreference
    public Supplier<Integer> produceIntegerValue(InjectionPoint injectionPoint)
    {
        return () -> getIntegerValue(injectionPoint);
    }

    /** @since 5.5.1 */
    @Produces
    @Dependent
    @ToolPreference
    public Supplier<Long> produceLongValue(InjectionPoint injectionPoint)
    {
        return () -> getLongValue(injectionPoint);
    }

    /** @since 5.5.1 */
    @Produces
    @Dependent
    @ToolPreference
    public Supplier<Boolean> produceBooleanValue(InjectionPoint injectionPoint)
    {
        return () -> getBooleanValue(injectionPoint);
    }

    /** @since 5.5.1 */
    @Produces
    @Dependent
    @ToolPreference
    public Supplier<Float> produceFloatValue(InjectionPoint injectionPoint)
    {
        return () -> getFloatValue(injectionPoint);
    }

    /** @since 5.5.1 */
    @Produces
    @Dependent
    @ToolPreference
    public Supplier<Double> produceDoubleValue(InjectionPoint injectionPoint)
    {
        return () -> getDoubleValue(injectionPoint);
    }

    @Override
    protected IPreferenceEntry getPreferenceEntryFromInjection(InjectionPoint injectionPoint)
    {
        final var toolPreference = InjectionUtils.getAnnotation(injectionPoint, ToolPreference.class)
                .orElseThrow(() -> new IllegalStateException("No @ToolPreference on: " + injectionPoint));

        return getToolPreferenceEntry(toolPreference.toolUid(), toolPreference.preferenceId());
    }

    /**
     * @return The qualifier to use for the given change event
     * @since 5.5.1
     */
    @Override
    protected Annotation[] getPreferenceEntryChangeQualifier(PreferenceEntryChange change)
    {
        return new Annotation[]{
                ENTRY_TO_QUALIFIER.computeIfAbsent(change.getPreferenceEntry(), preferenceEntry -> {
                    if (preferenceEntry instanceof ToolPreferenceEntry toolPreferenceEntry)
                    {
                        return new ToolPreferenceType.Literal(toolPreferenceEntry.getToolUid(), toolPreferenceEntry.getPreferenceId());
                    }
                    logger.error("Don't know how to handle change type for: {}", preferenceEntry.getClass());
                    return null;
                })
        };
    }
}