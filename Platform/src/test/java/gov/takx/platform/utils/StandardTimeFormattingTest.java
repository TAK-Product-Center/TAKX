package gov.takx.platform.utils;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.test.AContainerTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link StandardTimeFormatting}.
 *
 * @since 5.3
 */
@PseudoBean
public class StandardTimeFormattingTest extends AContainerTest
{
    @Inject
    private StandardTimeFormatting standardTimeFormatting;

    @Test
    public void formatApproximateDuration_returnsCorrectResults()
    {
        final var startInstant = Instant.now();

        // seconds
        var result = standardTimeFormatting.formatApproximateDuration(startInstant, startInstant.plus(35, ChronoUnit.SECONDS));
        assertThat(result).isEqualTo("0m 35s");

        // minutes, seconds
        result = standardTimeFormatting.formatApproximateDuration(startInstant, startInstant.plus(95, ChronoUnit.SECONDS));
        assertThat(result).isEqualTo("1m 35s");

        // hours, minutes
        result = standardTimeFormatting.formatApproximateDuration(startInstant, startInstant.plus(95, ChronoUnit.MINUTES));
        assertThat(result).isEqualTo("1h 35m");

        // 1 day+
        result = standardTimeFormatting.formatApproximateDuration(startInstant, startInstant.plus(25, ChronoUnit.HOURS));
        assertThat(result).isEqualTo("1 day");

        // 2 days+
        result = standardTimeFormatting.formatApproximateDuration(startInstant, startInstant.plus(49, ChronoUnit.HOURS));
        assertThat(result).isEqualTo("2 days");
    }

    @Test
    public void formatApproximateDurationSince_returnsCorrectResults()
    {
        // seconds
        var result = standardTimeFormatting.formatApproximateDurationSince(Instant.now().minus(5, ChronoUnit.SECONDS), null);
        assertThat(result).isEqualTo("0m 5s");

        // minutes, seconds
        result = standardTimeFormatting.formatApproximateDurationSince(Instant.now().minus(95, ChronoUnit.SECONDS), "ago");
        assertThat(result).isEqualTo("1m 35s ago");

        // hours, minutes
        result = standardTimeFormatting.formatApproximateDurationSince(Instant.now().minus(95, ChronoUnit.MINUTES), "ago");
        assertThat(result).isEqualTo("1h 35m ago");

        // 1 day+
        result = standardTimeFormatting.formatApproximateDurationSince(Instant.now().minus(25, ChronoUnit.HOURS), "since");
        assertThat(result).isEqualTo("1 day since");

        // 2 days+
        result = standardTimeFormatting.formatApproximateDurationSince(Instant.now().minus(49, ChronoUnit.HOURS), "since");
        assertThat(result).isEqualTo("2 days since");
    }
}
