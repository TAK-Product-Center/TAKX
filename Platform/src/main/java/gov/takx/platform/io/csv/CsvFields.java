package gov.takx.platform.io.csv;

import gov.takx.api.messages.DataFieldEncoding;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IPrePersistRaptorMessage;
import gov.takx.commons.date.DateTimeParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants used for CSV formats. This class defines constants for special fields
 * in CSV documents, and also "friendly" versions of those fields for use in
 * format definition tools (like a wizard interface that helps the user choose fields
 * to export).
 *
 * @see IDataField
 */

public class CsvFields
{
    /*
     * Computer/parser friendly names.
     */
    public static final String TYPE = "type";
    public static final String VERSION = "version";
    public static final String ID = "id";
    public static final String KEY = "key";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String MANUALLY_GENERATED_LOCATION = "manually_generated_location";
    public static final String ELEVATION = "elevation";
    public static final String PITCH = "pitch";
    public static final String HEADING = "heading";
    public static final String ROLL = "roll";
    public static final String SPEED = "speed";
    public static final String MOTION = "motion";
    public static final String TIME = "time";
    public static final String RECEIVE_TIME = "receive_time";

    /*
     * Human friendly field names.
     */
    public static final String FRIENDLY_TYPE = "Map Object Type";
    public static final String FRIENDLY_VERSION = "Map Object Version";
    public static final String FRIENDLY_ID = "Unit ID";
    public static final String FRIENDLY_LATITUDE = "Latitude";
    public static final String FRIENDLY_LONGITUDE = "Longitude";
    public static final String FRIENDLY_MANUALLY_GENERATED_LOCATION = "Is manually generated location";
    public static final String FRIENDLY_ELEVATION = "Elevation";
    public static final String FRIENDLY_PITCH = "Pitch";
    public static final String FRIENDLY_HEADING = "Heading";
    public static final String FRIENDLY_ROLL = "Roll";
    public static final String FRIENDLY_SPEED = "Speed";
    public static final String FRIENDLY_MOTION = "Motion";
    public static final String FRIENDLY_TIME = "Time";
    public static final String CUSTOM = "Custom";

    /*
     * String versions of common data types.
     */
    public static final String STRING = "String";
    public static final String BOOLEAN = "True/False";
    public static final String INTEGER = "Integer";
    public static final String FLOAT = "Decimal (Float)";
    public static final String SHORT = "Integer (Short)";
    public static final String LONG = "Integer (Long)";
    public static final String DOUBLE = "Decimal (Double)";
    public static final String DATE = "Date";
    public static final String BYTES = "Bytes";
    public static final String STRING_ARRAY = "List of Strings";
    public static final String BOOLEAN_ARRAY = "List of True/False";
    public static final String INT_ARRAY = "List of Integers (Integer)";
    public static final String SHORT_ARRAY = "List of Integers (Short)";
    public static final String LONG_ARRAY = "List of Integers (Long)";
    public static final String FLOAT_ARRAY = "List of Decimals (Float)";
    public static final String DOUBLE_ARRAY = "List of Decimals (Double)";
    public static final String DATE_ARRAY = "List of Dates";

    private static final Map<String, String> GUI_2_CSV;
    private static final Map<String, String> CSV_2_GUI;
    private static final Map<String, Integer> GUI_2_DATA_FIELD;
    private static final Map<Integer, String> DATA_FIELD_2_GUI;
    private static final Map<String, String> TYPE_MAP;

