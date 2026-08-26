package gov.takx.commons.swing.entity;

import gov.takx.commons.lang.ListenerManager;
import gov.takx.commons.swing.entity.common.CommonEntityDefinition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;

/**
 * @since 5.7
 */
public class TestEntityDefinition extends CommonEntityDefinition<TestEntity>
{
    public static final String CUSTOM_NAME_COLUMN_NAME = "Custom Name";
    public final EntityColumn<TestEntity, Integer> idColumn;
    public final EntityColumn<TestEntity, String> emptyColumn;

    public TestEntityDefinition()
    {
        idColumn = EntityColumn.<TestEntity, Integer>builder().name("Id").type(Integer.class).value(TestEntity::getId).cached(true).build();
        emptyColumn = EntityColumn.<TestEntity, String>builder().name("Empty").header("").type(String.class).value(TestEntity::getEmpty).sortable(false).build();
        addColumns(idColumn, emptyColumn);
    }

    @Override
    protected void addPropertyChangeListener(@Nonnull ListenerManager listenerManager, @Nonnull TestEntity object, @Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
        object.listener = listener;
    }

    @Nonnull
    @Override
    protected EntityColumn.Builder<TestEntity, String> buildNameColumn()
    {
        // Change the common column name and add a tooltip function
        return super.buildNameColumn()
                .tooltip(TestEntity::getName) // Provide a tooltip
                .name(CUSTOM_NAME_COLUMN_NAME); // Override with a custom name
    }

    @Nonnull
    @Override
    protected EntityColumn.Builder<TestEntity, String> buildSummaryColumn()
    {
        return super.buildSummaryColumn().tooltip(e -> "Summary");
    }

    @Nullable
    @Override
    protected String getDetailsHtml(@Nonnull TestEntity entity)
    {
        return entity.getDetails();
    }

    @Nullable
    @Override
    protected String getName(@Nonnull TestEntity entity)
    {
        return entity.getName();
    }
}
