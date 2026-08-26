package gov.takx.api.explorer;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Interface for providing / overriding the primary path for an {@link IExplorable}. All explorables must have a primary path,
 * which can be obtained and set via {@link IExplorable#getParent()} / {@link IExplorable#setParent(IExplorable)}}. This
 * primary path is determined when the explorable is first added to the model, either by the {@link IExplorable#getPath()} value
 * or by an implementation of {@link IExplorablePathProvider}. These path providers are given an opportunity to provide the
 * path for any explorable added to the model via {@link IExplorerDelegate#addExplorable(IExplorable)}. Most core explorables
 * have default path providers, but these can be overridden by adding custom path providers to the explorer via
 * {@link IExplorerDelegate#addPathProvider(IExplorablePathProvider)}. Path providers are processed last-to-first, so instances
 * added post-explorer-construction will take precedence over any default path providers. Common use cases for this are:<br/>
 * <ol>
 *     <li>Overriding the primary path of core explorables e.g. map objects and shapes</li>
 *     <li>Providing the default path for custom explorable implementations</li>
 * </ol>
 * This API should NOT be used to add the explorable to secondary groups, because it is called prior to the explorable
 * being fully added to the model. Secondary groups should be managed via instances of {@link IExplorerDelegateListener}.
 *
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
public interface IExplorablePathProvider
{
    /**
     * Create the primary path for the given explorable if possible.
     *
     * @param explorable the explorable
     * @return the primary path, or {@code null} if this instance cannot handle the explorable
     */
    @Nullable
    List<String> createDefaultPath(@Nonnull IExplorable explorable);
}