package gov.takx.core.view.gui.maptools;

import gov.takx.commons.swing.JComboBoxExt;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JSpinner;

import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A swing combo box that is prefilled with the available {@link SpeedUnits} values and can be linked to a {@link JSpinner}
 * that it will apply the proper conversions to when the user changes the unit selection.
 * 
 * @since 5.6
 */
public class SpeedUnitComboBox extends JComboBoxExt<SpeedUnits>
{
    @Nullable
    private Supplier<Double> supplier;
    @Nullable
    private Consumer<Double> consumer;

    /**
     * Construct a new instance with no value and no spinner component set.
     */
    public SpeedUnitComboBox()
    {
        super(SpeedUnits.values());
        setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((SpeedUnits) value).getAbbreviation());
                return this;
            }
        });
    }

    /**
     * @param spinner The spinner associated with this combo box
     * @throws IllegalStateException if the spinner has already been set
     */
    public void setSpinner(@Nonnull JSpinner spinner)
    {
        setComponentAccessors(() -> (Double) spinner.getValue(), spinner::setValue);
    }

    /**
     * @param supplier The component value supplier
     * @param consumer The component value consumer
     * @throws IllegalStateException if the supplier/consumer have already been set
     */
    public void setComponentAccessors(@Nonnull Supplier<Double> supplier, @Nonnull Consumer<Double> consumer)
    {
        if (this.supplier != null || this.consumer != null) throw new IllegalStateException("component accessors already set");
        this.supplier = supplier;
        this.consumer = consumer;
    }

    /**
     * Set the selected value and update spinner with conversion.
     * 
     * @param object the item to select
     */
    @Override
    public void setSelectedItem(@Nullable Object object)
    {
        if (supplier == null || consumer == null) throw new IllegalStateException("component accessors must be set");

        final var previousUnit = getTypedSelectedItem();
        super.setSelectedItem(object);

        // Convert spinner value to new unit of measure
        if (previousUnit != null && object != null)
        {
            consumer.accept((supplier.get()) * ((SpeedUnits) object).getMultiplier() / previousUnit.getMultiplier());
        }
    }

    /**
     * @param mps The mps value to convert to the selected unit and set on the associated component
     */
    public void setValueMetersPerSecond(double mps)
    {
        //noinspection DataFlowIssue
        consumer.accept(getTypedSelectedItem().getMultiplier() * mps);
    }

    /**
     * @return The associated component's value, converted to mps
     * @see #setSpinner(JSpinner)
     * @see #setComponentAccessors(Supplier, Consumer)
     */
    public double getValueMetersPerSecond()
    {
        //noinspection DataFlowIssue
        return supplier.get() / getTypedSelectedItem().getMultiplier();
    }
}
