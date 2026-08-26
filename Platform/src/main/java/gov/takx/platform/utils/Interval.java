package gov.takx.platform.utils;

import jakarta.annotation.Nonnull;

import java.time.Instant;

/**
 * Represents a time interval.
 * 
 * @param start The start time
 * @param end The end time
 * @since 5.4
 */
public record Interval(@Nonnull Instant start, @Nonnull Instant end)
{
    public Interval {
        if (start.isAfter(end)) throw new IllegalArgumentException("start is after end");
    }
}
