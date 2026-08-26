package gov.takx.api.explorer;

import gov.takx.commons.utils.ParseUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.test.utils.ExplorerTestConstants.BOOLEAN_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.BOOLEAN_VALUE;
import static gov.takx.test.utils.ExplorerTestConstants.BYTE_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.BYTE_VALUE;
import static gov.takx.test.utils.ExplorerTestConstants.DOUBLE_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.DOUBLE_VALUE;
import static gov.takx.test.utils.ExplorerTestConstants.FLOAT_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.FLOAT_VALUE;
import static gov.takx.test.utils.ExplorerTestConstants.GROUP_1;
import static gov.takx.test.utils.ExplorerTestConstants.GROUP_2;
import static gov.takx.test.utils.ExplorerTestConstants.GROUP_3;
import static gov.takx.test.utils.ExplorerTestConstants.GROUP_4;
import static gov.takx.test.utils.ExplorerTestConstants.GROUP_5;
import static gov.takx.test.utils.ExplorerTestConstants.INT_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.INT_VALUE;
import static gov.takx.test.utils.ExplorerTestConstants.LONG_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.LONG_VALUE;
import static gov.takx.test.utils.ExplorerTestConstants.NAME;
import static gov.takx.test.utils.ExplorerTestConstants.OBJECT_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.OBJECT_VALUE;
import static gov.takx.test.utils.ExplorerTestConstants.SHORT_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.SHORT_VALUE;
import static gov.takx.test.utils.ExplorerTestConstants.STRING_KEY;
import static gov.takx.test.utils.ExplorerTestConstants.STRING_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ExplorableTest
{
    private static final String testType1 = "testType";
    private static final CustomType ct1 = new CustomType();
    private static final CustomType ct2 = new CustomType();

    @Test
    public void explorableTest()
    {
        MutableExplorable explorable = new MutableExplorable(NAME, GROUP_1, GROUP_2, GROUP_3, GROUP_4, GROUP_5);

        assertThat(explorable.getDisplayName()).isEqualTo(NAME);
        assertThat(ParseUtils.isUuid(explorable.getUniqueKey())).withFailMessage("default unique key should be a UUID").isTrue();

        assertThat(explorable.getParentPath()).containsExactly(GROUP_1, GROUP_2, GROUP_3, GROUP_4, GROUP_5);

        explorable.setValue(OBJECT_KEY, OBJECT_VALUE);
        explorable.setValue(STRING_KEY, STRING_VALUE);
        explorable.setValue(LONG_KEY, LONG_VALUE);
        explorable.setValue(INT_KEY, INT_VALUE);
        explorable.setValue(SHORT_KEY, SHORT_VALUE);
        explorable.setValue(BYTE_KEY, BYTE_VALUE);
        explorable.setValue(DOUBLE_KEY, DOUBLE_VALUE);
        explorable.setValue(FLOAT_KEY, FLOAT_VALUE);
        explorable.setValue(BOOLEAN_KEY, BOOLEAN_VALUE);

        Collection<String> keys = explorable.getKeys();
        assertThat(keys).contains(OBJECT_KEY,
                STRING_KEY,
                LONG_KEY,
                INT_KEY,
                SHORT_KEY,
                BYTE_KEY,
                DOUBLE_KEY,
                FLOAT_KEY,
                BOOLEAN_KEY);

        assertThat(explorable.getValue(OBJECT_KEY)).isEqualTo(OBJECT_VALUE);
        assertThat(explorable.getStringValue(STRING_KEY, null)).isEqualTo(STRING_VALUE);
        assertThat(explorable.getLongValue(LONG_KEY, null)).isEqualTo(LONG_VALUE);
        assertThat(explorable.getIntValue(INT_KEY, null)).isEqualTo(INT_VALUE);
        assertThat(explorable.getShortValue(SHORT_KEY, null)).isEqualTo(SHORT_VALUE);
        assertThat(explorable.getByteValue(BYTE_KEY, null)).isEqualTo(BYTE_VALUE);
        assertThat(explorable.getDoubleValue(DOUBLE_KEY, null)).isEqualTo(DOUBLE_VALUE);
        assertThat(explorable.getFloatValue(FLOAT_KEY, null)).isEqualTo(FLOAT_VALUE);
        assertThat(explorable.getBooleanValue(BOOLEAN_KEY, null)).isEqualTo(BOOLEAN_VALUE);

        // Test the generic typed getter
        explorable.setValue(testType1, ct1);
        CustomType t1 = explorable.getValue(testType1, CustomType.class, ct2);
        assertThat(t1).isEqualTo(ct1);

        CustomType t2 = explorable.getValue(testType1, CustomType.class, null);
        assertThat(t2).isNotNull();

        CustomType t3 = explorable.getValue(testType1, null, ct2);
        assertThat(t3).isNotNull();
        assertThat(t3).isNotEqualTo(ct1);

        CustomType t4 = explorable.getValue(testType1, null, null);
        assertThat(t4).isNull();
    }

    public static class MutableExplorable extends Explorable
    {
        private final Map<String, Object> map;

        public MutableExplorable(String displayName, String... parentPath)
        {
            super(displayName, Arrays.asList(parentPath));
            map = new ConcurrentHashMap<>();
        }

        public void setValue(String key, Object value)
        {
            map.put(key, value);
        }

        @Override
        public Object getValue(String key)
        {
            return map.get(key);
        }

        @Override
        public Collection<String> getKeys()
        {
            return map.keySet();
        }
    }

    private static class CustomType
    {
        // does nothing - only used to test type casting.
    }
}
