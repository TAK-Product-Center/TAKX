package gov.takx.core.view.gui.maptools;

import com.atakmap.coremap.conversions.AngleUtilities;
import gov.takx.platform.utils.units.BearingUnits;
import org.junit.jupiter.api.Test;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.6
 */
public class BearingUnitComboBoxTest
{
    @Test
    public void testConversions()
    {
        double testValue = 11.15;
        SpinnerNumberModel model = new SpinnerNumberModel(0d, -100d, 100d, .01d);
        JSpinner valueField = new JSpinner(model);
        BearingUnitComboBox unitField = new BearingUnitComboBox();
        unitField.setSpinner(valueField);
        unitField.setSelectedItem(BearingUnits.DEGREE);
        valueField.setValue(testValue);
        unitField.setSelectedItem(BearingUnits.RADIAN);
        assertThat(valueField.getValue()).isEqualTo(AngleUtilities.convert(testValue, BearingUnits.DEGREE.getAngle(), BearingUnits.RADIAN.getAngle()));
        unitField.setSelectedItem(BearingUnits.MIL);
        assertThat(valueField.getValue()).isEqualTo(AngleUtilities.convert(testValue, BearingUnits.DEGREE.getAngle(), BearingUnits.MIL.getAngle()));
        unitField.setSelectedItem(BearingUnits.DEGREE);
        assertThat(valueField.getValue()).isEqualTo(AngleUtilities.convert(testValue, BearingUnits.DEGREE.getAngle(), BearingUnits.DEGREE.getAngle()));
        unitField.setSelectedItem(BearingUnits.MIL);
        testValue = 90;
        unitField.setValueDegrees(testValue);
        assertThat(valueField.getValue()).isEqualTo(AngleUtilities.convert(testValue, BearingUnits.DEGREE.getAngle(), BearingUnits.MIL.getAngle()));
    }

    @Test
    public void testRanges()
    {
        SpinnerNumberModel model = new SpinnerNumberModel(0d, -100d, 100d, .01d);
        JSpinner valueField = new JSpinner(model);
        BearingUnitComboBox unitField = new BearingUnitComboBox();
        unitField.setSpinner(valueField);
        unitField.setSelectedItem(BearingUnits.RADIAN);
        assertThat(model.getMaximum()).isEqualTo(Math.PI * 2);
        unitField.setSelectedItem(BearingUnits.MIL);
        assertThat(model.getMaximum()).isEqualTo(6400d);
        unitField.setSelectedItem(BearingUnits.DEGREE);
        assertThat(model.getMaximum()).isEqualTo(360d);
    }
}
