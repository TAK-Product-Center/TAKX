package gov.takx.core.view.gui;

import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.ui.IPreferenceSpaceManager;
import gov.takx.api.ui.IProgramPreferencesPanel;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of the Preference Space Manager interface that allows plugin developers to
 * contribute program preference panels to the user preferences dialog (maybe more later) rather than having to
 * use some home-grown preferences UI in a non-intuitive location.
 *
 * @since 1.49
 */
@ApplicationScoped
public class PreferenceSpaceManager implements IPreferenceSpaceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<String, Supplier<IProgramPreferencesPanel>> panelMap = new HashMap<>();

    @Override
    public void addUserPreferencePanel(@Nonnull String name, @Nonnull Supplier<IProgramPreferencesPanel> panelSupplier)
    {
        panelMap.put(name, panelSupplier);
    }

    @Override
    public void removeUserPreferencePanel(@Nonnull String name)
    {
        if (panelMap.remove(name) != null)
        {
            logger.warn("No user preferences panel named {} was registered.", name);
        }
    }

    /**
     * Get the map of custom panels contributed by plugins.  A new instance of each registered panel is created.
     *
     * @return the custom panel map
     */
    public Map<String, IProgramPreferencesPanel> getCustomPanels()
    {
        final Map<String, IProgramPreferencesPanel> instanceMap = new HashMap<>(panelMap.size());
        panelMap.forEach((name, producer) -> instanceMap.put(name, producer.get()));
        return instanceMap;
    }

    /**
     * @return {@code true} if there are any custom panels registered
     * @since 5.4
     */
    public boolean hasCustomPanels()
    {
        return !panelMap.isEmpty();
    }

    /**
     * On project close, remove all the custom panels.
     */
    private void handleProjectClosing(@Observes ProjectClosing event)
    {
        panelMap.clear();
    }
}
