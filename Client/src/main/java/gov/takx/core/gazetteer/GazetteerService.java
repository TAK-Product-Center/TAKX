package gov.takx.core.gazetteer;

import au.com.bytecode.opencsv.CSVReader;
import gov.takx.api.gazetteer.GazetteerEntry;
import gov.takx.api.gazetteer.IGazetteerService;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gazetteer search service.  Offers partial name matching search of a geolocation database of world cities.
 * <p>
 * The gazetteer data is stored in a simple CSV text file with 3 columns:
 * <ul>
 *     <li>Name: Full name of the city (this is what we search against)</li>
 *     <li>Latitude</li>
 *     <li>Longitude</li>
 * </ul>
 *
 * @since 3.0
 */
@ApplicationScoped
public class GazetteerService implements IGazetteerService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String WORLD_CITIES_CSV = "gazetteer/worldCities.csv";

    private List<GazetteerEntry> gazetteerEntries;

    @Override
    public Collection<GazetteerEntry> search(String query, int maxResults)
    {
        return searchStream(query)
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    @Override
    public int count(String query)
    {
        return (int) searchStream(query).count();
    }

    /**
     * Read and parse the gazetteer source data.
     *
     * @return Data parsed into a list of {@link GazetteerEntry}
     */
    private synchronized List<GazetteerEntry> getGazetteerData()
    {
        if (gazetteerEntries == null)
        {
            try (var reader = new FileReader(WORLD_CITIES_CSV))
            {
                gazetteerEntries = new CSVReader(reader).readAll().stream()
                        .skip(1) // skip header row
                        .map(this::makeGazetteerEntry)
                        .collect(Collectors.toList());

                if (logger.isDebugEnabled()) logger.debug("Loaded entries: {}", gazetteerEntries.size());
            } catch (IOException e)
            {
                logger.error("Can not read Gazetteer data", e);
                gazetteerEntries = Collections.emptyList();
            }
        }

        return gazetteerEntries;
    }

    /**
     * Search the gazetteer data for a match to the given query phrase.  This search will try to find a match where
     * the given query is at the <em>start</em> of a word in the city/state/country name.
     *
     * @param query Search phrase
     * @return Stream of matching gazetteer entries
     */
    private Stream<GazetteerEntry> searchStream(String query)
    {
        final Pattern pattern = makeQueryPattern(query);

        logger.debug("Pattern: {}", pattern);

        return getGazetteerData().stream()
                .filter(entry -> pattern.matcher(entry.getName()).find());
    }

    /**
     * Convert the given query phrase into a regex pattern.  The phrase will be prepended with "\\b" to attempt
     * to match the beginning of a word in the city full name.  If parsing that pattern fails, then the input string
     * will be quoted, using {@link Pattern#quote(String)}, and the search will be performed on that.
     * <p>
     * This allows the query to include properly formatted regex syntax, falling back to an exact match if parsing
     * the pattern fails.
     *
     * @param query Query phrase
     * @return Pattern to use for matching entries
     */
    private Pattern makeQueryPattern(String query)
    {
        try
        {
            return Pattern.compile("\\b" + query, Pattern.CASE_INSENSITIVE);
        } catch (Exception e)
        {
            logger.debug("Can't compile '{}'; reverting to quoted pattern", query);
            return Pattern.compile(query, Pattern.LITERAL);
        }
    }

    /**
     * Construct a gazetteer entry from the given CSV row, containing the city full name, latitude, and longitude.
     * <p>
     * If either of the lat/lon can not be parsed, the resulting entry will have 0 for both.
     *
     * @param rowData CSV row to intern
     * @return Gazetteer entry created from the row data
     */
    private GazetteerEntry makeGazetteerEntry(String[] rowData)
    {
        final String name = rowData[0];

        try
        {
            return new GazetteerEntry(name, Double.parseDouble(rowData[1]), Double.parseDouble(rowData[2]));
        } catch (NumberFormatException e)
        {
            logger.error("Gazetteer data is invalid: {}", Arrays.toString(rowData));
            return new GazetteerEntry(name, 0, 0);
        }
    }
}
