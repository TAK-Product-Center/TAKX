package gov.takx.core.view.gui.maptools.elevationtools.contour;

import com.atakmap.coremap.conversions.Span;
import jakarta.annotation.Nonnull;

/**
 * Shared utilities between {@link ContourListPanel} and {@link GenerationDialog}.
 * Both follow the unit model of:
 * <ul>
 *     <li>Meters</li>
 *     <li>Feet</li>
 *     <li>Yards</li>
 * </ul>
 *
 * @since 5.8
 */
public class ContourUnits
{
    private static final int INDEX_METER = 0;
    private static final int INDEX_FOOT = 1;
    private static final int INDEX_YARD = 2;

    /**
     * @param selectedIndex Combo-box selection index.
     * @return Mapped span units.
     */
    @Nonnull
    protected static Span fromSelectionIndex(int selectedIndex)
    {
        // The combo model just has:
        //  - meters
        //  - feet
        //  - yards
        // The larger ones are kinda redundant.
        return switch (selectedIndex)
        {
            case INDEX_METER -> Span.METER;
            case INDEX_FOOT -> Span.FOOT;
            case INDEX_YARD -> Span.YARD;
            default -> Span.METER;
        };
    }

    /**
     * @param units Span units to map.
     * @return Combo-box selection index.
     */
    protected static int toIndex(@Nonnull Span units)
    {
        return switch (units)
        {
            case KILOMETER, METER -> INDEX_METER;
            case MILE, FOOT, NAUTICALMILE -> INDEX_FOOT;
            case YARD -> INDEX_YARD;
        };
    }
}
