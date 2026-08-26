package gov.takx.core.globe.tak.elevation;

import com.atakmap.database.IteratorCursor;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * A re-implementation of TAK kernel's {@link com.atakmap.map.elevation.ElevationSourceBuilder.ElevationSourceImpl ElevationSourceImpl} that uses a {@link Supplier}
 * for {@link ElevationChunk ElevationChunks} since elevation chunks will be disposed between calls.
 *
 * @since 5.3
 */
final class ElevationSourceImpl extends ElevationSource
{
    private final static Comparator<ElevationChunk> RESOLUTION_ASC = (lhs, rhs) -> {
        final double a = lhs.getResolution();
        final double b = rhs.getResolution();
        if (a < b)
        {
            return 1;
        } else if (a > b)
        {
            return -1;
        } else
        {
            return lhs.getUri().compareTo(rhs.getUri());
        }
    };
    private final static Comparator<ElevationChunk> RESOLUTION_DESC = RESOLUTION_ASC.reversed();
    private final static Comparator<ElevationChunk> CE_ASC = (lhs, rhs) -> {
        final double a = lhs.getCE();
        final double b = rhs.getCE();
        if (a < b)
        {
            return 1;
        } else if (a > b)
        {
            return -1;
        } else
        {
            return lhs.getUri().compareTo(rhs.getUri());
        }
    };
    private final static Comparator<ElevationChunk> CE_DESC = CE_ASC.reversed();
    private final static Comparator<ElevationChunk> LE_ASC = (lhs, rhs) -> {
        final double a = lhs.getLE();
        final double b = rhs.getLE();
        if (a < b)
        {
            return 1;
        } else if (a > b)
        {
            return -1;
        } else
        {
            return lhs.getUri().compareTo(rhs.getUri());
        }
    };
    private final static Comparator<ElevationChunk> LE_DESC = LE_ASC.reversed();

    private final String name;
    private final Supplier<Collection<ElevationChunk>> chunks;
    private Envelope bounds;

    ElevationSourceImpl(String name, Supplier<Collection<ElevationChunk>> chunks)
    {
        this.name = name;
        this.chunks = chunks;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public Cursor query(QueryParameters params)
    {
        if (params == null || (params.order == null || params.order.isEmpty()))
        {
            return new CursorImpl(chunks.get().iterator(), params);
        } else
        {
            Comparator<ElevationChunk> comparator = (o1, o2) -> 0;
            for (QueryParameters.Order order : params.order)
            {
                comparator = switch (order)
                {
                    case CEAsc -> comparator.thenComparing(ElevationSourceImpl.CE_ASC);
                    case LEAsc -> comparator.thenComparing(ElevationSourceImpl.LE_ASC);
                    case CEDesc -> comparator.thenComparing(ElevationSourceImpl.CE_DESC);
                    case LEDesc -> comparator.thenComparing(ElevationSourceImpl.LE_DESC);
                    case ResolutionAsc -> comparator.thenComparing(ElevationSourceImpl.RESOLUTION_ASC);
                    case ResolutionDesc -> comparator.thenComparing(ElevationSourceImpl.RESOLUTION_DESC);
                };
            }

            ArrayList<ElevationChunk> sorted = new ArrayList<>(chunks.get());
            sorted.sort(comparator);
            return new CursorImpl(sorted.iterator(), params);
        }
    }

    @Override
    public Envelope getBounds()
    {
        // Note: this is a race, but considering pretty low-impact since the data is read-only
        Collection<ElevationChunk> elevationChunks = chunks.get();
        if (bounds == null && !elevationChunks.isEmpty())
        {
            Envelope mbb = null;
            for (ElevationChunk chunk : elevationChunks)
            {
                Geometry cbounds = chunk.getBounds();
                if (cbounds == null)
                    continue;
                Envelope cmbb = cbounds.getEnvelope();
                if (mbb == null)
                {
                    mbb = cmbb;
                } else
                {
                    if (cmbb.minX < mbb.minX)
                        cmbb.minX = mbb.minX;
                    if (cmbb.minY < mbb.minY)
                        cmbb.minY = mbb.minY;
                    if (cmbb.minZ < mbb.minZ)
                        cmbb.minZ = mbb.minZ;
                    if (cmbb.maxX > mbb.maxX)
                        cmbb.maxX = mbb.maxX;
                    if (cmbb.maxY > mbb.maxY)
                        cmbb.maxY = mbb.maxY;
                    if (cmbb.maxZ > mbb.maxZ)
                        cmbb.maxZ = mbb.maxZ;
                }
            }
            bounds = mbb;
        }
        return bounds;
    }

    @Override
    public void addOnContentChangedListener(OnContentChangedListener l)
    {
        // no-op; immutable
    }

    @Override
    public void removeOnContentChangedListener(OnContentChangedListener l)
    {
        // no-op; immutable
    }

    private final static class CursorImpl extends IteratorCursor<ElevationChunk> implements ElevationSource.Cursor
    {
        private final ElevationSource.QueryParameters filter;

        CursorImpl(Iterator<ElevationChunk> iter, ElevationSource.QueryParameters filter)
        {
            super(iter);
            this.filter = filter;
        }

        @Override
        public ElevationChunk get()
        {
            return this.getRowData();
        }

        @Override
        public double getResolution()
        {
            return this.getRowData().getResolution();
        }

        @Override
        public boolean isAuthoritative()
        {
            return this.getRowData().isAuthoritative();
        }

        @Override
        public double getCE()
        {
            return this.getRowData().getCE();
        }

        @Override
        public double getLE()
        {
            return this.getRowData().getLE();
        }

        @Override
        public String getUri()
        {
            return this.getRowData().getUri();
        }

        @Override
        public String getType()
        {
            return this.getRowData().getType();
        }

        @Override
        public Geometry getBounds()
        {
            return this.getRowData().getBounds();
        }

        @Override
        public int getFlags()
        {
            return this.getRowData().getFlags();
        }

        @Override
        public boolean moveToNext()
        {
            do
            {
                if (!super.moveToNext())
                    return false;
                if (ElevationSource.accept(this, filter))
                    return true;
            } while (true);
        }
    }
}