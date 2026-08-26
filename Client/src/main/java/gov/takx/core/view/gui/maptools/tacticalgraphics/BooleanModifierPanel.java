package gov.takx.core.view.gui.maptools.tacticalgraphics;

import gov.takx.commons.swing.TriStateJCheckBox;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Displays an editor for modifiers with boolean values.
 *
 * @since 5.5
 */
public class BooleanModifierPanel extends AModifierPanel<TriStateJCheckBox>
{
    /**
     * @param modifierId     The modifier id
     * @param name           The modifier name, or the field name if the modifier has multiple fields
     * @param valueSupplier  The supplier that retrieves the currently persisted modifier value. If the modifier has
     *                       multiple fields, the supplier should return the substring for the field displayed in this panel
     * @param commitConsumer Consumer that handles request to persist the editor's value
     */
    protected BooleanModifierPanel(@Nonnull String modifierId, @Nonnull String name,
                                   @Nonnull Supplier<String> valueSupplier,
                                   @Nonnull Consumer<String> valueChangeListener,
                                   @Nonnull BiConsumer<String, String> commitConsumer)
    {
        super(modifierId, name, new TriStateJCheckBox(true, true), valueSupplier, commitConsumer, c -> toString(c.getState()));
        refresh();
        editor.addActionListener(e -> valueChangeListener.accept(toString(editor.getState())));
    }

    @Override
    protected void refresh()
    {
        editor.setState(toState(valueSupplier.get()));
    }

    @Override
    protected void setModifierComponentEnabled(boolean enabled)
    {
        editor.setEnabled(enabled);
    }

    @Nonnull
    private static String toString(@Nullable TriStateJCheckBox.State value)
    {
        if (value == null) return "";
        return switch (value)
        {
            case SELECTED -> "1";
            case NOT_SELECTED -> "0";
            case INDETERMINATE -> "";
        };
    }

    @Nonnull
    private static TriStateJCheckBox.State toState(@Nullable String value)
    {
        if ("0".equals(value)) return TriStateJCheckBox.State.NOT_SELECTED;
        if ("1".equals(value)) return TriStateJCheckBox.State.SELECTED;
        return TriStateJCheckBox.State.INDETERMINATE;
    }
}
