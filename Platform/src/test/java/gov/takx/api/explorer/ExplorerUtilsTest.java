package gov.takx.api.explorer;

import gov.takx.platform.KeyUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ExplorerUtilsTest
{
    @Test
    public void defaultKeyTest()
    {
        List<String> parentPath = Arrays.asList("1", "2", "3");
        String name = "name";
        StringBuilder sb = new StringBuilder();
        for (String s : parentPath)
        {
            sb.append(s).append(KeyUtils.KEY_SEP);
        }
        sb.append(name);
        assertThat(ExplorerUtils.createDefaultKey(name, parentPath)).isEqualTo(sb.toString());
    }

    @Test
    public void pathToFlatStringTest()
    {
        List<String> path = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        StringBuilder sb = new StringBuilder();
        for (String s : path)
        {
            sb.append(s);
            if (!s.equals("10"))
            {
                sb.append(KeyUtils.KEY_SEP);
            }
        }
        assertThat(ExplorerUtils.pathToFlatString(path)).isEqualTo(sb.toString());
    }

    @Test
    public void pathToFlatStringCustomSeparatorTest()
    {
        List<String> path = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        StringBuilder sb = new StringBuilder();
        for (String s : path)
        {
            sb.append(s);
            if (!s.equals("10"))
            {
                sb.append(File.separator);
            }
        }
        assertThat(ExplorerUtils.pathToFlatString(path, File.separator)).isEqualTo(sb.toString());
    }

    @Test
    public void getParentNameTest()
    {
        List<String> parentPath = Arrays.asList("1", "2", "3");
        assertThat(ExplorerUtils.getParentName(parentPath)).isEqualTo("3");
        assertThat(ExplorerUtils.getParentName(null)).isEmpty();
        assertThat(ExplorerUtils.getParentName(Collections.emptyList())).isEmpty();
    }

    @Test
    public void valueChangedTest()
    {
        assertThat(ExplorerUtils.valueChanged(0, 0)).isFalse();
        assertThat(ExplorerUtils.valueChanged(1, 0)).isTrue();
        assertThat(ExplorerUtils.valueChanged(0f, 0f)).isFalse();
        assertThat(ExplorerUtils.valueChanged(1f, 0f)).isTrue();
        assertThat(ExplorerUtils.valueChanged(0d, 0d)).isFalse();
        assertThat(ExplorerUtils.valueChanged(1d, 0d)).isTrue();
        assertThat(ExplorerUtils.valueChanged(0L, 0L)).isFalse();
        assertThat(ExplorerUtils.valueChanged(1L, 0L)).isTrue();
        assertThat(ExplorerUtils.valueChanged((short) 0, (short) 0)).isFalse();
        assertThat(ExplorerUtils.valueChanged((short) 1, (short) 0)).isTrue();
        assertThat(ExplorerUtils.valueChanged((byte) 0, (byte) 0)).isFalse();
        assertThat(ExplorerUtils.valueChanged((byte) 1, (byte) 0)).isTrue();
        assertThat(ExplorerUtils.valueChanged(false, false)).isFalse();
        assertThat(ExplorerUtils.valueChanged(true, false)).isTrue();
        assertThat(ExplorerUtils.valueChanged("test", "test")).isFalse();
        assertThat(ExplorerUtils.valueChanged("test", "different")).isTrue();
        assertThat(ExplorerUtils.valueChanged(null, null)).isFalse();
        assertThat(ExplorerUtils.valueChanged(new Object(), null)).isTrue();
        assertThat(ExplorerUtils.valueChanged(new Object[]{1, 2, 3}, new Object[]{1, 2, 3})).isFalse();
        assertThat(ExplorerUtils.valueChanged(new Object[]{1, 2, 3}, new Object[]{1, 2})).isTrue();
        assertThat(ExplorerUtils.valueChanged(new byte[]{1, 2, 3}, new byte[]{1, 2, 3})).isFalse();
        assertThat(ExplorerUtils.valueChanged(new byte[]{1, 2, 3}, new byte[]{1, 2})).isTrue();
        assertThat(ExplorerUtils.valueChanged(new short[]{1, 2, 3}, new short[]{1, 2, 3})).isFalse();
        assertThat(ExplorerUtils.valueChanged(new short[]{1, 2, 3}, new short[]{1, 2})).isTrue();
        assertThat(ExplorerUtils.valueChanged(new int[]{1, 2, 3}, new int[]{1, 2, 3})).isFalse();
        assertThat(ExplorerUtils.valueChanged(new int[]{1, 2, 3}, new int[]{1, 2})).isTrue();
        assertThat(ExplorerUtils.valueChanged(new float[]{1, 2, 3}, new float[]{1, 2, 3})).isFalse();
        assertThat(ExplorerUtils.valueChanged(new float[]{1, 2, 3}, new float[]{1, 2})).isTrue();
        assertThat(ExplorerUtils.valueChanged(new double[]{1, 2, 3}, new double[]{1, 2, 3})).isFalse();
        assertThat(ExplorerUtils.valueChanged(new double[]{1, 2, 3}, new double[]{1, 2})).isTrue();
        assertThat(ExplorerUtils.valueChanged(new boolean[]{true, false, true}, new boolean[]{true, false, true})).isFalse();
        assertThat(ExplorerUtils.valueChanged(new boolean[]{true, false, true}, new boolean[]{true, false})).isTrue();
        assertThat(ExplorerUtils.valueChanged(Arrays.asList("1", "2", "3"), Arrays.asList("1", "2", "3"))).isFalse();
        assertThat(ExplorerUtils.valueChanged(Arrays.asList("1", "2", "3"), Arrays.asList("1", "2"))).isTrue();
    }
}
