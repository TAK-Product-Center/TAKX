package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.spi.InteractiveServiceProvider;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.core.configuration.ProgramEnvironment;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * Abstract service provider class for creating a {@link ElevationSource} and {@link ITakElevationModel} pair from an {@link IRaptorLayer}.
 *
 * @since 5.3
 */
@PseudoBean
public abstract class AElevationLayerFactorySpi implements ElevationLayerFactorySpi
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected File cacheDir;

    @Inject
    private ProgramEnvironment programEnvironment;

    @PostConstruct
    private void postConstruct()
    {
        cacheDir = new File(programEnvironment.getProjectDataDirectory(), "OfflineCache/Elevation");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) logger.error("Could not create cache directory at {}", cacheDir.getAbsolutePath());
    }

    @Override
    public int getPriority()
    {
        return 0;
    }

    @Override
    public ElevationLayerFactory.ElevationLayerPair create(@Nonnull IRaptorLayer object, @Nullable Callback callback)
    {
        if (callback != null && callback.isProbeOnly())
        {
            final boolean success = this.probe(object, callback);
            callback.setProbeMatch(success);
            return null;
        }
        return create(object);
    }

    /**
     * Returns a flag indicating whether this spi can derive any datasets
     * from the specified {@link IRaptorLayer raptor layer}.
     *
     * @param object the raptor layer
     * @param callback A callback, may be <code>null</code>
     * @return <code>true</code> if this spi can derive datasets from the
     * specified raptor layer, <code>false</code> otherwise.
     */
    protected abstract boolean probe(@Nonnull IRaptorLayer object, @Nullable InteractiveServiceProvider.Callback callback);
}
