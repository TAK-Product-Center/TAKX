package gov.takx.core.placemarkoverlay;

import au.com.bytecode.opencsv.CSVReader;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines all the data required to build a placemark overlay and its placemarks.
 *
 */
public class PlacemarkOverlayData
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * All the header values extracted from the placemark overlay data file.
     */
    private final List<String> headers = new ArrayList<>();

    /**
     * A list in which each entry represents a placemark.
     */
    private final List<Map<String, String>> csvMaps = new ArrayList<>();

    /**
     * The display name specified by the user. May be {@code null}, in which case the first column is used.
     */
    private final String customDisplayNameHeader;

    /**
     * The header for the column defining the placemarks' latitudes.
     */
    private String latitudeHeader = null;

    /**
     * The header for the column defining the placemarks' longitudes.
     */
    private String longitudeHeader = null;

    /**
     * The columns specified by the user to use to generate tooltips. May be {@code null},
     * in which case the display name is used.
     */
    private final Collection<String> tooltipColumns;

    /**
     * Constructor.
     *
     * @param csvFile                 The source CSV file.
     * @param delimiter               The delimiter
     * @param customDisplayNameHeader The user-specified column to extract the display names from. May be {@code null}.
     * @param tooltipColumns          The user-specified columns to extract the tooltips from. May be {@code null}.
     * @throws IOException Thrown if there is an error reading the CSV file.
     * @since 1.16
     */
    public PlacemarkOverlayData(File csvFile, char delimiter, String customDisplayNameHeader, Collection<String> tooltipColumns)
            throws IOException
    {
        this.customDisplayNameHeader = customDisplayNameHeader;
        this.tooltipColumns = tooltipColumns == null ? null : new ArrayList<>(tooltipColumns);

        try (CSVReader csvReader = new CSVReader(new FileReader(csvFile), delimiter))
        {
            String[] nextPlacemark;
            parseHeader(csvReader.readNext());

            while ((nextPlacemark = csvReader.readNext()) != null)
            {
                parsePlacemark(nextPlacemark);
            }
        } catch (Exception e)
        {
            logger.error("Error parsing file", e);
            AlwaysOnTopJOptionPane.showErrorDialog("Error parsing CSV file: " + e.getMessage(), "Error Parsing Placemark Overlay");
        }
    }

    /**
     * Add a placemark to the map.
     *
     * @param csvMap A mapping of each of the placemark's headers to its corresponding value.
     * @since 1.16
     */
    public void addPlacemark(Map<String, String> csvMap)
    {
        csvMaps.add(csvMap);
    }

    /**
     * Return the name of the column that contains the latitude values.
     *
     * @return The name of the column that contains the latitude values.
     * @since 1.16
     */
    public String getLatitudeHeader()
    {
        return latitudeHeader;
    }

    /**
     * Return the name of the column that contains the longitude values.
     *
     * @return The name of the column that contains the longitude values.
     * @since 1.16
     */
    public String getLongitudeHeader()
    {
        return longitudeHeader;
    }

    /**
     * Return the name of the column that contains the display names.
     *
     * @return The name of the column that contains the display name values.
     * @since 1.16
     */
    public String getDisplayNameHeader()
    {
        return customDisplayNameHeader == null ? headers.get(0) : customDisplayNameHeader;
    }

    /**
     * Return a placemark value based on the placemark's row and the name of the value's column.
     *
     * @return A placemark value.
     * @since 1.16
     */
    public String getValue(int row, String header)
    {
        return csvMaps.get(row).get(header);
    }

    /**
     * Returns all of the column names for the placemarks.
     *
     * @return The column names.
     */
    public List<String> getHeaders()
    {
        return Collections.unmodifiableList(headers);
    }

    /**
     * Returns the current number of placemarks.
     *
     * @return The current number of placemarks.
     */
    public int getSize()
    {
        return csvMaps.size();
    }

    /**
     * Return the entire row defining one placemark.
     *
     * @param row The row to extract.
     * @return An entire row of values for one placemark.
     */
    public Map<String, String> getCSVMap(int row)
    {
        return csvMaps.get(row);
    }

    /**
     * Get the display name for a placemark.
     *
     * @param row The placemark's row.
     * @return The placemark's display name.
     */
    public String getDisplayName(int row)
    {
        return csvMaps.get(row).get(getDisplayNameHeader());
    }

    /**
     * Get the tooltip for a placemark.
     *
     * @param row The placemark's row.
     * @return The placemark's tooltip.
     */
    public String getToolTip(int row)
    {
        if (tooltipColumns == null)
        {
            return getDisplayName(row);
        } else
        {
            return PlacemarkOverlayUtils.generateTooltip(getDisplayName(row), tooltipColumns, csvMaps.get(row));
        }
    }

    /**
     * Get a placemark's latitude.
     *
     * @param row The placemark's row.
     * @return The placemark's latitude.
     * @since 1.16
     */
    public Double getLatitude(int row)
    {
        return parseDoubleString(csvMaps.get(row).get(latitudeHeader));
    }

    /**
     * Get a placemark's longitude.
     *
     * @param row The placemark's row.
     * @return The placemark's longitude.
     * @since 1.16
     */
    public Double getLongitude(int row)
    {
        return parseDoubleString(csvMaps.get(row).get(longitudeHeader));
    }

    /**
     * Parse a double value out of a String with some error checking.
     *
     * @param doubleString The String value of the double.
     * @return The double value extracted from the String; {@code null} if the String is {@code null}, 0 if it is empty.
     */
    private Double parseDoubleString(String doubleString)
    {
        if (doubleString == null)
        {
            return null;
        }
        return doubleString.isEmpty() ? 0 : Double.parseDouble(doubleString);
    }

    /**
     * Parses the header row of a placemark CSV file, capturing the column values
     * and verifying that header names that qualify as latitude and longitude header
     * names exist.
     *
     * @param headerRow The row to parse.
     * @throws IOException Thrown if the row is an invalid header.
     * @since 1.16
     */
    private void parseHeader(String[] headerRow)
            throws IOException
    {
        if (headerRow == null || headerRow.length < 1)
        {
            throw new IOException("Placemark data file must contain column headers.");
        }

        for (String header : headerRow)
        {
            header = header.trim();

            if (PlacemarkOverlayUtils.isLatitudeColumn(header))
            {
                latitudeHeader = header;
            } else if (PlacemarkOverlayUtils.isLongitudeColumn(header))
            {
                longitudeHeader = header;
            }

            headers.add(header);
        }

        if (latitudeHeader == null || longitudeHeader == null)
        {
            throw new IOException("Invalid placemark coordinates.");
        }
    }

    /**
     * Parses a placemark row, mapping each column name to the value for that column
     * provided by this row.
     *
     * @param placemarkRow The placemark row to parse.
     * @since 1.16
     */
    private void parsePlacemark(String[] placemarkRow)
    {
        int size = Math.min(placemarkRow.length, headers.size());
        Map<String, String> csvMap = new HashMap<>(size);

        for (int i = 0; i < size; i++)
        {
            csvMap.put(headers.get(i), placemarkRow[i]);
        }

        csvMaps.add(csvMap);
    }
}
