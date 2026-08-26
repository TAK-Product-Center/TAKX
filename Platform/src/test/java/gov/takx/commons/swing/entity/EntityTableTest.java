package gov.takx.commons.swing.entity;

import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.test.utils.SwingTestUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.event.TableModelEvent;

import java.util.ArrayList;

import static gov.takx.commons.swing.entity.TestEntityDefinition.CUSTOM_NAME_COLUMN_NAME;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @since 5.7
 */
public class EntityTableTest
{
    @Test
    void testModel()
    {
        var definition = new TestEntityDefinition();
        var model = new EntityTableModel<>(definition);
        assertThat(model.getColumnCount()).isEqualTo(definition.getColumns().size());
        var table = new CommonEntityTable<TestEntity>();

        table.setModel(model);
        assertThatThrownBy(() -> table.setModel(model)).isInstanceOf(UnsupportedOperationException.class);

        assertThat(table.requireColumn(definition.getNameColumn()).getTitle()).isEqualTo(CUSTOM_NAME_COLUMN_NAME);

        // CommonEntityTable adds an extra column
        assertThat(table.getColumns(true).size()).isEqualTo(definition.getColumns().size() + 1);

        var count = 3;
        var entityList = new ArrayList<TestEntity>();
        for (var i = 0; i < count; i++)
        {
            var e = new TestEntity("Name " + i, i);
            model.addEntity(e);
            entityList.add(e);
        }

        assertThat(model.indexOf(entityList.get(0))).isEqualTo(0);

        assertThat(model.getRowCount()).isEqualTo(count);
        assertThat(table.getRowCount()).isEqualTo(count);

        model.clear();
        assertThat(model.getRowCount()).isEqualTo(0);
        assertThat(table.getRowCount()).isEqualTo(0);

        model.setEntities(entityList);
        assertThat(model.getRowCount()).isEqualTo(entityList.size());
        assertThat(table.getRowCount()).isEqualTo(entityList.size());

        var removed = entityList.remove(count - 1);
        model.removeEntity(removed);
        assertThat(model.getRowCount()).isEqualTo(entityList.size());
        assertThat(table.getRowCount()).isEqualTo(entityList.size());

        // Cache refresh
        var entity = entityList.get(0);
        assertThat(model.getValueAt(0, model.getColumnIndex(definition.getSummaryColumn()))).asString().contains(entity.getDetails());
        entity.setDetails("New Details");
        assertThat(model.getValueAt(0, model.getColumnIndex(definition.getSummaryColumn()))).asString().doesNotContain(entity.getDetails());
        model.fireTableColumnUpdated(definition.getSummaryColumn());
        assertThat(model.getValueAt(0, model.getColumnIndex(definition.getSummaryColumn()))).asString().contains(entity.getDetails());

        var columnData = new EntityColumnData<TestEntity>();
        definition.getColumns().forEach(columnData::addColumn);
        assertThat(columnData.getColumnCount()).isEqualTo(definition.getColumns().size());

        // Tooltips
        table.requireColumn(definition.getNameColumn()).setVisible(true);
        table.requireColumn(definition.getSummaryColumn()).setVisible(true);
        var checkRow = 0;
        var nameViewCol = table.convertColumnIndexToView(model.getColumnIndex(definition.getNameColumn()));
        var summaryViewCol = table.convertColumnIndexToView(model.getColumnIndex(definition.getSummaryColumn()));
        assertThat(table.getToolTipText(checkRow, nameViewCol)).isEqualTo(model.getRow(checkRow).getName());
        assertThat(table.getToolTipText(checkRow, summaryViewCol)).isEqualTo("Summary");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testAutoHide()
    {
        var definition = new TestEntityDefinition();
        var model = new EntityTableModel<>(definition);
        var table = new EntityTable<TestEntity>();

        table.setAutoHideColumns(true, model.getColumnIndex(definition.emptyColumn));
        table.setModel(model);
        assertThat(table.getColumnCount()).isEqualTo(definition.getColumns().size() - 1);
        assertThat(table.getColumn(definition.emptyColumn).isVisible()).isFalse();

        var e = new TestEntity("Name", 0);
        e.setEmpty("");
        model.addEntity(e);

        assertThat(table.getColumn(definition.emptyColumn).isVisible()).isTrue();
    }

    @Test
    void testListeners()
    {
        var definition = new TestEntityDefinition();
        definition.addPropertyChangeMapping(TestEntity.NAME_PROP, definition.getNameColumn());
        var model = new EntityTableModel<>(definition);
        var e = new TestEntity("Name", 0);
        model.addEntity(e);
        var eventHolder = new MutableObject<TableModelEvent>();
        model.addTableModelListener(eventHolder::setValue);
        e.setName("New Name");
        SwingTestUtils.waitForEdtEvents();
        assertThat(eventHolder.get()).isNotNull();
    }

    @Test
    void testHeader()
    {
        var definition = new TestEntityDefinition();
        var model = new EntityTableModel<>(definition);
        var table = new EntityTable<TestEntity>();
        table.setModel(model);
        var entityTableHeader = table.getTableHeader();
        var button = new JButton("Test");
        entityTableHeader.addActionButton(button);
        var sortMenu = entityTableHeader.sortMenu;
        sortMenu.update();
        var definitionColumns = definition.getColumns().size();
        var sortableDefinitionColumns = definition.getColumns().stream().filter(column -> !column.name().isEmpty() && column.sortable()).count();
        assertThat(sortMenu.menuItemMap.size()).isEqualTo(definitionColumns + 1); // columns + Default
        assertThat(sortMenu.getSortableMenuItems().size()).isEqualTo(sortableDefinitionColumns); // columns
        assertThat(sortMenu.menuItemMap.get(definition.emptyColumn).isVisible()).isFalse(); // Empty column was flagged as not sortable
        assertThat(sortMenu.menuItemMap.get(definition.getSummaryColumn()).isVisible()).isFalse(); // Summary column was flagged as not sortable
        sortMenu.setDefaultSortVisible(true);
        assertThat(sortMenu.getSortableMenuItems().size()).isEqualTo(sortableDefinitionColumns + 1); // columns + Default
        sortMenu.sort(definition.getNameColumn());
        assertThat(table.getSortedColumn()).isEqualTo(table.getColumn(definition.getNameColumn()));
        assertThat(table.requireColumn(definition.emptyColumn).getHeaderValue()).isEqualTo(definition.emptyColumn.header());

        // Test props dont throw anything
        table.setCompactHeaderVisible(false);
        table.setStandardHeaderVisible(false);
        table.setActionButtonPanelVisible(false);
        assertThat(table.isCompactHeaderVisible()).isFalse();
        assertThat(table.isStandardHeaderVisible()).isFalse();
        assertThat(table.isActionButtonPanelVisible()).isFalse();
        table.setCompactHeaderVisible(true);
        table.setStandardHeaderVisible(true);
        table.setActionButtonPanelVisible(true);
        assertThat(table.isCompactHeaderVisible()).isTrue();
        assertThat(table.isStandardHeaderVisible()).isTrue();
        assertThat(table.isActionButtonPanelVisible()).isTrue();
    }
}
