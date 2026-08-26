package gov.takx.commons.swing.entity.common;

import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.TestEntity;
import gov.takx.commons.swing.entity.TestEntityDefinition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @since 5.8
 */
public class CommonEntityTableTest
{
    @Test
    void testRowHeight()
    {
        var definition = new TestEntityDefinition();
        var model = new EntityTableModel<>(definition);
        var table = new CommonEntityTable<TestEntity>();
        table.setModel(model);
        table.getActionsColumn().setVisible(true);
        Assertions.assertThat(table.getRowHeight()).isEqualTo(CommonEntityTable.ACTIONS_COLUMN_MIN_ROW_HEIGHT);
    }
}
