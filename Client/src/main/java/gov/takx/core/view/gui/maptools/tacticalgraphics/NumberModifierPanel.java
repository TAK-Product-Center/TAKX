package gov.takx.core.view.gui.maptools.tacticalgraphics;

import jakarta.annotation.Nonnull;

import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Displays a JFormattedTextField for modifiers with number values.
 *
 * @since 5.1
 */
public class NumberModifierPanel extends ATextModifierPanel<JFormattedTextField>
{
    /**
     * @param modifierId The modifier id
     * @param name The modifier name, or the field name if the modifier has multiple fields
     * @param valueBuilder Function that takes the editor value and generates the full modifier String. The function will
     *                     handle multi-field modifiers as needed.
     * @param valueSupplier The supplier that retrieves the currently persisted modifier value. If the modifier has
     *                      multiple fields, the supplier should return the substring for the field displayed in this panel
     * @param valueChangeListener Listener that's notified when the editor value changes
     * @param commitConsumer Consumer that handles request to persist the editor's value. Pass the modifier id and
     *                       editor value to the consumer when the value should be committed.
     */
    public NumberModifierPanel(@Nonnull String modifierId, @Nonnull String name,
                               @Nonnull Function<String, String> valueBuilder, @Nonnull Supplier<String> valueSupplier,
                               @Nonnull Consumer<String> valueChangeListener, BiConsumer<String, String> commitConsumer)
    {
        super(modifierId, name, new JFormattedTextField(makeFormatter()), valueBuilder, valueSupplier, valueChangeListener, commitConsumer);
    }

    @Override
    void notifyListener(@Nonnull Consumer<String> valueChangeListener)
    {
        // Verify that the data is valid before notifying the consumer. Invalid values will automatically be reverted
        // in the text field. The NumberFormatter is configured not to allow commas, which are used as a delimiter in
        // modifiers with multiple fields.
        if (editor.isEditValid())
        {
            final String value = editor.getText();
            if (!Objects.equals(valueSupplier.get(), value)) valueChangeListener.accept(value);
        }
    }

    /**
     * @return A formatter that will limit the text field to numbers (without commas)
     */
    private static NumberFormatter makeFormatter()
    {
        final NumberFormat format = NumberFormat.getNumberInstance();
        format.setGroupingUsed(false); // no commas

        final NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Double.class);
        formatter.setAllowsInvalid(true); // required to allow the user to delete entire values

        return formatter;
    }
}
