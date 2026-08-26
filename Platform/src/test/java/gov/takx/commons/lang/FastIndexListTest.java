package gov.takx.commons.lang;

import org.apache.commons.lang3.NotImplementedException;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 5.7
 */
public class FastIndexListTest
{
    @Test
    void test()
    {
        var s0 = "";
        var list = new FastIndexList<String>();
        assertThat(list.add(s0)).isTrue();
        assertThat(list).hasSize(1);
        assertThat(list.contains(s0)).isTrue();
        assertThatThrownBy(() -> list.add(s0)).isInstanceOf(IllegalArgumentException.class);

        var s1 = "another";
        list.add(s1);
        assertThat(list.contains(s1)).isTrue();
        assertThat(list).contains(s0, Index.atIndex(0));
        assertThat(list.indexOfRemove(s0)).isEqualTo(0);
        assertThat(list.contains(s0)).isFalse();
        assertThat(list).doesNotContain(s0);
        assertThat(list).hasSize(1);
        assertThat(list).contains(s1, Index.atIndex(0));

        var s2 = "2";
        list.add(s2);
        list.removeAll(List.of(s1));
        assertThat(list).containsExactly(s2);

        var s3 = "3";
        list.add(s3);
        list.retainAll(List.of(s3));
        assertThat(list).containsExactly(s3);

        list.clear();
        assertThat(list).isEmpty();

        list.addAll(List.of("4", "5"));
        assertThat(list).hasSize(2);
        list.remove(0);
        assertThat(list).hasSize(1);

        list.set(0, s3);
        assertThat(list).containsExactly(s3);

        var s4 = "6";
        list.addAll(0, List.of(s4, "7"));
        assertThat(list).contains(s4, Index.atIndex(0));

        // Don't let anyone add this method without providing a test
        assertThatThrownBy(() -> list.subList(0, 0)).isInstanceOf(NotImplementedException.class);
    }
}
