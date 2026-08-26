package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.AToolbarComponent;
import jakarta.annotation.Nonnull;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree node that contains a toolbar component.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarComponentTreeNode extends DefaultMutableTreeNode
{
    public final AToolbarComponent component;

    public ToolbarComponentTreeNode(@Nonnull AToolbarComponent component)
    {
        super(component);
        this.component = component;
    }
}
