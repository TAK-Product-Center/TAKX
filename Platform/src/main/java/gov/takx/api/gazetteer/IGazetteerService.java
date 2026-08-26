package gov.takx.api.gazetteer;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.Collection;

/**
 * Contract for the gazetteer service, which offers a simple mechanism to search a database
 * (actually a simple CSV text file) of city names, providing the location of matched cities.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface IGazetteerService
{
    /**
     * The default maximum number of search results to return from {@link #search(String)}.
     */
    int DEFAULT_MAX_RESULTS = 500;

    /**
     * Execute the search query, limiting the result set to {@link #DEFAULT_MAX_RESULTS}.
     *
     * @param query Query to execute
     * @return Non-null collection of entries that match the query
     */
    default Collection<GazetteerEntry> search(String query)
    {
        return search(query, DEFAULT_MAX_RESULTS);
    }

    /**
     * Execute the search query, limiting the result set to a maximum number of hits.
     *
     * @param query      Query to execute
     * @param maxResults Maximum number of hits to return
     * @return Non-null collection of entries that match the query
     */
    Collection<GazetteerEntry> search(String query, int maxResults);

    /**
     * Count how many entries match the given query.
     *
     * @param query Query to execute
     * @return Number of hits identified by the Gazetteer search provider
     */
    int count(String query);
}
