package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationChunkSpi;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.gdal.GdalElevationChunk;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.util.ReferenceCount;
import org.gdal.gdal.gdal;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link ElevationChunkSpi} that creates {@link ElevationChunk ElevationChunks} from GDAL supported terrain data.
 *
 * @since 5.3
 */
public class GdalElevationChunkSpi implements ElevationChunkSpi
{
    // Always synchronize on sharedChunks before accessing ReferenceCount to avoid deadlock
    private final static Map<String, ReferenceCount<ElevationChunk>> sharedChunks = new HashMap<>();
    private final String basePath;

    public GdalElevationChunkSpi(String name)
    {
        this.basePath = "/vsimem/" + name + "/";
    }

    @Override
    public ElevationChunk create(ByteBuffer data, Hints hints)
    {
        String file = hints.tileIndex.x + "_" + hints.tileIndex.y + "_" + hints.tileIndex.z + ".tiff";
        String path = basePath + file;
        synchronized (sharedChunks)
        {
            ReferenceCount<ElevationChunk> elevationChunkRef = sharedChunks.computeIfAbsent(path, p -> {
                org.gdal.gdal.gdal.FileFromMemBuffer(path, data.array());
                ElevationChunk elevationChunk = GdalElevationChunk.create(path, null, Double.NaN, ElevationData.MODEL_TERRAIN, true);
                return new ReferenceCount<>(elevationChunk, false)
                {
                    @Override
                    protected void onDereferenced()
                    {
                        value.dispose();
                        gdal.Unlink(path);
                        sharedChunks.remove(path);
                    }
                };
            });
            return new SharedElevationChunk(elevationChunkRef);
        }
    }

    @Override
    public String getMimeType()
    {
        return null;
    }

    /**
     * A shared {@link ElevationChunk} implementation controlled by a {@link ReferenceCount} instance.
     */
    private static class SharedElevationChunk implements ElevationChunk
    {
        private final ReferenceCount<ElevationChunk> impl;
        private boolean disposed;

        private SharedElevationChunk(ReferenceCount<ElevationChunk> impl)
        {
            this.impl = impl;
            impl.reference();
        }

        @Override
        public String getUri()
        {
            return impl.value.getUri();
        }

        @Override
        public String getType()
        {
            return impl.value.getType();
        }

        @Override
        public double getResolution()
        {
            return impl.value.getResolution();
        }

        @Override
        public Polygon getBounds()
        {
            return impl.value.getBounds();
        }

        @Override
        public Data createData()
        {
            return impl.value.createData();
        }

        @Override
        public double sample(double latitude, double longitude)
        {
            return impl.value.sample(latitude, longitude);
        }

        @Override
        public boolean sample(double[] lla, int off, int len)
        {
            return impl.value.sample(lla, off, len);
        }

        @Override
        public double getCE()
        {
            return impl.value.getCE();
        }

        @Override
        public double getLE()
        {
            return impl.value.getLE();
        }

        @Override
        public boolean isAuthoritative()
        {
            return impl.value.isAuthoritative();
        }

        @Override
        public int getFlags()
        {
            return impl.value.getFlags();
        }

        @Override
        public void dispose()
        {
            synchronized (sharedChunks)
            {
                // We sometimes get double dispose() calls, so only deference once per instance
                if (disposed) return;
                impl.dereference();
                disposed = true;
            }
        }
    }
}
