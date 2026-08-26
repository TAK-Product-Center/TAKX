package gov.takx.commons.swing.actionlist;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.TableEditCellRolloverListener;
import jakarta.annotation.Nonnull;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import java.awt.event.ActionEvent;

/**
 * Implementation of {@link TableCellEditor} that accepts a List of {@link Action} instances as the model value
 * and renders each as a {@link JButton}. When a button is clicked, the corresponding {@link Action} is
 * invoked via {@link Action#actionPerformed(ActionEvent)}. The action event is by default an {@link ActionListActionEvent}
 * instance, which has accessors for the row, and uses the button as the source. Rendering is handled by the {@link ActionListTableCellRenderer}.
 * Any {@link Action} will work but further customization of functionality can be achieved by using one of the provided {@link ActionListAction}
 * classes. The renderer and editor expect the table cell value to be an instance of {@link ActionList}.
 * <p>
 * @apiNote Given that this class is useless unless set as both the renderer and the editor, the
 * {@link #setRendererAndEditor(JTable, TableColumn...)} method is provided to attach both to multiple columns. Additionally, because
 * Swing tables do not go into edit mode automatically, a {@link TableEditCellRolloverListener} is automatically attached so
 * that button components operate as expected (i.e. not requiring double clicks).
 *
 * @since 5.3 (significant changes in 5.7)
 */
@IncubatingApi(since = "5.7")
public class ActionListTableCellRendererEditor
{
    @VisibleForTesting
    final ActionListTableCellRenderer renderer;
    @VisibleForTesting
    final ActionListTableCellEditor editor;

    @VisibleForTesting
    ActionListTableCellRendererEditor()
    {
        editor = new ActionListTableCellEditor();
        renderer = new ActionListTableCellRenderer();
    }

    /**
     * Sets the renderer and editor for the given column to instances of {@link ActionListTableCellRendererEditor} and
     * {@link ActionListTableCellRenderer}. Note that the editor instance has its own embedded renderer instance. This
     * guarantees that the rendering is identical while also separating the editor component hierarchy.
     *
     * @param column the table column
     * @param actionEventCreator the action event creator
     * @param stopEditOnAction {@code true} if table editing should be stopped after an action is performed, otherwise {@code false}
     * @since 5.3 (removed return value in 5.7)
     * @deprecated Use {@link #setRendererAndEditor(JTable, TableColumn...)}
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    public static void setRendererAndEditor(@Nonnull TableColumn column, @Nonnull ActionEventCreator actionEventCreator, boolean stopEditOnAction)
    {
        var rendererEditor = new ActionListTableCellRendererEditor();
        column.setCellRenderer(rendererEditor.renderer);
        column.setCellEditor(rendererEditor.editor);
        rendererEditor.editor.setEventCreator(actionEventCreator);
        rendererEditor.editor.setStopEditOnAction(stopEditOnAction);
    }

    /**
     * Sets a renderer and editor for the given columns to instances of {@link ActionListTableCellRenderer} and
     * {@link ActionListTableCellEditor}. Each column has its own instances. The editor instance has its own embedded
     * renderer instance, which guarantees that the rendering is identical while also separating the editor component
     * hierarchy.
     * <p>See {@link ActionListTableCellRendererEditor} docs for explanation on how to best use this feature.
     *
     * @param table the table that the columns are within (necessary for the rollover listener)
     * @param columns the columns to set a cell renderer and editor for
     * @since 5.7
     */
    public static void setRendererAndEditor(@Nonnull JTable table, @Nonnull TableColumn... columns)
    {
        for (TableColumn column : columns)
        {
            var rendererEditor = new ActionListTableCellRendererEditor();
            column.setCellRenderer(rendererEditor.renderer);
            column.setCellEditor(rendererEditor.editor);

        }
        TableEditCellRolloverListener.addForColumns(table, columns);
    }

}
