package gov.takx.commons.swing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;

/**
 * @since 5.7
 */
public class LineBorderExtTest
{
    @Test
    public void test()
    {
        var border = new LineBorderExt();
        border.setLineColor(Color.YELLOW);
        border.setThickness(11);
        border.setRoundedCorners(true);
        Assertions.assertThat(border.getLineColor()).isEqualTo(Color.YELLOW);
        Assertions.assertThat(border.getThickness()).isEqualTo(11);
        Assertions.assertThat(border.getRoundedCorners()).isEqualTo(true);
    }
}
