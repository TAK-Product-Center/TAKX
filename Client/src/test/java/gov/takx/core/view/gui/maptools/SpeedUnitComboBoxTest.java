package gov.takx.core.view.gui.maptools;

import gov.takx.platform.utils.units.SpeedUnits;
import org.junit.jupiter.api.Test;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.6
 */
public class SpeedUnitComboBoxTest
{
    @Test
    public void testConversions()
    {
        double testValue = 11.15;
        JSpinner valueField = new JSpinner(new SpinnerNumberModel(0d, -100d, 100d, .01d));
        SpeedUnitComboBox unitField = new SpeedUnitComboBox();
        unitField.setSpinner(valueField);
        unitField.setSelectedItem(SpeedUnits.MPS);
        valueField.setValue(testValue);
        unitField.setSelectedItem(SpeedUnits.MPH);
        assertThat(valueField.getValue()).isEqualTo(SpeedUnits.MPH.getMultiplier() * testValue);
        unitField.setSelectedItem(SpeedUnits.KMPH);
        assertThat(valueField.getValue()).isEqualTo(SpeedUnits.KMPH.getMultiplier() * testValue);
    }
}
