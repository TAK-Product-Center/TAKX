package gov.takx.core.view.gui.layers.viewmanager;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jdesktop.swingx.JXTaskPane;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.MouseListener;

// TODO KMB: We really shouldn't need this class since it's basically equivalent to JXTaskPane. I was able use JXTaskPane
//  directly in PointDropperPanel, but every time I try to use it directly on the explorer or layers panel, I run into
//  issues:
//   - In LayerViewPanel, the layout is wrong and the panel doesn't expand to fill the width
//   - In ExplorerPanel, everything looks fine, but the scrollbars don't appear when the trees are too big
/**
 * A CollapsiblePanel panel similar to {@link JXTaskPane} that solves the issue of panels that need to expand vertically and
 * scroll.
 */
public class CollapsiblePanel extends JPanel
{
    private final TaskPaneButton collapseButton;

    /**
     * Create a new CollapsiblePanel with an empty title.
     */
    public CollapsiblePanel()
    {
        this("", false);
    }

    public CollapsiblePanel(String title, boolean special)
    {
        initComponents();
        collapseButton = new TaskPaneButton(title);
        collapseButton.setSpecial(special);
        collapseButton.setCollapsed(false);
        //Must call super method here
        super.addImpl(collapseButton, new CellConstraints(1, 1), -1);
        updateCollapsed();
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

    public void setContentView(JComponent component)
    {
        invalidate();
        contentPane.removeAll();
        revalidate();
        contentPane.add(component, new CellConstraints(1, 1));
        collapseButton.setToolTipText((collapseButton.isCollapsed() ? "Show " : "Hide ") + collapseButton.getTitle());
        revalidate();
    }

    public void setIcon(Icon icon)
    {
        this.collapseButton.setIcon(icon);
    }

    public void addCollapseButtonMouseListener(MouseListener listener)
    {
        collapseButton.addMouseListener(listener);
    }

    public void setCollapsed(boolean collapsed)
    {
        collapseButton.setCollapsed(collapsed);
    }

    public boolean isCollapsed()
    {
        return collapseButton.isCollapsed();
    }

    /**
     * Set the Panels title.
     *
     * @param title the title
     */
    public void setTitle(String title)
    {
        collapseButton.setTitle(title);
    }

    /**
     * @return the title
     */
    public String getTitle()
    {
        return collapseButton.getTitle();
    }

    protected void updateCollapsed()
    {
        invalidate();
        boolean collapsed = collapseButton.isCollapsed();
        contentPane.setVisible(!collapsed);
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
