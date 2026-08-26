package gov.takx.core.mapdata;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Special extension of MapPoint used to represent a group of map points that all share a common styling context.
 * The group is rendered as a normal marker, but also includes a tool tip that displays the count of the number
 * of map points in the group.
 *
 * @since 1.14
 */
public class MapPointGroup extends MapPoint implements AVList
{
    private final int count;
    private final AVListImpl avlist = new AVListImpl();
    private final Sector searchSector;

    /**
     * Constructor for a group marker including the search sector covered by the group marker.
     *
     * @param pos          Position of the map point
     * @param moId         Map object ID
     * @param count        The count of points represented by this group
     * @param latestTime   The relevant time to use for this group marker
     * @param searchSector The search sector for the area covered by this group marker
     */
    public MapPointGroup(Position pos, int moId, int count, long latestTime, Sector searchSector)
    {
        super(pos, moId, latestTime);

        this.count = count;
        this.searchSector = searchSector;
    }

    /**
     * @return Count of map points in this group
     */
    public int getCount()
    {
        return count;
    }

    /**
     * Get the search sector for this group.
     *
     * @return Search sector
     */
    @Override
    public Sector getSearchSector(Globe globe)
    {
        return searchSector;
    }

    /**
     * Overridden to enforce strict identity equality (ordering isn't really important).
     */
    @Override
    public int compareTo(MapPoint o)
    {
        return Integer.compare(System.identityHashCode(this), System.identityHashCode(o));
    }

    /**
     * Overridden to enforce strict instance equality.
     */
    @Override
    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object obj)
    {
        return (this == obj);
    }

    /**
     * @return hash code as determined by {@link System#identityHashCode(Object)}
     */
    @Override
    public int hashCode()
    {
        return System.identityHashCode(this);
    }

    @Override
    public String toString()
    {
        return "[MapPointGroup: " +
                ", moId=" + moId +
                ", count=" + count +
                ", pos=" + position +
                ']';
    }

    // We need to use the AVList for tool tips. Just delegate all calls to our avlist impl.

    @Override
    public Object setValue(String key, Object value)
    {
        return avlist.setValue(key, value);
    }

    @Override
    public AVList setValues(AVList avList)
    {
        return avlist.setValues(avList);
    }

    @Override
    public Object getValue(String key)
    {
        return avlist.getValue(key);
    }

    @Override
    public Collection<Object> getValues()
    {
        return avlist.getValues();
    }

    @Override
    public String getStringValue(String key)
    {
        return avlist.getStringValue(key);
    }

    @Override
    public Set<Map.Entry<String, Object>> getEntries()
    {
        return avlist.getEntries();
    }

    @Override
    public boolean hasKey(String key)
    {
        return avlist.hasKey(key);
    }

    @Override
    public Object removeKey(String key)
    {
        return avlist.removeKey(key);
    }

    /**
     * {@inheritDoc}
     * A No-op implementation.
     */
    @Override
    public void addPropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
    }

    /**
     * {@inheritDoc}
     * A No-op implementation.
     */
    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
    }

    /**
     * {@inheritDoc}
     * A No-op implementation.
     */
    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
    }

    /**
     * {@inheritDoc}
     * A No-op implementation.
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
    }

    /**
     * {@inheritDoc}
     * A No-op implementation.
     */
    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
    }

    /**
     * {@inheritDoc}
     * A No-op implementation.
     */
    @Override
    public void firePropertyChange(PropertyChangeEvent propertyChangeEvent)
    {
    }

    @Override
    public AVList copy()
    {
        return avlist.copy();
    }

    @Override
    public AVList clearList()
    {
        return avlist.clearList();
    }
}
