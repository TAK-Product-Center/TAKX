package gov.takx.api.mapobject;

import jakarta.persistence.Embeddable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Instances of this class record a selection of {@link MapObjectOption} enum values for a Map Object.
 * <p>
 * These options allow a plugin developer greater control over the core features that will be applied to a
 * Map Object.  More specifically, these options <em>disable</em> certain operations on the Map Object and the
 * data (RDMs) that it generates.  For example, one can state that the Map Object will never generate alarms,
 * or that it produces no "styled" tracks.  These configurations can significantly reduce the overhead associated
 * with the creation and handling of the Map Object and its data.  Further, various UI elements will customize
 * their content based on these option settings.
 *
 * @see MapObjectSpec.Builder#mapObjectOptions()
 * @since 2.29
 */
@Embeddable
public class MapObjectOptions
{
    /**
     * A default for standard devices, no features disabled.
     */
    public static final MapObjectOptions STANDARD_DEVICE_OPTIONS = new MapObjectOptions();

    /**
     * A default for "resource" map objects (like markers), disable styled tracks, alarms, and plugin state.
     */
    public static final MapObjectOptions STANDARD_RESOURCE_OPTIONS = new MapObjectOptions(
            MapObjectOption.NoAlarms, MapObjectOption.NoStyledTracks, MapObjectOption.NoPluginState);

    /**
     * A default for "transient" markers, disable styled tracks, alarms, plugin state, and persistence.
     * @since 5.6
     */
    public static final MapObjectOptions TRANSIENT_MARKER_OPTIONS = new MapObjectOptions(
            MapObjectOption.NoPersistence, MapObjectOption.NoAlarms, MapObjectOption.NoStyledTracks,
            MapObjectOption.NoPluginState, MapObjectOption.NoEventLogs);

    private long optionFlags;

    @SuppressWarnings("unused") // For hibernate
    protected MapObjectOptions()
    {
    }

    /**
     * Construct using the given set of options.
     *
     * @param options Options to set
     */
    public MapObjectOptions(MapObjectOption... options)
    {
        for (MapObjectOption option : options)
        {
            setOption(option);
        }
    }

    /**
     * Test an option.
     *
     * @param option Map Object Option to test
     * @return {@code true} if the option is set
     */
    public boolean hasOption(MapObjectOption option)
    {
        return (optionFlags & (1L << option.ordinal())) != 0;
    }

    /**
     * Create a list of all the configured options.
     *
     * @return List of each {@link MapObjectOption} that is configured
     */
    public List<MapObjectOption> toList()
    {
        return Stream.of(MapObjectOption.VALUES)
                .filter(this::hasOption)
                .collect(Collectors.toList());
    }

    /**
     * Set an option.
     *
     * @param option Map Object Option to set.
     */
    private void setOption(MapObjectOption option)
    {
        optionFlags |= 1L << option.ordinal();
    }

    @Override
    public String toString()
    {
        return "MOOptions{" +
                Stream.of(MapObjectOption.VALUES)
                        .filter(this::hasOption)
                        .map(Enum::toString)
                        .collect(Collectors.joining(", "))
                + "}";
    }
}
