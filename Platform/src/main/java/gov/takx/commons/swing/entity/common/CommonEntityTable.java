package gov.takx.commons.swing.entity.common;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.PopupTriggerListener;
import gov.takx.commons.swing.TableUtils;
import gov.takx.commons.swing.actionlist.ActionListPopupMenu;
import gov.takx.commons.swing.actionlist.ActionListTableCellRendererEditor;
import gov.takx.commons.swing.entity.EntityTable;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.table.TableColumnExt;

import javax.swing.table.TableModel;

import java.awt.event.MouseEvent;

/**
 * A reusable swing table to be used with {@link EntityTableModel} and {@link CommonEntityDefinition}. Primarily for
 * displaying entities with similar properties to:
 * <ul>
 *     <li>{@link IExplorable}</li>
 *     <li>{@link IMapObject}</li>
 *     <li>{@link gov.takx.api.drifter.DrifterConfiguration}</li>
 * </ul>
 * Handles setting up renderers for some specialized columns that are common across many
 * entities:
 * <ul>
 *     <li>Summary</li>
 *     <li>Row Actions</li>
 *     <li>Icon</li>
 * </ul>
 * Also handles setting up row highlighters and roll over editing, for a consistent look and feel across the application.
 * Much of the initialization is delayed until
 * {@link #setModel(TableModel)} is called with an instanceof {@link EntityTableModel} because rendering configuration
 * is tied specifically to columns from the model.
 *
 * @param <E> the object type that this table works with
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class CommonEntityTable<E> extends EntityTable<E>
{
    @VisibleForTesting
    static final int ACTIONS_COLUMN_MIN_ROW_HEIGHT = UIResources.ACTION_BUTTON_IMAGE_SIZE.height + 21;

    private final ActionListPopupMenu<E> popupMenu = new ActionListPopupMenu<>();
    private boolean popupMenuEnabled = true;

    /**
     * @return {@code true} if the popup menu is enabled, otherwise {@code false}
     * @since 5.8
     */
    public boolean isPopupMenuEnabled()
    {
        return popupMenuEnabled;
    }

    /**
     * @param enabled {@code true} if the popup menu should be enabled, otherwise {@code false}
     * @since 5.8
     */
    public void setPopupMenuEnabled(boolean enabled)
    {
        popupMenuEnabled = enabled;
    }

    @Nonnull
    @Override
    public CommonEntityDefinition<E> getEntityDefinition()
    {
        return (CommonEntityDefinition<E>) super.getEntityDefinition();
    }

    /**
     * @return the actions column
     */
    @Nonnull
    public TableColumnExt getActionsColumn()
    {
        return requireColumn(getEntityDefinition().getActionsColumn());
    }

    /**
     * @return the name column
     */
    @Nonnull
    public TableColumnExt getNameColumn()
    {
        return requireColumn(getEntityDefinition().getNameColumn());
    }

    /**
     * @return the summary column
     */
    @SuppressWarnings("unused")
    @Nonnull
    public TableColumnExt getSummaryColumn()
    {
        return requireColumn(getEntityDefinition().getSummaryColumn());
    }

    /**
     * @return the icon column
     */
    @SuppressWarnings("unused")
    @Nonnull
    public TableColumnExt getIconColumn()
    {
        return requireColumn(getEntityDefinition().getIconColumn());
    }

    /**
     * @return the visibility table column
     */
    @Nonnull
    public TableColumnExt getVisibilityColumn()
    {
        return requireColumn(getEntityDefinition().getVisibilityColumn());
    }

    protected void initModel()
    {
        super.initModel();

        setAutoSizeColumns(true, getActionsColumn().getModelIndex());
        TableUtils.setColumnWidths(ICON_COLUMN_SIZE, selectColumn);
        TableUtils.setColumnWidths(ICON_COLUMN_SIZE + 10, getVisibilityColumn());

        // Set all auto columns hidden by default
        getActionsColumn().setVisible(false);
        getNameColumn().setVisible(false);
        getSummaryColumn().setVisible(false);
        getIconColumn().setVisible(false);
        getVisibilityColumn().setVisible(false);
        getActionsColumn().addPropertyChangeListener(e -> {
            // Ensure the row height can fit action buttons if we make that column visible
            if (e.getPropertyName().equals("visible") && Boolean.TRUE.equals(e.getNewValue()) && getRowHeight() < ACTIONS_COLUMN_MIN_ROW_HEIGHT) setRowHeight(ACTIONS_COLUMN_MIN_ROW_HEIGHT);
        });
    }

    @Override
    protected void initRenderers()
    {
        super.initRenderers();
        ActionListTableCellRendererEditor.setRendererAndEditor(this, getActionsColumn(), getVisibilityColumn());
    }

    @Override
    protected void initListeners()
    {
        super.initListeners();
        addMouseListener((PopupTriggerListener) this::popupTriggered);
    }

    /**
     * Called when a popup event is triggered and displays a popup menu with actions obtained from the entity definition.
     *
     * @param event the popup mouse event
     * @since 5.8
     */
    protected void popupTriggered(@Nonnull MouseEvent event)
    {
        if (popupMenuEnabled)
        {
            var entity = getEntityAtPoint(event.getPoint());
            if (entity != null)
            {
                var actionList = getEntityDefinition().getPopupActions(entity);
                if (actionList != null) popupMenu.show(actionList, event);
            }
        }
    }
}
