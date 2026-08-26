package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.intern.DefaultCommonDockable;

/**
 * Simple extension class so we can more easily determine when a docked component is playback aware.
 *
 * @see PlaybackBorderModifierBridge
 * @since 2.25
 */
public class PlaybackAwareDockable extends DefaultCommonDockable
{
    /**
     * Creates a new dockable
     *
     * @param dockable the model of this element
     * @param sources  action source which shows the close action
     */
    public PlaybackAwareDockable(CDockable dockable, DockActionSource... sources)
    {
        super(dockable, sources);
    }
}
