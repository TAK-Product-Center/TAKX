package gov.takx.core.view.gui;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

/**
 */
public class GUIUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Determines whether a given {@link MouseEvent} occurred over a specific component within a {@link JTree} node/cell. This
     * can be used in tandem with a {@link javax.swing.tree.TreeCellRenderer} to detect mouse events on specific tree cell
     * components.
     * @param event the mouse event
     * @param cellComponent the component of the node
     * @return {@code true} if the mouse event occurred over the given component, otherwise {@code false}
     * @since 5.3
     */
    public static boolean isEventForTreeCellComponent(@Nonnull MouseEvent event, @Nonnull Component cellComponent)
    {
        if (event.getSource() instanceof JTree tree)
        {
            Point p = event.getPoint();
            TreePath clickedForPath = tree.getPathForLocation(p.x, p.y);
            if (clickedForPath != null)
            {
                Rectangle rowBounds = tree.getRowBounds(tree.getRowForPath(clickedForPath));
                Rectangle componentBounds = cellComponent.getBounds();
                Rectangle rowComponentBounds = new Rectangle(rowBounds.x, rowBounds.y, componentBounds.width, componentBounds.height);
                return rowComponentBounds.contains(p);
            }
        }
        return false;
    }

    /**
     * Set the text in a {@link JTextField} if the given {@code text} is not {@code null}.
     * @since 5.2
     */
    public static void setTextIfNonNull(@Nonnull JTextField textField, @Nullable String text)
    {
        if (text != null) textField.setText(text);
    }

    /**
     * Recursively enable/disable all of a component's descendants.
     *
     * @param parentComponent The parent component
     * @param enable          Whether the components should be enabled
     */
    public static void enableComponents(@Nonnull JComponent parentComponent, boolean enable)
    {
        performOperationForChildren(parentComponent, c -> c.setEnabled(enable));
    }

    /**
     * Recursively add a key listener to a component and all of its descendants.
     *
     * @param component Top-level component to which the listener will be added
     * @param listener  key listener to add
     * @since 3.0
     */
    public static void addKeyListenerToAll(@Nonnull Component component, @Nonnull KeyListener listener)
    {
        // Add listener to the component itself
        component.addKeyListener(listener);
        performOperationForChildren(component, c -> c.addKeyListener(listener));
        // TODO KMB: Using registerKeyboardAction is a cleaner way to accomplish this since we don't have to add a
        //  listener to every component in the hierarchy. We should review callers of this method to see if they can be
        //  updated to something like the following:
        //      registerKeyboardAction((e) -> <action>), ESCAPE_KEY, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Recursively apply the provided operation to all of a component's descendants.
     *
     * @param component The parent component
     * @param operation The operation to perform
     */
    private static void performOperationForChildren(@Nonnull Component component, @Nonnull Consumer<Component> operation)
    {
        if (component instanceof Container container)
        {
            synchronized (component.getTreeLock())
            {
                for (Component childComponent : container.getComponents())
                {
                    operation.accept(childComponent);
                    performOperationForChildren(childComponent, operation);
                }
            }
        }
    }

    /**
     * Add a {@link SingleActionDocumentListener} invoking the given handler to the document underlying the given text component.
     *
     * @param textComponent Text component on which to add a document listener
     * @param handler The handler to invoke on any document event
     * @since 3.4
     * @see JTextComponent#getDocument()
     */
    public static void addSingleActionDocumentListener(@Nonnull JTextComponent textComponent, @Nonnull Consumer<DocumentEvent> handler)
    {
        textComponent.getDocument().addDocumentListener(new SingleActionDocumentListener(handler));
    }

    /**
     * Document listener that invokes a provided consumer for all document events.
     * @since 3.1
     */
    public static class SingleActionDocumentListener implements DocumentListener
    {
        private final Consumer<DocumentEvent> handler;

        /**
         * @param handler The handler to invoke on any document event
         */
        public SingleActionDocumentListener(@Nonnull Consumer<DocumentEvent> handler)
        {
            this.handler = handler;
        }

        @Override
        public void insertUpdate(DocumentEvent e)
        {
            handler.accept(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e)
        {
            handler.accept(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e)
        {
            handler.accept(e);
        }
    }

    /**
     * Set the given table's row height to the maximum row height it contains.
     *
     * @param table The table to update
     * @param padding Additional padding to add to the calculated row height
     * @since 3.1
     */
    public static void updateRowHeights(JTable table, int padding)
    {
        for (int row = 0; row < table.getRowCount(); row++)
        {
            int rowHeight = table.getRowHeight();

            for (int column = 0; column < table.getColumnCount(); column++)
            {
                final TableCellRenderer renderer = table.getCellRenderer(row, column);
                final int cellHeight = table.prepareRenderer(renderer, row, column).getPreferredSize().height;

                rowHeight = Math.max(rowHeight, cellHeight);
            }

            table.setRowHeight(row, rowHeight + padding);
        }
    }

    /**
     * Set table column min and max widths to a specified value.
     *
     * @param width the width
     * @param columns the columns
     * @since 5.6
     */
    public static void setColumnWidths(int width, @Nonnull TableColumn... columns)
    {
        for (TableColumn column : columns)
        {
            column.setMaxWidth(width);
            column.setMinWidth(width);
        }
    }

    /**
     * Update table column widths to match their contents.
     *
     * @param table the table
     * @param min {@code true} to set min, max, and preferred widths, {@code false} to set only max and preferred
     * @param columns the columns to update
     * @since 5.6
     */
    public static void setColumnWidthsFromContents(@Nonnull JTable table, boolean min, @Nonnull TableColumn... columns)
    {
        for (TableColumn column : columns)
        {
            int width = 0;
            int col = table.convertColumnIndexToView(column.getModelIndex());
            if (col > -1)
            {
                for (int row = 0; row < table.getRowCount(); row++)
                {
                    try
                    {
                        TableCellRenderer renderer = table.getCellRenderer(row, col);
                        Component comp = table.prepareRenderer(renderer, row, col);
                        width = Math.max(width, comp.getPreferredSize().width);
                    } catch (IndexOutOfBoundsException e)
                    {
                        if (SwingUtilities.isEventDispatchThread())
                        {
                            logger.error("Caught IndexOutOfBoundsException, likely because you are adjusting the UI outside the EDT.", e);
                        }
                        throw e;
                    }
                }
                width = width + 10; // To account for borders, otherwise labels get cut off and show "..."
                if (min) column.setMinWidth(width);
                column.setMaxWidth(width);
                column.setPreferredWidth(width);
            }
        }
    }

    /**
     * Update table column min, max, and preferred widths to match their contents.
     *
     * @param table the table
     * @param columns the columns to update
     * @since 5.3 (renamed from updateColumnWidths in 5.6)
     */
    public static void setColumnWidthsFromContents(@Nonnull JTable table, @Nonnull TableColumn... columns)
    {
        setColumnWidthsFromContents(table, true, columns);
    }

    private GUIUtils()
    {
    }
}
