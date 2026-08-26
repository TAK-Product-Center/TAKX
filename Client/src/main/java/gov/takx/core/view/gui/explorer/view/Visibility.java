package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.explorer.IExplorable;

/**
 * The explorer has a unique way of determining visibility. Rather than depending solely on the visible state of the
 * {@link IExplorable}, it rolls-up the visible states based on the states of children. This result is that even if a tree
 * node does not itself support visibility, it may still report a visibility state and the user can take action on it.
 *
 * @since 5.7
 */
public enum Visibility
{
    /**
     * State when the node is visible or all children of the node have the visible state.
     */
    Visible,
    /**
     * State when children of the node are mixed.
     */
    Partial,
    /**
     * State when the node is invisible or all children of the node have the invisible state.
     */
    Invisible,
    /**
     * State when the node does not support visibility and has no children or all children do not support visibility.
     */
    Unsupported
}
