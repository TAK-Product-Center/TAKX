package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXTaskPane;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.LayoutManager;

/**
 * A collapsible panel that mimics {@link JXTaskPane} but solves the issues of using that component directly, specifically panels
 * that need to expand vertically and scroll.
 *
 * @since 5.8 (Moved from client in 5.8)
 */
@IncubatingApi(since = "5.8")
public class CollapsiblePanel extends JPanel
{
    private final TaskPaneButton collapseButton;

    /**
     * Create a new instance with an empty title, using the normal color scheme.
     */
    public CollapsiblePanel()
    {
        this(null, false);
    }

    /**
     * Construct a new instance with the given title and optionally using the "special" color scheme (See
     * {@link JXTaskPane#setSpecial(boolean)}).
     *
     * @param title the title of the panel
     * @param special {@code true} to use the special color scheme, or {@code false} for the normal color scheme
     */
    public CollapsiblePanel(@Nullable String title, boolean special)
    {
        this(title, special, null);
    }

    /**
     * Construct a new instance with the given title, contents, and optionally using the "special" color scheme (See
     * {@link JXTaskPane#setSpecial(boolean)}).
     *
     * @param title the title of the panel
     * @param special {@code true} to use the special color scheme, otherwise {@code false}
     * @param contentView the contents component
     */
    public CollapsiblePanel(@Nullable String title, boolean special, @Nullable JComponent contentView)
    {
        this(title, special, contentView, false);
    }

    /**
     * Construct a new instance with the given title, contents, initial collapsed state, and optionally using the "special" color scheme (See
     * {@link JXTaskPane#setSpecial(boolean)}).
     *
     * @param title the title of the panel
     * @param special {@code true} to use the special color scheme, otherwise {@code false}
     * @param contentView the contents component
     * @param collapsed {@code true} to collapse the panel, otherwise {@code false}
     */
    public CollapsiblePanel(@Nullable String title, boolean special, @Nullable JComponent contentView, boolean collapsed)
    {
        initComponents();
        collapseButton = new TaskPaneButton(title);
        collapseButton.setSpecial(special);
        collapseButton.setCollapsed(collapsed);
        //Must call super method here
        super.addImpl(collapseButton, new CellConstraints(1, 1), -1);
        updateCollapsed();
        setContentView(contentView);
    }

    /**
     * Sets the layout manager for the content pane. NOTE: checking {@link #collapseButton} here is unavoidable because
     * {@link JPanel} calls these overridable methods from its constructor
     *
     * @param mgr the specified layout manager
     */
    @Override
    public void setLayout(LayoutManager mgr)
    {
        if (collapseButton == null)
        {
            super.setLayout(mgr);
        } else
        {
            contentPane.setLayout(mgr);
        }
    }

    /**
     * Adds the specified component to the contentPane at the specified index. NOTE: checking {@link #collapseButton} here is
     * unavoidable because {@link JPanel} calls these overridable methods from its constructor
     *
     * @param comp the component to be added
     * @param constraints an object expressing layout constraints for this component
     * @param index the position in the container's list at which to insert the component, where {@code -1} means append to
     * the end
     */
    @Override
    protected void addImpl(Component comp, Object constraints, int index)
    {
        if (collapseButton == null)
        {
            super.addImpl(comp, constraints, index);
        } else
        {
            contentPane.add(comp, constraints, index);
        }
    }

    /**
     * @param component the component to use as the contents of the panel, or {@code null} for no contents.
     */
    public void setContentView(@Nullable JComponent component)
    {
        contentPane.removeAll();
        if (component != null) contentPane.add(component, new CellConstraints(1, 1));
        collapseButton.setToolTipText((collapseButton.isCollapsed() ? "Show " : "Hide ") + collapseButton.getTitle());
        revalidate();
    }

    /**
     * @return the component used as the contents
     */
    @Nullable
    public JComponent getContentView()
    {
        return contentPane.getComponentCount() > 0 ? (JComponent) contentPane.getComponent(0) : null;
    }

    /**
     * @return the collapse task pane button
     */
    @Nonnull
    public JXTaskPane getCollapseButton()
    {
        return collapseButton;
    }

    /**
     * @param icon the collapse button icon
     */
    public void setIcon(@Nullable Icon icon)
    {
        collapseButton.setIcon(icon);
    }

    /**
     * @return the collapse button icon
     */
    @Nullable
    public Icon getIcon()
    {
        return collapseButton.getIcon();
    }

    /**
     * @param collapsed {@code true} to collapse the panel, otherwise {@code false}
     */
    public void setCollapsed(boolean collapsed)
    {
        collapseButton.setCollapsed(collapsed);
    }

    /**
     * @return {@code true} is the panel is collapsed or {@code false} if it is visible
     */
    public boolean isCollapsed()
    {
        return collapseButton.isCollapsed();
    }

    /**
     * Set the Panels title.
     *
     * @param title the title
     */
    public void setTitle(@Nullable String title)
    {
        collapseButton.setTitle(title);
    }

    /**
     * @return the title
     */
    @Nullable
    public String getTitle()
    {
        return collapseButton.getTitle();
    }

    /**
     * @return {@code true} if this uses the "special" color scheme, otherwise {@code false}
     */
    public boolean isSpecial()
    {
        return collapseButton.isSpecial();
    }

    /**
     * @param special {@code true} if this should use the "special" color scheme, otherwise {@code false}
     */
    public void setSpecial(boolean special)
    {
        collapseButton.setSpecial(special);
    }

    protected void updateCollapsed()
    {
        contentPane.setVisible(!collapseButton.isCollapsed());
        revalidate();
    }

    private class TaskPaneButton extends JXTaskPane
    {
        public TaskPaneButton(String title)
        {
            setTitle(title);
        }

        @Override
        public void setCollapsed(boolean collapsed)
        {
            super.setCollapsed(collapsed);
            updateCollapsed();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        contentPane = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelCollapsible");
        setLayout(new FormLayout(
                "default:grow",
                "default, default:grow"));

        //======== contentPane ========
        {
            contentPane.setName("contentPane");
            contentPane.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));
        }
        add(contentPane, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contentPane;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
