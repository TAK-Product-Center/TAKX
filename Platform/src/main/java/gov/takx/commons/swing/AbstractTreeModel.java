package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.util.EventListener;

/**
 * A {@link TreeModel} version of the {@link javax.swing.table.AbstractTableModel}, since Swing doesn't include one.
 * Allows firing {@link TreeModel} events without having to extend {@link javax.swing.tree.DefaultTreeModel}.
 * <p>
 * Note that most methods and javadocs have been copied from {@link javax.swing.tree.DefaultTreeModel}.
 *
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
public abstract class AbstractTreeModel implements TreeModel
{
    protected final EventListenerList listenerList = new EventListenerList();

    /**
     * Adds a listener for the {@link TreeModelEvent} posted after the tree changes.
     *
     * @param l the listener to add
     * @see #removeTreeModelListener
     */
    public void addTreeModelListener(TreeModelListener l)
    {
        listenerList.add(TreeModelListener.class, l);
    }

    /**
     * Removes a listener previously added with {@link #addTreeModelListener(TreeModelListener)}
     *
     * @param l the listener to remove
     * @see #addTreeModelListener
     */
    public void removeTreeModelListener(TreeModelListener l)
    {
        listenerList.remove(TreeModelListener.class, l);
    }

    /**
     * Returns an array of all the tree model listeners registered on this model.
     *
     * @return all of this model's {@link TreeModelListener}s or an empty array if no tree model listeners are currently
     * registered
     * @see #addTreeModelListener
     * @see #removeTreeModelListener
     */
    public TreeModelListener[] getTreeModelListeners()
    {
        return listenerList.getListeners(TreeModelListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type. The event instance is
     * lazily created using the parameters passed into the fire method.
     *
     * @param source the source of the {@link TreeModelEvent}; typically {@code this}
     * @param path the path to the parent of the nodes that changed; use {@code null} to identify the root has changed
     * @param childIndices the indices of the changed elements
     * @param children the changed elements
     */
    protected void fireTreeNodesChanged(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == TreeModelListener.class)
            {
                // Lazily create the event:
                if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
                ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type. The event instance is
     * lazily created using the parameters passed into the fire method.
     *
     * @param source the source of the {@link TreeModelEvent}; typically {@code this}
     * @param path the path to the parent the nodes were added to
     * @param childIndices the indices of the new elements
     * @param children the new elements
     */
    protected void fireTreeNodesInserted(Object source, Object[] path,
                                         int[] childIndices,
                                         Object[] children)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == TreeModelListener.class)
            {
                // Lazily create the event:
                if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
                ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type. The event instance is
     * lazily created using the parameters passed into the fire method.
     *
     * @param source the source of the {@link TreeModelEvent}; typically {@code this}
     * @param path the path to the parent the nodes were removed from
     * @param childIndices the indices of the removed elements
     * @param children the removed elements
     */
    protected void fireTreeNodesRemoved(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == TreeModelListener.class)
            {
                // Lazily create the event:
                if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
                ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type. The event instance is
     * lazily created using the parameters passed into the fire method.
     *
     * @param source the source of the {@link TreeModelEvent}; typically {@code this}
     * @param path the path to the parent of the structure that has changed; use {@code null} to identify the root has
     * changed
     * @param childIndices the indices of the affected elements
     * @param children the affected elements
     */
    protected void fireTreeStructureChanged(Object source, Object[] path,
                                            int[] childIndices,
                                            Object[] children)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == TreeModelListener.class)
            {
                // Lazily create the event:
                if (e == null) e = new TreeModelEvent(source, path, childIndices, children);
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for notification on this event type. The event instance is
     * lazily created using the parameters passed into the fire method.
     *
     * @param source the source of the {@link TreeModelEvent}; typically {@code this}
     * @param path the path to the parent of the structure that has changed; use {@code null} to identify the root has
     * changed
     */
    protected void fireTreeStructureChanged(Object source, TreePath path)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == TreeModelListener.class)
            {
                // Lazily create the event:
                if (e == null) e = new TreeModelEvent(source, path);
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }

    /**
     * Returns an array of all the listeners currently registered. If no such listeners exist, this method returns an empty
     * array.
     *
     * @param <T> the listener type
     * @param listenerType the type of listeners requested
     * @return an array of all listeners registered of the specified type
     * @throws ClassCastException if listenerType doesn't specify a class or interface that implements java.util.{@link EventListener}
     * @see #getTreeModelListeners
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType)
    {
        return listenerList.getListeners(listenerType);
    }
}