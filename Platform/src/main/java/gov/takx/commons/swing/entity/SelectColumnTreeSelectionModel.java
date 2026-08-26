package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.CascadingTreeSelectionModel;
import jakarta.annotation.Nonnull;

import javax.swing.tree.TreeModel;

/**
 * A Selection model to be used alongside {@link EntityTreeTable} to track selection paths.
 * <p>
 * This is intended to be used when selecting from a tree using checkboxes, and not as a drop-in replacement for the standard
 * tree selection model, which is populated by mouse clicks.
 *
 * @param <E> the type of entities that this model works with
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class SelectColumnTreeSelectionModel<E> extends CascadingTreeSelectionModel // TODO DCW TAKX-3542 allow returning entities directly
{
    /**
     * Construct a new instance using a tree model to determine full or partial selection state.
     *
     * @param model the tree model
     */
    public SelectColumnTreeSelectionModel(@Nonnull TreeModel model)
    {
        super(model);
    }
}
