package gov.takx.core.globe.tak;

import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLMapView;
import gov.tak.api.engine.map.MapRenderer;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

/**
 * Service that allows managed access to the {@link GLLabelManager}. Labels can be added to {@link #createLabel(BiConsumer) all globes} or
 * a {@link #createLabel(int, BiConsumer)  single globe}. The service handles created/deleted globe events.
 * 
 * @since 5.5.1
 */
@ApplicationScoped
public class TakLabelManager implements IGlobeLifeCycleListener
{
    private final ApplicationGlobeManager globeManager;
    private final boolean headless;
    private final List<LabelRegistration> registrations = new CopyOnWriteArrayList<>();
    private final Set<MapRenderer<?>> loadedGlobes = new CopyOnWriteArraySet<>();

    @Inject
    TakLabelManager(ApplicationGlobeManager globeManager, @Headless boolean headless)
    {
        this.globeManager = globeManager;
        this.headless = headless;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices ignored)
    {
        if (headless) return;

        globeManager.addGlobeLifeCycleListener(this);
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices ignored)
    {
        if (headless) return;

        globeManager.removeGlobeLifeCycleListener(this);
        registrations.clear();
        loadedGlobes.clear();
    }

    @Override
    public void globeCreated(IGlobeDelegate globe)
    {
        registrations.stream()
                .filter(r -> r.forAllGlobes)
                .forEach(r -> getGlobeLabelManager(globe, (mapRenderer, labelManager) -> addLabelToRegistration(mapRenderer, labelManager, r)));
        loadedGlobes.add(globe.getPortableMapRenderer());
    }

    @Override
    public void globeRemoved(IGlobeDelegate globe)
    {
        MapRenderer<?> mapRenderer = globe.getPortableMapRenderer();
        registrations.forEach(r -> r.labelIdPerGlobe.remove(mapRenderer));
        loadedGlobes.remove(mapRenderer);
    }

    /**
     * Create a new label. This label will be added to all globes. The provided {@code updateConsumer} will be called upon to style the label when needed.
     * @param updateConsumer a function to style the label
     * @return the label registration object
     */
    @Nonnull
    public LabelRegistration createLabel(BiConsumer<GLLabelManager, Integer> updateConsumer)
    {
        LabelRegistration registration = new LabelRegistration(new HashMap<>(), updateConsumer, true);
        forEachLabelManager((mapRenderer, labelManager) -> {
            // Only add to label manager if globe is loaded, otherwise defer to {@link #globeCreated} since {@code forAllGlobes} is true
            if (loadedGlobes.contains(mapRenderer)) addLabelToRegistration(mapRenderer, labelManager, registration);
        });
        registrations.add(registration);
        return registration;
    }

    /**
     * Create a new label. This label will only be added the specified globe. The provided {@code updateConsumer} will be called upon to style the label when needed.
     * @param globeId the globe id to add the label to
     * @param updateConsumer a function to style the label
     * @return the label registration object
     */
    @Nonnull
    public LabelRegistration createLabel(int globeId, BiConsumer<GLLabelManager, Integer> updateConsumer)
    {
        LabelRegistration registration = new LabelRegistration(new HashMap<>(), updateConsumer, false);
        var applicationGlobe = globeManager.getGlobeDelegate(globeId);
        if (applicationGlobe != null)
        {
            getGlobeLabelManager(applicationGlobe, (mapRenderer, labelManager) -> addLabelToRegistration(mapRenderer, labelManager, registration));
        }
        registrations.add(registration);
        return registration;
    }

    private void addLabelToRegistration(MapRenderer<?> mapRenderer, GLLabelManager labelManager, LabelRegistration registration)
    {
        int labelId = labelManager.addLabel();
        registration.updateConsumer.accept(labelManager, labelId);
        registration.labelIdPerGlobe.put(mapRenderer, labelId);
    }

    /**
     * Delete a label.
     * @param registration the label registration object for the label to delete
     */
    public void deleteLabel(LabelRegistration registration)
    {
        forEachLabelManager(((mapRenderer, labelManager) -> {
            Integer labelId = registration.labelIdPerGlobe.remove(mapRenderer);
            if (labelId != null) labelManager.removeLabel(labelId);
        }));
        registrations.remove(registration);
    }

    /**
     * Update the styling of a label. The label will use the {@code updateConsumer} function provided when the label was created.
     * @param registration the label registration object for the label to be updated
     */
    public void updateLabel(LabelRegistration registration)
    {
        updateLabel(registration, registration.updateConsumer);
    }

    /**
     * Update the styling of a label. The label will use the provided {@code updateConsumer} function provided for this method call only.
     * @param registration the label registration object for the label to be updated
     */
    public void updateLabel(LabelRegistration registration, BiConsumer<GLLabelManager, Integer> updateConsumer)
    {
        forEachLabelManager((mapRenderer, labelManager) -> {
            Integer labelId = registration.labelIdPerGlobe.get(mapRenderer);
            if (labelId != null) updateConsumer.accept(labelManager, labelId);
        });
    }

    private void forEachLabelManager(BiConsumer<MapRenderer<?>, GLLabelManager> labelManagerConsumer)
    {
        globeManager.getAllGlobeDelegates().forEach(g -> getGlobeLabelManager(g, labelManagerConsumer));
    }

    private void getGlobeLabelManager(IGlobeDelegate applicationGlobe, BiConsumer<MapRenderer<?>, GLLabelManager> labelManagerConsumer)
    {
        MapRenderer<?> mapRenderer = applicationGlobe.getPortableMapRenderer();
        GLMapView glMapView = LegacyAdapters.adapt(mapRenderer);
        GLLabelManager labelManager = glMapView.getLabelManager();
        labelManagerConsumer.accept(mapRenderer, labelManager);
    }

    /**
     * A label registration object.
     * @param labelIdPerGlobe a map of label ids for each globe
     * @param updateConsumer a function to style the label
     * @param forAllGlobes {@code true} if this label should be shown on all globes, {@code false} if only for the globes in {@code labelIdPerGlobe}
     */
    public record LabelRegistration(@Nonnull HashMap<MapRenderer<?>, Integer> labelIdPerGlobe, BiConsumer<GLLabelManager, Integer> updateConsumer, boolean forAllGlobes)
    {
    }
}
