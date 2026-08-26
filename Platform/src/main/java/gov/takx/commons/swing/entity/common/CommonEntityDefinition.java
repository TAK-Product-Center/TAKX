package gov.takx.commons.swing.entity.common;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.actionlist.ActionListToggleAction;
import gov.takx.commons.swing.entity.EntityColumn;
import gov.takx.commons.swing.entity.EntityDefinition;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.UIManager;

import java.util.Comparator;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;

/**
 * Entity definition that creates default common columns that many entities use:
 * <ul>
 *     <li>Name</li>
 *     <li>Icon</li>
 *     <li>Summary</li>
 *     <li>Actions</li>
 *     <li>Visibility</li>
 * </ul>
 * Often only a subset of these columns are necessary, but the overhead of having them here is negligible as
 * they will not be rendered unless visible in the table.
 *
 * @param <E> the entity type that this defines
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class CommonEntityDefinition<E> extends EntityDefinition<E>
{
    public static final Icon VISIBILITY_ON_ICON = UIResources.getSizedIcon(PLATFORM_ICONS, "visibility.png", ACTION_BUTTON_IMAGE_SIZE);
    public static final Icon VISIBILITY_OFF_ICON = UIResources.getSizedColorizedIcon(UIResources.getUrl(PLATFORM_ICONS, "visibility_off.png"), ACTION_BUTTON_IMAGE_SIZE, UIManager.getColor("Label.disabledForeground").darker().darker().darker());

    public final ActionListToggleAction<E> visibilityAction = new ActionListToggleAction<>("Visibility", VISIBILITY_OFF_ICON, this::isVisible)
            .withSelectedIcon(VISIBILITY_ON_ICON)
            .withButtonConfigurer((button, action, value, row) -> button.setContentAreaFilled(false)) // By default, we want the visibility action to appear only as an Icon, not a Button
            .withValueConsumer(this::toggleVisibility);
    private final List<Action> visibilityActions = List.of(visibilityAction);

    private final EntityColumn<E, String> nameColumn;
    private final EntityColumn<E, Icon> iconColumn;
    private final EntityColumn<E, String> summaryColumn;
    private final EntityColumn<E, ActionList<E>> actionsColumn;
    private final EntityColumn<E, ActionList<E>> visibilityColumn;
    protected SummaryProvider<E> summaryProvider;

    /**
     * Construct a new instance and create the 4 common columns.
     */
    public CommonEntityDefinition()
    {
        visibilityColumn = EntityColumn.<E, ActionList<E>>builder().editable(true).type(ActionList.class)
                .value(o -> new ActionList<>(visibilityActions, o))
                .comparator(Comparator.comparing(al -> isVisible(al.value())))
                .build();
        iconColumn = EntityColumn.<E, Icon>builder().type(Icon.class).value(this::getIcon).cached(true).build(); // TODO DCW TAKX-3258 figure out a way to sort on the icon/type column
        nameColumn = EntityColumn.<E, String>builder().name("Name").type(String.class).value(this::getName).cached(true).build();
        summaryProvider = new SummaryProvider<>(this);
        summaryColumn = EntityColumn.<E, String>builder().name("Details").type(String.class).value(e -> summaryProvider.createSummaryHtml(e)).cached(true).sortable(false).build();
        actionsColumn = EntityColumn.<E, ActionList<E>>builder().editable(true).type(ActionList.class).value(this::getActions).build();

        addColumns(visibilityColumn, iconColumn, nameColumn, summaryColumn, actionsColumn);
    }

    /**
     * @return the visibility column
     */
    @Nonnull
    public EntityColumn<E, ActionList<E>> getVisibilityColumn()
    {
        return visibilityColumn;
    }

    /**
     * @return the name column
     */
    @Nonnull
    public EntityColumn<E, String> getNameColumn()
    {
        return nameColumn;
    }

    /**
     * @return the summary column
     */
    @Nonnull
    public EntityColumn<E, String> getSummaryColumn()
    {
        return summaryColumn;
    }

    /**
     * @return the icon column
     */
    @Nonnull
    public EntityColumn<E, Icon> getIconColumn()
    {
        return iconColumn;
    }

    /**
     * @return the actions column
     */
    @Nonnull
    public EntityColumn<E, ActionList<E>> getActionsColumn()
    {
        return actionsColumn;
    }

    /**
     * @return the summary text provider for this entity
     */
    @Nonnull
    public SummaryProvider<E> getSummaryProvider()
    {
        return summaryProvider;
    }

    /**
     * @implNote Default implementation always returns {@code null}
     * @param entity the model object to get the icon for
     * @return the icon for the given object
     */
    @Nullable
    protected Icon getIcon(@Nonnull E entity)
    {
        return null;
    }

    /**
     * @implNote Default implementation always returns {@code null}
     * @param entity the model object to get a name for
     * @return the name for the given object
     */
    @Nullable
    protected String getName(@Nonnull E entity)
    {
        return null;
    }

    /**
     * @implNote Default implementation always returns {@code true}
     * @param entity the model object to check visibility for
     * @return {@code true} if the object is visible, otherwise {@code false}
     */
    protected boolean isVisible(@Nonnull E entity)
    {
        return true;
    }

    /**
     * Toggle the visibility of an object. Triggered when the user clicks the visibility action.
     *
     * @param entity the object
     */
    protected void toggleVisibility(@Nonnull E entity)
    {
    }

    /**
     * @implNote Default implementation always returns {@code null}
     * @param entity the model object to get row actions for
     * @return the list of row actions for the given object
     */
    @Nullable
    protected ActionList<E> getActions(@Nonnull E entity)
    {
        return null;
    }

    /**
     * Gets the detail HTML string for a model object.
     *
     * @implNote Default implementation always returns {@code null}
     * @param entity the model object to get details HTML for
     * @return the details HTML for the given object, <i>not</i> including {@code <html>} tags
     */
    @Nullable
    protected String getDetailsHtml(@Nonnull E entity)
    {
        return null;
    }
}
