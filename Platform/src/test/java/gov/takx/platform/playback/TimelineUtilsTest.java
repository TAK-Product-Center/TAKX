package gov.takx.platform.playback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class TimelineUtilsTest
{
    final long minute = TimelineUtils.MINUTE;

    @Test
    public void testRoundSecond()
    {
        assertThat(TimelineUtils.roundSecond(0)).isEqualTo(0);
        assertThat(TimelineUtils.roundSecond(499)).isEqualTo(0);
        assertThat(TimelineUtils.roundSecond(500)).isEqualTo(1000);
        assertThat(TimelineUtils.roundSecond(999)).isEqualTo(1000);
        assertThat(TimelineUtils.roundSecond(1000)).isEqualTo(1000);
    }

    @Test
    public void testRoundMinute()
    {
        final long halfMinute = 30 * 1000;

        assertThat(TimelineUtils.roundMinute(0)).isEqualTo(0);
        assertThat(TimelineUtils.roundMinute(halfMinute - 1)).isEqualTo(0);
        assertThat(TimelineUtils.roundMinute(halfMinute)).isEqualTo(minute);
        assertThat(TimelineUtils.roundMinute(minute - 1)).isEqualTo(minute);
        assertThat(TimelineUtils.roundMinute(minute)).isEqualTo(minute);
    }

    @Test
    public void testCeilMinute()
    {
        assertThat(TimelineUtils.ceilMinute(0)).isEqualTo(0);
        assertThat(TimelineUtils.ceilMinute(1)).isEqualTo(minute);
        assertThat(TimelineUtils.ceilMinute(minute)).isEqualTo(minute);
        assertThat(TimelineUtils.ceilMinute(minute - 1)).isEqualTo(minute);
    }

    @Test
    public void testFloorMinute()
    {
        assertThat(TimelineUtils.floorMinute(0)).isEqualTo(0);
        assertThat(TimelineUtils.floorMinute(minute - 1)).isEqualTo(0);
        assertThat(TimelineUtils.floorMinute(minute)).isEqualTo(minute);
        assertThat(TimelineUtils.floorMinute(minute + 1)).isEqualTo(minute);
    }

    @Test
    public void testElapsedTime()
    {
        final long fiveDays = 5 * TimelineUtils.DAY + 4 * TimelineUtils.HOUR + 3 * TimelineUtils.MINUTE + 2 * TimelineUtils.SECOND + 50;
        final long fourHours = 4 * TimelineUtils.HOUR + 3 * TimelineUtils.MINUTE + 2 * TimelineUtils.SECOND + 50;

        assertThat(TimelineUtils.elapsedTimeDays(fiveDays)).isEqualTo("05:04:03:02");
        assertThat(TimelineUtils.elapsedTimeDays(fourHours)).isEqualTo("00:04:03:02");
        assertThat(TimelineUtils.elapsedTime(fiveDays)).isEqualTo("124:03:02.050");

        assertThat(TimelineUtils.elapsedTime(fourHours)).isEqualTo("04:03:02.050");
        assertThat(TimelineUtils.elapsedTimeNoMillis(fourHours)).isEqualTo("04:03:02");
    }
}
