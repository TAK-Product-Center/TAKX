package gov.takx.platform.ui.devicelog;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Internal;
import gov.takx.platform.utils.UnitsFormat;
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
     * The current units of measure data.
     *
     * @deprecated Use {@link #units} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    protected UnitsFormat uf;

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
     * @param uf The initial units of measure data
     * @param df The format to use when rendering decimals
     * @see #toNewUnits(UnitsFormat) 
     * @deprecated Use {@link #AUnitsOfMeasureStringValue(T, DecimalFormat)} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public AUnitsOfMeasureStringValue(UnitsFormat uf, DecimalFormat df)
    {
        this.units = toNewUnits(uf);
        this.df = df;
    }

    /**
     * Update the current units of measure data.
     *
     * @param uf The current units of measure data
     * @deprecated Use {@link #updateUnitsOfMeasure(Object)} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public void updateUnitsOfMeasure(UnitsFormat uf)
    {
        updateUnitsOfMeasure(toNewUnits(uf));
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

    /**
     * Temp method to support deprecated constructor. For internal use only.
     *
     * @param unitsFormat the old units format
     * @return The new equivalent units
     * @since 5.5
     */
    @Internal // temp method to support deprecated methods
    abstract T toNewUnits(UnitsFormat unitsFormat);
}
