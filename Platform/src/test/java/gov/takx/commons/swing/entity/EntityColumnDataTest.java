package gov.takx.commons.swing.entity;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
class EntityColumnDataTest
{
    @Test
    public void test()
    {
        var definition = new TestEntityDefinition();
        var columnData = new EntityColumnData<TestEntity>();

        // Note that only name and id are cached
        var testColumns = List.of(definition.getNameColumn(), definition.idColumn, definition.emptyColumn);
        var nameIndex = 0;
        var idIndex = 1;
        var emptyIndex = 2;
        testColumns.forEach(columnData::addColumn);
        assertThat(columnData.valueMaps).hasSameSizeAs(testColumns);
        assertThat(columnData.valueMaps).contains(null, Index.atIndex(2));

        var entityCount = 3;
        var entityList = new ArrayList<TestEntity>();
        for (var i = 0; i < entityCount; i++)
        {
            var e = new TestEntity("Name " + i, i);
            entityList.add(e);
        }

        for (var entity : entityList)
        {
            var name = columnData.getValue(entity, nameIndex);
            assertThat(name).isEqualTo(entity.getName());
            var id = columnData.getValue(entity, idIndex);
            assertThat(id).isEqualTo(entity.getId());
            var empty = columnData.getValue(entity, emptyIndex);
            assertThat(empty).isEqualTo(entity.getEmpty());
        }

        assertThat(columnData.valueMaps.get(0)).isNotEmpty();
        assertThat(columnData.valueMaps.get(1)).isNotEmpty();
        assertThat(columnData.valueMaps.get(2)).isNull();

        columnData.clearValues();
        assertThat(columnData.valueMaps.get(0)).isEmpty();
        assertThat(columnData.valueMaps.get(1)).isEmpty();

        columnData.removeColumn(definition.emptyColumn);
        assertThat(columnData.valueMaps).hasSize(2);

        columnData.removeAllColumns();
        assertThat(columnData.valueMaps).isEmpty();
    }
}
