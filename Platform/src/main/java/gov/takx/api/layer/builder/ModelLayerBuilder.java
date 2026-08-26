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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A builder to construct a {@link LayerConfiguration} for adding 3D model layers to the globe. Supported data types include:
 * <ul>
 *     <li>Cesium 3D tiles - point to the {@code tileset.json} file.</li>
 *     <li>Collada - point to the {@code .dae} file.</li>
 *     <li>OBJ - point to the {@code .obj} file.</li>
 *     <li>LAS - point to the {@code .las} file.</li>
 * </ul>
 *
 * @since 5.4
 */
@AutoBuilder(ofClass = ModelLayerBuilder.class, callMethod = "build")
public abstract class ModelLayerBuilder extends LayerBuilder<ModelLayerBuilder>
{
    private static final String PATH_SEPARATOR = "\0";

    /**
     * Construct a {@link ModelLayerBuilder}.
     * @return the builder
     */
    public static ModelLayerBuilder builder()
    {
        return setOptionals(new AutoBuilder_ModelLayerBuilder());
    }

    /**
     * @param name The layer's name
     * @param displayName The layer's display name
     * @param visible {@code true} if the layer should be visible, {@code false} otherwise
     * @param files The files to use to build the layer
     * @return the layer configuration
     * @since 5.7
     */
    static LayerConfiguration build(@Nonnull String name, @Nullable String displayName, boolean visible, @Nonnull List<File> files)
    {
        return new LayerConfiguration(name, displayName, LayerClassType.Model, visible, false, null,
                LayerGroup.MODELS, null, getRestorableString(files), false, null);
    }

    /**
     * Set a {@link File} that points to the 3D model data. This can be either a file or a directory.
     * @param file the path to the 3D model data
     * @return the builder for chaining
     * @deprecated Use {@link #setFiles(List)} instead
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    public ModelLayerBuilder setFile(@Nonnull File file)
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
    @SuppressWarnings("NullableProblems")
    public abstract ModelLayerBuilder setFiles(@Nonnull List<File> files);

    /**
     * @deprecated No longer has any effect
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    @SuppressWarnings("unused")
    public ModelLayerBuilder setLegacyModel(boolean legacyModel)
    {
        return this;
    }

    /**
     * Build the layer configuration.
     * @return the layer configuration
     */
    public abstract LayerConfiguration build();

    @Nonnull
    private static String getRestorableString(@Nonnull List<File> files)
    {
        return files.stream().map(File::getAbsolutePath).collect(Collectors.joining(PATH_SEPARATOR));
    }

    /**
     * Retrieves the set of file paths from a model layer's restorable string.
     *
     * @param restorableState The state string
     * @return The list of file path strings stored in the restorable state string
     * @see IRaptorLayer#getRestorableState()
     * @since 5.7
     */
    @Nonnull
    @Internal
    public static List<String> parseRestorableString(@Nonnull String restorableState)
    {
        return Arrays.stream(restorableState.split(PATH_SEPARATOR)).toList();
    }
}
