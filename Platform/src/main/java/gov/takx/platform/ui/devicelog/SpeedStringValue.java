package gov.takx.platform.ui.devicelog;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.platform.utils.UnitsFormat;
import gov.takx.platform.utils.units.SpeedUnits;
import gov.takx.platform.utils.units.UnitsFormatUtils;

import java.text.DecimalFormat;

/**
 * Format a speed value using the current units of measure.
 *
 * @since 1.49
 */
public class SpeedStringValue extends AUnitsOfMeasureStringValue<SpeedUnits>
{
    /**
     * Construct a Speed formatter using the given units of measure.
     *
     * @param uf Initial Units
     * @deprecated Use {@link #SpeedStringValue(SpeedUnits)} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public SpeedStringValue(UnitsFormat uf)
    {
        super(uf, new DecimalFormat("#.##"));
    }

    /**
     * Construct a Speed formatter using the given units of measure.
     *
     * @param units Initial Units
     * @since 5.5
     */
    public SpeedStringValue(SpeedUnits units)
    {
        super(units, new DecimalFormat("#.##"));
    }

    @Override
    public double getMultiplier()
    {
        return units.getMultiplier();
    }

    @Override
    public String getSymbol()
    {
        return units.getAbbreviation();
    }

    @Override
    SpeedUnits toNewUnits(UnitsFormat unitsFormat)
    {
        return UnitsFormatUtils.toSpeedUnits(unitsFormat);
    }
}
