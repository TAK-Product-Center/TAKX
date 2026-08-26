package gov.takx.platform.logging;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Empty (just some logging) implementation of {@link IImportExportProgressMonitor} for use in headless operations.
 *
 * @since 2.35
 */
@SuppressWarnings("unused")
public class NullImportExportProgressMonitor implements IImportExportProgressMonitor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void setMapObjectKey(String mapObjectKey) throws IllegalArgumentException
    {
        logger.debug("setMOKey: {}", mapObjectKey);
    }

    @Override
    public void setLocationOfInterest(@Nonnull ILocation location)
    {
        logger.debug("setLocationOfInterest: {}", location);
    }

    @Override
    public void setSectorOfInterest(Sector sector) throws IllegalArgumentException
    {
        logger.debug("setSectorOfInterest: {}", sector);
    }

    @Override
    public void printMessage(String s)
    {
        logger.debug(s);
    }

    @Override
    public void setProgress(int i)
    {
    }

    @Override
    public void setProgressIndeterminate(boolean b)
    {
    }

    @Override
    public void setMinProgress(int i)
    {
        logger.debug("setMinProgress: {}", i);
    }

    @Override
    public void setMaxProgress(int i)
    {
        logger.debug("setMaxProgress: {}", i);
    }

    @Override
    public void setComplete()
    {
        logger.debug("setComplete");
    }
}
