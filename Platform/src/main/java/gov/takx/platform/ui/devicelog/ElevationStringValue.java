package gov.takx.platform.ui.devicelog;

import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;

import java.text.DecimalFormat;

/**
 * Format an elevation value using the current units of measure.
 *
 * @since 1.49
 */
public class ElevationStringValue extends AUnitsOfMeasureStringValue<AltitudeUnits>
{
    /**
     * Construct an elevation formatter using the given units of measure.
     *
     * @param units Initial Units
     * @since 5.5
     */
    public ElevationStringValue(@Nonnull AltitudeUnits units)
    {
        super(units, new DecimalFormat("#.####"));
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
