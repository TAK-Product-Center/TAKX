package gov.takx.commons.swing.entity;

import gov.takx.commons.swing.entity.common.CommonEntityDefinition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;

/**
 * @since 5.7
 */
public class TestEntityDefinition extends CommonEntityDefinition<TestEntity>
{
    public final EntityColumn<TestEntity, Integer> idColumn;
    public final EntityColumn<TestEntity, String> emptyColumn;

    public TestEntityDefinition()
    {
        idColumn = EntityColumn.<TestEntity, Integer>builder().name("Id").type(Integer.class).value(TestEntity::getId).cached(true).build();
        emptyColumn = EntityColumn.<TestEntity, String>builder().name("Empty").type(String.class).value(TestEntity::getEmpty).sortable(false).build();
        addColumns(idColumn, emptyColumn);
    }

    @Override
    protected void addPropertyChangeListener(@Nonnull TestEntity object, @Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
        object.listener = listener;
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
