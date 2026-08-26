package gov.takx.platform.playback;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class TimeBoundaryTest
{
    @Test
    public void testTimeBoundary()
    {
        final TimeBoundary tb1 = new TimeBoundary(20, 50);
        final TimeBoundary tb2 = new TimeBoundary(tb1);

        assertThat(tb1).isEqualTo(tb2);
        assertThat(tb1).isEqualTo(new TimeBoundary(20, 50));

        assertThat(tb1.startTime).isEqualTo(20);
        assertThat(tb1.getStartTime()).isEqualTo(20);
        assertThat(tb1.endTime).isEqualTo(50);
        assertThat(tb1.getEndTime()).isEqualTo(50);

        assertThat(tb2.getStartTime()).isEqualTo(tb1.getStartTime());
        assertThat(tb2.getEndTime()).isEqualTo(tb1.getEndTime());
        assertThat(tb1.intersects(tb2)).isTrue();
        assertThat(tb1.intersects(new TimeBoundary(30, 60))).isTrue();

        assertThat(tb1.getTotalTime()).isEqualTo(30);
        assertThat(tb1.getCenterTime()).isEqualTo(35);

        assertThat(tb1.isContained(20)).isTrue();
        assertThat(tb1.isContained(50)).isTrue();
        assertThat(tb1.isContained(30)).isTrue();
        assertThat(tb1.isContained(10)).isFalse();
        assertThat(tb1.isContained(60)).isFalse();

        tb1.setStartTime(60);
        tb1.setEndTime(100);

        assertThat(tb1.getStartTime()).isEqualTo(60);
        assertThat(tb1.getEndTime()).isEqualTo(100);
        assertThat(tb1.getTotalTime()).isEqualTo(40);

        assertThat(tb1.intersects(tb2)).isFalse();

        tb1.move(30);
        assertThat(tb1.getTotalTime()).isEqualTo(40);
        assertThat(tb1.getCenterTime()).isEqualTo(50);

        tb1.union(tb2);
        assertThat(tb1.getStartTime()).isEqualTo(20);
        assertThat(tb1.getEndTime()).isEqualTo(70);
        assertThat(tb1.getTotalTime()).isEqualTo(50);

        final TimeBoundary union = TimeBoundary.union(Arrays.asList(new TimeBoundary(10, 40), new TimeBoundary(20, 30), new TimeBoundary(20, 50)));
        assertThat(union.getStartTime()).isEqualTo(10);
        assertThat(union.getEndTime()).isEqualTo(50);
    }
}
