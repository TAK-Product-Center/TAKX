package gov.takx.api.layer.builder;

import com.google.auto.value.AutoBuilder;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Internal;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.LayerConfiguration.LayerClassType;
import gov.takx.api.layer.LayerGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A builder to construct a {@link LayerConfiguration} for adding local imagery layers to the globe. Supported data types include:
 * <ul>
 *     <li>GeoTIFF - point to the individual {@code .tiff} file.</li>
 *     <li>MBTiles/OSMDroid/SQLite database - point to the database file.</li>
 *     <li>GeoPackage - point to the {@code .gpkg} file.</li>
 *     <li>MrSID - point to the root directory of the MrSID data.</li>
 *     <li>NITF - point to the root directory of NITF data.</li>
 *     <li>CADRG/CIB/RPF - point to an individual CADRG/CIB file or point to the root directory of RPF data.</li>
 *     <li>APASS - point to the APASS catalog file {@code images.pfiva.sqlite3}.</li>
 *     <li>PFPS - point to the root directory of the PFPS data.</li>
 * </ul>
 *
 * @since 5.4
 * @see <a href="https://github.com/mapbox/mbtiles-spec">MBTiles spec</a>
 */
@AutoBuilder(ofClass = LocalImageryLayerBuilder.class, callMethod = "build")
public abstract class LocalImageryLayerBuilder extends LayerBuilder<LocalImageryLayerBuilder>
{
    private static final String PATH_SEPARATOR = "|";

    /**
     * Escaped separator used to separate file paths in the string accessed via {@link IRaptorLayer#getRestorableState()}.
     *
     * @since 5.7
     */
    @Internal
    public static final String ESCAPED_PATH_SEPARATOR = "\\" + PATH_SEPARATOR;

    /**
     * Construct a {@link LocalImageryLayerBuilder}.
     * @return the builder
     */
    public static LocalImageryLayerBuilder builder()
    {
        return LayerBuilder.setOptionals(new AutoBuilder_LocalImageryLayerBuilder());
    }

    static LayerConfiguration build(@Nonnull String name, @Nullable String displayName, boolean visible, @Nonnull List<File> files)
    {
        return new LayerConfiguration(name, displayName, LayerClassType.LocalImagery, visible, false, "resources/globe.png",
                LayerGroup.IMAGERY, null, getRestorableString(files), false, null);
    }

    /**
     * Set a {@link File} that points to the imagery data. This can be either a file or a directory.
     * @param file the path to the imagery data
     * @return the builder for chaining
     * @deprecated Use {@link #setFiles(List)} instead
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    public LocalImageryLayerBuilder setFile(@Nonnull File file)
    {
        setFiles(List.of(file));
        return this;
    }

    /**
     * Sets a list of {@link File files} that point to the imagery data. This can be either a single directory a or set 
     * of files.
     * 
     * @param files the path to the imagery data
     * @return the builder for chaining
     * @since 5.7
     */
    public abstract LocalImageryLayerBuilder setFiles(@Nonnull List<File> files);

    /**
     * Build the layer configuration.
     * @return the layer configuration
     */
    public abstract LayerConfiguration build();

    private static String getRestorableString(List<File> files)
    {
        return files.stream().map(File::getAbsolutePath).collect(Collectors.joining(PATH_SEPARATOR));
    }
}
