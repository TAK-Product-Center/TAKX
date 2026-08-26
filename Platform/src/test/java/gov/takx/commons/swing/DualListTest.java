package gov.takx.commons.swing;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultListModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.6
 */
public class DualListTest
{
    @Test
    public void testComponent()
    {
        DualList<MyObject> dualList = new DualList<>();
        dualList.setSelectedText("a");
        assertThat(dualList.getSelectedText()).isEqualTo("a");
        dualList.setUnselectedText("b");
        assertThat(dualList.getUnselectedText()).isEqualTo("b");
        dualList.setRowHeight(5);
        assertThat(dualList.getSelectedTable().getRowHeight()).isEqualTo(5);
        assertThat(dualList.getUnselectedTable().getRowHeight()).isEqualTo(5);
        assertThat(dualList.getRowHeight()).isEqualTo(5);
    }

    @Test
    public void testAddModelItems()
    {
        DualList<MyObject> dualList = new DualList<>();
        List<MyObject> items = List.of(new MyObject(), new MyObject());
        ((DefaultListModel<MyObject>) dualList.getModel()).addAll(items);
        List<MyObject> unselected = dualList.getUnselectedItems();
        assertThat(unselected).containsExactlyElementsOf(items);
        assertThat(dualList.getUnselectedTable().getRowCount()).isEqualTo(items.size());
    }

    @Test
    public void testSelectItems()
    {
        DualList<MyObject> dualList = new DualList<>();
        List<MyObject> items = List.of(new MyObject(), new MyObject());
        ((DefaultListModel<MyObject>) dualList.getModel()).addAll(items);
        dualList.setSelectedItems(List.of(items.get(0)));
        assertThat(dualList.getUnselectedItems()).containsExactly(items.get(1));
        assertThat(dualList.getSelectedItems()).containsExactly(items.get(0));
        assertThat(dualList.getUnselectedTable().getRowCount()).isEqualTo(1);
        assertThat(dualList.getSelectedTable().getRowCount()).isEqualTo(1);
    }

    @Test
    public void testSetModel()
    {
        DualList<MyObject> dualList = new DualList<>();
        List<MyObject> items = List.of(new MyObject(), new MyObject());
        DefaultListModel<MyObject> myModel = new DefaultListModel<>();
        myModel.addAll(items);
        dualList.setModel(myModel);
        List<MyObject> unselected = dualList.getUnselectedItems();
        assertThat(unselected).containsExactlyElementsOf(items);
        assertThat(dualList.getUnselectedTable().getRowCount()).isEqualTo(items.size());
    }

    private static class MyObject
    {
        private MyObject()
        {
        }
    }
}
