package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nonnull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

/**
 * Panel that displays a slider with custom locations for the min, max and current slider values to reduce the amount of
 * vertical space required to display the component. The min and max values will be displayed at each end of the slider
 * bar and the current value will be displayed above it.
 *
 * @since 5.0
 */
public class SliderControlPanel extends JPanel
{
    /**
     * @param min The min slider value
     * @param max The max slider value
     * @param changeListener A listener that will be notified whenever the slider value changes
     */
    public SliderControlPanel(int min, int max, @Nonnull ChangeListener changeListener)
    {
        initComponents();

        slider.setMinimum(min);
        slider.setMaximum(max);

        slider.setMajorTickSpacing(max - min);
        minLabel.setText(String.valueOf(min));
        maxLabel.setText(String.valueOf(max));

        slider.addChangeListener(changeListener);

        slider.addChangeListener(e -> valueLabel.setText(String.valueOf(slider.getValue())));
    }

    /**
     * Updates the slider value and value label
     *
     * @param value The value that should be displayed by the slider and the value label
     */
    public void setValue(int value)
    {
        slider.setValue(value);
        valueLabel.setText(String.valueOf(value));
    }

    /**
     * @return The current slider value
     */
    public int getValue()
    {
        return slider.getValue();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        valueLabel = new JLabel();
        minLabel = new JLabel();
        slider = new JSlider();
        maxLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setLayout(new FormLayout(
                "3*(default)",
                "2*(default)"));

        //---- valueLabel ----
        valueLabel.setText("text");
        valueLabel.setFont(valueLabel.getFont().deriveFont(valueLabel.getFont().getSize() - 1f));
        add(valueLabel, cc.xy(2, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- minLabel ----
        minLabel.setText("text");
        add(minLabel, cc.xy(1, 2));
        add(slider, cc.xy(2, 2));

        //---- maxLabel ----
        maxLabel.setText("text");
        add(maxLabel, cc.xy(3, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel valueLabel;
    private JLabel minLabel;
    private JSlider slider;
    private JLabel maxLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
