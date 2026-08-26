package gov.takx.core.view.gui.maptools.tacticalgraphics;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Select methods adapted from 
 * <a href="https://github.com/missioncommand/mil-sym-java/blob/master/core/JavaRendererUtils/src/main/java/ArmyC2/C2SD/Utilities/SymbolUtilities.java">SymbolUtilities.java</a>.
 *
 * @since 5.1
 */
public class SymbolUtilities
{
    /**
     * Maps second offsets to NATO time zone codes
     */
    private static final Map<Long, String> ZULU_MAP = new HashMap<>();

    // Map offsets to their Zulu characters
    static
    {
        // 0:00 Z
        ZULU_MAP.put(0L, "Z");

        // -01:00 to -12:00, N to Y
        for (int i = -1, c = 'N'; i >= -12; i--, c++)
        {
            ZULU_MAP.put(TimeUnit.HOURS.toSeconds(i), String.valueOf((char) c));
        }

        // 01:00 to 12:00, A to M (skip J)
        for (int i = 1, c = 'A'; i <= 12; i++, c++)
        {
            if (c == 'J') c++; // Zulu skips J

            ZULU_MAP.put(TimeUnit.HOURS.toSeconds(i), String.valueOf((char) c));
        }
    }

    /**
     * @return A formatter that can format and parse the date String required for milsym dates
     */
    public static DateTimeFormatter getNatoDateTimeFormatter()
    {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("ddHHmmss")
                .appendText(ChronoField.OFFSET_SECONDS, ZULU_MAP)
                .appendPattern("MMMyy")
                .toFormatter(Locale.US);
    }
}