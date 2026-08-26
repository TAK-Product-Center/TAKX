package gov.takx.core.view.gui.bloodhound;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.actionlist.ActionListAction;
import gov.takx.commons.swing.actionlist.ActionListToggleAction;
import gov.takx.commons.swing.entity.EntityColumn;
import gov.takx.commons.swing.entity.common.CommonEntityDefinition;
import gov.takx.core.navigation.NavigationConfiguration;
import gov.takx.core.navigation.NavigationConstants;
import gov.takx.core.navigation.NavigationManager;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.UIManager;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.util.List;

import static gov.takx.core.navigation.NavigationConstants.NAVIGATE_TO_ACTION_BUTTON_ICON;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_BEARING_UNIT;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_RANGE_UNITS;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.CANCEL_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.KEEP_IN_VIEW_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.KEEP_IN_VIEW_ICON_FILE;

/**
 * Entity definition for {@link NavigationConfiguration} instances.
 *
 * @since 5.7
 */
@PseudoBean
public class BloodhoundEntityDefinition extends CommonEntityDefinition<NavigationConfiguration>
{
    private static final String DETAILS_HTML_FORMAT = "<span>Range: %s</span><br/>" +
            "<span>Heading: %s %s</span><br/>" +
            "<span>ETA: %s</span>";

    private static final Icon KEEP_IN_VIEW_ICON_SELECTED = UIResources.getSizedColorizedIcon(UIResources.getUrl(PLATFORM_ICONS, KEEP_IN_VIEW_ICON_FILE), ACTION_BUTTON_IMAGE_SIZE, UIManager.getColor("Table.background"));

    @Inject
    private NavigationManager navigationManager;

    @SuppressWarnings("DataFlowIssue") // the getActions method always passes a value to the ActionList
    private final Action deleteAction = new ActionListAction<NavigationConfiguration>("Cancel", CANCEL_ACTION_BUTTON_ICON)
            .withEventConsumer(e -> navigationManager.removeConfiguration(e.getValue(), (Component) e.getSource()));
    private final ActionListToggleAction<NavigationConfiguration> keepInViewAction = new ActionListToggleAction<NavigationConfiguration>(
            "Toggle Keep In View",
            KEEP_IN_VIEW_ACTION_BUTTON_ICON,
            config -> navigationManager.isKeepInView(config))
            .withSelectedIconAndBackground(KEEP_IN_VIEW_ICON_SELECTED, Color.WHITE)
            .withValueConsumer(config -> navigationManager.toggleKeepInView(config));
    private final Action navigateAction = new ActionListAction<NavigationConfiguration>("Navigate", NAVIGATE_TO_ACTION_BUTTON_ICON)
            .withValueConsumer(bh -> navigationManager.navigate(bh));
    private final List<Action> actions = List.of(navigateAction, keepInViewAction, deleteAction);

    final EntityColumn<NavigationConfiguration, Double> rangeColumn;
    final EntityColumn<NavigationConfiguration, BearingUnits.Direction> dirColumn;
    final EntityColumn<NavigationConfiguration, Long> etaColumn;
    final EntityColumn<NavigationConfiguration, Double> headingColumn;
    final EntityColumn<NavigationConfiguration, Icon> toIconColumn;
    final EntityColumn<NavigationConfiguration, String> fromColumn;
    final EntityColumn<NavigationConfiguration, String> toColumn;
    final EntityColumn<NavigationConfiguration, Long> stateColumn;

