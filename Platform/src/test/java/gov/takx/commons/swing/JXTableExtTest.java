package gov.takx.commons.swing;

import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableModel;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

/**
 * @since 5.8
 */
public class JXTableExtTest
{
    @Test
    public void testTextLines()
    {
        var table = new JXTableExt();
        var numLines = 4;
        table.setTextLinesForRowHeight(numLines);
        var metrics = table.getFontMetrics(table.getFont());
        var expectedHeight = (metrics.getHeight() * numLines) + (metrics.getDescent() * 2); // each row + descent sized padding above and below

        Assertions.assertThat(table.getRowHeight()).isEqualTo(expectedHeight);
        Assertions.assertThat(table.getTextLinesForRowHeight()).isEqualTo(numLines);
    }

    @Test
    public void testToolTipProvider()
    {
        var table = new JXTableExt();
        var model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[]{"A", "B", "C"});
        model.addRow(new Object[]{"row 0 data A", "row 0 data B", "row 0 data C"});
        table.setModel(model);
        var provider = new CustomToolTipProvider(model);
        table.setToolTipProvider(provider);

        table.setPreferredSize(new Dimension(200, 200));

        var mouseEvent = new MouseEvent(table, 0, 0, 0, 0, 0, 1, true, 0);
        var tooltip = table.getToolTipText(mouseEvent);
        Assertions.assertThat(tooltip).isEqualTo(provider.getToolTipText(0, 0));

        mouseEvent = new MouseEvent(table, 0, 0, 0, 100, 0, 1, true, 0);
        tooltip = table.getToolTipText(mouseEvent);
        Assertions.assertThat(tooltip).isEqualTo(provider.getToolTipText(0, 1));

        mouseEvent = new MouseEvent(table, 0, 0, 0, 200, 0, 1, true, 0);
        tooltip = table.getToolTipText(mouseEvent);
        Assertions.assertThat(tooltip).isEqualTo(provider.getToolTipText(0, 2));
    }

    private static class CustomToolTipProvider implements JXTableExt.ToolTipProvider
    {
        private final DefaultTableModel model;

        public CustomToolTipProvider(DefaultTableModel model)
        {
            this.model = model;
        }

        @Override
        public @Nullable String getToolTipText(int row, int column)
        {
            return model.getValueAt(row, column) + " tooltip";
        }
    }
}
