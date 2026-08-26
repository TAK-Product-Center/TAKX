package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.CombiningTreeSelectionModel;
import gov.takx.commons.swing.TriStateJCheckBox;
import jakarta.annotation.Nonnull;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * A Selection model to be used alongside {@link EntityTreeTable} to track selection paths. It can be used in the normal way
 * by adding full tree paths via {@link #addSelectionPath(TreePath)}, or alternatively to track individual selections by
 * first setting a context node via {@link #setCurrentParentPath(TreePath)} and then individual node selections via
 * {@link #addSelection(Object)}.
 * <p>
 * This is intended to be used when selecting from a tree using checkboxes, and not as a drop-in replacement for the standard
 * tree selection model, which is populated by mouse clicks.
 *
 * @param <E> the type of entities that this model works with
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTreeTableSelectionModel<E> extends CombiningTreeSelectionModel
{
    private TreePath currentParentPath;

    /**
     * Construct a new instance using the root of the given tree model as the current parent path. See {@link #setCurrentParentPath(TreePath)}.
     * 
     * @param model the tree model
     */
    public EntityTreeTableSelectionModel(@Nonnull TreeModel model)
    {
        super(model);
        currentParentPath = new TreePath(model.getRoot());
    }

    /**
     * @param currentParentPath the path to use as the current parent
     */
    public void setCurrentParentPath(@Nonnull TreePath currentParentPath)
    {
        this.currentParentPath = currentParentPath;
    }

    /**
     * @param entity an object in this model
     * @return the current selection state of the object
     */
    @Nonnull
    public TriStateJCheckBox.State getSelectionState(@Nonnull E entity)
    {
        var state = getSelectionState(currentParentPath.pathByAddingChild(entity));
        return state == null ? TriStateJCheckBox.State.INDETERMINATE : state ? TriStateJCheckBox.State.SELECTED : TriStateJCheckBox.State.NOT_SELECTED;
    }

    /**
     * @param entity an object within this model to be marked as selected
     */
    public void addSelection(@Nonnull E entity)
    {
        addSelectionPath(currentParentPath.pathByAddingChild(entity));
    }

    /**
     * @param entity an object within this model to be marked as unselected
     */
    public void removeSelection(@Nonnull E entity)
    {
        removeSelectionPath(currentParentPath.pathByAddingChild(entity));
    }
}
