package gov.takx.core.globe;

import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IFavoritesListener;
import gov.takx.api.globe.IFavoritesManager;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.platform.globe.Favorite;
import gov.takx.platform.preferences.PreferencesNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Abstract base class for favorites managers.  Basic functions to manage an ordered (indexed) set
 * of favorites in a preferences store are provided.
 *
 * @since massive update in 2.35
 */
public abstract class AFavoritesManager implements IFavoritesManager, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Derived classes can specify a set of ids to ignore (w.r.t. ordering).  These nodes will not be
     * included in the indexing/ordering operations.
     */
    protected final Collection<String> unIndexedIds = new ArrayList<>();

    /**
     * Ordered list of bookmarks.  Does not contain the un-indexed bookmarks.
     */
    private List<IndexedFavorite> bookmarks = new ArrayList<>();

    /**
     * Map from id (UUID or pre-defined string) to bookmark.  This DOES contain the un-indexed bookmarks.
     */
    private Map<String, IndexedFavorite> bookmarksById = new HashMap<>();

    private PreferencesNode bookmarksPrefsNode;
    private final List<IFavoritesListener> listeners = new CopyOnWriteArrayList<>();

    protected AFavoritesManager()
    {
    }

    /**
     * Construct on the given preferences store.
     *
     * @param unIndexedIds List of IDs to ignore when loading
     */
    protected AFavoritesManager(Collection<String> unIndexedIds)
    {
        this.unIndexedIds.addAll(unIndexedIds);
    }

    /**
     * Clear out our state.
     */
    protected void clear()
    {
        bookmarks.clear();
        bookmarksById.clear();
        listeners.clear();
        bookmarksPrefsNode = null;
    }

    /**
     * Load the bookmarks from preferences.
     *
     * @param preferenceEntry Preference entry from which to load our bookmarks
     */
    protected void loadPreferences(IPreferenceEntry preferenceEntry)
    {
        if (logger.isDebugEnabled()) logger.debug("Load: {} from {}", getClass().getName(), preferenceEntry);

        bookmarksPrefsNode = preferenceEntry.node();

        // Add ALL favorites to the map by Id
        bookmarksById = bookmarksPrefsNode.childrenToMap(IndexedFavorite::new);

        // For all indexed favorites, add a listener and collect into the ordered list
        bookmarks = bookmarksById.values().stream()
                .filter(favorite -> !unIndexedIds.contains(favorite.id))
                .peek(favorite -> favorite.favorite.addPropertyChangeListener(this))
                .collect(Collectors.toList());

        bookmarks.sort(Comparator.naturalOrder()); // Sort by existing index
        flattenIndices();
    }

    /**
     * Flatten the indices (reassign them sequentially from 0).
     */
    private void flattenIndices()
    {
        for (int i = 0, count = bookmarks.size(); i < count; i++)
        {
            bookmarks.get(i).setIndex(i);
        }
    }

    @Override
    public synchronized void setIndex(IFavorite favorite, int index)
    {
        int oldIndex = getIndex(favorite);
        if (oldIndex >= 0)
        {
            final IndexedFavorite indexedFavorite = bookmarks.remove(oldIndex);

            bookmarks.add(index, indexedFavorite);

            flattenIndices(); // Reassign indices based on new ordering

            fireBookmarkEdited(favorite, index);
        } else
        {
            logger.error("Error changing index. Could not find: {}", favorite);
        }
    }

    @Override
    public synchronized void add(IFavorite bookmark)
    {
        final String id = UUID.randomUUID().toString();

        if (logger.isDebugEnabled()) logger.debug("Assign {} to: {}", id, bookmark);

        final int index = bookmarks.size() - 1;
        final IndexedFavorite indexedFavorite = new IndexedFavorite(bookmarksPrefsNode, id, index, bookmark);

        bookmarks.add(indexedFavorite);
        bookmarksById.put(id, indexedFavorite);

        bookmark.addPropertyChangeListener(this);

        callListeners(l -> l.onFavoriteAdded(bookmark), "added");
    }

    @Override
    public int count()
    {
        return bookmarks.size();
    }

    @Override
    public IFavorite get(int index)
    {
        return bookmarks.get(index).favorite;
    }

    @Override
    public synchronized void remove(int index)
    {
        final IndexedFavorite indexedFavorite = bookmarks.remove(index);

        indexedFavorite.removeNode();
        bookmarksById.remove(indexedFavorite.id);

        indexedFavorite.favorite.removePropertyChangeListener(this);

        flattenIndices();

        callListeners(l -> l.onFavoriteRemoved(indexedFavorite.favorite), "removed");
    }

    @Override
    public synchronized void remove(IFavorite bookmark)
    {
        final int index = getIndex(bookmark);
        if (index != -1)
        {
            remove(index);
        }
    }

    /**
     * @return Index of given bookmark within our ordered list, or -1 if not found
     */
    private int getIndex(IFavorite bookmark)
    {
        for (int i = 0, count = bookmarks.size(); i < count; i++)
        {
            if (bookmarks.get(i).favorite.equals(bookmark)) return i;
        }
        return -1;
    }

    @Override
    public List<IFavorite> getBookmarks()
    {
        return bookmarks.stream()
                .map(indexedFavorite -> indexedFavorite.favorite)
                .collect(Collectors.toList());
    }

    @Override
    public void addListener(IFavoritesListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IFavoritesListener listener)
    {
        listeners.remove(listener);
    }

    private void fireBookmarkEdited(IFavorite bookmark, int index)
    {
        callListeners(l -> l.onFavoriteChanged(bookmark, index), "changed");
    }

    /**
     * Invoke a operation on each registered listener, protecting against exceptions in the listener.
     *
     * @param handler  Handler to invoke with listener
     * @param callType Description of change, for error message
     */
    private void callListeners(Consumer<IFavoritesListener> handler, String callType)
    {
        for (IFavoritesListener listener : listeners)
        {
            try
            {
                handler.accept(listener);
            } catch (Exception e)
            {
                logger.warn("Exception notifying listener of bookmark {}", callType, e);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final IFavorite bookmark = (IFavorite) evt.getSource();
        final int index = getIndex(bookmark);

        update(bookmark, bookmarks.get(index).id);

        fireBookmarkEdited(bookmark, index);
    }

    /**
     * Get the favorite by its id.
     *
     * @param id Id of favorite to get
     * @return Favorite, {@code null} if not found
     */
    protected IFavorite getById(String id)
    {
        final IndexedFavorite indexedFavorite = bookmarksById.get(id);
        return indexedFavorite != null ? indexedFavorite.favorite : null;
    }

    /**
     * Update the contents of the preferences with the given favorite data.  The index is unchanged.
     *
     * @param bookmark Bookmark/favorite to update in preferences store
     * @param id       Id of favorite
     */
    protected void update(IFavorite bookmark, String id)
    {
        if (logger.isDebugEnabled()) logger.debug("{}.update(bookmark, {})", getClass().getName(), id);

        final IndexedFavorite indexedFavorite = bookmarksById.computeIfAbsent(id,
                k -> new IndexedFavorite(bookmarksPrefsNode, id, 0, bookmark));

        indexedFavorite.updateFrom(bookmark);

        dumpFavoriteNode("update", id, indexedFavorite);
    }

    /**
     * Debug support to dump out a preferences node representing a favorite.
     *
     * @param tag  Tag for output
     * @param id   Id of favorite
     * @param node Node to dump, may be {@code null} in which case nothing is output
     */
    private void dumpFavoriteNode(String tag, String id, PreferencesNode node)
    {
        if (node == null) return;

        if (logger.isDebugEnabled())
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("Dump favorite: ").append(tag).append(": ");

            node.dump(sb);

            logger.debug(sb.toString());
        }
    }

    /**
     * Interns the preference for a single favorite
     * <p>
     * Holds an index, id, and favorite tuple so it can be managed as a whole.
     * The natural ordering is based on the index.
     * <p>
     * The properties of the favorite write through to the preferences.
     */
    private static final class IndexedFavorite extends PreferencesNode implements Comparable<IndexedFavorite>
    {
        private static final String INDEX = "Index";
        private static final String OWNER = "Owner";
        private static final String DESCRIPTION = "Description";
        private static final String LATITUDE = "Latitude";
        private static final String LONGITUDE = "Longitude";
        private static final String ELEVATION = "Elevation";
        private static final String HEADING = "Heading";
        private static final String PITCH = "Pitch";
        private static final String ZOOM = "Zoom";

        private final String id;
        private int index;
        private final IFavorite favorite;

        /**
         * Construct from a given preferences node.
         *
         * @param node Preferences node
         */
        IndexedFavorite(PreferencesNode node)
        {
            super(node);

            id = node.name();
            index = node.getInt(INDEX, Integer.MAX_VALUE);

            String owner = node.get(OWNER, Favorite.USER_OWNER);
            String description = node.get(DESCRIPTION, "");
            double latitude = node.getDouble(LATITUDE, 0);
            double longitude = node.getDouble(LONGITUDE, 0);
            double elevation = node.getDouble(ELEVATION, 0);
            double heading = node.getDouble(HEADING, 0);
            double pitch = node.getDouble(PITCH, 0);
            double zoom = node.getDouble(ZOOM, 0);

            favorite = Favorite.createViewBookmark(owner, description, latitude, longitude, elevation, heading, pitch, zoom);
        }

        /**
         * Constructor when creating a new node from the given Favorite.
         *
         * @param parent   Preferences node in which to create the favorite node
         * @param id       Id of node to create
         * @param index    Index of favorite
         * @param favorite Favorite
         */
        IndexedFavorite(PreferencesNode parent, String id, int index, IFavorite favorite)
        {
            super(parent.node(id));

            this.id = id;
            this.index = index;
            this.favorite = favorite;

            updateFrom(favorite);
        }

        /**
         * Set the index on this favorite.
         */
        private void setIndex(int index)
        {
            this.index = index;
            put(INDEX, index);
        }

        /**
         * Update the preference values from the given favorite.
         */
        private void updateFrom(IFavorite favorite)
        {
            put(OWNER, favorite.getOwner());
            put(DESCRIPTION, favorite.getDescription());
            put(LATITUDE, favorite.getLatitude());
            put(LONGITUDE, favorite.getLongitude());
            put(ELEVATION, favorite.getElevation());
            put(HEADING, favorite.getHeading());
            put(PITCH, favorite.getPitch());
            put(ZOOM, favorite.getZoom());
            put(INDEX, index);
        }

        @Override
        public int compareTo(IndexedFavorite o)
        {
            return Integer.compare(index, o.index);
        }
    }
}
