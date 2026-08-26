package gov.takx.core.globe;

import bibliothek.gui.dock.common.MultipleCDockableFactory;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.core.view.gui.docking.DockingManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Factory responsible for managing {@link GlobeDockable} and {@link GlobeLayout} instances.
 */
@Singleton
public class GlobeDockableFactory implements MultipleCDockableFactory<GlobeDockable, GlobeLayout>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IExtendedGlobeManager globeManager;
    private final DockingManager dockingManager;

    @Inject
    GlobeDockableFactory(IExtendedGlobeManager globeManager, DockingManager dockingManager)
    {
        this.globeManager = globeManager;
        this.dockingManager = dockingManager;
    }

    /**
     * An empty layout is required to read a layout from an XML file or from a byte stream
     */
    @Override
    public GlobeLayout create()
    {
        return new GlobeLayout();
    }

    public GlobeLayout create(int globeId)
    {
        return new GlobeLayout(globeId);
    }

    /**
     * Called when storing the current layout
     */
    @Override
    public GlobeLayout write(GlobeDockable globeDockable)
    {
        return new GlobeLayout(globeDockable.getGlobe().getGlobeId());
    }

    /**
     * Called when applying a stored layout
     */
    @Override
    public GlobeDockable read(GlobeLayout globeLayout)
    {
        final IApplicationGlobe primaryGlobe = globeManager.getPrimaryGlobeDelegate();
        final GlobeDockable dockable = new GlobeDockable(this);

        primaryGlobe.doWhenReady(() -> {
            // Then get and install the globe component into the dockable
            final IApplicationGlobe globe = globeManager.getOrCreateGlobe(globeLayout.getGlobeId());
            dockable.addGlobe(globe);
            dockingManager.addGlobeDockable(dockable);
            globe.goTo(globeLayout.getFavorite());
        });

        logger.debug("GlobeDockableFactory.read: {} -> {}", globeLayout, dockable);

        return dockable;
    }

    @Override
    public boolean match(GlobeDockable globeDockable, GlobeLayout globeLayout)
    {
        return globeDockable.getGlobe().getGlobeId() == globeLayout.getGlobeId();
    }
}
