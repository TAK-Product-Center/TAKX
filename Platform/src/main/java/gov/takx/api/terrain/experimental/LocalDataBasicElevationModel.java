package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.terrain.BasicElevationModel;
import gov.nasa.worldwind.util.TileKey;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;

/**
 * This is an extended version of BasicElevationModel, which exposes some otherwise protected functionality to load
 * specific tiles from disk on the current thread.
 * <p/>
 * This class is used for elevation models which have been determined to not contain the highest-resolution data
 * possible.  Typically, this occurs when a terrain-based calculation is processed with no network connection, on a
 * previously unused new region-of-interest.
 * <p/>
 * This model is created by cloning the relevant information from an ordinary BasicElevationModel.  Note that there are
 * other WorldWind classes which extend BasicElevationModel, such as WMSBasicElevationModel and
 * LocalRasterServerElevationModel.  This class *DOES NOT* replicate the features available in these other classes.  It
 * only provides access to tile data currently available on disk in native WorldWind file formats.
 * <p/>
 * Also, the original elevation model continues to exist in memory, on the WorldWind globe.  This means that these tow
 * layers may attempt to concurrently access the same tile data file.  The file access code synchronizes on a lock
 * object unique to each instance of BasicElevationModel.  If multiple threads use the same model instance to access
 * the same file, they will be properly synchronized.  However, if multiple threads simultaneously use an instance of
 * this class and the original elevation model instance to access the same file, the synchronization code will fail to
 * protect access to the file.  Due to this, IOExceptions may be thrown by the loadElevations() method.  In order to
 * best ensure that terrain-based calculations are consistent, this class will loop over the loadElevations() method
 * until either no IOExceptions are thrown, or the tile data is found in memory (see loadTile()).
 *
 * @since 1.35
 * @deprecated to be removed
 */
@Experimental
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class LocalDataBasicElevationModel extends BasicElevationModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public LocalDataBasicElevationModel(BasicElevationModel original)
    {
        super((AVList) original.getValue(AVKey.CONSTRUCTION_PARAMETERS));

        // Copy the values from the original elevation model into this model (including AVKey.RETRIEVER_FACTORY_LOCAL)
        this.setValues(original);

        // Important to load the current elevation type/order from the original model, otherwise data can be loaded
        // in a corrupt, unusable state.
        this.setByteOrder(original.getElevationDataByteOrder());
        this.setElevationDataType(original.getElevationDataType());

        if (logger.isDebugEnabled())
        {
            logger.debug("Creating new LocalDataBasicElevationModel from: {}", original.getClass());
        }
    }

    /**
     * This is a synchronous re-creation of the BasicElevationModel.RequestTask class.  It is nearly identical to the
     * original, except that it does not attempt to delete potentially corrupt data (the real RequestTask can be
     * responsible for that).  Also, its synchronous retrieve methods are local to this class, so they won't
     * interfere with the parent's normal operations.
     *
     * @param tileKey key information on tile to load.
     * @return {@code true} if tile load was successful or tile already exists in memory.  Otherwise, returns
     * {@code false}.
     */
    public boolean loadTile(TileKey tileKey) throws InterruptedException
    {
        if (Thread.currentThread().isInterrupted())
        {
            throw new InterruptedException("Load Tile Cancelled");
        }

        if (areElevationsInMemory(tileKey))
        {
            return true;
        }

        ElevationTile tile = createTile(tileKey);
        final URL url = getDataFileStore().findFile(tile.getPath(), false);

        if (url != null && !this.isFileExpired(tile, url, this.getDataFileStore()))
        {
            // Wrap the loadElevations() call an a loop.  The loop won't iterate unless an IOException is thrown
            // during loadElevations().  Otherwise, since it is a synchronous call - we don't need to continue looping.
            boolean loop;
            do
            {
                try
                {
                    loop = false;
                    boolean success = loadElevations(tile, url);
                    if (!success)
                    {
                        return false;
                    }
                } catch (Exception e)
                {
                    final boolean isIOException = e instanceof IOException;
                    if (isIOException && logger.isDebugEnabled())
                    {
                        logger.debug("Exception loading elevation data.  " +
                                "May be due to another thread accessing the file.  Will try again...", e);
                    } else
                    {
                        logger.warn("Failed to load elevation data", e);
                    }

                    if (isIOException)
                    {
                        loop = true;
                        Thread.sleep(2);
                        if (areElevationsInMemory(tileKey))
                        {
                            this.levels.unmarkResourceAbsent(tile);
                            return true;
                        }
                    } else
                    {
                        return false;
                    }
                }
            } while (loop);

            this.levels.unmarkResourceAbsent(tile);
            this.firePropertyChange(AVKey.ELEVATION_MODEL, null, this);
            return true;
        }
        return false;
    }
}
