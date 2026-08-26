package gov.takx.core.view.gui.components;

import com.formdev.flatlaf.FlatLaf;
import gov.takx.core.view.gui.themes.TakDarkTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import java.lang.invoke.MethodHandles;

/**
 * A slider implementation that consists of two points and a selected range between them.
 *
 * @since 1.43
 */
public class RangedSlider extends JSlider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public RangedSlider()
    {
        super();
    }

    public RangedSlider(int orientation)
    {
        super(orientation);
    }

    public RangedSlider(int min, int max)
    {
        super(min, max);
    }

    /**
     * Overrides the superclass method to install the UI delegate to draw two
     * thumbs.
     */
    @Override
    public void updateUI()
    {
        setUI(new RangedSliderUI());
        // Update UI for slider labels.  This must be called after updating the
        // UI of the slider.  Refer to JSlider.updateUI().
        updateLabelUIs();
    }

    /**
     * @return the lower value.
     */
    @Override
    public int getValue()
    {
        return super.getValue();
    }

    /**
     * Sets the lower value in the range.
     */
    @Override
    public void setValue(int value)
    {
        int oldValue = getValue();
        if (oldValue == value)
        {
            return;
        }

        // Compute new value and extent to maintain upper value.
        int oldExtent = getExtent();
        int newValue = Math.min(Math.max(getMinimum(), value), oldValue + oldExtent);
        int newExtent = oldExtent + oldValue - newValue;

        // Set new value and extent, and fire a single change event.
        getModel().setRangeProperties(newValue, newExtent, getMinimum(),
                getMaximum(), getValueIsAdjusting());
    }

    /**
     * Delegates to setValue.
     *
     * @param value The lower value to set.
     */
    public void setLowerValue(int value)
    {
        this.setValue(value);
    }

    /**
     * Delegates to getValue.
     *
     * @return the lower value.
     */
    public int getLowerValue()
    {
        return this.getValue();
    }

    /**
     * Returns the upper value in the range.
     */
    public int getUpperValue()
    {
        return getValue() + getExtent();
    }

    /**
     * Sets the upper value in the range.
     */
    public void setUpperValue(int value)
    {
        // Compute new extent.
        int lowerValue = getValue();
        int newExtent = Math.min(Math.max(0, value - lowerValue), getMaximum() - lowerValue);

        // Set extent to set upper value.
        setExtent(newExtent);
    }

    public static void main(String[] args)
    {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        SwingUtilities.invokeLater(() -> {
            FlatLaf.setup(new TakDarkTheme());

            JFrame frame = new JFrame("multi-slider");
            final RangedSlider rangedSlider = new RangedSlider();
            rangedSlider.setMinimum(0);
            rangedSlider.setMaximum(10);
            rangedSlider.setLowerValue(1);
            rangedSlider.setUpperValue(9);
            rangedSlider.setMajorTickSpacing(10);
            rangedSlider.setMinorTickSpacing(2);
            rangedSlider.setPaintTicks(true);
            rangedSlider.setPaintLabels(true);
            rangedSlider.setPaintTrack(true);
            rangedSlider.addChangeListener(e -> {
                if (logger.isTraceEnabled()) logger.trace("is adjusting : {}", rangedSlider.getValueIsAdjusting());
            });

            frame.add(rangedSlider);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
