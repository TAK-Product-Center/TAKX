package gov.takx.core.view.gui.explorer.view;

import gov.takx.api.explorer.IExplorable;
import gov.takx.commons.constants.State;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.entity.EntityColumn;
import gov.takx.commons.swing.entity.common.CommonEntityDefinition;
import gov.takx.commons.swing.entity.common.SummaryProvider;
import gov.takx.core.explorer.common.CoreExplorerUtils;
import gov.takx.core.explorer.controller.IExplorerController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;

import java.util.List;

/**
 * Entity definition for {@link IExplorable} based types.
 *
 * @since 5.7
 */
public class ExplorableEntityDefinition extends CommonEntityDefinition<IExplorable>
{
    private final EntityColumn<IExplorable, State> alarmColumn;
    private final EntityColumn<IExplorable, String> hierarchicalColumn;
    private final EntityColumn<IExplorable, Visibility> visibilityRollUpColumn;

    /**
     * Construct a new instance using a controller to derive certain state information e.g. alarms, and add mappings from
     * explorable properties to entity columns.
     * 
     * @param controller the explorer controller
     * @param visibilityManager the visibility manager
     */
    public ExplorableEntityDefinition(@Nonnull IExplorerController controller, @Nonnull VisibilityManager visibilityManager)
    {
        alarmColumn = EntityColumn.<IExplorable, State>builder()
                .type(State.class)
                .cached(true)
                .value(e -> controller.getAlarmsController().getHighestVisibleAlarmState(e))
                .build();
        hierarchicalColumn = EntityColumn.<IExplorable, String>builder()
                .type(String.class)
                .cached(true)
                .value(e -> getSummaryProvider().getTreeHtml(e))
                .build();
        visibilityRollUpColumn = EntityColumn.<IExplorable, Visibility>builder()
                .type(Visibility.class)
                .cached(true)
                .value(visibilityManager::getVisibilityIfSupported)
                .build();

        addColumns(alarmColumn, hierarchicalColumn, visibilityRollUpColumn);
        addPropertyChangeMapping(IExplorable.VISIBLE, visibilityRollUpColumn);

        addPropertyChangeMapping(List.of(IExplorable.DISPLAY_NAME, IExplorable.DISPLAY_NAME_COLOR), List.of(getSummaryColumn(), getNameColumn()));
        addPropertyChangeMapping(List.of(IExplorable.CHILD), List.of(getSummaryColumn()));
        addPropertyChangeMapping(IExplorable.HIGHEST_ALARM_STATE_KEY, getAlarmColumn());
        addPropertyChangeMapping(IExplorable.ICON_IMAGE, getIconColumn());
    }

    /**
     * @return the tree hierarchical column
     */
    @Nonnull
    public EntityColumn<IExplorable, String> getHierarchicalColumn()
    {
        return hierarchicalColumn;
    }

    /**
     * @return the visibility rollup column
     */
    @Nonnull
    public EntityColumn<IExplorable, Visibility> getVisibilityRollUpColumn()
    {
        return visibilityRollUpColumn;
    }

    /**
     * @return the alarm state column
     */
    @Nonnull
    public EntityColumn<IExplorable, State> getAlarmColumn()
    {
        return alarmColumn;
    }

    /**
     * @param summaryProvider the summary provider
     */
    public void setSummaryProvider(@Nonnull SummaryProvider<IExplorable> summaryProvider)
    {
        this.summaryProvider = summaryProvider;
    }

    @Override
    @Nonnull
    public ExplorerSummaryProvider getSummaryProvider()
    {
        return (ExplorerSummaryProvider) summaryProvider;
    }

    @Override
    public Icon getIcon(@Nonnull IExplorable explorable)
    {
        return CoreExplorerUtils.iconForExplorable(explorable);
    }

    @Override
    protected String getName(@Nonnull IExplorable explorable)
    {
        return explorable.getDisplayName();
    }

    @Override
    protected void toggleVisibility(@Nonnull IExplorable entity)
    {
        entity.setVisible(!entity.isVisible());
    }

    @Override
    protected boolean isVisible(@Nonnull IExplorable entity)
    {
        return entity.isVisible();
    }

    @Nullable
    @Override
    protected String getDetailsHtml(@Nonnull IExplorable entity)
    {
        return entity.getDetailsTextHtml();
    }

    @Nullable
    @Override
    protected ActionList<IExplorable> getActions(@Nonnull IExplorable entity)
    {
        var actions = entity.createQuickActions();
        return !actions.isEmpty() ? new ActionList<>(actions, entity) : null;
    }
}
