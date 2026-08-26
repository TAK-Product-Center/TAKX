package gov.takx.platform.query;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.CoreFieldSelector;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.StructureFieldSelector;
import org.hibernate.transform.ResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to extract field values from an RDM given a {@link FieldSelector}.  Only core and
 * extended fields are supported.  Aggregate field selectors are not supported.
 *
 * @since 1.49
 */
@SuppressWarnings("unused") // Used by plugins
public class FieldExtractor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Extract the fields specified in the given {@code fieldSelector} and return a List of Object array of extracted values.
     *
     * @param rdms          Stream of RDMs from which to extract values
     * @param fieldSelector Field selector specifying fields to extract
     * @return List of Object[] of extracted values from each RDM
     * @since 2.30
     */
    public static List<Object[]> getFields(Stream<? extends IRaptorDataMessage> rdms, FieldSelector fieldSelector)
    {
        // Determine the total number of fields we will extract
        final List<CoreFieldSelector> coreFieldSelectors = fieldSelector.getCoreFieldSelectors();
        final List<StructureFieldSelector> extendedFieldSelectors = fieldSelector.getExtendedFieldSelectors();
        final int fieldCount = coreFieldSelectors.size() + extendedFieldSelectors.size();

        return rdms
                .map(rdm -> extractValues(rdm, fieldCount, coreFieldSelectors, extendedFieldSelectors))
                .collect(Collectors.toList());
    }

    /**
     * Extract the fields specified in the given {@code fieldSelector} and return a list of maps from field name
     * to extracted value.
     *
     * @param rdms          Stream of RDMs from which to extract values
     * @param fieldSelector Field selector specifying fields to extract
     * @return List of Maps from field name to extracted value
     * @since 2.30
     */
    public static List<Map<String, Object>> getFieldsAsMap(Stream<? extends IRaptorDataMessage> rdms, FieldSelector fieldSelector)
    {
        return rdms.map(rdm -> getFieldsAsMap(rdm, fieldSelector))
                .collect(Collectors.toList());
    }

    /**
     * Extract the fields specified in the given {@code fieldSelector} and return a map from field name to extracted value.
     *
     * @param rdm           RDM from which to extract values
     * @param fieldSelector Field selector specifying fields to extract
     * @return Map from field name to extracted value
     */
    public static Map<String, Object> getFieldsAsMap(IRaptorDataMessage rdm, FieldSelector fieldSelector)
    {
        final Map<String, Object> valueMap = new HashMap<>();

        // Extract all core fields
        for (CoreFieldSelector coreFieldSelector : fieldSelector.getCoreFieldSelectors())
        {
            valueMap.put(coreFieldSelector.getAlias(), getCoreField(rdm, coreFieldSelector.getCoreField()));
        }

        // Extract all extended fields
        if (fieldSelector.hasExtendedFields())
        {
            validateStructures(rdm, fieldSelector.getStructure());

            for (StructureFieldSelector structureFieldSelector : fieldSelector.getExtendedFieldSelectors())
            {
                valueMap.put(structureFieldSelector.getAlias(), rdm.getField(structureFieldSelector.getFieldName()));
            }
        }

        return valueMap;
    }

    /**
     * Return a list of "bean" instances containing extracted values from the given stream of RDMs.
     *
     * @param rdms          RDMs from which to extract values
     * @param fieldSelector The selector indicating the fields to retrieve
     * @param toBean        tuple transformer to construct the return object
     * @return Bean instance
     * @since 2.30
     */
    public static List<Object> getFieldsAsBean(Stream<? extends IRaptorDataMessage> rdms, FieldSelector fieldSelector, ResultTransformer toBean)
    {
        // Collect all the field selector aliases
        final List<CoreFieldSelector> coreFieldSelectors = fieldSelector.getCoreFieldSelectors();
        final List<StructureFieldSelector> extendedFieldSelectors = fieldSelector.getExtendedFieldSelectors();

        final int fieldCount = coreFieldSelectors.size() + extendedFieldSelectors.size();
        int index = 0;

        final String[] aliases = new String[fieldCount];

        for (CoreFieldSelector coreFieldSelector : coreFieldSelectors)
        {
            aliases[index++] = coreFieldSelector.getAlias();
        }

        for (StructureFieldSelector structureFieldSelector : extendedFieldSelectors)
        {
            aliases[index++] = structureFieldSelector.getAlias();
        }

        // Convert the field values to beans
        return rdms
                .map(rdm -> toBean.transformTuple(extractValues(rdm, fieldCount, coreFieldSelectors, extendedFieldSelectors), aliases))
                .collect(Collectors.toList());
    }

    /**
     * Extract the specified values from an RDM as an Object[].
     *
     * @param rdm                    RDM from which to extract values
     * @param fieldCount             Count of fields to extract
     * @param coreFieldSelectors     Core field selectors to process
     * @param extendedFieldSelectors Extended field selectors to process
     * @return Object[]
     * @since 2.30
     */
    private static Object[] extractValues(IRaptorDataMessage rdm, int fieldCount, List<CoreFieldSelector> coreFieldSelectors, List<StructureFieldSelector> extendedFieldSelectors)
    {
        final Object[] values = new Object[fieldCount];
        int index = 0;

        // Extract all core fields
        for (CoreFieldSelector coreFieldSelector : coreFieldSelectors)
        {
            values[index++] = getCoreField(rdm, coreFieldSelector.getCoreField());
        }

        // Extract all extended fields
        if (!extendedFieldSelectors.isEmpty())
        {
            validateStructures(rdm, extendedFieldSelectors.get(0).getStructureName());

            for (StructureFieldSelector structureFieldSelector : extendedFieldSelectors)
            {
                values[index++] = rdm.getField(structureFieldSelector.getFieldName());
            }
        }

        return values;
    }

    /**
     * Validate that the structure used in the field selector is the same as the message type on the RDM.
     *
     * @param rdm           RDM to test
     * @param structureName Structure name specified in the field selectors
     * @since 2.30
     */
    private static void validateStructures(IRaptorDataMessage rdm, String structureName)
    {
        final String messageType = rdm.getMessageType();

        if (!messageType.equals(structureName))
        {
            throw new IllegalStateException("Different structures in use: " + messageType + " != " + structureName);
        }
    }

    /**
     * Get a core field value from the given RDM.
     *
     * @param rdm       RDM from which to extract value
     * @param coreField The core field to extract
     * @return The extracted field value
     */
    public static Object getCoreField(IRaptorDataMessage rdm, CoreField coreField)
    {
        switch (coreField)
        {
            case id:
                return rdm.getId();
            case receiveTime:
                return rdm.getReceiveTime();
            case time:
                return rdm.hasTime() ? rdm.getTime() : null;
            case relevantTime:
                return rdm.getRelevantTime();
            case latitude:
                return rdm.getLatitude();
            case longitude:
                return rdm.getLongitude();
            case elevation:
                return rdm.getElevation();
            case speed:
                return rdm.getSpeedInMps();
            case heading:
                return rdm.getHeading();
            case pitch:
                return rdm.getPitch();
            case roll:
                return rdm.getRoll();
            case sourceName:
                return rdm.getSourceName();
            case sourceType:
                return rdm.getSourceType().ordinal();
            case family:
                return rdm.getFamily();
            case type:
                return rdm.getType();
            case unitId:
                return rdm.getUnitID();
            case hasMotion:
                return rdm.isMotionSet() ? rdm.hasMotion() : null;
            case hasLocation:
                return rdm.hasLocation();
            case hasElevation:
                return rdm.hasElevation();
            case hasHeading:
                return rdm.hasHeading();
            case has3dOrientation:
                return rdm.has3dOrientation();
            case hasSpeed:
                return rdm.hasSpeed();
            case hasTime:
                return rdm.hasTime();
            case isManualLocation:
                return rdm.isManuallyGeneratedLocation();
            case isRenderable:
                return rdm.isRenderable();
            case trackStyleId:
            {
                Integer v = rdm.getTrackStyleDefinitionId();
                return v != null ? v : 0;
            }
            case overrideTrackStyleId:
            {
                Integer v = rdm.getOverrideTrackStyleDefinitionId();
                return v != null ? v : 0;
            }
            case uuid:
                return rdm.getUUID();
            case isImported:
                return rdm.isImported();

            default:
                logger.error("Unknown CORE field: {}", coreField);
                return null;
        }
    }

    private FieldExtractor()
    {
        /* no instances */
    }
}