    static
    {
        GUI_2_CSV = new HashMap<>();
        GUI_2_CSV.put(FRIENDLY_TYPE, TYPE);
        GUI_2_CSV.put(FRIENDLY_VERSION, VERSION);
        GUI_2_CSV.put(FRIENDLY_ID, ID);
        GUI_2_CSV.put(FRIENDLY_LATITUDE, LATITUDE);
        GUI_2_CSV.put(FRIENDLY_LONGITUDE, LONGITUDE);
        GUI_2_CSV.put(FRIENDLY_ELEVATION, ELEVATION);
        GUI_2_CSV.put(FRIENDLY_MANUALLY_GENERATED_LOCATION, MANUALLY_GENERATED_LOCATION);
        GUI_2_CSV.put(FRIENDLY_PITCH, PITCH);
        GUI_2_CSV.put(FRIENDLY_HEADING, HEADING);
        GUI_2_CSV.put(FRIENDLY_ROLL, ROLL);
        GUI_2_CSV.put(FRIENDLY_SPEED, SPEED);
        GUI_2_CSV.put(FRIENDLY_MOTION, MOTION);
        GUI_2_CSV.put(FRIENDLY_TIME, TIME);
        GUI_2_CSV.put(CUSTOM, CUSTOM);

        CSV_2_GUI = new HashMap<>();
        CSV_2_GUI.put(ID, FRIENDLY_ID);
        CSV_2_GUI.put(LATITUDE, FRIENDLY_LATITUDE);
        CSV_2_GUI.put(LONGITUDE, FRIENDLY_LONGITUDE);
        CSV_2_GUI.put(ELEVATION, FRIENDLY_ELEVATION);
        CSV_2_GUI.put(MANUALLY_GENERATED_LOCATION, FRIENDLY_MANUALLY_GENERATED_LOCATION);
        CSV_2_GUI.put(PITCH, FRIENDLY_PITCH);
        CSV_2_GUI.put(HEADING, FRIENDLY_HEADING);
        CSV_2_GUI.put(ROLL, FRIENDLY_ROLL);
        CSV_2_GUI.put(SPEED, FRIENDLY_SPEED);
        CSV_2_GUI.put(MOTION, FRIENDLY_MOTION);
        CSV_2_GUI.put(TIME, FRIENDLY_TIME);

        GUI_2_DATA_FIELD = new HashMap<>();
        GUI_2_DATA_FIELD.put(STRING, IDataField.STRING);
        GUI_2_DATA_FIELD.put(BOOLEAN, IDataField.BOOLEAN);
        GUI_2_DATA_FIELD.put(INTEGER, IDataField.INT);
        GUI_2_DATA_FIELD.put(FLOAT, IDataField.FLOAT);
        GUI_2_DATA_FIELD.put(SHORT, IDataField.SHORT);
        GUI_2_DATA_FIELD.put(LONG, IDataField.LONG);
        GUI_2_DATA_FIELD.put(DOUBLE, IDataField.DOUBLE);
        GUI_2_DATA_FIELD.put(DATE, IDataField.DATE);
        GUI_2_DATA_FIELD.put(BYTES, IDataField.BYTE_ARRAY);
        GUI_2_DATA_FIELD.put(STRING_ARRAY, IDataField.STRING_ARRAY);
        GUI_2_DATA_FIELD.put(BOOLEAN_ARRAY, IDataField.BOOLEAN_ARRAY);
        GUI_2_DATA_FIELD.put(INT_ARRAY, IDataField.INT_ARRAY);
        GUI_2_DATA_FIELD.put(FLOAT_ARRAY, IDataField.FLOAT_ARRAY);
        GUI_2_DATA_FIELD.put(SHORT_ARRAY, IDataField.SHORT_ARRAY);
        GUI_2_DATA_FIELD.put(LONG_ARRAY, IDataField.LONG_ARRAY);
        GUI_2_DATA_FIELD.put(DOUBLE_ARRAY, IDataField.DOUBLE_ARRAY);
        GUI_2_DATA_FIELD.put(DATE_ARRAY, IDataField.DATE_ARRAY);

        DATA_FIELD_2_GUI = new HashMap<>();
        DATA_FIELD_2_GUI.put(IDataField.STRING, STRING);
        DATA_FIELD_2_GUI.put(IDataField.BOOLEAN, BOOLEAN);
        DATA_FIELD_2_GUI.put(IDataField.INT, INTEGER);
        DATA_FIELD_2_GUI.put(IDataField.FLOAT, FLOAT);
        DATA_FIELD_2_GUI.put(IDataField.SHORT, SHORT);
        DATA_FIELD_2_GUI.put(IDataField.LONG, LONG);
        DATA_FIELD_2_GUI.put(IDataField.DOUBLE, DOUBLE);
        DATA_FIELD_2_GUI.put(IDataField.DATE, DATE);
        DATA_FIELD_2_GUI.put(IDataField.BYTE_ARRAY, BYTES);
        DATA_FIELD_2_GUI.put(IDataField.STRING_ARRAY, STRING_ARRAY);
        DATA_FIELD_2_GUI.put(IDataField.BOOLEAN_ARRAY, BOOLEAN_ARRAY);
        DATA_FIELD_2_GUI.put(IDataField.INT_ARRAY, INT_ARRAY);
        DATA_FIELD_2_GUI.put(IDataField.FLOAT_ARRAY, FLOAT_ARRAY);
        DATA_FIELD_2_GUI.put(IDataField.SHORT_ARRAY, SHORT_ARRAY);
        DATA_FIELD_2_GUI.put(IDataField.LONG_ARRAY, LONG_ARRAY);
        DATA_FIELD_2_GUI.put(IDataField.DOUBLE_ARRAY, DOUBLE_ARRAY);
        DATA_FIELD_2_GUI.put(IDataField.DATE_ARRAY, DATE_ARRAY);

        TYPE_MAP = new HashMap<>();
        TYPE_MAP.put(ID, STRING);
        TYPE_MAP.put(LATITUDE, DOUBLE);
        TYPE_MAP.put(LONGITUDE, DOUBLE);
        TYPE_MAP.put(ELEVATION, DOUBLE);
        TYPE_MAP.put(MANUALLY_GENERATED_LOCATION, BOOLEAN);
        TYPE_MAP.put(PITCH, DOUBLE);
        TYPE_MAP.put(HEADING, DOUBLE);
        TYPE_MAP.put(ROLL, DOUBLE);
        TYPE_MAP.put(SPEED, DOUBLE);
        TYPE_MAP.put(MOTION, BOOLEAN);
        TYPE_MAP.put(TIME, DATE);
    }

