package gov.takx.commons.date;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Tests {@link DateTimeParser} date String parsing.
 *
 * @since 3.0
 */
public class DateTimeParserTest
{
    /**
     * Verify that appropriate parsers are located for all input formats and can parse/format correctly.
     */
    @Test
    public void test_parse_and_format()
    {
        final DateTimeParserSet parserSet = new DateTimeParserSet();

        // RFC_822_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
        verifyParse(parserSet, "Wed, 02 Oct 2002 08:00:00 EDT");

        // ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZ";
        verifyParse(parserSet, "2020-01-01T13:01:01-05:00");
        verifyParse(parserSet, "2020-01-01T13:01:01.123-05:00");

        // ISO_8601_DATE_FORMAT_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        verifyParse(parserSet, "2020-01-01T13:01:01Z");

        // ISO_8601_DATE_FORMAT_NO_TZ = "yyyy-MM-dd'T'HH:mm:ss";
        verifyParse(parserSet, "2020-01-01T13:01:01");

        verifyParse(parserSet, "8/2/06");
        verifyParse(parserSet, "8-2-06");
        verifyParse(parserSet, "8.2.06");

        verifyParse(parserSet, "08/2/06");
        verifyParse(parserSet, "08-2-06");
        verifyParse(parserSet, "08.2.06");

        verifyParse(parserSet, "08/02/06");
        verifyParse(parserSet, "08-02-06");
        verifyParse(parserSet, "08.02.06");

        verifyParse(parserSet, "8/2/1999");
        verifyParse(parserSet, "8-2-1999");
        verifyParse(parserSet, "8.2.1999");

        verifyParse(parserSet, "08/2/1999");
        verifyParse(parserSet, "08-2-1999");
        verifyParse(parserSet, "08.2.1999");

        verifyParse(parserSet, "08/02/1999");
        verifyParse(parserSet, "08-02-1999");
        verifyParse(parserSet, "08.02.1999");

        verifyParse(parserSet, "8/2/2006 3:17");
        verifyParse(parserSet, "8/2/2006 3:17 AM");
        verifyParse(parserSet, "8/2/2006 13:17");
        verifyParse(parserSet, "8/2/2006 3:17:06");
        verifyParse(parserSet, "08/2/2006 3:17:06 AM");
        verifyParse(parserSet, "08/02/2006 3:17:06 PM");

        verifyParse(parserSet, "8/2/2006 13:17:06.999");
        verifyParse(parserSet, "8/2/2006 13:17:06.99");
        verifyParse(parserSet, "8/2/2006 13:17:06.9");
        verifyParse(parserSet, "08/2/2006 3:17:06.1 AM");
        verifyParse(parserSet, "08/2/2006 3:17:06.12 AM");
        verifyParse(parserSet, "08/02/2006 3:17:06.001 AM");

        verifyParse(parserSet, "8/2/2006 3:17:06 PST");
        verifyParse(parserSet, "8/2/2006 3:17:06 PM PST");
        verifyParse(parserSet, "8/02/06 13:17:06 PST");

        verifyParse(parserSet, "8/2/2006 3:17:06.0 PST");
        verifyParse(parserSet, "8/2/2006 3:17:06.01 PM PST");
        verifyParse(parserSet, "8/02/06 13:17:06.99 PST");
    }

    /**
     * Locate the parser for a given input string, parse the input into an Instant, and then verify that
     * the formatted output produced by the parser when parsed again yields the same Instant.  Essentially
     * a round-trip test.
     */
    private void verifyParse(DateTimeParserSet parserSet, String input)
    {
        final DateTimeParser parser = parserSet.findParser(input);

        Assertions.assertThat(parser).describedAs("no parser for '%s'", input).isNotNull();

        final Instant instant = parser.toInstant(input);
        final String formatted = parser.format(instant);

        Assertions.assertThat(instant).isEqualTo(parser.toInstant(formatted));
    }
}
