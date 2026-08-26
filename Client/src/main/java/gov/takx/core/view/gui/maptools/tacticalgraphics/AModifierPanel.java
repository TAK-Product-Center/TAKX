package gov.takx.core.view.gui.maptools.tacticalgraphics;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Component;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class for panels that display a modifier name and editor component.
 *
 * @since 5.1
 * @param <T> The type of Component used to display and edit the modifier value
 */
public abstract class AModifierPanel<T extends Component> extends JPanel
{
    private static final CellConstraints CELL_CONSTRAINTS = new CellConstraints(3, 1);

    private final String modifierId;
    protected final String name;
    protected final T editor;
    protected final Supplier<String> valueSupplier;
    protected final BiConsumer<String, String> commitConsumer;
    private final Function<T, String> valueExtractor;

    /**
     * @param modifierId The modifier id
     * @param name The modifier name, or the field name if the modifier has multiple fields
     * @param editor The component that is used to modify the value
     * @param valueSupplier The supplier that retrieves the currently persisted modifier value. If the modifier has
     *                      multiple fields, the supplier should return the substring for the field displayed in this panel
     * @param commitConsumer Consumer that handles request to persist the editor's value
     * @param valueExtractor Function that can extract the editor's value
     */
    protected AModifierPanel(@Nonnull String modifierId, @Nonnull String name, @Nonnull T editor,
                             @Nonnull Supplier<String> valueSupplier,
                             @Nonnull BiConsumer<String, String> commitConsumer,
                             @Nonnull Function<T, String> valueExtractor)
    {
        this.modifierId = modifierId;
        this.name = name;
        this.editor = editor;
        this.valueSupplier = valueSupplier;
        this.commitConsumer = commitConsumer;
        this.valueExtractor = valueExtractor;

        initComponents();
        nameLabel.setText(name);
        add(editor, CELL_CONSTRAINTS);
    }

    /**
     * @param enabled true if this component and its children should be enabled, false otherwise
     * @since 5.2
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        setModifierComponentEnabled(enabled);
    }

    /**
     * Refreshes the editor value with the persisted value.
     */
    protected abstract void refresh();

    /**
     * @param enabled boolean indicating if the modifier editor component should be enabled
     * @since 5.2
     */
    protected abstract void setModifierComponentEnabled(boolean enabled);

    /**
     * @return The modifier's id
     */
    public String getModifierId()
    {
        return modifierId;
    }

    /**
     * Persists the editor value.
     */
    public void commit()
    {
        commitConsumer.accept(modifierId, getCurrentValue());
    }

    /**
     * @return Current editor value.
     * @since 5.5
     */
    @Nullable
    protected String getCurrentValue()
    {
        return valueExtractor.apply(editor);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        nameLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $lcgap, 130dlu",
                "fill:default"));

        //---- nameLabel ----
        nameLabel.setText("name");
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel nameLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
