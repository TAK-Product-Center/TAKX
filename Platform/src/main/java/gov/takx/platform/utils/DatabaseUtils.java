package gov.takx.platform.utils;

import gov.takx.api.messages.IDataType;
import org.hibernate.Length;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 1.7
 */
public class DatabaseUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Map<Integer, Class<?>> SQL_TYPE_TO_JAVA_TYPE = Map.of(
            Types.BOOLEAN, boolean.class,
            Types.BIT, boolean.class,
            Types.BIGINT, long.class,
            Types.DOUBLE, double.class,
            Types.FLOAT, float.class,
            Types.INTEGER, int.class,
            Types.SMALLINT, short.class,
            Types.VARCHAR, String.class,
            Types.LONGVARCHAR, String.class,
            Types.TIMESTAMP, long.class);

    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * @param o object
     * @return true if the supplied object is a Hibernate proxy object
     */
    public static boolean isProxy(Object o)
    {
        return o instanceof HibernateProxy;
    }

    /**
     * Get a string representing the SQL column definition portion of a "Create Table" statement for the given typeConfiguration.
     *
     * @param database The database in use (for access to schema/type information)
     * @param type    the {@link IDataType}
     * @param length  the column length (if applicable)
     * @return The data type of the column in the requested typeConfiguration.
     * @since 3.4 (first parameter changed from Dialect to Database as part of Hibernate update)
     */
    public static String getColumnType(Database database, int type, Integer length)
    {
        final var typeRegistry = database.getTypeConfiguration().getDdlTypeRegistry();

        if (type == IDataType.STRING)
        {
            final long len = length == null ? 0 : length;
            return len == Integer.MAX_VALUE
                    ? typeRegistry.getTypeName(Types.LONGVARCHAR, Size.nil())
                    : typeRegistry.getTypeName(Types.VARCHAR, Size.length(len == 0 ? Length.DEFAULT : len));
        } else
        {
            return typeRegistry.getTypeName(getSqlTypeForDataType(type), database.getDialect());
        }
    }

    public static byte[] getBytes(String[] array)
    {
        if (null == array) return EMPTY_BYTES;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (DataOutputStream dataOut = new DataOutputStream(out))
        {
            for (String s : array)
            {
                dataOut.writeUTF(s);
            }
            return out.toByteArray();
        } catch (IOException e)
        {
            logger.warn("Failed to write string to byte array");
        }

        return EMPTY_BYTES;
    }

    public static byte[] getBytes(double[] array)
    {
        if (null == array) return EMPTY_BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(array.length * 8);
        buffer.asDoubleBuffer().put(array);
        return buffer.array();
    }

    public static byte[] getBytes(long[] array)
    {
        if (null == array) return EMPTY_BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(array.length * 8);
        buffer.asLongBuffer().put(array);
        return buffer.array();
    }

    public static byte[] getBytes(short[] array)
    {
        if (null == array) return EMPTY_BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(array.length * 2);
        buffer.asShortBuffer().put(array);
        return buffer.array();
    }

    public static byte[] getBytes(int[] array)
    {
        if (null == array) return EMPTY_BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);
        buffer.asIntBuffer().put(array);
        return buffer.array();
    }

    public static byte[] getBytes(float[] array)
    {
        if (null == array) return EMPTY_BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);
        buffer.asFloatBuffer().put(array);
        return buffer.array();
    }

    public static byte[] getBytes(boolean[] array)
    {
        if (null == array) return EMPTY_BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(array.length);

        // TODO: using a full byte for each boolean is a terribly inefficient way to store an array of bits.
        // TODO: rewrite this to pack the bits into bytes.
        for (boolean b : array)
        {
            buffer.put(b ? (byte) 1 : 0);
        }

        return buffer.array();
    }

    public static byte[] getBytes(Date[] array)
    {
        if (null == array) return EMPTY_BYTES;

        ByteBuffer buffer = ByteBuffer.allocate(array.length * 8);
        LongBuffer longBuffer = buffer.asLongBuffer();
        for (Date d : array)
        {
            longBuffer.put(d.getTime());
        }
        return buffer.array();
    }

    public static String[] stringArrayFromBytes(byte[] bytes)
    {
        List<String> strings = new ArrayList<>();

        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try (DataInputStream dataIn = new DataInputStream(in))
        {
            while (dataIn.available() > 0)
            {
                strings.add(dataIn.readUTF());
            }
            return strings.toArray(new String[0]);
        } catch (IOException e)
        {
            logger.warn("Error reading string from byte array");
        }

        return new String[0];
    }

    public static boolean[] boolArrayFromBytes(byte[] bytes)
    {
        boolean[] array = new boolean[bytes.length];

        for (int i = 0; i < bytes.length; i++)
        {
            array[i] = bytes[i] == 1;
        }

        return array;
    }

    /**
     * Convert a byte array into an int array.
     *
     * @param bytes Bytes to convert
     * @return int[]
     */
    public static int[] intArrayFromBytes(byte[] bytes)
    {
        final IntBuffer buffer = ByteBuffer.wrap(bytes).asIntBuffer();
        final int[] a = new int[buffer.limit()];
        buffer.get(a);
        return a;
    }

    /**
     * Convert a byte array into an short array.
     *
     * @param bytes Bytes to convert
     * @return short[]
     */
    public static short[] shortArrayFromBytes(byte[] bytes)
    {
        final ShortBuffer buffer = ByteBuffer.wrap(bytes).asShortBuffer();
        short[] a = new short[buffer.limit()];
        buffer.get(a);
        return a;
    }

    /**
     * Convert a byte array into an long array.
     *
     * @param bytes Bytes to convert
     * @return long[]
     */
    public static long[] longArrayFromBytes(byte[] bytes)
    {
        final LongBuffer buffer = ByteBuffer.wrap(bytes).asLongBuffer();
        long[] a = new long[buffer.limit()];
        buffer.get(a);
        return a;
    }

    /**
     * Convert a byte array into an float array.
     *
     * @param bytes Bytes to convert
     * @return float[]
     */
    public static float[] floatArrayFromBytes(byte[] bytes)
    {
        final FloatBuffer buffer = ByteBuffer.wrap(bytes).asFloatBuffer();
        float[] a = new float[buffer.limit()];
        buffer.get(a);
        return a;
    }

    /**
     * Convert a byte array into an double array.
     *
     * @param bytes Bytes to convert
     * @return double[]
     */
    public static double[] doubleArrayFromBytes(byte[] bytes)
    {
        final DoubleBuffer buffer = ByteBuffer.wrap(bytes).asDoubleBuffer();
        double[] a = new double[buffer.limit()];
        buffer.get(a);
        return a;
    }

    /**
     * Convert a byte array to a Date array. The byte array is read eight bytes at a time, and these bytes are interpreted
     * as a {@code long} representing the number of milliseconds since the Epoch.
     *
     * @param bytes Bytes to convert to Date[]
     * @return New date array
     */
    public static Date[] dateArrayFromBytes(byte[] bytes)
    {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        LongBuffer longBuffer = byteBuffer.asLongBuffer();

        int i = 0;
        Date[] dates = new Date[longBuffer.limit()];
        while (longBuffer.hasRemaining())
        {
            dates[i] = new Date(longBuffer.get());
            i++;
        }

        return dates;
    }

    /**
     * get the java.sql.Types value for the IDataType supplied
     *
     * @param type the {@link IDataType}
     * @return the {@link java.sql.Types}
     */
    public static int getSqlTypeForDataType(int type)
    {
        return switch (type)
        {
            case IDataType.BOOLEAN -> Types.BOOLEAN;
            case IDataType.DATE -> Types.TIMESTAMP;
            case IDataType.DOUBLE -> Types.DOUBLE;
            case IDataType.FLOAT -> Types.FLOAT;
            case IDataType.INT -> Types.INTEGER;
            case IDataType.LONG -> Types.BIGINT;
            case IDataType.SHORT -> Types.SMALLINT;
            case IDataType.STRING -> Types.VARCHAR;
            case IDataType.BYTE_ARRAY, IDataType.BOOLEAN_ARRAY, IDataType.DOUBLE_ARRAY, IDataType.FLOAT_ARRAY, IDataType.INT_ARRAY, IDataType.LONG_ARRAY, IDataType.SHORT_ARRAY, IDataType.STRING_ARRAY, IDataType.DATE_ARRAY -> Types.LONGVARBINARY;
            default -> Types.NULL;
        };
    }

    /**
     * Given a Class and a set of SQL types, return the constructor for that class that matches the given sql
     * types.
     *
     * @param cls      Class to process
     * @param sqlTypes The list of SQL types to match to constructor arguments
     * @return Java class used to represent that SQL type
     * @throws NoSuchMethodException If there is no matching constructor on the given class
     * @since 2.14
     */
    public static Constructor<?> getConstructorForSqlTypes(Class<?> cls, int[] sqlTypes) throws NoSuchMethodException
    {
        // Loop through all the constructors on the class looking for a match.
        for (Constructor<?> constructor : cls.getConstructors())
        {
            if (constructor.getParameterCount() != sqlTypes.length) continue; // Wrong arg count

            final Class<?>[] types = constructor.getParameterTypes();

            boolean allMatched = true;
            for (int i = 0; i < sqlTypes.length; i++)
            {
                if (!javaTypeMatchesSqlType(types[i], sqlTypes[i]))
                {
                    allMatched = false;
                    break;
                }
            }

            if (allMatched) return constructor;
        }

        // Not match found, make a good error message
        final String types = Arrays.stream(sqlTypes)
                .boxed()
                .map(st -> SQL_TYPE_TO_JAVA_TYPE.get(st).getName())
                .collect(Collectors.joining(", "));

        throw new NoSuchMethodException("No constructor on " + cls + " with arg types (" + types + ")");
    }

    /**
     * Determine if the given java type is compatible with the given sql type.  This handles matching both the
     * primitive type and the wrapper type for java primitive types.
     *
     * @param type    Java type (class)
     * @param sqlType SQL type
     * @return {@code true} if the types are compatible
     * @since 2.14
     */
    private static boolean javaTypeMatchesSqlType(Class<?> type, int sqlType)
    {
        return switch (sqlType)
        {
            case Types.BOOLEAN, Types.BIT -> type == Boolean.class || type == boolean.class;
            case Types.DOUBLE -> type == Double.class || type == double.class;
            case Types.FLOAT -> type == Float.class || type == float.class;
            case Types.INTEGER -> type == Integer.class || type == int.class;
            case Types.BIGINT, Types.TIMESTAMP -> type == Long.class || type == long.class;
            case Types.SMALLINT -> type == Short.class || type == short.class;
            case Types.VARCHAR, Types.LONGVARCHAR -> type == String.class;
            default ->
            {
                logger.error("Unsupported SQL type: {}", sqlType);
                yield false;
            }
        };
    }

    /**
     * Determine if a table exists.
     *
     * @param tableName Name of table to test
     * @return {@code true} if the table exists, {@code false} otherwise
     * @since 2.14
     */
    public static boolean tableExists(Connection connection, String tableName)
    {
        // This used to use a SQL query of this form:
        //   "select count(*) FROM information_schema.tables WHERE lower(table_name) = '" + tableName.toLowerCase() + "'"
        //
        // But, simply attempting to query against a table that doesn't exist throws an exception when it is parsed, so
        // we never actually need to execute the query.  This is roughly 8 times faster than the query above.

        //noinspection SqlSourceToSinkFlow
        try (var stmt = connection.createStatement(); var ignored = stmt.executeQuery("select 1 from " + tableName))
        {
            return true;
        } catch (SQLException e)
        {
            return false;
        }
    }
}
