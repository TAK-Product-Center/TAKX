package gov.takx.commons.swing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 5.7
 */
public class JSpinnerExtTest
{
    @Test
    public void testModel()
    {
        var spinner = new JSpinnerExt();

        spinner.setModel(new SpinnerNumberModel());
        assertThat(spinner.getNumberModel()).isSameAs(spinner.getModel());

        spinner.setModel(new SpinnerDateModel());

        assertThatThrownBy(spinner::getNumberModel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(spinner::getValueInt).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(spinner::getValueDouble).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(spinner::getValueFloat).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(spinner::getValueLong).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testNumberValues()
    {
        var spinner = new JSpinnerExt();

        Double doubleValue = 11.1d;
        spinner.setValue(doubleValue);
        assertThat(spinner.getValueNumber()).isSameAs(doubleValue);

        assertThat(spinner.getValueDouble()).isEqualTo(doubleValue);
        assertThat(spinner.getValueInt()).isEqualTo(doubleValue.intValue());
        assertThat(spinner.getValueLong()).isEqualTo(doubleValue.longValue());
        assertThat(spinner.getValueFloat()).isEqualTo(doubleValue.floatValue());

        var intValue = 22;
        spinner.setValueInt(intValue);
        assertThat(spinner.getValueDouble()).isEqualTo(intValue);
        assertThat(spinner.getValueLong()).isEqualTo(intValue);
        assertThat(spinner.getValueFloat()).isEqualTo((float) intValue);
    }

    @Test
    public void testFocusListener()
    {
        var spinner = new JSpinnerExt();
        FocusListener fl = new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                super.focusGained(e);
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                super.focusLost(e);
            }
        };
        spinner.addFocusListener(fl);

        var listeners = List.of(((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().getFocusListeners());
        Assertions.assertThat(listeners).contains(fl);
        spinner.removeFocusListener(fl);
        listeners = List.of(((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().getFocusListeners());
        Assertions.assertThat(listeners).doesNotContain(fl);
    }
}
