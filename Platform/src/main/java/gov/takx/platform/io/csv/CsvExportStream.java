package gov.takx.platform.io.csv;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.platform.utils.UnitsFormat;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import gov.takx.platform.utils.units.UnitsFormatUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Consumer;

/**
 * A export stream that writes {@link IRaptorDataMessage} instances in CSV format. Since a CSV file can represent only
 * one structure, CSV export is limited to only one type of data message. It can hold data on multiple devices, as long
 * as all the devices use the same structure (for example, you can export all position messages from several Bloodhound
 * devices).
 * <p/>
 * The user can provide a map to filter which fields should be included, and what they should be called in the CSV
 * header row.
 * <p/>
 * Example: To set up an export stream exporting only latitude and longitude data with names "lat" and "lng".
 * <p/>
 * <pre>
 * try {
 *     export = new CsvExportStream(myOutputStream, myDataStruct);
 *     java.util.Map&lt;String, String&gt; map = new HashMap&lt;String, String&gt;();
 *     map.put(CsvFields.LATITUDE, "lat");
 *     map.put(CsvFields.LONGITUDE, "lng");
 *     export.setMapping(map);
 * }
 * catch (IOException e) { }
 * </pre>
 *
 * @see CsvFields
 */
public class CsvExportStream implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Writer out;
    private final CsvEmitter emitter;
    private boolean headerWritten;
    private boolean useHeader;

    /**
     * Create an export stream to write messages in CSV format to <tt>os</tt>.
     *
     * @param os     Stream to receive CSV data.
     * @param struct The structure of the messages to be exported. A
     *               CSV file can only store a single data structure.
     * @throws IOException on write error
     */
    public CsvExportStream(OutputStream os, IRaptorDataStructure struct)
            throws IOException
    {
        out = new PrintWriter(os);
        emitter = new CsvEmitter();
        emitter.initializeMapping(struct);

        useHeader = true;
    }

    public CsvExportStream(OutputStream os, List<CsvMapping> mapping)
    {
        out = new PrintWriter(os);
        emitter = new CsvEmitter();
        useHeader = true;
        setMapping(mapping);
    }

    public void setMapping(List<CsvMapping> mapping)
    {
        emitter.setMapping(mapping);
    }

    public List<CsvMapping> getMapping()
    {
        return emitter.getMapping();
    }

    public void setUseHeader(boolean val)
    {
        useHeader = val;
    }

    public boolean getUseHeader()
    {
        return useHeader;
    }

    /**
     * Set the units format used by the underlying emitter.
     *
     * @param unitsFormat Format to use
     * @deprecated Use {@link #setAltitudeUnits(AltitudeUnits)} and {@link #setSpeedUnits(SpeedUnits)} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public void setUnitsFormat(UnitsFormat unitsFormat)
    {
        setAltitudeUnits(UnitsFormatUtils.toAltitudeUnits(unitsFormat));
        setSpeedUnits(UnitsFormatUtils.toSpeedUnits(unitsFormat));
    }

    /**
     * Set the altitude units used by the underlying emitter.
     *
     * @param altitudeUnits The altitude units to use
     * @since 5.5
     */
    public void setAltitudeUnits(@Nonnull AltitudeUnits altitudeUnits)
    {
        emitter.setAltitudeUnits(altitudeUnits);
    }

    /**
     * Set the speed units used by the underlying emitter.
     *
     * @param speedUnits The speed units to use
     * @since 5.5
     */
    public void setSpeedUnits(@Nonnull SpeedUnits speedUnits)
    {
        emitter.setSpeedUnits(speedUnits);
    }

    /**
     * @param csvTransformer The Transforming function to be applied to each CSV String Array before it is written to the
     *                       output stream.
     */
    public void setCsvTransformer(Consumer<String[]> csvTransformer)
    {
        emitter.setCsvTransformer(csvTransformer);
    }

    /**
     * Sets whether each field should be surrounded by quotes, oe if quotes should be suppressed.
     * <p>
     * The default behavior is to include quotes.
     *
     * @param useQuotes True to include quotes around each field, false to exclude quotes.
     */
    public void setUseQuotes(boolean useQuotes)
    {
        emitter.setUseQuotes(useQuotes);
    }

    /**
     * If true, each field in the CSV record will be surrounded by quotes.
     *
     * @return True if quotes are employed, false if they are suppressed.
     */
    public boolean isUseQuotes()
    {
        return emitter.isUseQuotes();
    }

    /**
     * Serialize a RaptorDataMessage to CSV, and write the serialized
     * message to an output stream. If this is the first message, the
     * header row will be written first.
     *
     * @param msg The message to serialize.
     * @throws IOException on write error
     */
    public void write(IRaptorDataMessage msg) throws IOException
    {
        logger.debug("writing message");

        if (useHeader && !headerWritten)
        {
            emitter.writeHeader(out);
            headerWritten = true;
        }

        emitter.write(msg, out);
    }

    /**
     * Close the stream.
     *
     * @throws IOException If the stream cannot be closed.
     */
    @Override
    public void close() throws IOException
    {
        emitter.writeFooter(out);
        out.close();
    }

    /**
     * Reset the stream so that it will write the CSV header before
     * the next data line.
     */
    public void reset()
    {
        headerWritten = false;
    }
}
