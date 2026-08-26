package gov.takx.core.view.gui.docking;

import bibliothek.gui.Dockable;
import bibliothek.gui.dock.event.DockHierarchyEvent;
import bibliothek.gui.dock.event.DockHierarchyListener;
import bibliothek.gui.dock.title.DockTitle;
import bibliothek.gui.dock.title.OrientationToRotationStrategy;
import bibliothek.gui.dock.title.OrientationToRotationStrategyListener;
import bibliothek.gui.dock.util.swing.Rotation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rotation strategy for our dockable titles.
 *
 * @since 2.25
 */
public class RotationStrategy implements OrientationToRotationStrategy, DockHierarchyListener
{
    private final Set<DockTitle> titles = new HashSet<>();
    private final List<OrientationToRotationStrategyListener> listeners = new ArrayList<>();

    @Override
    public Rotation convert(DockTitle.Orientation orientation, DockTitle title)
    {
        return switch (orientation)
        {
            case EAST_SIDED -> Rotation.DEGREE_90;
            case WEST_SIDED -> Rotation.DEGREE_270;
            default -> Rotation.DEGREE_0;
        };
    }

    @Override
    public void install(DockTitle title)
    {
        final Dockable dockable = title.getDockable();

        if (unmonitored(dockable))
        {
            dockable.addDockHierarchyListener(this);
        }
        titles.add(title);
    }

    @Override
    public void uninstall(DockTitle title)
    {
        titles.remove(title);
        final Dockable dockable = title.getDockable();
        if (unmonitored(dockable))
        {
            dockable.removeDockHierarchyListener(this);
        }
    }

    @Override
    public void controllerChanged(DockHierarchyEvent event)
    {
        // ignore
    }

    @Override
    public void hierarchyChanged(DockHierarchyEvent event)
    {
        for (OrientationToRotationStrategyListener listener : listeners)
        {
            listener.rotationChanged(event.getDockable(), null);
        }
    }

    private boolean unmonitored(Dockable dockable)
    {
        for (DockTitle title : titles)
        {
            if (title.getDockable() == dockable)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addListener(OrientationToRotationStrategyListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(OrientationToRotationStrategyListener listener)
    {
        listeners.remove(listener);
    }
}
