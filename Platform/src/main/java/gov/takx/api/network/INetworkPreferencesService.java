package gov.takx.api.network;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.preferences.IPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Provides simplified management of network-related {@link IPreferenceEntry preferences} that are used to configure the
 * network managed by {@link gov.tak.api.comms.commo.ICommonCommoManager}.
 *
 * @since 5.8
 */
@InjectableService
@NoPluginImplementations
@IncubatingApi(since = "5.8")
public interface INetworkPreferencesService
{
    /***
     * @param entry The definition for the input to add
     * @throws IllegalArgumentException if an entry with the provided name already exists or the protocol is not supported
     */
    void addInput(@Nonnull NetworkEntry entry) throws IllegalArgumentException;

    /**
     * @param entry provides the name of the input to update via {@link NetworkEntry#name()}
     * @param enabled {@code true} if the input should be enabled
     * @throws IllegalArgumentException if no matching input exists
     */
    void setInputEnabled(@Nonnull NetworkEntry entry, boolean enabled) throws IllegalArgumentException;

    /**
     * @param name The name of the requested input
     * @return the matching {@link NetworkEntry} or {@code null} if there is no match
     */
    @Nullable
    NetworkEntry getInput(@Nonnull String name);

    /**
     * @return all registered {@link NetworkEntry inputs}
     */
    @Nonnull
    List<NetworkEntry> getInputs();

    /**
     * @param entry provides the name of the input to remove via {@link NetworkEntry#name()}; if no matching input is
     *              found, no action is taken
     */
    void removeInput(@Nonnull NetworkEntry entry);

    /***
     * @param entry The definition for the output to add
     * @throws IllegalArgumentException an entry with the provided name already exists or the protocol is not supported
     */
    void addOutput(@Nonnull NetworkEntry entry) throws IllegalArgumentException;

    /**
     * @param entry provides the name of the output to update via {@link NetworkEntry#name()}
     * @param enabled {@code true} if the output should be enabled
     * @throws IllegalArgumentException if no matching output exists
     */
    void setOutputEnabled(@Nonnull NetworkEntry entry, boolean enabled) throws IllegalArgumentException;

    /**
     * @param name The name of the requested output
     * @return the matching {@link NetworkEntry} or {@code null} if there is no match
     */
    @Nullable
    NetworkEntry getOutput(@Nonnull String name);

    /**
     * @return all registered {@link NetworkEntry outputs}
     */
    @Nonnull
    List<NetworkEntry> getOutputs();

    /**
     * @param entry provides the name of the output to remove via {@link NetworkEntry#name()}; if no matching output is
     *              found, no action is taken
     */
    void removeOutput(@Nonnull NetworkEntry entry);
}
