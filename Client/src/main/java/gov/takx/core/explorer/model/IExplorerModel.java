package gov.takx.core.explorer.model;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorerEventSupport;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Interface for the explorer model.  The explorer model is responsible for holding data relevant to the explorer
 * controller.  For the most part, this model is a collection of getter and setter methods, where calls to set methods
 * trigger property change events IF the state has actually changed. Children of a given parent are stored in insertion order
 * and can be retrieved by index if necessary.
 *
 * @since 1.48
 */
public interface IExplorerModel extends IExplorerEventSupport
{
    /**
     * Property names for events.
     */
    String EXPLORABLE_ADDED = "Explorable Added";
    String EXPLORABLE_REMOVED = "Explorable Removed";

    /**
     * Add an explorer object to this model.
     *
     * @param explorable explorable to add.
     * @since 5.3
     */
    boolean addExplorable(@Nonnull IExplorable explorable);

    /**
     * Determine whether an explorable exists in this model.
     *
     * @param key unique key of explorable of interest.
     * @return {@code true} if the explorable exists in this model, or {@code false} otherwise.
     */
    boolean hasExplorable(String key);

    /**
     * Remove an explorer object from this manager.
     * <p>
     * Note that this method will call {@link IExplorable#dispose()} on the explorable. The
     * explorable should be considered ready for garbage-collection once this method returns.
     *
     * @param explorable explorable to remove.
     * @return {@code true} if the explorable was contained in this manager and removed, or
     * {@code false} otherwise.
     */
    boolean removeExplorable(IExplorable explorable);

    /**
     * Get an collection of all explorable.
     *
     * @return an collection of all explorable.
     */
    Collection<IExplorable> getExplorables();

    /**
     * Get a collection of all explorables, filtered by the given predicate.
     *
     * @param filter the Predicate
     * @return the filtered Collection
     * @since 5.3
     */
    Collection<IExplorable> getExplorables(Predicate<IExplorable> filter);

    /**
     * Set the root explorable group of the model.
     *
     * @param root the root group to use for this model
     * @since 5.3
     */
    void setRootGroup(@Nonnull IPathExplorableGroup root);

    /**
     * Attempt to get an explorable by its unique string key.
     *
     * @param key unique string key associated with the explorable of interest.
     * @return explorer object or {@code null} if the object was not found.
     */
    @Nullable
    IExplorable getExplorable(@Nonnull String key);

    /**
     * @return the internal root object of this model
     * @since 5.3
     */
    @Nonnull
    IPathExplorableGroup getRoot();

    /**
     * @param explorable an explorable
     * @return the List of paths in this model that end with the given explorable
     * @since 5.3
     */
    @Nonnull
    List<List<IExplorable>> getExplorablePaths(@Nonnull IExplorable explorable);

    /**
     * @return List of all {@link IRuleGroupController} explorables within this model
     * @since 5.5.1
     */
    @Nonnull
    List<IRuleGroupController> getRuleGroupControllers();

    /**
     * Dispose all resources from this model.
     */
    void dispose();
}
