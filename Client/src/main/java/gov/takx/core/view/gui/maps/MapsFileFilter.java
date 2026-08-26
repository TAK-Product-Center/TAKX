package gov.takx.core.view.gui.maps;

import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.TiledElevationProducer;
import gov.nasa.worldwind.data.TiledImageProducer;
import gov.nasa.worldwind.formats.rpf.RPFFrameFilename;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;
import java.util.stream.Stream;

/**
 * A file filter for the file choosers used in {@link LoadImageryFilesDialog} and
 * {@link LoadElevationFilesDialog}. It mostly just filters by filename extensions. Imagery has a special file type
 * (RPF) which requires examination of the entire filename.
 * @since 3.4.1 (renamed from ConvertMapsFileFilter)
 */
public class MapsFileFilter extends FileFilter
{
    private final FileNameExtensionFilter extensionFilter;
    private final String description;
    private final boolean isElevation;

    /**
     * Constructor
     *
     * @param isElevation if the file filter should work with Elevation files, this should be {@code true}, if
     *                    this filter should work with Imagery, this value should be {@code false}.
     */
    public MapsFileFilter(boolean isElevation)
    {
        ProducerTypes producerTypes;
        this.isElevation = isElevation;

        //we need to extract different filename extensions from WorldWind for Imagery and Elevation
        if (isElevation)
        {
            description = "Elevation";
            producerTypes = new ElevationProducerTypes();
        } else
        {
            description = "Imagery";
            producerTypes = new ImageryProducerTypes();
        }

        //pull an array of extensions from the instantiated producer - this is the only information we need from
        //the producer, so we will not keep it as a member
        extensionFilter = new FileNameExtensionFilter(null, getExtensions(producerTypes.getReaders()));
    }

    /**
     * Return the file name extension filter.
     *
     * @return The file name extension filter.
     */
    public FileNameExtensionFilter getExtensionFilter()
    {
        return extensionFilter;
    }

    /**
     * Accept files based mostly on file extension.  If this is an Imagery file filter, then also examine the entire
     * filename for the RPF file type.
     *
     * @param f file to filter
     * @return {@code true} on files which are accepted by this filter, otherwise returns {@code false}.
     */
    @Override
    public boolean accept(File f)
    {
        boolean accept = extensionFilter.accept(f);
        if (!accept && !isElevation)
        {
            accept = RPFFrameFilename.isFilename(f.getName());
        }
        return accept;
    }

    /**
     * Get this file filter's description to display in the file chooser.
     *
     * @return a description of this file filter.
     */
    @Override
    public String getDescription()
    {
        return description;
    }

    /**
     * Pull acceptable filename extensions from WW DataRasterReaders associated with Imagery or Elevation tiled raster
     * producers.
     *
     * @param readers WorldWind data raster readers pulled from a tiled raster producer.
     * @return an array of String file extensions.
     */
    private String[] getExtensions(DataRasterReader[] readers)
    {
        if (readers == null || readers.length < 1)
        {
            return null;
        }

        // Collect all the unique format suffixes available in all readers

        return Stream.of(readers)
                .flatMap(reader -> Stream.of(reader.getSuffixes()))
                .filter(sfx -> sfx != null && !sfx.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }

    /**
     * Interface defining a public getter method for WorldWind DataRasterReaders
     */
    private interface ProducerTypes
    {
        DataRasterReader[] getReaders();
    }

    /**
     * A wrapper class which exposes the WorldWind TiledImageProducer data raster readers to this class.
     */
    private static class ImageryProducerTypes extends TiledImageProducer implements ProducerTypes
    {
        @Override
        public DataRasterReader[] getReaders()
        {
            return getDataRasterReaders();
        }
    }

    /**
     * A wrapper class which exposes the WorldWind TiledElevationProducer data raster readers to this class.
     */
    private static class ElevationProducerTypes extends TiledElevationProducer implements ProducerTypes
    {
        @Override
        public DataRasterReader[] getReaders()
        {
            return getDataRasterReaders();
        }
    }
}