    public static String getField(String friendlyName)
    {
        String field = GUI_2_CSV.get(friendlyName);
        return (field != null) ? field : friendlyName;
    }

    public static String getFriendlyField(String fieldName)
    {
        return CSV_2_GUI.get(fieldName);
    }

    public static int getDataType(String csvDataType)
    {
        return GUI_2_DATA_FIELD.get(csvDataType);
    }

    public static String getFieldType(String fieldName)
    {
        return TYPE_MAP.get(fieldName);
    }

    public static String getDataString(int dataType)
    {
        return DATA_FIELD_2_GUI.get(dataType);
    }

    public static Collection<String> getDataTypes()
    {
        return GUI_2_DATA_FIELD.keySet();
    }

    public static Collection<String> getFriendlyFields()
    {
        return GUI_2_CSV.keySet();
    }

    public static String[] getExportFields()
    {
        return new String[]{
                FRIENDLY_ID,
                FRIENDLY_LATITUDE,
                FRIENDLY_LONGITUDE,
                FRIENDLY_ELEVATION,
                FRIENDLY_MANUALLY_GENERATED_LOCATION,
                FRIENDLY_PITCH,
                FRIENDLY_HEADING,
                FRIENDLY_ROLL,
                FRIENDLY_SPEED,
                FRIENDLY_MOTION,
                FRIENDLY_TIME};
    }

    /**
     * Parses the field value according to the type.  If a message is passed in the message is assigned the value.
     * The message is added for performance reasons so we don't have to call both methods.
     *
     * @param type           The type the value should be parsed as.
     * @param value          The string value of the csv field
     * @param name           The name of the csv column
     * @param dateTimeParser Nullable: If the type is a date this is the format that will be applied to it.
     * @param msg            Nullable: If this argument is provided then the parsed value will be added to the message.
     * @return The parsed value as the passed type.
     * @throws NumberFormatException If a primitive value can't be parsed
     */
    public static Object parseFieldValue(final int type, final String value, final String name,
                                         DateTimeParser dateTimeParser, IPrePersistRaptorMessage msg)
            throws NumberFormatException
    {
        final Object fieldValue = DataFieldEncoding.decodeFieldValue(name, type, value, dateTimeParser);

        if (msg != null) msg.setField(name, fieldValue);

        return fieldValue;
    }
}
