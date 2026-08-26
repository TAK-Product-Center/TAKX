package gov.takx.core.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.commons.constants.CommServiceType;
import org.apache.commons.lang3.time.DatePrinter;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import static gov.takx.api.messages.RaptorDataMessageColumns.ALL_RDM_COLUMNS;

/**
 * Utility methods for working with the Postgres Copy operation.
 *
 * @since 2.14
 */
@SuppressWarnings("unused") // Methods called from generated code
@Internal
public class PostgresCopyUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Delimiter to be used to delineate fields in COPY data buffers.
     *
     * @since 2.14
     */
    public static final char COPY_DELIMITER = '|';

    /**
     * The NULL field marker.
     */
    public static final String NULL_FIELD_MARKER = "\\N";

    /**
     * The SQL to use to perform a bulk copy of RDM core data.
     *
     * @since 2.14
     */
    public static final String RDM_COPY_SQL =
            "COPY MapPoint (" + ALL_RDM_COLUMNS + ") from STDIN with delimiter '" + COPY_DELIMITER + "'";

    private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // Thread-safe date formatter
    private static final DatePrinter DATE_PRINTER = FastDateFormat.getInstance(ISO_8601_DATE_FORMAT);

    /**
     * Construct the COPY data for the given list of RDMs.
     *
     * @param data String builder to which to append
     * @param rdms RDMs to process
     */
    public static void buildCopyData(StringBuilder data, Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        for (IRaptorDataMessage rdm : rdms)
        {
            buildCopyData(data, rdm);
        }
    }

    /**
     * Construct the COPY data for the given RDM.
     *
     * @param data String builder to which to append
     * @param rdm  RDM to process
     */
    public static void buildCopyData(StringBuilder data, IRaptorDataMessage rdm)
    {
        final CommServiceType sourceType = rdm.getSourceType();
        final long uuidMsb = rdm.getUuidMsb();
        final long uuidLsb = rdm.getUuidLsb();
        final Boolean hasMotion = rdm.isMotionSet() ? rdm.hasMotion() : null;

        data.append(rdm.getIdAsInt()).append(COPY_DELIMITER);
        data.append(rdm.getMapObject().getId()).append(COPY_DELIMITER);
        data.append(rdm.getStructure().getId()).append(COPY_DELIMITER);
        data.append(uuidMsb).append(COPY_DELIMITER);
        data.append(uuidLsb).append(COPY_DELIMITER);

        data.append(rdm.getElevation()).append(COPY_DELIMITER);

        data.append(rdm.has3dOrientation()).append(COPY_DELIMITER);
        data.append(rdm.hasElevation()).append(COPY_DELIMITER);
        data.append(rdm.hasHeading()).append(COPY_DELIMITER);
        data.append(rdm.hasLocation()).append(COPY_DELIMITER);

        data.append(hasMotion == null ? NULL_FIELD_MARKER : hasMotion).append(COPY_DELIMITER);

        data.append(rdm.hasSpeed()).append(COPY_DELIMITER);
        data.append(rdm.hasTime()).append(COPY_DELIMITER);
        data.append(rdm.getHeading()).append(COPY_DELIMITER);
        data.append(rdm.isManuallyGeneratedLocation()).append(COPY_DELIMITER);
        data.append(rdm.isRenderable()).append(COPY_DELIMITER);
        data.append(rdm.getLatitude()).append(COPY_DELIMITER);
        data.append(rdm.getLongitude()).append(COPY_DELIMITER);

        final Integer overrideTrackStyleDefinitionId = rdm.getOverrideTrackStyleDefinitionId();
        data.append(overrideTrackStyleDefinitionId == null ? NULL_FIELD_MARKER : overrideTrackStyleDefinitionId).append(COPY_DELIMITER);

        data.append(rdm.getPitch()).append(COPY_DELIMITER);
        data.append(rdm.getReceiveTime()).append(COPY_DELIMITER);

        encodeTimestamp(data, new Date(rdm.getRelevantTime()));
        data.append(COPY_DELIMITER);

        data.append(rdm.getRoll()).append(COPY_DELIMITER);

        encodeString(data, rdm.getSourceName());
        data.append(COPY_DELIMITER);

        data.append(sourceType == null ? NULL_FIELD_MARKER : sourceType.ordinal()).append(COPY_DELIMITER);
        data.append(rdm.getSpeedInMps()).append(COPY_DELIMITER);
        data.append(rdm.getTime()).append(COPY_DELIMITER);

        final Integer trackStyleDefinitionId = rdm.getTrackStyleDefinitionId();
        data.append(trackStyleDefinitionId == null ? NULL_FIELD_MARKER : trackStyleDefinitionId).append(COPY_DELIMITER);

        data.append(rdm.isImported());

        data.append('\n'); // End of data line
    }

    /**
     * Encode a String value into the COPY data.  As appropriate, characters in the string will be escaped.
     *
     * @param data  String builder to which to append
     * @param value String value to encode
     */
    public static void encodeString(StringBuilder data, String value)
    {
        if (value == null)
        {
            data.append(NULL_FIELD_MARKER);
        } else
        {
            final int length = value.length();
            for (int i = 0; i < length; i++)
            {
                final char c = value.charAt(i);

                switch (c)
                {
                    case '\\' -> data.append("\\\\");
                    case COPY_DELIMITER -> data.append("\\|");
                    case '\r' -> data.append("\\r");
                    case '\n' -> data.append("\\n");
                    default -> data.append(c);
                }
            }
        }
    }

    /**
     * Encode a Timestamp value into the COPY data.
     *
     * @param data  String builder to which to append
     * @param value Date value to encode in timestamp string form
     */
    public static void encodeTimestamp(StringBuilder data, Date value)
    {
        DATE_PRINTER.format(value, data);
    }

    /**
     * Encode a byte[] value into the COPY data.
     *
     * @param data  String builder to which to append
     * @param value byte[] value to encode, as a hex string
     */
    public static void encodeBytes(StringBuilder data, byte[] value)
    {
        data.append("\\\\x");

        for (byte b : value)
        {
            data.append(Integer.toHexString((b >>> 4) & 0x0F));
            data.append(Integer.toHexString(0x0F & b));
        }
    }
}
