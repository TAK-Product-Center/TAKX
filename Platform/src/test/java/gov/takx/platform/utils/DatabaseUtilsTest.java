package gov.takx.platform.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.Random;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test encoding and decoding array data types to byte arrays.
 *
 */
@SuppressWarnings("JavaUtilDate")
public class DatabaseUtilsTest
{
    // Range of characters to use for generating random strings
    private static final int CHAR_RANGE = 'z' - 'A';

    @Test
    public void booleanArrayTest()
    {
        final boolean[] array = ArrayUtils.toPrimitive(makeRandomArray(Boolean.class, Random::nextBoolean));
        final byte[] bytes = DatabaseUtils.getBytes(array);

        assertThat(DatabaseUtils.boolArrayFromBytes(bytes))
                .withFailMessage("boolean[] does not equal original array")
                .isEqualTo(array);
    }

    @Test
    public void shortArrayTest()
    {
        final short[] array = ArrayUtils.toPrimitive(makeRandomArray(Short.class, r -> (short) r.nextInt()));
        final byte[] bytes = DatabaseUtils.getBytes(array);

        assertThat(DatabaseUtils.shortArrayFromBytes(bytes))
                .withFailMessage("short[] does not equal original array")
                .isEqualTo(array);
    }

    @Test
    public void intArrayTest()
    {
        final int[] array = ArrayUtils.toPrimitive(makeRandomArray(Integer.class, Random::nextInt));
        final byte[] bytes = DatabaseUtils.getBytes(array);

        assertThat(DatabaseUtils.intArrayFromBytes(bytes))
                .withFailMessage("int[] does not equal original array")
                .isEqualTo(array);
    }

    @Test
    public void longArrayTest()
    {
        final long[] array = ArrayUtils.toPrimitive(makeRandomArray(Long.class, Random::nextLong));
        final byte[] bytes = DatabaseUtils.getBytes(array);

        assertThat(DatabaseUtils.longArrayFromBytes(bytes))
                .withFailMessage("long[] does not equal original array")
                .isEqualTo(array);
    }

    @Test
    public void floatArrayTest()
    {
        final float[] array = ArrayUtils.toPrimitive(makeRandomArray(Float.class, Random::nextFloat));
        final byte[] bytes = DatabaseUtils.getBytes(array);

        assertThat(DatabaseUtils.floatArrayFromBytes(bytes))
                .withFailMessage("float[] does not equal original array")
                .isEqualTo(array);
    }

    @Test
    public void doubleArrayTest()
    {
        final double[] array = ArrayUtils.toPrimitive(makeRandomArray(Double.class, Random::nextDouble));
        final byte[] bytes = DatabaseUtils.getBytes(array);

        assertThat(DatabaseUtils.doubleArrayFromBytes(bytes))
                .withFailMessage("double[] does not equal original array")
                .isEqualTo(array);
    }

    @Test
    public void dateArrayTest()
    {
        final Date[] array = makeRandomArray(Date.class, r -> new Date(r.nextLong()));
        final byte[] bytes = DatabaseUtils.getBytes(array);

        assertThat(DatabaseUtils.dateArrayFromBytes(bytes))
                .withFailMessage("Date[] does not equal original array")
                .isEqualTo(array);
    }

    @Test
    public void stringArrayTest()
    {
        final String[] array = makeRandomArray(String.class, r -> randomString(r, r.nextInt(1024)));
        final byte[] bytes = DatabaseUtils.getBytes(array);

        assertThat(DatabaseUtils.stringArrayFromBytes(bytes))
                .withFailMessage("String[] does not equal original array")
                .isEqualTo(array);
    }

    /**
     * Generate an array of random values produced by a specified value generator.
     *
     * @param type           Type of value being produced
     * @param valueGenerator Function to take a {@code Random} and produce a value of the proper type
     * @param <T>            Inferred type
     * @return Array of T filled with random values
     */
    @SuppressWarnings("unchecked")
    private <T> T[] makeRandomArray(Class<T> type, Function<Random, T> valueGenerator)
    {
        final Random rand = new Random(42); // Used a known seed value for reproducible tests
        final T[] array = (T[]) Array.newInstance(type, 1024);

        for (int i = 0; i < array.length; i++)
        {
            array[i] = valueGenerator.apply(rand);
        }

        return array;
    }

    private String randomString(Random rand, int length)
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            sb.append((char) (rand.nextInt(CHAR_RANGE) + 'A'));
        }
        return sb.toString();
    }
}
