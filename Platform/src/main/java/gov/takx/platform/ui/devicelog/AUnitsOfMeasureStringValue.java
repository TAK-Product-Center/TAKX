package gov.takx.platform.ui.devicelog;

import org.jdesktop.swingx.renderer.StringValue;

import java.text.DecimalFormat;

/**
 * Base class for renderers that must convert data from TAKX's default unit of measure in the model to the user's
 * current units of measure preference in the view. All values provided by the model are expected to be {@code Doubles}.
 * The instantiator of this object is responsible for providing the initial units of measure in the form of
 * a units object (e.g. {@link gov.takx.platform.utils.units.AltitudeUnits}) and must update this class when the user's
 * units of measure display preference changes via a call to {@link #updateUnitsOfMeasure(Object)}.
 *
 * @see gov.takx.platform.utils.units.AltitudeUnits
 * @see gov.takx.platform.utils.units.RangeUnits
 * @see gov.takx.platform.utils.units.SpeedUnits
 * @param <T> The unit of measure type
 * @since 1.49
 */

public abstract class AUnitsOfMeasureStringValue<T> implements StringValue
{
    /**
     * The decimal format to use when displaying the value.
     */
    private final DecimalFormat df;

    /** @since 5.5 */
    protected T units;

    /**
     * @param units The units to use
     * @param df The format to use when rendering decimals
     * @since 5.5
     */
    public AUnitsOfMeasureStringValue(T units, DecimalFormat df)
    {
        this.units = units;
        this.df = df;
    }

    /**
     * Update the current units of measure data.
     *
     * @param units The current units of measure data
     * @since 5.5
     */
    public void updateUnitsOfMeasure(T units)
    {
        this.units = units;
    }

    @Override
    public String getString(Object value)
    {
        return value == null
                ? "Unknown"
                : df.format(getMultiplier() * (Double) value) + " " + getSymbol();
    }

    /**
     * Subclasses are responsible for providing a multiplier to convert the value provided by the
     * model to the value that should be displayed in the view based on the current units of measure selected by the
     * user.
     *
     * @return The multiplier to convert the value provided by the model to the value shown in the view
     */
    public abstract double getMultiplier();

    /**
     * Subclasses are responsible for providing the symbol that should be displayed along with
     * the converted model value to indicate the unit of measure being displayed.
     *
     * @return The symbol corresponding to the current units of measure being used
     */
    public abstract String getSymbol();
}