    /**
     * Construct a new definition and add all custom Bloodhound columns.
     */
    public BloodhoundEntityDefinition()
    {
        stateColumn = EntityColumn.<NavigationConfiguration, Long>builder().type(Long.class).sortable(false).cached(true).value(NavigationConfiguration::getEtaSec).build();
        fromColumn = EntityColumn.<NavigationConfiguration, String>builder().name("Source").type(String.class).cached(true).value(config -> config.getSourceMapObject().getName()).build();
        toColumn = EntityColumn.<NavigationConfiguration, String>builder().name("Target").type(String.class).cached(true).value(config -> config.getTargetMapObject().getName()).build();
        toIconColumn = EntityColumn.<NavigationConfiguration, Icon>builder().type(Icon.class).cached(true).value(config -> config.getTargetMapObject().getSizedIcon(ImageUtils.MEDIUM_ICON_WIDTH)).cached(true).build();
        dirColumn = EntityColumn.<NavigationConfiguration, BearingUnits.Direction>builder().name("Dir").sortable(false).cached(true).type(BearingUnits.Direction.class).value(config -> BearingUnits.Direction.fromDegrees8(config.getCurrentTarget().getTargetArrow().getBearing())).cached(true).build();
        rangeColumn = EntityColumn.<NavigationConfiguration, Double>builder().name("Range").type(Double.class).cached(true).value(config -> config.getCurrentTarget().getTargetArrow().getRange()).build();
        headingColumn = EntityColumn.<NavigationConfiguration, Double>builder().name("Heading").type(Double.class).cached(true).value(config -> config.getCurrentTarget().getTargetArrow().getBearing()).build();
        etaColumn = EntityColumn.<NavigationConfiguration, Long>builder().name("ETA").type(Long.class).cached(true).value(NavigationConfiguration::getEtaSec).build();
        addColumns(stateColumn, fromColumn, toIconColumn, toColumn, dirColumn, rangeColumn, headingColumn, etaColumn);

        addPropertyChangeMapping(List.of(IMapObject.SPEED, NavigationConfiguration.ETA), List.of(getSummaryColumn(), etaColumn, stateColumn));
        // TODO TAKX-4239 Summary column always needs to be first due to a sort issue
        addPropertyChangeMapping(List.of(ShapeProperties.RANGE, IMapObject.LOCATION), List.of(getSummaryColumn(), rangeColumn, headingColumn, etaColumn, stateColumn));
        addPropertyChangeMapping(List.of(IMapObject.NAME), List.of(getSummaryColumn(), fromColumn, toColumn));
    }

    @Nullable
    @Override
    protected Icon getIcon(@Nonnull NavigationConfiguration entity)
    {
        return entity.getSourceMapObject().getSizedIcon(ImageUtils.MEDIUM_ICON_WIDTH);
    }

    @Nullable
    @Override
    protected String getName(@Nonnull NavigationConfiguration entity)
    {
        return getData().getValue(entity, fromColumn) + " → " + getData().getValue(entity, toColumn);
    }

    @SuppressWarnings("DataFlowIssue") // Entity definition never returns null for the columns
    @Nullable
    @Override
    protected String getDetailsHtml(@Nonnull NavigationConfiguration entity)
    {
        var range = getData().getValue(entity, rangeColumn);
        var rangeFormatted = DISPLAY_RANGE_UNITS.getValueAs(RangeUnits.class).format(range, 1);
        var heading = getData().getValue(entity, headingColumn);
        var headingFormatted = DISPLAY_BEARING_UNIT.getValueAs(BearingUnits.class).format(entity.getSourceMapObject().getLocation(), range, heading);
        var eta = getData().getValue(entity, etaColumn);
        var etaFormatted = eta < 0 ? "" : DurationFormatUtils.formatDuration(eta * 1000, NavigationConstants.ETA_FORMAT);
        var dir = getData().getValue(entity, dirColumn);
        return DETAILS_HTML_FORMAT.formatted(rangeFormatted, dir, headingFormatted, etaFormatted);
    }

    @Override
    protected void addPropertyChangeListener(@Nonnull NavigationConfiguration object, @Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
        object.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    protected void removePropertyChangeListener(@Nonnull NavigationConfiguration object, @Nonnull String propertyName, @Nonnull PropertyChangeListener propertyChangeListener)
    {
        object.removePropertyChangeListener(propertyName, propertyChangeListener);
    }

    @Override
    @Nullable
    protected ActionList<NavigationConfiguration> getActions(@Nonnull NavigationConfiguration entity)
    {
        return new ActionList<>(actions, entity);
    }
}
