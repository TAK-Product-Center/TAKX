package gov.takx.api.plugin.io;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.tasks.ITaskProgressMonitor;
import jakarta.annotation.Nonnull;

/**
 * Progress monitor for Import/Export operations. Progress is represented
 * graphically as a progress bar, and also text area where messages
 * can be printed. If the progress of the operation is not trackable,
 * the progress bar can be set to an indeterminate state.
 * <p/>
 * Note: this interface is not intended to be implemented by Plugins.  Core components will implement it and be
 * provided to Import/Export plugins when they are executed.
 * <p>
 * Note that as of 2.14, many methods have been moved into inherited {@link ITaskProgressMonitor} base interface.
 *
 * @since 1.3
 */
public interface IImportExportProgressMonitor extends ITaskProgressMonitor
{
    /**
     * Set the key for the imported map object.  Some importers will be setup such that the operator does not
     * select the map object to import into, rather, the map object is determined at import time.  The importer
     * has the ability to manually notify the progress monitor of the map object that was imported.  This is
     * important because the progress monitor can use this to enable the goto feature after the import is complete.
     *
     * @param key map object key
     * @throws IllegalArgumentException if key is empty
     * @since 1.34
     */
    void setMapObjectKey(@Nonnull String key) throws IllegalArgumentException;

    /**
     * Set the location of interest for the task.  This is primarily used to indicate a location at which something
     * was imported and enables a "Go To" button in the UI.
     *
     * @param location The location
     * @since 1.42
     */
    void setLocationOfInterest(@Nonnull ILocation location);

    /**
     * Set the sector of interest for the task.  This is primarily used to indicate an area "operated upon" by
     * an import process and enables a "Go To" button in the UI.
     *
     * @param sector The sector
     * @throws IllegalArgumentException if sector is empty
     * @since 1.42
     */
    void setSectorOfInterest(@Nonnull Sector sector) throws IllegalArgumentException;
}
