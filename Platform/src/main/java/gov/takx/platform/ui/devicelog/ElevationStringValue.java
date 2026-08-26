package gov.takx.platform.ui.devicelog;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.platform.utils.UnitsFormat;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.UnitsFormatUtils;
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
     * @param uf Initial Units
     * @deprecated Use {@link #ElevationStringValue(AltitudeUnits)} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public ElevationStringValue(@Nonnull UnitsFormat uf)
    {
        super(uf, new DecimalFormat("#.####"));
    }

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

    @Override
    AltitudeUnits toNewUnits(UnitsFormat unitsFormat)
    {
        return UnitsFormatUtils.toAltitudeUnits(unitsFormat);
    }
}
