package gov.takx.platform.ui.devicelog;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.query.ASimpleFieldSelector;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.FieldConstraint;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.api.query.StructureFieldSelector;
import gov.takx.commons.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static gov.takx.api.messages.IDataType.BOOLEAN;
import static gov.takx.api.messages.IDataType.DATE;
import static gov.takx.api.messages.IDataType.DOUBLE;
import static gov.takx.api.messages.IDataType.FLOAT;
import static gov.takx.api.messages.IDataType.INT;
import static gov.takx.api.messages.IDataType.LONG;
import static gov.takx.api.messages.IDataType.SHORT;
import static gov.takx.api.messages.IDataType.STRING;
import static gov.takx.api.messages.IDataType.getDataTypeName;

/**
 * Manage the specification details of a filter on field values.  Also track whether the value used
 * in the comparison is one of our "special" date/time formats, which affects how the filtered query is executed.
 *
 * @see SpecialDateHandler
 * @since 1.49
 */
@Internal
public class FieldFilter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DateFormat STANDARD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss z");

    private final IRaptorDataStructure structure;
    private final String fieldName;
    private FilterComparison filterComparison1;
    private FilterComparison filterComparison2;
    private String description;
    private final SpecialDateHandler specialDateHandler = new SpecialDateHandler();

    /**
     * Construct a field filter on the given values.  Comparison 2 values may be ignored if comparison type 1
     * doesn't allow the use of a 2nd comparison.
     *
     * @param structure       Currently selected structure
     * @param fieldName       Name of field to filer on (may be a core field name)
     * @param comparisonType1 1st comparison type selected
     * @param value1          1st comparison value
     * @param comparisonType2 2nd comparison type selected
     * @param value2          2nd comparison value
     * @throws ParseException if a value fails to parse
     */
    public FieldFilter(IRaptorDataStructure structure, String fieldName, ComparisonType comparisonType1, String value1, ComparisonType comparisonType2, String value2) throws ParseException
    {
        if (structure == null) throw new IllegalArgumentException("'structure' can not be null");
        if (fieldName == null) throw new IllegalArgumentException("'fieldName' can not be null");

        this.structure = structure;
        this.fieldName = fieldName;

        init(comparisonType1, value1, comparisonType2, value2);
    }

    /**
     * @return {@code true} if we assembled a valid filter
     */
    public boolean isValid()
    {
        return filterComparison1 != null;
    }

    /**
     * @return Selected field name
     */
    public String getFieldName()
    {
        return fieldName;
    }

    /**
     * @return Description of this filter (for human consumption)
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return {@code true} if a special date type was matched
     */
    public boolean isSpecialDateTypeMatched()
    {
        return specialDateHandler.isSpecialDateTypeMatched();
    }

    /**
     * Check the filtered field in the RDM against the "special" date handler.
     *
     * @param rdm RDM to inspect
     * @return {@code true} if the given time field in the RDM matches the date specification
     */
    public boolean matchSpecialDate(IRaptorDataMessage rdm)
    {
        final long time = fieldName.equals(CoreField.receiveTime.name())
                ? rdm.getReceiveTime()
                : fieldName.equals(CoreField.time.name())
                        ? rdm.getTime()
                : rdm.getLong(fieldName);

        return specialDateHandler.matchDate(time,
                filterComparison1.comparisonType, (String) filterComparison1.value,
                filterComparison2 != null ? filterComparison2.comparisonType : null,
                filterComparison2 != null ? (String) filterComparison2.value : null);
    }

    /**
     * Apply the data within this filter to the given query constraints.
     *
     * @param constraints Query constraints to update
     * @throws NoSuchFieldException           should not happen
     * @throws ConflictingConstraintException should not happen
     */
    public void applyFilter(IQueryConstraints constraints) throws NoSuchFieldException, ConflictingConstraintException
    {
        logger.debug("Adding field filter of {} {}", fieldName, filterComparison1);

        final CoreField coreField = CoreField.forName(fieldName);
        final ASimpleFieldSelector selector = coreField != null
                ? coreField.selector
                : new StructureFieldSelector(structure.getMessageType(), fieldName);

        constraints.addFieldConstraints(new FieldConstraint(selector, filterComparison1.comparisonType, filterComparison1.value));

        if (filterComparison2 != null && filterComparison2.isUsable())
        {
            logger.debug("2nd constraint {} {}", fieldName, filterComparison2);
            constraints.addFieldConstraints(new FieldConstraint(selector, filterComparison2.comparisonType, filterComparison2.value));
        }
    }

    /**
     * Initialize our internal state from the values provided.  If the values are usable, then the internal members
     * fieldComparison1 and (optionally) fieldComparison2 will be initialized.
     *
     * @param comparisonType1 1st comparison type selected
     * @param value1          1st comparison value
     * @param comparisonType2 2nd comparison type selected
     * @param value2          2nd comparison value
     */
    private void init(ComparisonType comparisonType1, String value1, ComparisonType comparisonType2, String value2) throws ParseException
    {
        boolean usesValue = comparisonType1.usesValue();

        if (!usesValue || !StringUtils.isEmpty(value1))
        {
            // Determine the type of the selected field (structure or core)

            int fieldType = -1;
            IDataField selectedField = structure.getField(fieldName);

            if (selectedField != null)
            {
                fieldType = selectedField.getFieldType();
            } else
            {
                final CoreField coreField = CoreField.forName(fieldName);
                if (coreField != null)
                {
                    // Special handling for time and receiveTime - we make them Dates
                    if (coreField == CoreField.time || coreField == CoreField.receiveTime)
                    {
                        fieldType = DATE;
                    } else
                    {
                        fieldType = coreField.getFieldType();
                    }
                }
            }

            if (fieldType != -1)
            {
                if (usesValue)
                {
                    logger.debug("comparison with value detected - attempting to cast filter value to appropriate data type");

                    try
                    {
                        Object valueObject1 = getFilterValueObject(fieldType, value1);
                        filterComparison1 = new FilterComparison(comparisonType1, valueObject1);

                        if (!StringUtils.isEmpty(value2))
                        {
                            Object valueObject2 = getFilterValueObject(fieldType, value2);
                            filterComparison2 = new FilterComparison(comparisonType2, valueObject2);
                        }
                    } catch (ParseException e)
                    {
                        filterComparison1 = filterComparison2 = null; // We are not valid
                        throw e; // rethrow for our caller
                    }
                } else
                {
                    filterComparison1 = new FilterComparison(comparisonType1, null);
                    filterComparison2 = null;
                }

                description = makeDescription();

                if (logger.isDebugEnabled())
                {
                    logger.debug("Established filter on field name {} fc1 = '{}' fc2= '{}' as {}",
                            fieldName, filterComparison1, filterComparison2, description);
                }
            }
        }
    }

    /**
     * Make a human readable description from the filter values.
     */
    private String makeDescription()
    {
        if (filterComparison1.comparisonType.usesValue())
        {
            if (filterComparison2 != null)
            {
                if (filterComparison1.comparisonType == ComparisonType.GT)
                {
                    return filterComparison1.value + " " +
                            ComparisonType.LT + " " +
                            fieldName + " " +
                            filterComparison2;
                } else if (filterComparison1.comparisonType == ComparisonType.GTE)
                {
                    return filterComparison1.value + " " +
                            ComparisonType.LTE + " " +
                            fieldName + " " +
                            filterComparison2;
                } else if (filterComparison2.comparisonType == ComparisonType.GT)
                {
                    return filterComparison2.value + " " +
                            ComparisonType.LT + " " +
                            fieldName + " " +
                            filterComparison1;
                } else if (filterComparison2.comparisonType == ComparisonType.GTE)
                {
                    return filterComparison2.value + " " +
                            ComparisonType.LTE + " " +
                            fieldName + " " +
                            filterComparison1;
                } else
                {
                    return "Unexpected filter config: " + filterComparison1 + " : " + filterComparison2;
                }
            } else
            {
                return fieldName + " " + filterComparison1;
            }
        } else
        {
            return fieldName + " " + filterComparison1;
        }
    }

    /**
     * Convert the given string value to a properly typed object based on the field data type.
     *
     * @param fieldType Field data type
     * @param value     String value to convert
     * @return Properly typed object
     * @throws ParseException If the value fails to parse
     */
    private Object getFilterValueObject(int fieldType, String value) throws ParseException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("convert '{}' to {}", value, getDataTypeName(fieldType));
        }

        Object result = null;

        // We limit the combo box to only simple data types, not arrays.  Therefore, we don't
        // have to worry about switching on any array types here.

        switch (fieldType)
        {
            case STRING:
                result = value;
                break;
            case BOOLEAN:
                result = Boolean.valueOf(value);
                break;
            case INT:
                result = Integer.valueOf(value);
                break;
            case FLOAT:
                result = Float.valueOf(value);
                break;
            case SHORT:
                result = Short.valueOf(value);
                break;
            case LONG:
                result = Long.valueOf(value);
                break;
            case DOUBLE:
                result = Double.valueOf(value);
                break;
            case DATE:
                try
                {
                    result = STANDARD_DATE_FORMAT.parse(value).getTime();
                } catch (ParseException pe)
                {
                    result = specialDateHandler.parseSpecialDateType(value, pe);
                }
                break;
        }

        return result;
    }
}
