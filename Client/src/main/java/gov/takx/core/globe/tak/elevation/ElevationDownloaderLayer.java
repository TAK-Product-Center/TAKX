package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.layer.AbstractLayer;
import gov.tak.platform.client.dted.ElevationDownloader;
import gov.takx.platform.preferences.ProjectPreferenceEntry;

/**
 * A layer that represents and controls the {@link ElevationDownloader}
 * in the {@link ElevationDownloaderManager}.
 *
 * @since 3.4
 */
public class ElevationDownloaderLayer extends AbstractLayer
{
    public ElevationDownloaderLayer()
    {
        super("TAK Elevation Server");
    }

    @Override
    public synchronized void setVisible(boolean visible)
    {
        ProjectPreferenceEntry.DTED_STREAM.put(visible);
        super.setVisible(visible);
    }

    @Override
    public synchronized boolean isVisible()
    {
        return ProjectPreferenceEntry.DTED_STREAM.getBooleanValue();
    }
}
