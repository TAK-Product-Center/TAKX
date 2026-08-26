package gov.takx.commons.swing;

import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import java.text.NumberFormat;
import java.util.function.Consumer;

/**
 * Text field that displays double values constrained by upper and lower bounds. Invalid values will be rejected before
 * they are committed with the enter key or loss of focus.
 * <p>
 * Decimal values will be rounded to the smallest displayable value, e.g. 0.5555 will be displayed as 0.6 and 0.000135
 * will be displayed as 0.0001. The number of decimal places is bounded by {@link #maxDecimalDigits}. If the number of
 * decimal places required to show a value exceeds the max, 0 will be displayed.
 *
 * @since 5.5.1
 */
public class BoundedDoubleTextField extends JFormattedTextField
{
    private final NumberFormatter formatter;
    private final NumberFormat format;

    private int maxDecimalDigits = 1; // default

    public BoundedDoubleTextField()
    {
        formatter = getNumberFormatter();
        format = (NumberFormat) formatter.getFormat();

        setFormatterFactory(new DefaultFormatterFactory(formatter));
        setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
    }

    /**
     * @param consumer The consumer that will be notified when the value changes
     */
    public void setChangeListener(@Nonnull Consumer<Double> consumer)
    {
        addPropertyChangeListener("value", evt -> consumer.accept((Double) evt.getNewValue()));
    }

    /**
     * @param min The min value, or {@code null} if not bounded
     * @param max The max value, or {@code null} if not bounded
     */
    public void setBounds(@Nullable Double min, @Nullable Double max)
    {
        formatter.setMinimum(min);
        formatter.setMaximum(max);
    }

    /**
     * @param digits the max allowable decimal digits
     */
    public void setMaxDecimalDigits(int digits)
    {
        if (digits < 0) throw new IllegalArgumentException("Digits must not be less than 0, but was " + digits);

        this.maxDecimalDigits = digits;
    }

    /**
     * @return The current value as a double, or zero if no value is present
     */
    public double getDoubleValue()
    {
        final Double value = (Double) getValue();
        if (value != null) return value; // might be null when initialized by JFD via setText()

        final String text = getText();
        return StringUtils.isBlank(text) ? 0 : Double.parseDouble(getText());
    }

    /**
     * Overridden to update the decimal formatter with the number of decimal places required to show a meaningful value.
     * <p>
     * {@inheritDoc}
     *
     * @param value Current value to display
     */
    @Override
    public void setValue(Object value)
    {
        final var doubleValue = (Double) value;

        // Calculate the min number of decimal digits required to display a value
        final var decimalDigits = doubleValue >= 0.1 ? 1 : (int) Math.ceil(-Math.log10(doubleValue));
        format.setMaximumFractionDigits(Math.min(decimalDigits, maxDecimalDigits));

        super.setValue(value);
    }

    /**
     * @return A new double formatter
     */
    @Nonnull
    private NumberFormatter getNumberFormatter()
    {
        final var doubleFormat = NumberFormat.getNumberInstance();
        doubleFormat.setGroupingUsed(false);

        final var formatter = new NumberFormatter(doubleFormat);
        formatter.setValueClass(Double.class);
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(false);

        return formatter;
    }
}
