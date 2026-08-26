package gov.takx.core.mapobject;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.messages.IPluginState;
import gov.takx.api.persist.ICustomDataMonitorPersist;
import gov.takx.api.persist.IPluginStatePersist;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.CustomDataMonitor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class exists strictly as a performance optimization when loading the various state objects
 * (plugin state and custom data monitors) for Map Objects as an existing project is opened.
 * <p>
 * Instead of performing individual DB queries for each MO as it is restored, we do bulk queries of each table.
 * This can have a dramatic effect on the startup time when opening a project with thousands of Map Objects,
 * especially when the DB is remote (e.g., an RDS instance on AWS).
 *
 * @since 2.36
 */
@ApplicationScoped
public class MapObjectStateLoader
{
    @Inject
    private ICustomDataMonitorPersist customDataMonitorPersist;

    @Inject
    private IPluginStatePersist pluginStatePersist;

    private final Map<String, List<CustomDataMonitor>> customDataMonitorsByKey = new HashMap<>();
    private final Map<String, IPluginState> pluginStateByKey = new HashMap<>();

    /**
     * Get the list of custom data monitors for the given Map Object key.
     *
     * @param mapObjectKey Key of Map Object
     * @return List of data monitors, or {@code null} if none
     */
    public List<CustomDataMonitor> getCustomDataMonitors(String mapObjectKey)
    {
        return customDataMonitorsByKey.get(mapObjectKey);
    }

    /**
     * Get the plugin state for the given Map Object key.
     *
     * @param mapObjectKey Key of Map Object
     * @return plugin state, or {@code null} if none
     */
    public IPluginState getPluginState(String mapObjectKey)
    {
        return pluginStateByKey.get(mapObjectKey);
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_MapObjectStateLoader) StartProjectServices event)
    {
        customDataMonitorPersist.queryAll()
                .forEach(cdm -> customDataMonitorsByKey.computeIfAbsent(cdm.getKey(), k -> new ArrayList<>())
                        .add(cdm));

        pluginStatePersist.queryAll().forEach(state -> pluginStateByKey.put(state.getUniqueKey(), state));
    }

    /**
     * Once the project is fully opened, we can clear our data since it is no longer needed.
     */
    private void onProjectOpened(@Observes ProjectOpened event)
    {
        customDataMonitorsByKey.clear();
        pluginStateByKey.clear();
    }
}
