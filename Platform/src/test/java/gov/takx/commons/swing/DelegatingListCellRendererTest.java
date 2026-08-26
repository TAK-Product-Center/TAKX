package gov.takx.commons.swing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JList;

/**
 * @since 5.7
 */
public class DelegatingListCellRendererTest
{
    @SuppressWarnings("unchecked")
    @Test
    public void test()
    {
        DelegatingListCellRenderer<MyObject> renderer = new DelegatingListCellRenderer<>(MyObject::name);
        var myObject = new MyObject("Some Name");
        JList<MyObject> list = new JList<>();
        var rendered = (DelegatingListCellRenderer<MyObject>) renderer.getListCellRendererComponent(list, myObject, -1, false, false);
        Assertions.assertThat(rendered.getText()).isEqualTo(myObject.name());
    }

    record MyObject(String name)
    {
    }
}
