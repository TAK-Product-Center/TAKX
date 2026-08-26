package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTree;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableCellEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.invoke.MethodHandles;
import java.util.EventObject;
import java.util.function.Function;

/**
 * A combined renderer and editor that renders a clickable {@link TriStateJCheckBox} in the hierarchical column of a {@link JXTreeTable}. This class
 * assumes that the hierarchical column value can be converted to a String via {@link Object#toString()}. The inner {@link RendererPanel} class is
 * used to lay out the checkbox and the column value.
 * <p>
 * This class is pretty confusing, but it's the only way to get a checkbox nicely rendered in the hierarchical column of a {@link JXTreeTable}. The
 * core issue being that {@link JXTreeTable} never exposed a proper way to control hierarchical column editors. There are multiple wrinkles which this
 * class has to address:
 * <ul>
 *     <li>To display the familiar layout where the checkbox is nested next to the tree value, the checkbox must be embedded into the hierarchical
 *     column, as opposed to existing as a
 *     separate column (As it does in {@link gov.takx.commons.swing.CheckBoxTableCellRendererEditor}).
 *     <li>Because the checkbox is embedded into an existing column, the checkbox state cannot be used
 *     as the column value, so a separate {@link CombiningTreeSelectionModel} must be
 *     provided to determine the checkbox state for each path. But note that rather than update the selection model directly, we intentionally only trigger an
 *     {@link Action} when the checkbox is clicked. It is up to the consumer to decide how to modify the selection model, and fire data model update
 *     events as necessary.</li>
 *     <li>{@link JXTreeTable} requires you to set the tree column renderer with a {@link TreeCellRenderer}, but you must set the editor with a {@link TableCellEditor}. Notice any difference there?</li>
 *     <li>The {@link TableCellEditor} knows nothing about the left padding that tree cells have and will by default cover the entire cell.</li>
 *     <li>The {@link TableCellEditor} requires a reference to a {@link JTree}, which is internal and private to the {@link JXTreeTable}.</li>
 * </ul>
 *
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class CheckBoxTreeTableTreeCellRendererEditor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Renderer renderer = new Renderer();
    private final JXTreeTable treeTable;
    private final Editor editor;
    private final Function<TreePath, Boolean> stateFunction;

    private CheckBoxTreeTableTreeCellRendererEditor(@Nonnull JXTreeTable treeTable, @Nonnull Function<TreePath, Boolean> stateFunction, @Nonnull Action action)
    {
        this.treeTable = treeTable;
        this.stateFunction = stateFunction;
        // Hack to get tree instance for the editor
        var tree = (JXTree) treeTable.getCellRenderer(0, treeTable.getHierarchicalColumn());
        editor = new Editor(tree, action);
    }

    /**
     * @return the tree cell renderer
     */
    @Nonnull
    public TreeCellRenderer getRenderer()
    {
        return renderer;
    }

    /**
     * @return the tree table cell editor
     */
    @Nonnull
    public TreeTableCellEditor getEditor()
    {
        return editor;
    }

    /**
     * Creates a new instance and sets it as the cell renderer and editor for the given column. Also adds required listeners to the table.
     *
     * @param treeTable the table
     * @param stateFunction a function to provide the checkbox state for a tree path
     * @param action the action to be performed when the checkbox is clicked
     * @return the newly created instance
     */
    @Nonnull
    public static CheckBoxTreeTableTreeCellRendererEditor setRendererAndEditor(@Nonnull JXTreeTable treeTable, @Nonnull Function<TreePath, Boolean> stateFunction, @Nonnull Action action)
    {
        var rendererEditor = new CheckBoxTreeTableTreeCellRendererEditor(treeTable, stateFunction, action);
        treeTable.setTreeCellRenderer(rendererEditor.renderer);
        // There is no way to set an editor for the tree column on a base tree table
        if (treeTable instanceof JXTreeTableExt ext)
        {
            ext.setHierarchicalEditor(rendererEditor.editor);
        } else
        {
            logger.warn("Cannot set editor on JXTreeTable instance. Make sure your table instance handles returning the editor, or use JXTreeTableExt.");
        }
        TableEditCellRolloverListener.addForColumns(treeTable, treeTable.getColumn(treeTable.getHierarchicalColumn()));
        return rendererEditor;
    }

    /**
     * Creates a new instance and sets it as the cell renderer and editor for the given column. Also adds required listeners to the table.
     *
     * @param tree the table
     * @param stateFunction a function to provide the checkbox state for a tree path
     * @param actionPerformedListener the listener to be called when the checkbox is clicked
     * @return the newly created instance
     */
    @SuppressWarnings("UnusedReturnValue")
    @Nonnull
    public static CheckBoxTreeTableTreeCellRendererEditor setRendererAndEditor(@Nonnull JXTreeTable tree, @Nonnull Function<TreePath, Boolean> stateFunction, @Nonnull SimpleAction.ActionPerformedListener actionPerformedListener)
    {
        return setRendererAndEditor(tree, stateFunction, new SimpleAction(null, actionPerformedListener));
    }

    private class Renderer extends RendererPanel implements TreeCellRenderer
    {
        private final DefaultTreeCellRenderer defaultTreeCellRenderer = new DefaultTreeCellRenderer();

        public Renderer()
        {
            checkBox.setSelected(false);
            setOpaque(true);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            var defaultComponent = (JLabel) defaultTreeCellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            setBackground(defaultComponent.getBackground());
            var val = treeTable.getValueAt(row, treeTable.getHierarchicalColumn());
            label.setText(val != null ? val.toString() : "");
            var path = treeTable.getPathForRow(row);
            // A null path can sometimes happen when adding rows. The renderer gets called before the model is fully updated. Probably something
            //  that can / should be fixed in the model, but its easy enough to cover it here as well.
            checkBox.setState(path == null ? Boolean.FALSE : stateFunction.apply(path));
            return this;
        }
    }

    private class Editor extends TreeTableCellEditor implements TableCellEditor
    {
        private final Renderer renderer = new Renderer();
        private final Action action;
        private final JTree tree;
        private int row;

        public Editor(@Nonnull JTree tree, @Nonnull Action action)
        {
            super(tree);
            this.action = action;
            this.tree = tree;
            renderer.checkBox.addActionListener(this::actionPerformed);
            renderer.setOpaque(false);
        }

        @Override
        public boolean isCellEditable(EventObject e)
        {
            return true;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
            this.row = row;
            super.getTableCellEditorComponent(table, value, isSelected, row, column); // We need this, even though we aren't returning this component

            var path = treeTable.getPathForRow(row);
            var state = stateFunction.apply(path);
            renderer.checkBox.setState(state);

            // We have to figure out the proper left margin for the tree node and add it as a border
            var rowPath = tree.getPathForRow(row);
            var bounds = tree.getPathBounds(rowPath);
            //noinspection DataFlowIssue bounds always exist
            var eb = new EmptyBorder(1, bounds.x, 0, 0); // This is hacky, and we need to find a better way to handle these borders
            renderer.setBorder(eb);
            return renderer;
        }

        private void actionPerformed(ActionEvent e)
        {
            action.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, "" + row));
        }
    }

    // Initially created with JFormDesigner, hence the syntax and structure, but removed .jfd file intentionally after testing
    private static class RendererPanel extends JPanel
    {
        private RendererPanel()
        {
            initComponents();
        }

        private void initComponents()
        {
            checkBox = new TriStateJCheckBox();
            label = new JLabel();
            CellConstraints cc = new CellConstraints();

            //======== this ========
            setOpaque(false);
            setName("panel");
            setLayout(new FormLayout(
                    "default, $lcgap, default",
                    "default:grow"));

            //---- checkBox ----
            checkBox.setHorizontalAlignment(SwingConstants.RIGHT);
            checkBox.setName("checkBox");
            add(checkBox, cc.xy(1, 1));

            //---- label ----
            label.setName("label");
            add(label, cc.xy(3, 1));
        }

        public TriStateJCheckBox checkBox;
        public JLabel label;
    }
}