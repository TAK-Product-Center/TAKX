package gov.takx.platform.ui.devicelog;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IFieldDisplayProcessor;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.query.CoreField;
import gov.takx.platform.query.FieldExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.api.messages.IDataType.BOOLEAN;
import static gov.takx.api.messages.IDataType.BOOLEAN_ARRAY;
import static gov.takx.api.messages.IDataType.BYTE_ARRAY;
import static gov.takx.api.messages.IDataType.DATE;
import static gov.takx.api.messages.IDataType.DATE_ARRAY;
import static gov.takx.api.messages.IDataType.DOUBLE;
import static gov.takx.api.messages.IDataType.DOUBLE_ARRAY;
import static gov.takx.api.messages.IDataType.FLOAT;
import static gov.takx.api.messages.IDataType.FLOAT_ARRAY;
import static gov.takx.api.messages.IDataType.INT;
import static gov.takx.api.messages.IDataType.INT_ARRAY;
import static gov.takx.api.messages.IDataType.LONG;
import static gov.takx.api.messages.IDataType.LONG_ARRAY;
import static gov.takx.api.messages.IDataType.SHORT;
import static gov.takx.api.messages.IDataType.SHORT_ARRAY;
import static gov.takx.api.messages.IDataType.STRING;
import static gov.takx.api.messages.IDataType.STRING_ARRAY;
import static gov.takx.api.messages.IDataType.UUID;

/**
 * Table model for holding the RDM structure data (core and extended fields).  The core fields are at the start
 * of the column model, then the extended fields are sorted (according to their natural order) prior to adding their
 * names to the column model.
 *
 * @since 1.49
 */
