package gov.takx.core.view.gui.toolbar;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import java.util.ArrayList;
import java.util.List;

/**
 * An extension of {@link javax.swing.JToolBar} that adds scroll buttons at the end of the toolbar when all of the
 * buttons won't fit on the visible toolbar.
 *
 * @since 3.3
 */
public class JScrollableToolBar extends JToolBar
{
    public static final String TOOL_ADDED = "toolAdded";
    public static final String TOOL_REMOVED = "toolRemoved";
    private static final String uiClassID = "ScrollableToolBarUI";
    private List<JComponent> tools;

    @Override
    public String getUIClassID()
    {
        return uiClassID;
    }

    @Override
    public ScrollableToolBarUI getUI()
    {
        return (ScrollableToolBarUI) ui;
    }

    @Override
    public void updateUI()
    {
        // Ok, this is annoying and hacky, but the super constructor will get called before instance variables are
        // initialized, and the super calls updateUI(), which ends up trying to use this tools list as part of UI setup.
        // This is the only way I could find to make sure the list would be initialized before the UI needed to use it.
        if (tools == null) tools = new ArrayList<>(1);

        if (UIManager.get(getUIClassID()) != null)
        {
            setUI((ScrollableToolBarUI) UIManager.getUI(this));
        } else
        {
            setUI(new ScrollableToolBarUI());
        }
    }

    /**
     * Add a tool (e.g. button or separator) to be included in the toolbar.
     *
     * @param tool The tool to add
     */
    public void addTool(@Nonnull JComponent tool)
    {
        insertTool(tool, tools.size());
    }

    /**
     * Add a tool (e.g. button or separator) at the specified index on the toolbar.
     *
     * @param tool  The tool to add
     * @param index The index at which to add the tool
     */
    public void insertTool(@Nonnull JComponent tool, int index)
    {
        // FIXME: TAKX-3070
        // This is a stopgap fix while we investigate the underlying issue related to ToolbarUpdater::getComponentEffectiveIndex
        // returning an invalid index.

        // If we try to place a tool at an invalid position, just append it to the end to avoid index out of bounds exceptions.
        if (index > tools.size())
        {
            tools.add(tool);
        } else
        {
            tools.add(index, tool);
        }

        firePropertyChange(TOOL_ADDED, null, tool);
    }

    /**
     * @return The number of tools (e.g. buttons and separators) on the toolbar.
     */
    public int getToolCount()
    {
        return tools.size();
    }

    /**
     * @param index The index for which to get a tool
     * @return The tool at the provided index, or {@code null} for an invalid index.
     */
    @Nullable
    public JComponent getToolAtIndex(int index)
    {
        // Returning null for an invalid index matches the semantics of JToolbar.getComponentAtIndex()
        if (index < 0 || index >= tools.size()) return null;
        return tools.get(index);
    }

    /**
     * @param tool The tool for which to get the index
     * @return The index for the specified tool in the toolbar.
     */
    public int getIndexOfTool(@Nonnull JComponent tool)
    {
        return tools.indexOf(tool);
    }

    /**
     * @param index The index to check
     * @return True if the component at the specified index is a {@link JSeparator}.
     */
    protected boolean isSeparatorAtIndex(int index)
    {
        return getToolAtIndex(index) instanceof JSeparator;
    }

    /**
     * Remove the specified tool from the toolbar.
     *
     * @param tool The tool to remove
     */
    public void removeTool(JComponent tool)
    {
        if (tools.remove(tool)) firePropertyChange(TOOL_REMOVED, tool, null);
    }

    /**
     * Remove the tool at the specified index from the toolbar.
     *
     * @param index The index of the tool to remove
     */
    public void removeTool(int index)
    {
        final JComponent tool = tools.remove(index);
        firePropertyChange(TOOL_REMOVED, tool, null);
    }
}
