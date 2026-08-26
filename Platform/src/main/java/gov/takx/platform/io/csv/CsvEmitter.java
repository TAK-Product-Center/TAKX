package gov.takx.platform.io.csv;

import au.com.bytecode.opencsv.CSVWriter;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.messages.DataFieldEncoding;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.commons.date.DateTimeParser;
import gov.takx.platform.io.DefaultEmitter;
import gov.takx.platform.utils.UnitsFormat;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import gov.takx.platform.utils.units.UnitsFormatUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Emitter that writes data {@link IRaptorDataMessage} instances in CSV format. Emitters are similar
 * to ExportStreams, but work at a lower level and provide more direct
 * control over how things are exported. If you just want to write a
 * bunch of data to a stream, you probably want {@code CsvExportStream}.
 * <p/>
 * <b>Note:</b>Dates are emitted using the ISO 8601 Date Format
 *
 * @see CsvExportStream
 * @see CsvFields
 * @see DataFieldEncoding#DEFAULT_DATE_FORMATTER
 */

public class CsvEmitter extends DefaultEmitter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String DELIMITER = DataFieldEncoding.DELIMITER;

    private Consumer<String[]> csvTransformer = null;

    private boolean useQuotes = true;
    private final List<CsvMapping> mapping = new ArrayList<>();
    private static final DateTimeParser DATE_FORMAT = DataFieldEncoding.DEFAULT_DATE_FORMATTER;

    private AltitudeUnits altitudeUnits;
    private SpeedUnits speedUnits;

    /**
     * Construct the emitter. Initially the emitter has an empty
     * mapping. To get a meaningful export, either set a mapping, or call
     * initializeMapping() to build a map from a data structure.
     *
     * @see #setMapping
     * @see #initializeMapping
     */
    public CsvEmitter()
    {
        // Altitude default to meters since import defaults to meters, this way the defaults work rather
        // than requiring the user to intervene.
        altitudeUnits = AltitudeUnits.METERS;
        speedUnits = SpeedUnits.KMPH;
    }

    /**
     * @deprecated Use {@link #setAltitudeUnits(AltitudeUnits)} and {@link #setSpeedUnits(SpeedUnits)} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public void setUnitsFormat(UnitsFormat format)
    {
        setAltitudeUnits(UnitsFormatUtils.toAltitudeUnits(format));
        setSpeedUnits(UnitsFormatUtils.toSpeedUnits(format));
    }

    /**
     * @param altitudeUnits The altitude units to use when displaying altitude
     * @since 5.5
     */
    public void setAltitudeUnits(@Nonnull AltitudeUnits altitudeUnits)
    {
        this.altitudeUnits = altitudeUnits;
    }

    /**
     * @param speedUnits The speed units to use when displaying speed
     * @since 5.5
     */
    public void setSpeedUnits(@Nonnull SpeedUnits speedUnits)
    {
        this.speedUnits = speedUnits;
    }

    /**
     * @param csvTransformer The Transforming function to be applied to each CSV String Array before it is written to the
     *                       output stream.
     */
    public void setCsvTransformer(Consumer<String[]> csvTransformer)
    {
        this.csvTransformer = csvTransformer;
    }

    /**
     * Sets whether each field should be surrounded by quotes, or if quotes should be suppressed.
     * <p>
     * The default behavior is to include quotes.
     *
     * @param useQuotes True to include quotes around each field, false to exclude quotes.
     */
    public void setUseQuotes(boolean useQuotes)
    {
        this.useQuotes = useQuotes;
    }

    /**
     * If true, each field in the CSV record will be surrounded by quotes.
     *
     * @return True if quotes are employed, false if they are suppressed.
     */
    public boolean isUseQuotes()
    {
        return useQuotes;
    }

    public void setMapping(List<CsvMapping> mapping)
    {
        this.mapping.clear();
        this.mapping.addAll(mapping);
    }

    public List<CsvMapping> getMapping()
    {
        return Collections.unmodifiableList(mapping);
    }

    void initializeMapping(IRaptorDataStructure struct)
    {
        Collection<IDataField> fields = struct.getFields();
        mapping.clear();

        mapping.add(new CsvMapping(CsvFields.TYPE, CsvFields.TYPE));
        mapping.add(new CsvMapping(CsvFields.VERSION, CsvFields.VERSION));
        mapping.add(new CsvMapping(CsvFields.ID, CsvFields.ID));
        mapping.add(new CsvMapping(CsvFields.KEY, CsvFields.KEY));
        mapping.add(new CsvMapping(CsvFields.LATITUDE, CsvFields.LATITUDE));
        mapping.add(new CsvMapping(CsvFields.LONGITUDE, CsvFields.LONGITUDE));
        mapping.add(new CsvMapping(CsvFields.ELEVATION, CsvFields.ELEVATION));
        mapping.add(new CsvMapping(CsvFields.MANUALLY_GENERATED_LOCATION, CsvFields.MANUALLY_GENERATED_LOCATION));
        mapping.add(new CsvMapping(CsvFields.PITCH, CsvFields.PITCH));
        mapping.add(new CsvMapping(CsvFields.HEADING, CsvFields.HEADING));
        mapping.add(new CsvMapping(CsvFields.ROLL, CsvFields.ROLL));
        mapping.add(new CsvMapping(CsvFields.SPEED, CsvFields.SPEED));
        mapping.add(new CsvMapping(CsvFields.MOTION, CsvFields.MOTION));
        mapping.add(new CsvMapping(CsvFields.TIME, CsvFields.TIME));
        mapping.add(new CsvMapping(CsvFields.RECEIVE_TIME, CsvFields.RECEIVE_TIME));

        for (IDataField field : fields)
        {
            String name = field.getFieldName();
            mapping.add(new CsvMapping(name, name));
        }
    }

    @Override
    public void writeHeader(Writer out) throws IOException
    {
        String[] line = new String[mapping.size()];

        CSVWriter writer = new CSVWriter(out);

        for (int i = 0; i < mapping.size(); i++)
        {
            String to = mapping.get(i).to;
            if (to != null)
            {
                if (mapping.get(i).to.equals(CsvFields.ELEVATION))
                {
                    to = to + " (" + altitudeUnits.getAbbreviation() + ")";
                } else if (mapping.get(i).to.equals(CsvFields.SPEED))
                {
                    to = to + " (" + speedUnits.getAbbreviation() + ")";
                }
                line[i] = to;
            }
        }

        writer.writeNext(line);
        writer.flush();
    }

    @Override
    public void write(IRaptorDataMessage msg, Writer out) throws IOException
    {
        if (useQuotes)
        {
            writeCsv(msg, new CSVWriter(out));
        } else
        {
            writeCsv(msg, new CSVWriter(out, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER));
        }
    }

    /**
     * Base method for writing the RDM to the provided writer.  This method allows for passing in a custom
     * {@link CSVWriter} object so that custom writer options can be employed.
     *
     * @param msg    The RDM to write out as a CSV line.
     * @param writer The writer to receive the output.
     * @throws IOException If something goes writing to the provided writer.
     */
    private void writeCsv(IRaptorDataMessage msg, CSVWriter writer) throws IOException
    {
        final int size = mapping.size();
        final String[] line = new String[size];

        for (int i = 0; i < size; i++)
        {
            final String field = mapping.get(i).from;

            switch (field)
            {
                case CsvFields.TYPE:
                    line[i] = msg.getMessageType();
                    break;
                case CsvFields.VERSION:
                    line[i] = msg.getVersion();
                    break;
                case CsvFields.ID:
                    line[i] = msg.getUnitID();
                    break;
                case CsvFields.KEY:
                    line[i] = msg.getUniqueKey();
                    break;
                case CsvFields.LATITUDE:
                    line[i] = String.valueOf(msg.getLatitude());
                    break;
                case CsvFields.LONGITUDE:
                    line[i] = String.valueOf(msg.getLongitude());
                    break;
                case CsvFields.ELEVATION:
                    line[i] = String.valueOf(msg.getElevation() * altitudeUnits.getMultiplier());
                    break;
                case CsvFields.MANUALLY_GENERATED_LOCATION:
                    line[i] = String.valueOf(msg.isManuallyGeneratedLocation());
                    break;
                case CsvFields.PITCH:
                    line[i] = String.valueOf(msg.getPitch());
                    break;
                case CsvFields.HEADING:
                    line[i] = String.valueOf(msg.getHeading());
                    break;
                case CsvFields.ROLL:
                    line[i] = String.valueOf(msg.getRoll());
                    break;
                case CsvFields.SPEED:
                    line[i] = String.valueOf(msg.getSpeedInMps() * speedUnits.getMultiplier());
                    break;
                case CsvFields.MOTION:
                    line[i] = msg.isMotionSet() ? String.valueOf(msg.hasMotion()) : "";
                    break;
                case CsvFields.TIME:
                    long time = msg.getRelevantTime();
                    line[i] = time != 0L
                            ? DATE_FORMAT.format(Instant.ofEpochMilli(time))
                            : "";
                    break;
                case CsvFields.RECEIVE_TIME:
                    final long receiveTime = msg.getReceiveTime();
                    line[i] = receiveTime != 0L
                            ? DATE_FORMAT.format(Instant.ofEpochMilli(receiveTime))
                            : "";
                    break;
                default:
                    line[i] = getField(field, msg, msg.getStructure());
                    break;
            }
        }

        if (csvTransformer != null)
        {
            csvTransformer.accept(line);
        }

        writer.writeNext(line);
        writer.flush();
    }

    /**
     * Get the given field from the message and format it.
     *
     * @param fieldName Name of field to extract
     * @param rdm       Message from which to extract field
     * @param struct    Structure defining the message fields
     * @return The formatted field value to write in the exported CSV record.
     */
    private String getField(String fieldName, IRaptorDataMessage rdm, IStructure struct)
    {
        final IDataField dataField = struct.getField(fieldName);

        if (dataField == null)
        {
            logger.warn("Unknown field '{} in structure {}", fieldName, struct.getMessageType());
            return null;
        }

        return DataFieldEncoding.encodeFieldValue(fieldName, dataField.getFieldType(), rdm.getField(fieldName), DATE_FORMAT);
    }
}
