package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.explorer.IExplorable;
import gov.takx.commons.swing.entity.EntityTreeTableModel;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;

/**
 * This TreeTableModel is necessary to apply some Explorer specific logic to the base model class. Specifically:
 * <ul>
 *     <li>Adding children of root on initialization</li>
 *     <li>Catching and ignoring duplicate children</li>
 *     <li>Rolling up visibility changes</li>
 * </ul>
 *
 * @since 5.7
 */
public class ExplorerTreeTableModel extends EntityTreeTableModel<IExplorable>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final VisibilityManager visibilityManager;

    /**
     * Construct an empty model to hold explorables for the explorer tree.
     *
     * @param root the root explorable of the model
     * @param entityDefinition the entity definition
     * @param visibilityManager the visibility manager for the explorer panel
     */
    public ExplorerTreeTableModel(@Nonnull IExplorable root, @Nonnull ExplorableEntityDefinition entityDefinition, @Nonnull VisibilityManager visibilityManager)
    {
        super(entityDefinition, entityDefinition.getHierarchicalColumn(), IExplorable.CHILD);
        this.visibilityManager = visibilityManager;

        // Configure columns for this model
        entityColumnData.removeAllColumns();
        addColumn(getHierarchicalEntityColumn());
        addColumn(entityDefinition.getVisibilityRollUpColumn());

        setRoot(root);
        addChildren(root);
    }

    @Override
    @InvokeOnEDT
    public void removeEntity(@Nonnull IExplorable parent, @Nonnull IExplorable entity)
    {
        super.removeEntity(parent, entity);
        // Because we know that we show descendant counts, we always fire updates for the entire hierarchy when something is removed
        modelSupport.fireEntityHierarchyChanged(parent);
    }

    @Override
    protected void addListeners(@Nonnull IExplorable entity)
    {
        super.addListeners(entity);
        // Handling the vis listener has to happen here because the filtered entity models do not currently manage their own listeners
        var listeners = getListenerMap(entity);
        var visListener = (PropertyChangeListener) event -> visibilityManager.updateAndRollUpVisibility((IExplorable) event.getSource());
        ((ExplorableEntityDefinition) getEntityDefinition()).addPropertyChangeListener(entity, IExplorable.VISIBLE, visListener);
        listeners.put(IExplorable.VISIBLE, visListener);
    }

    @Override
    @InvokeOnEDT
    protected void addEntityInternal(@Nonnull IExplorable parent, @Nonnull IExplorable entity, int index)
    {
        // TODO DCW TAKX-4226 duplicate items are being added to the explorer somehow
        if (!getChildrenInternal(parent).contains(entity))
        {
            super.addEntityInternal(parent, entity, index);
            // When objects are added their children also need to be since we have already missed any PCEs for them
            addChildren(entity);
            // Because we know that we show descendant counts, we always fire updates for the entire hierarchy any time something is added
            modelSupport.fireEntityHierarchyChanged(parent);
        } else
        {
            logger.warn("Attempting to add duplicate child to parent: {} child: {}", parent, entity);
        }
    }

    private void addChildren(IExplorable explorable)
    {
        for (var child : explorable.getChildren())
        {
            addEntity(explorable, child);
        }
    }
}
