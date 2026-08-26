package gov.takx.api.state;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class JSONMapTest
{
    static final String key1 = "key1";
    static final String value1 = "value1";

    static final String key2 = "key2";
    static final boolean value2 = true;

    static final String key3 = "key3";
    static final int value3 = 3;

    static final String key4 = "key4";
    static final long value4 = 4;

    static final String key5 = "key5";
    static final double value5 = 5;

    static final String key6 = "key6";
    static final List<String> value6 = Arrays.asList("1", "2", "3");

    static final String key7 = "key7";
    static final List<Boolean> value7 = Arrays.asList(false, true, false, true);

    static final String key8 = "key8";
    static final List<Integer> value8 = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);

    static final String key9 = "key9";
    static final List<Long> value9 = Arrays.asList(9L, 8L, 7L, 6L);

    static final String key10 = "key10";
    static final List<Double> value10 = Arrays.asList(4d, 5d, 6d, 7d);

    static final String key11 = "key11";
    static final IRestorableMap value11 = JSONMapUtils.createNewRestorableMap();

    static
    {
        value11.setLong(key4, value4);
        value11.setBoolean(key2, value2);
    }

    @Test
    public void basicJSONMapTest()
    {
        // Create test state (also tests getters / setters).
        IRestorableMap jsonState = createTestRestoreMap();

        // Should behave sensibly even if the wrong typed getters are used
        assertThat(jsonState.getDoubleList(key1, null)).isNull();
        assertThat(jsonState.getString(key2, null)).isNull();
        assertThat(jsonState.getString(key3, null)).isNull();
        assertThat(jsonState.getString(key4, null)).isNull();
        assertThat(jsonState.getString(key5, null)).isNull();
        assertThat(jsonState.getString(key6, null)).isNull();
        assertThat(jsonState.getString(key7, null)).isNull();
        assertThat(jsonState.getString(key8, null)).isNull();
        assertThat(jsonState.getString(key9, null)).isNull();
        assertThat(jsonState.getString(key10, null)).isNull();
        assertThat(jsonState.getString(key11, null)).isNull();
    }

    @Test
    public void jsonMapToStringRestoreTest()
    {
        // Create test state
        IRestorableMap jsonMap = createTestRestoreMap();

        // Dump state to a restorable string.
        String stateString = jsonMap.toString();

        // Restore state from the string.
        IRestorableMap restoredState = JSONMapUtils.restoreMap(stateString);

        // Compare original state and restored state.
        assertThat(restoredState).isEqualTo(jsonMap);
    }

    @Test
    public void jsonMapToFileRestoreTest() throws Exception
    {
        // Create test state
        IRestorableMap jsonMap = createTestRestoreMap();

        // Dump state to a restorable file.
        File tempFile = File.createTempFile("testJSONMap", "json");
        jsonMap.writeToFile(tempFile);

        // Restore state from the string.
        IRestorableMap restoredState = JSONMapUtils.restoreMap(tempFile);

        // Compare original state and restored state.
        assertThat(restoredState).isEqualTo(jsonMap);

        tempFile.delete();
    }

    private IRestorableMap createTestRestoreMap()
    {
        IRestorableMap jsonMap = JSONMapUtils.createNewRestorableMap();

        // Do some basic setter / getter tests

        assertThat(jsonMap.hasKey(key1)).isFalse();
        jsonMap.setString(key1, value1);
        assertThat(jsonMap.hasKey(key1)).isTrue();
        assertThat(jsonMap.getString(key1, null)).isEqualTo(value1);

        assertThat(jsonMap.hasKey(key2)).isFalse();
        jsonMap.setBoolean(key2, value2);
        assertThat(jsonMap.hasKey(key2)).isTrue();
        assertThat(jsonMap.getBoolean(key2, !value2)).isEqualTo(value2);

        assertThat(jsonMap.hasKey(key3)).isFalse();
        jsonMap.setInt(key3, value3);
        assertThat(jsonMap.hasKey(key3)).isTrue();
        assertThat(jsonMap.getInt(key3, value3 + 1)).isEqualTo(value3);

        assertThat(jsonMap.hasKey(key4)).isFalse();
        jsonMap.setLong(key4, value4);
        assertThat(jsonMap.hasKey(key4)).isTrue();
        assertThat(jsonMap.getLong(key4, value4 + 1)).isEqualTo(value4);

        assertThat(jsonMap.hasKey(key5)).isFalse();
        jsonMap.setDouble(key5, value5);
        assertThat(jsonMap.hasKey(key5)).isTrue();
        assertThat(jsonMap.getDouble(key5, value5 + 1)).isEqualTo(value5);

        assertThat(jsonMap.hasKey(key6)).isFalse();
        jsonMap.setStringList(key6, value6);
        assertThat(jsonMap.hasKey(key6)).isTrue();
        assertThat(jsonMap.getStringList(key6, null)).isEqualTo(value6);

        assertThat(jsonMap.hasKey(key7)).isFalse();
        jsonMap.setBooleanList(key7, value7);
        assertThat(jsonMap.hasKey(key7)).isTrue();
        assertThat(jsonMap.getBooleanList(key7, null)).isEqualTo(value7);

        assertThat(jsonMap.hasKey(key8)).isFalse();
        jsonMap.setIntList(key8, value8);
        assertThat(jsonMap.hasKey(key8)).isTrue();
        assertThat(jsonMap.getIntList(key8, null)).isEqualTo(value8);

        assertThat(jsonMap.hasKey(key9)).isFalse();
        jsonMap.setLongList(key9, value9);
        assertThat(jsonMap.hasKey(key9)).isTrue();
        assertThat(jsonMap.getLongList(key9, null)).isEqualTo(value9);

        assertThat(jsonMap.hasKey(key10)).isFalse();
        jsonMap.setDoubleList(key10, value10);
        assertThat(jsonMap.hasKey(key10)).isTrue();
        assertThat(jsonMap.getDoubleList(key10, null)).isEqualTo(value10);

        assertThat(jsonMap.hasKey(key11)).isFalse();
        jsonMap.setRestorableMap(key11, value11);
        assertThat(jsonMap.hasKey(key11)).isTrue();
        assertThat(jsonMap.getRestorableMap(key11, null)).isEqualTo(value11);

        return jsonMap;
    }
}
