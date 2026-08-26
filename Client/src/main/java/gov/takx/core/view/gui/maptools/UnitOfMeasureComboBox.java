package gov.takx.core.view.gui.maptools;

import com.atakmap.coremap.conversions.ConversionFactors;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComboBox;
import javax.swing.JSpinner;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Combo box that displays all units of measure available when entering altitude and range values. Provides utility
 * methods to convert between the selected unit and meters.
 *
 * @since 5.5
 */
public class UnitOfMeasureComboBox extends JComboBox<UnitOfMeasureComboBox.Unit>
{
    /**
     * Defines units of measure available for selection.
     */
    enum Unit
    {
        m(1),
        ft(ConversionFactors.METERS_TO_FEET),
        yd(ConversionFactors.METERS_TO_YARDS),
        km(ConversionFactors.METERS_TO_KM),
        mi(ConversionFactors.METERS_TO_MILES),
        NM(ConversionFactors.METERS_TO_NM);

        final double conversionFactor;

        Unit(double conversionFactor)
        {
            this.conversionFactor = conversionFactor;
        }
    }

    private boolean updating; // tracks programmatic updates to skip action events
    private boolean initialized; // tracks initialization to skip calls to update spinner before value is set

    @Nullable
    private Supplier<Double> supplier;
    @Nullable
    private Consumer<Double> consumer;

    public UnitOfMeasureComboBox()
    {
        super(Unit.values());
    }

    /**
     * @param spinner The spinner associated with this combo box
     * @throws IllegalStateException if the spinner has already been set
     */
    public void setSpinner(@Nonnull JSpinner spinner)
    {
        setComponentAccessors(() -> (double) spinner.getValue(), spinner::setValue);
    }

    /**
     * @param supplier The component value supplier
     * @param consumer The component value consumer
     * @throws IllegalStateException if the supplier/consumer have already been set
     * @since 5.5.1
     */
    public void setComponentAccessors(@Nonnull Supplier<Double> supplier, @Nonnull Consumer<Double> consumer)
    {
        if (this.supplier != null || this.consumer != null) throw new IllegalStateException("component already set");

        this.supplier = supplier;
        this.consumer = consumer;
    }

    /**
     * Sets the selected unit of measure. Will not trigger action events.
     *
     * @param rangeUnits The unit to select
     */
    public void setSelectedItem(@Nonnull RangeUnits rangeUnits)
    {
        updating = true;

        switch (rangeUnits)
        {
            case METERS -> setSelectedItem(Unit.m);
            case FEET -> setSelectedItem(Unit.ft);
            case NAUTICAL_MILES -> setSelectedItem(Unit.NM);
        }

        updating = false;
    }

    /**
     * Sets the selected unit of measure. Will not trigger action events.
     *
     * @param altitudeUnits The unit to select
     */
    public void setSelectedItem(@Nonnull AltitudeUnits altitudeUnits)
    {
        updating = true;

        switch (altitudeUnits)
        {
            case FEET -> setSelectedItem(Unit.ft);
            case METERS -> setSelectedItem(Unit.m);
        }

        updating = false;
    }

    /**
     * Overridden to catch all programmatic and UI change events to do spinner value conversion if necessary.
     */
    @Override
    public void setSelectedItem(Object object)
    {
        if (supplier == null || consumer == null) throw new IllegalStateException("component accessors must be set");

        final var previousUnit = (Unit) getSelectedItem();

        super.setSelectedItem(object);

        // Convert spinner value to new unit of measure
        if (initialized && previousUnit != null)
        {
            final var value = supplier.get();
            final var unit = (Unit) object;
            consumer.accept(value * unit.conversionFactor / previousUnit.conversionFactor);
        }

        initialized = true;
    }

    /**
     * @param meters The meters to convert to the selected unit and set on the associated component
     * @since 5.5.1
     */
    public void fromMeters(double meters)
    {
        //noinspection DataFlowIssue
        consumer.accept(convertToUnit(meters));
    }

    /**
     * @return The associated component's value, converted to meters
     * @see #setSpinner(JSpinner)
     * @see #setComponentAccessors(Supplier, Consumer)
     * @since 5.5.1
     */
    public double toMeters()
    {
        //noinspection DataFlowIssue
        return supplier.get() / ((Unit) getSelectedItem()).conversionFactor;
    }

    /**
     * Converts meters to the unit selected in the combo box.
     *
     * @param meters The meters to convert
     * @return The converted value
     * @since 5.5.1
     */
    public double convertToUnit(double meters)
    {
        //noinspection DataFlowIssue
        return ((Unit) getSelectedItem()).conversionFactor * meters;
    }

    @Override
    protected void fireActionEvent()
    {
        // Ignore programmatic changes
        if (!updating) super.fireActionEvent();
    }
}