public class StructureTableModel extends AbstractTableModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<String> columnIdentifiers = new ArrayList<>();
    private List<IRaptorDataMessage> data = new ArrayList<>();
    private final IDataField[] structureFields;
    private final int latitudeColumnIndex;
    private final int longitudeColumnIndex;
    private final int hasLocationColumnIndex;

    private final Map<String, Integer> columnIndexMap = new HashMap<>();

    private static final List<String> coreFields = Arrays.asList(
            CoreField.receiveTime.name(), CoreField.time.name(),
            CoreField.latitude.name(), CoreField.longitude.name(), CoreField.elevation.name(),
            CoreField.speed.name(), CoreField.heading.name(), CoreField.pitch.name(), CoreField.roll.name(),
            CoreField.hasMotion.name(),
            CoreField.family.name(), CoreField.type.name(), CoreField.unitId.name(),
            CoreField.hasLocation.name(), CoreField.uuid.name());

    /**
     * Constructor for table model which includes all core fields and all fields defined in the given structure.
     *
     * @param structure Structure from which to create table model
     */
    StructureTableModel(IStructure structure)
    {
        columnIdentifiers.addAll(coreFields);

        final Collection<IDataField> fields = structure.getFields();
        structureFields = fields.toArray(new IDataField[0]);
        Arrays.sort(structureFields);

        for (IDataField curField : structureFields)
        {
            columnIdentifiers.add(curField.getFieldName());
        }

        // Construct a map from column name to index (to speed up findColumn())
        for (int i = 0, count = getColumnCount(); i < count; i++)
        {
            columnIndexMap.put(columnIdentifiers.get(i), i);
        }

        latitudeColumnIndex = findColumn(CoreField.latitude.name());
        longitudeColumnIndex = findColumn(CoreField.longitude.name());
        hasLocationColumnIndex = findColumn(CoreField.hasLocation.name());
    }

    /**
     * Get the location of the RDM in the given row.
     *
     * @param row Model row index
     * @return Location, or {@code null} if the RDM has no location
     */
    public ILocation getLocation(int row)
    {
        final boolean hasLocation = (boolean) getValueAt(row, hasLocationColumnIndex);
        return hasLocation
                ? new RaptorLocation((double) getValueAt(row, latitudeColumnIndex), (double) getValueAt(row, longitudeColumnIndex))
                : null;
    }

    /**
     * Get the {@link IRaptorDataMessage} corresponding to a particular row in the model.
     *
     * @param row the row in the model for which to get the RDM
     * @return corresponding RDM
     */
    public IRaptorDataMessage getRaptorDataMessage(int row)
    {
        if (row < getRowCount())
        {
            return data.get(row);
        }
        return null;
    }

    /**
     * Get the value (as a String) at the given row in the column with the given name.
     *
     * @param row        Row to access
     * @param columnName Name of column to access
     * @return String value
     */
    public String getStringValueAt(int row, String columnName)
    {
        return String.valueOf(getValueAt(row, findColumn(columnName)));
    }

    /**
     * Replace any existing model data with the given collection of RDMs.
     *
     * @param rdms new model data
     */
    void setModelData(Collection<IRaptorDataMessage> rdms)
    {
        data = new ArrayList<>(rdms);
        fireTableDataChanged();
    }

    /**
     * Get a copy of the current table model data.  Makes a copy of the RaptorDataMessage in List form and returns it.
     *
     * @return a copy of the data model as a list of {@link IRaptorDataMessage} instances
     * @since 2.8
     */
    List<IRaptorDataMessage> getDataModel()
    {
        return new ArrayList<>(data);
    }

    /**
     * Add a collection of RDMs to the end of the model.
     *
     * @param rdms RDMs to add
     */
    void addRows(Collection<IRaptorDataMessage> rdms)
    {
        int start = data.size();
        data.addAll(rdms);

        fireTableRowsInserted(start, data.size() - 1);
    }

    /**
     * Add a collection of RDMs to the specified position / row in the model
     *
     * @param rdms  RDMs to add
     * @param start starting position to add to in the model.
     * @since 1.49
     */
    void addRows(int start, Collection<IRaptorDataMessage> rdms)
    {
        data.addAll(start, rdms);

        fireTableRowsInserted(start, data.size() - 1);
    }

    /**
     * Remove all the data from the model.
     */
    public void clear()
    {
        if (!data.isEmpty())
        {
            int size = data.size();
            data.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }

    @Override
    public int getRowCount()
    {
        return data.size();
    }

    @Override
    public int getColumnCount()
    {
        return columnIdentifiers.size();
    }

    @Override
    public String getColumnName(int column)
    {
        return columnIdentifiers.get(column);
    }

    /**
     * Overridden to use our column index map for performance.
     *
     * @param columnName string containing name of column to be located
     * @return the column with {@code columnName}, or -1 if not found
     * @since 2.16
     */
    @Override
    public int findColumn(String columnName)
    {
        return columnIndexMap.getOrDefault(columnName, -1);
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (row < getRowCount())
        {
            IRaptorDataMessage rdm = data.get(row);
            if (column < coreFields.size())
            {
                final CoreField coreField = CoreField.forName(coreFields.get(column));
                final Object fieldValue = FieldExtractor.getCoreField(rdm, coreField);

                return switch (coreField)
                {
                    case receiveTime, time -> fieldValue != null ? new Date((Long) fieldValue) : null;
                    default -> fieldValue;
                };
            } else
            {
                int index = column - coreFields.size();
                if (index < structureFields.length)
                {
                    IDataField field = structureFields[index];
                    String fieldName = field.getFieldName();

                    // If the field is null, then return null
                    if (rdm.isNull(fieldName)) return null;

                    try
                    {
                        Object fieldValue = rdm.getField(fieldName);
                        // If a plugin specifies a field processor, we want to allow it to
                        // generate the string that will be displayed in the device log.
                        IFieldDisplayProcessor fieldDisplayProcessor = field.getFieldDisplayProcessor();
                        if (fieldDisplayProcessor != null) return fieldDisplayProcessor.getDisplayString(fieldValue);
                        return fieldValue;
                    } catch (Exception e)
                    {
                        logger.error("Unexpected exception (mismatch between structure and message definitions on field '{}'\nPlease confirm that the Message Type matches the Raptor Data Message data types.)", fieldName, e);
                    }
                }
            }
        }
        return "Not Implemented";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        Class<?> clazz;

        if (columnIndex == 0 || columnIndex == 1)
        {
            clazz = Date.class; // Both times are rendered as Dates
        } else
        {
            int fieldType;
            String fieldName;

            if (columnIndex < coreFields.size())
            {
                fieldName = getColumnName(columnIndex);
                fieldType = CoreField.forName(fieldName).getFieldType();
            } else
            {
                int offset = columnIndex - coreFields.size();
                IDataField field = structureFields[offset];

                fieldName = field.getFieldName();
                fieldType = field.getFieldType();
            }

            clazz = switch (fieldType)
            {
                case BOOLEAN -> Boolean.class;
                case INT -> Integer.class;
                case FLOAT -> Float.class;
                case SHORT -> Short.class;
                case LONG -> Long.class;
                case DOUBLE -> Double.class;
                case DATE -> Date.class;
                case STRING, BYTE_ARRAY, STRING_ARRAY, BOOLEAN_ARRAY, INT_ARRAY, SHORT_ARRAY, LONG_ARRAY, FLOAT_ARRAY, DOUBLE_ARRAY, DATE_ARRAY, UUID -> String.class;
                default ->
                {
                    logger.warn("Unrecognized data type encountered on field {}: {}.  Defaulting to a String representation.", fieldName, fieldType);
                    yield String.class;
                }
            };
        }

        return clazz;
    }
}
