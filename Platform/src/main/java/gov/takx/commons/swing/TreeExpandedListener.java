package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;

/**
 * This is a FunctionalInterface that allows users to easily handle a tree expansion event without having to extend base classes
 * or implement unnecessary methods. <br/>
 * To use it with a method reference, the method
 * must be cast to {@link TreeExpandedListener} so that the default methods are inherited.
 * <br/>
 * Usage:
 * <br/>
 * <pre>
 * {@code
 * treeComponent.addTreeExpansionListener((TreeExpandedListener)this::handleTreeExpanded);
 * ...
 * void handleTreeExpanded(TreeExpansionEvent e){...}
 * }
 * </pre>
 * Note: Because duplicate method references are not equal to one another, they cannot be used inline if you need to remove the listener
 * at later time. The method reference will need to be assigned to a variable first, which can then be used to remove the listener.
 * @since 5.3
 */
@FunctionalInterface
public interface TreeExpandedListener extends TreeExpansionListener
{
    @Override
    void treeExpanded(@Nonnull TreeExpansionEvent event);

    @Override
    default void treeCollapsed(@Nonnull TreeExpansionEvent event)
    {
    }
}
