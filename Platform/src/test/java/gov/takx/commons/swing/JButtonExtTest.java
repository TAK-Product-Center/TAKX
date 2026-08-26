package gov.takx.commons.swing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JButtonExtTest
{

    @Test
    public void testDisabledToolTipText()
    {
        JButtonExt button = new JButtonExt();

        // test setting tooltip
        assertThat(button.getToolTipText()).isNull();
        button.setToolTipText("tooltip");
        assertThat(button.getToolTipText()).isEqualTo("tooltip");
        button.setToolTipText(null);

        // test setting disabled tooltip
        button.setDisabledToolTipText("disabled");
        button.setEnabled(false);
        assertThat(button.getToolTipText()).isEqualTo("disabled");
        button.setEnabled(true);
        assertThat(button.getToolTipText()).isNull();

        // test changing disabled text while disabled
        button.setEnabled(false);
        button.setDisabledToolTipText("disabled2");
        assertThat(button.getToolTipText()).isEqualTo("disabled2");

        // test setting normal while disabled
        button.setToolTipText("tooltip2");
        assertThat(button.getToolTipText()).isEqualTo("disabled2");

        // test enabling after live change
        button.setEnabled(true);
        assertThat(button.getToolTipText()).isEqualTo("tooltip2");

        // test removing while disabled
        button.setEnabled(false);
        assertThat(button.getToolTipText()).isEqualTo("disabled2");
        button.setDisabledToolTipText(null);
        assertThat(button.getToolTipText()).isEqualTo("tooltip2");

        // test disabling after setting
        button.setToolTipText("tooltip");
        button.setDisabledToolTipText(null);
        assertThat(button.getToolTipText()).isEqualTo("tooltip");
    }
}
