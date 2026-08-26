package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.ILazyLoadingExplorable;
import gov.takx.commons.swing.entity.EntityTreeModelEvent;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages which lazy explorables have been expanded.
 *
 * @apiNote There is currently no mechanism to "un-expand" lazy explorables. They get once chance to expand, and after that are
 * considered live.
 * @since 5.7
 */
public class LazyManager
{
    private final Set<ILazyLoadingExplorable> expandedLazyExplorables = new HashSet<>();
    private ExplorerTreeTableModel explorerTreeTableModel;

    /**
     * @param explorerTreeTableModel the explorer tree table model to use
     * @throws IllegalStateException if the model has already been set
     */
    public void init(@Nonnull ExplorerTreeTableModel explorerTreeTableModel)
    {
        if (this.explorerTreeTableModel != null) throw new IllegalStateException("Init has already been called");
        this.explorerTreeTableModel = explorerTreeTableModel;
        explorerTreeTableModel.addEntityTreeModelListener(this::modelChanged);
    }

    /**
     * @param explorable the explorable to check
     * @return {@code true} if the explorable has not been expanded by this manager, otherwise {@code false}
     */
    public boolean isLazyAndUnexpanded(@Nonnull IExplorable explorable)
    {
        return explorable instanceof ILazyLoadingExplorable && !expandedLazyExplorables.contains(explorable);
    }

    /**
     * @param explorable the explorable to expand, if not already expanded
     */
    public void expand(@Nonnull IExplorable explorable)
    {
        if (explorable instanceof ILazyLoadingExplorable lazy && !expandedLazyExplorables.contains(lazy))
        {
            expandedLazyExplorables.add(lazy);
            lazy.onExpand();
        }
    }

    private void modelChanged(EntityTreeModelEvent<IExplorable> event)
    {
        if (event.type() == EntityTreeModelEvent.Type.Remove && event.entity() instanceof ILazyLoadingExplorable)
        {
            expandedLazyExplorables.remove(event.entity());
        }
    }
}
