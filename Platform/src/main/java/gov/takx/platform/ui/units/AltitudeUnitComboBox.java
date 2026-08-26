package gov.takx.platform.ui.units;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.JComboBoxExt;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JSpinner;

import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A swing combo box that is prefilled with the available {@link AltitudeUnits} values and can be linked to a {@link JSpinner}
 * that it will apply proper conversions to when the user changes the unit selection.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class AltitudeUnitComboBox extends JComboBoxExt<AltitudeUnits>
{
    @Nullable
    private Supplier<Double> supplier;
    @Nullable
    private Consumer<Double> consumer;

    /**
     * Construct a new instance with no value and no spinner component set.
     */
    public AltitudeUnitComboBox()
    {
        super(AltitudeUnits.values());
        setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((AltitudeUnits) value).getAbbreviation());
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
     * @throws IllegalStateException if component accessors have not been set
     */
    @Override
    public void setSelectedItem(@Nullable Object object)
    {
        if (supplier == null || consumer == null) throw new IllegalStateException("component accessors must be set");

        var previousUnit = getTypedSelectedItem();
        super.setSelectedItem(object);

        // Convert spinner value to new unit of measure
        if (previousUnit != null && object != null)
        {
            consumer.accept((supplier.get()) * ((AltitudeUnits) object).getMultiplier() / previousUnit.getMultiplier());
        }
    }

    /**
     * @param meters the elevation value in meters
     */
    public void setValueMeters(double meters)
    {
        //noinspection DataFlowIssue Selected item is never null
        consumer.accept(getTypedSelectedItem().getMultiplier() * meters);
    }

    /**
     * @return the elevation value in meters, from the associated spinner
     */
    public double getValueMeters()
    {
        //noinspection DataFlowIssue Selected item is never null
        return supplier != null ? supplier.get() / getTypedSelectedItem().getMultiplier() : 0;
    }
}
