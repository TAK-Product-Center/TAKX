package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.common.MultipleCDockableFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Factory responsible for managing {@link ComponentDockable} and {@link DockableLayout} instances.
 */
@Singleton
public class ComponentDockableFactory implements MultipleCDockableFactory<ComponentDockable, DockableLayout>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private DockingManager dockingManager;

    /**
     * An empty layout is required to read a layout from an XML file or from a byte stream
     */
    @Override
    public DockableLayout create()
    {
        return new DockableLayout();
    }

    /**
     * Called when storing the current layout
     */
    @Override
    public DockableLayout write(ComponentDockable dockable)
    {
        DockableLayout dockableLayout;
        DockableAndProps dap = dockingManager.findDockableAndProps(dockable);
        if (dap != null)
        {
            dockableLayout = new DockableLayout(dap.getId());
        } else
        {
            dap = dockingManager.findDockableAndPropsByName(dockable.getName());

            logger.error("Cannot find dockable: {}{}", dockable, dap != null ? ". But found " + dap + " by name." : "");

            dockableLayout = new DockableLayout();
        }
        return dockableLayout;
    }

    /**
     * Called when applying a stored layout
     */
    @Nullable
    @Override
    public ComponentDockable read(@Nonnull DockableLayout dockableLayout)
    {
        DockableAndProps dockableAndProps = dockingManager.findDockableAndPropsById(dockableLayout.getDockableId());

        if (dockableAndProps != null && !dockableAndProps.hasDockable())
        {
            dockableAndProps = dockingManager.createDockable(dockableAndProps.getComponent(), dockableAndProps.getDescriptor());
        }

        final ComponentDockable dockable = dockableAndProps != null ? dockableAndProps.getDockable() : null;

        logger.debug("read: {} -> {}", dockableLayout, dockable);

        return dockable;
    }

    @Override
    public boolean match(ComponentDockable dockable, @Nonnull DockableLayout thatLayout)
    {
        DockableAndProps dap = dockingManager.findDockableAndProps(dockable);
        if (dap == null)
        {
            logger.warn("Unknown dockable: {}", dockable);
            return false;
        }

        return dap.getId().equals(thatLayout.getDockableId());
    }
}
