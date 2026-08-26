package gov.takx.core.view.gui.maptools.tacticalgraphics;

import jakarta.annotation.Nonnull;

import javax.swing.JTextField;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class for classes that use a JTextField modifier editor.
 *
 * @param <T> The editor component type
 * @since 5.1
 */
public abstract class ATextModifierPanel<T extends JTextField> extends AModifierPanel<T>
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
    public ATextModifierPanel(@Nonnull String modifierId, @Nonnull String name, T editor,
                              @Nonnull Function<String, String> valueBuilder, @Nonnull Supplier<String> valueSupplier,
                              @Nonnull Consumer<String> valueChangeListener, BiConsumer<String, String> commitConsumer)
    {
        super(modifierId, name, editor, valueSupplier, commitConsumer,
                textField -> valueBuilder.apply(textField.getText())); // The text field will already be sanitized when this is called

        editor.setText(valueSupplier.get());

        // Notify the listener when the user hits Enter
        editor.addActionListener(e -> notifyListener(valueChangeListener));

        // Notify the listener when the user navigates away from the editor without pressing Enter
        editor.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                notifyListener(valueChangeListener);
            }
        });
    }

    /**
     * @param valueChangeListener The listener to notify when the editor value changes
     */
    abstract void notifyListener(@Nonnull Consumer<String> valueChangeListener);

    @Override
    protected void setModifierComponentEnabled(boolean enabled)
    {
        editor.setEnabled(enabled);
    }

    @Override
    protected void refresh()
    {
        editor.setText(valueSupplier.get());
    }
}
