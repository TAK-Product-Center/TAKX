package gov.takx.core.view.gui.maptools.tacticalgraphics;

import jakarta.annotation.Nonnull;

import javax.swing.JTextField;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Displays an editor for modifiers with text values.
 * 
 * @since 5.1
 */
public class TextModifierPanel extends ATextModifierPanel<JTextField>
{
    // Indicates that this panel is displaying a single field for a multi-field modifier
    private final boolean multiFieldModifier;

    /**
     * @param modifierId The modifier id
     * @param name The modifier name, or the field name if the modifier has multiple fields
     * @param multiFieldModifier Pass {code true} if the modifier has multiple fields 
     * @param valueBuilder Function that takes the editor value and generates the full modifier String. The function will
     *                     handle multi-field modifiers as needed.
     * @param valueSupplier The supplier that retrieves the currently persisted modifier value. If the modifier has
     *                      multiple fields, the supplier should return the substring for the field displayed in this panel
     * @param valueChangeListener Listener that's notified when the editor value changes
     * @param commitConsumer Consumer that handles request to persist the editor's value. Pass the modifier id and
     *                       editor value to the consumer when the value should be committed.
     */
    public TextModifierPanel(@Nonnull String modifierId, @Nonnull String name, boolean multiFieldModifier,
                             @Nonnull Function<String, String> valueBuilder, @Nonnull Supplier<String> valueSupplier,
                             @Nonnull Consumer<String> valueChangeListener, BiConsumer<String, String> commitConsumer)
    {
        super(modifierId, name, new JTextField(), valueBuilder, valueSupplier, valueChangeListener, commitConsumer);
        this.multiFieldModifier = multiFieldModifier;
    }

    @Override
    void notifyListener(@Nonnull Consumer<String> valueChangeListener)
    {
        String value = editor.getText();

        if (multiFieldModifier)
        {
            value = value.replace(",", ""); // Remove the multi-field delimiter
            editor.setText(value);
        }

        if (!Objects.equals(valueSupplier.get(), value)) valueChangeListener.accept(value);
    }
}
