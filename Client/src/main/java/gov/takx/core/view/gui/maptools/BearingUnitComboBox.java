package gov.takx.core.view.gui.maptools;

import com.atakmap.coremap.conversions.AngleUtilities;
import gov.takx.commons.swing.JComboBoxExt;
import gov.takx.platform.utils.units.BearingUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.takx.platform.utils.units.BearingUnits.DEGREE;

/**
 * A swing combo box that is prefilled with the available {@link BearingUnits} values and can be linked to a {@link JSpinner}
 * that it will apply the proper conversions to when the user changes the unit selection.
 *
 * @since 5.6
 */
public class BearingUnitComboBox extends JComboBoxExt<BearingUnits>
{
    @Nullable
    private SpinnerNumberModel model;
    @Nullable
    private Supplier<Double> supplier;
    @Nullable
    private Consumer<Double> consumer;

    /**
     * Construct a new instance with no value and no spinner component set.
     */
    public BearingUnitComboBox()
    {
        super(BearingUnits.values());
        setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((BearingUnits) value).getAbbreviation());
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
        model = (SpinnerNumberModel) spinner.getModel();
        updateRanges();
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

    private void updateRanges()
    {
        if (model != null)
        {
            var units = getTypedSelectedItem();
            if (units != null)
            {
                switch (units)
                {
                    case DEGREE -> model.setMaximum(360d);
                    case MIL -> model.setMaximum(6400d);
                    case RADIAN -> model.setMaximum(Math.PI * 2);
                }
            }
        }
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
            updateRanges();
            consumer.accept((AngleUtilities.convert(supplier.get(), previousUnit.getAngle(), ((BearingUnits) object).getAngle())));
        }
    }

    /**
     * @param degrees The degrees value to convert to the selected unit and set on the associated component
     */
    public void setValueDegrees(double degrees)
    {
        //noinspection DataFlowIssue
        consumer.accept((AngleUtilities.convert(degrees, DEGREE.getAngle(), getTypedSelectedItem().getAngle())));
    }

    /**
     * @return The associated component's value, converted to degrees
     * @see #setSpinner(JSpinner)
     * @see #setComponentAccessors(Supplier, Consumer)
     */
    public double getValueDegrees()
    {
        //noinspection DataFlowIssue
        return AngleUtilities.convert(supplier.get(), getTypedSelectedItem().getAngle(), DEGREE.getAngle());
    }
}