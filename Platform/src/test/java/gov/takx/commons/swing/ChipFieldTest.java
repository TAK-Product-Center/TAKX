package gov.takx.commons.swing;

import org.apache.commons.lang3.mutable.MutableObject;
import org.assertj.core.api.Assertions;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;

import java.util.List;

/**
 * @since 5.8
 */
public class ChipFieldTest
{
    @Test
    public void test()
    {
        var values = List.of(
                new MyEntity(new EmptyIcon(4, 4), "Item 0"),
                new MyEntity(new EmptyIcon(8, 8), "Item 1"));
        var chipField = new ChipField<MyEntity>();
        Assertions.assertThat(chipField.listPanel.getComponents()).hasSize(1); // Make sure empty chip is present
        Assertions.assertThat(chipField.isEditable()).isTrue(); // Editable by default
        chipField.setIconProvider(e -> e.icon);
        chipField.setTextProvider(e -> e.name);
        var model = chipField.getEditableModel();
        model.addAll(values);

        Assertions.assertThat(chipField.panels).hasSize(values.size()); // Panel for each value
        Assertions.assertThat(chipField.panels.get(1).label.getText()).isEqualTo(values.get(1).name); // label text is correct
        Assertions.assertThat(chipField.panels.get(1).label.getIcon()).isEqualTo(values.get(1).icon); // label icon is correct
        Assertions.assertThat(chipField.panels.get(1).item).isEqualTo(values.get(1)); // panel item is correct

        // Remove button and listener works
        var removed = new MutableObject<MyEntity>();
        chipField.addRemoveListener(event -> removed.setValue(event.getItem()));
        var panel = chipField.panels.get(0);
        panel.removeButton.doClick();
        Assertions.assertThat(removed.get()).isSameAs(values.get(0)); // Remove event for specific button
    }

    public static class MyEntity
    {
        final Icon icon;
        final String name;

        public MyEntity(Icon icon, String name)
        {
            this.icon = icon;
            this.name = name;
        }
    }
}
