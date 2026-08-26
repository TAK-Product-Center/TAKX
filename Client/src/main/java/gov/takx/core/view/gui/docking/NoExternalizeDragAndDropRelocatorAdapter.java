package gov.takx.core.view.gui.docking;

import bibliothek.gui.Dockable;
import bibliothek.gui.dock.DockElement;
import bibliothek.gui.dock.ScreenDockStation;
import bibliothek.gui.dock.control.relocator.DockRelocatorEvent;
import bibliothek.gui.dock.control.relocator.VetoableDockRelocatorAdapter;

/**
 * Dock relocator which prevents dockables from being dragged from the application to an external window and prevents
 * dockables that are external from being dragged into the application.  Instead, the user must press the externalize/
 * normalize buttons.
 *
 * @since 2.2
 */
class NoExternalizeDragAndDropRelocatorAdapter extends VetoableDockRelocatorAdapter
{
    @Override
    public void searched(DockRelocatorEvent event)
    {
        boolean isDockableExternal = isExternal(event.getDockable());
        boolean isTargetStationExternal = isExternal(event.getTarget());

        // If the dockable is not in the same external state as the target station (i.e. both external or both
        // internal), forbid the drag and drop action.
        if (isDockableExternal != isTargetStationExternal)
        {
            event.forbid();
        }
    }

    private boolean isExternal(DockElement element)
    {
        if (element == null)
        {
            return false;
        }

        if (element.asDockStation() instanceof ScreenDockStation)
        {
            return true;
        }

        Dockable dockable = element.asDockable();
        return dockable != null && isExternal(dockable.getDockParent());
    }
}
