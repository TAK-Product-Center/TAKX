package gov.takx.api.mapobject;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;

import java.io.File;

/**
 * Any map object that implements this interface can potentially provide a preview image of its contents.
 *
 */
@Internal
@NoPluginImplementations
public interface IPreviewProvider
{
    /**
     * @return true if a preview image is available.
     */
    boolean isPreviewImageAvailable();

    /**
     * Get the description text for the preview.
     *
     * @return preview description text
     */
    String getPreviewDescription();

    /**
     * Get the File object for the image (used to launch external viewer on image).  Return {@code null} if there
     * is nothing to launch.
     *
     * @return image file
     */
    File getExternalFile();

    /**
     * Get the location at which to place the preview
     *
     * @return location
     */
    ILocation getLocation();
}
