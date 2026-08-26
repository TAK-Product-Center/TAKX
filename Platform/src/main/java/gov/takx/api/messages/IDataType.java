package gov.takx.api.messages;

import java.util.Date;

/**
 * Defines a set of data types used by {@link IRaptorDataStructure} fields.
 * <p/>
 * <b>Note:</b>Every *_ARRAY data type has an default limit of 10MB of encoded data. Every string data type has a
 * default limit of 256 characters. These limits can be adjusted on a per-field basis using
 * {@link DataStructureBuilder#setFieldLength(String, int)}.
 *
 * @since 1.3
 */
public interface IDataType
{
    /**
     * Default length of array data types, in bytes of encoded data.<br>
     *
     * @since 1.14
     */
    int DEFAULT_ARRAY_LENGTH = 1024 * 1024 * 10;

    /**
     * Default length of the string data type, in characters.
     *
     * @since 1.14
     */
    int DEFAULT_STRING_LENGTH = 256;

    // Use int constants here instead of an enum, because enums cause problems with serialization
    int STRING = 0;
    int BOOLEAN = 1;
    int INT = 2;
    int FLOAT = 3;
    int SHORT = 4;
    int LONG = 5;
    int DOUBLE = 6;
    int DATE = 7;

    /**
     * Byte Array.
     *
     * @since 1.3
     */
    int BYTE_ARRAY = 8;

    /**
     * String Array.
     *
     * @since 1.7
     */
    int STRING_ARRAY = 9;

    /**
     * Boolean Array.
     *
     * @since 1.7
     */
    int BOOLEAN_ARRAY = 10;

    /**
     * Integer Array.
     *
     * @since 1.7
     */
    int INT_ARRAY = 11;

    /**
     * Float Array.
     *
     * @since 1.7
     */
    int FLOAT_ARRAY = 12;

    /**
     * Short Array.
     *
     * @since 1.7
     */
    int SHORT_ARRAY = 13;

    /**
     * Long Array.
     *
     * @since 1.7
     */
    int LONG_ARRAY = 14;

    /**
     * Double Array.
     *
     * @since 1.7
     */
    int DOUBLE_ARRAY = 15;

    /**
     * Date Array.
     *
     * @since 1.7
     */
    int DATE_ARRAY = 16;

    int UUID = 17;

    /**
     * A set of displayable names for the types defined here, indexed using the type constant.
     * Also used by the MessageSerializer to encode the field type in XML.
     */
    String[] DATATYPE_NAMES = {
            "string", "boolean", "int", "float", "short", "long", "double", "date",
            "bytes", "string[]", "boolean[]", "int[]", "float[]", "short[]", "long[]", "double[]", "date[]", "UUID"
    };

    /**
     * Map from data type constant to java type (Class).
     *
     * @since 2.19
     */
    Class<?>[] JAVA_TYPES = {
            String.class, boolean.class, int.class, float.class, short.class, long.class, double.class, Date.class,
            byte[].class, String[].class, boolean[].class, int[].class, float[].class, short[].class, long[].class,
            double[].class, Date[].class, java.util.UUID.class
    };

    /**
     * Determine if a given data type represents a primitive type (boolean, short, int, long, float, or double).
     *
     * @param dataType The data type to test
     * @return {@code true} if the given data type represents a primitive data type
     * @since 2.14
     */
    static boolean isPrimitiveType(int dataType)
    {
        return dataType >= BOOLEAN && dataType <= DOUBLE;
    }

    /**
     * Determine if a given data type represents a scalar type (String, Date, or primitive).
     *
     * @param dataType The data type to test
     * @return {@code true} if the given data type represents a scalar data type
     * @see #isPrimitiveType(int)
     * @since 3.0
     */
    static boolean isScalarType(int dataType)
    {
        return dataType >= 0 && dataType < BYTE_ARRAY;
    }

    /**
     * Determines if the data type is an array type.
     *
     * @param dataType The data type to test
     * @return {@code true} if this field is an array data type.
     * @since 3.0
     */
    static boolean isArrayType(int dataType)
    {
        return dataType >= BYTE_ARRAY && dataType <= DATE_ARRAY;
    }

    /**
     * Return the Java type (Class) represented by a given data type constant
     *
     * @param dataType The data type of interest
     * @return Class that the given data type represents
     * @since 2.22
     */
    static Class<?> getJavaType(int dataType)
    {
        if (dataType < 0 || dataType >= JAVA_TYPES.length)
        {
            throw new IllegalArgumentException("Data type out of bounds: " + dataType);
        }

        return JAVA_TYPES[dataType];
    }

    /**
     * Return the name of a given data type constant.
     *
     * @param dataType The data type of interest
     * @return Name of the given data type
     * @since 2.22
     */
    static String getDataTypeName(int dataType)
    {
        if (dataType < 0 || dataType >= DATATYPE_NAMES.length)
        {
            throw new IllegalArgumentException("Data type out of bounds: " + dataType);
        }

        return DATATYPE_NAMES[dataType];
    }

    /**
     * Get the data type int for a given type name.
     *
     * @param typeName Type name to lookup
     * @return Corresponding data type int
     * @throws IllegalArgumentException If the given typeName is not one of our known names
     * @since 3.0
     */
    static int getDataTypeForName(String typeName)
    {
        for (int i = 0; i < DATATYPE_NAMES.length; i++)
        {
            if (DATATYPE_NAMES[i].equalsIgnoreCase(typeName)) return i;
        }

        throw new IllegalArgumentException("Unknown data type: " + typeName);
    }

    /**
     * Determine if a given data type is sortable (i.e., implements the {@link Comparable} interface).
     *
     * @param dataType The data type to test
     * @return {@code true} if the given data type implements {@link Comparable}
     * @since 2.20
     */
    static boolean isSortable(int dataType)
    {
        return isScalarType(dataType) || dataType == UUID;
    }
}
