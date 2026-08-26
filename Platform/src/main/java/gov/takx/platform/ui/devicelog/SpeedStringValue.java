package gov.takx.platform.ui.devicelog;

import gov.takx.platform.utils.units.SpeedUnits;

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
}
