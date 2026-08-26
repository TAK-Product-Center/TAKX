package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.util.Collections2;

import java.util.Set;

/**
 * An {@link ElevationSource} that tracks {@link OnContentChangedListener} as they are added and removed. The {@link #contentChanged()} method
 * is exposed to allow outside callers to trigger a {@link OnContentChangedListener#onContentChanged(ElevationSource)} event to the listeners.
 * No actual elevation data is supplied by this source.
 *
 * @since 5.2
 */
final class ElevationContentChangedDispatcher extends ElevationSource
{
    final String name;
    final Set<OnContentChangedListener> listeners;

    ElevationContentChangedDispatcher(String name)
    {
        this.name = name;
        listeners = Collections2.newIdentityHashSet();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Cursor query(QueryParameters params)
    {
        return Cursor.EMPTY;
    }

    @Override
    public Envelope getBounds()
    {
        return new Envelope(0d, 0d, 0d, 0d, 0d, 0d);
    }

    @Override
    public synchronized void addOnContentChangedListener(OnContentChangedListener l)
    {
        listeners.add(l);
    }

    @Override
    public synchronized void removeOnContentChangedListener(OnContentChangedListener l)
    {
        listeners.remove(l);
    }

    /**
     * Call {@link OnContentChangedListener#onContentChanged(ElevationSource)} on all listeners.
     */
    synchronized void contentChanged()
    {
        for (OnContentChangedListener l : listeners) l.onContentChanged(this);
    }
}
