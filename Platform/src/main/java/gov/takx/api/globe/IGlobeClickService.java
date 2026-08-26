package gov.takx.api.globe;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.pointdropper.PointDropperConfiguration;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * Contract for a service to invoke user interaction to click on the globe and receive the location clicked.
 * <p>
 * A typical "one shot" invocation to get a clicked location from the user would look like this:
 * <pre>{@code
 * globeClickService.activate("Click anywhere on the map", location - > {
 *    // do something with 'location'
 *    globeClickService.deactivate(); // deactivate after first use
 * });
 * }</pre>
 * The configured {@code clickHandler} will continue to be called every time the user clicks on a globe, until the
 * service is deactivated.
 *
 * @since 5.4
 */
@InjectableService
@NoPluginImplementations
@IncubatingApi(since = "5.4")
public interface IGlobeClickService
{
    /**
     * Activate the service to detect when the user clicks on any globe.  Once activated, if the user clicks anywhere
     * but on a globe, the operation will be deactivated.
     *
     * @param message message to display at the top of the globe (similar to {@link IGlobeManager#showGlobeMessage(String, IGlobeDelegate)})
     * @param clickHandler Handler to invoke with the location of the globe click
     * @throws IllegalStateException If an operation is already active
     */
    default void activate(@Nonnull String message, @Nonnull Consumer<ILocation> clickHandler)
            throws IllegalStateException
    {
        activate(null, message, clickHandler, new PointDropperConfiguration.OffGlobeDeactivator(), null);
    }

    /**
     * Activate the service to detect when the user clicks on the globe.  Once activated, if the user clicks anywhere
     * but on the specified {@code globe} (or on any globe if {@code globe} is {@code null}), the operation will be deactivated.
     *
     * @param globe The globe on which to listen for mouse clicks, {@code null} to listen on all globes
     * @param message Optional message to display at the top of the globe (similar to {@link IGlobeManager#showGlobeMessage(String, IGlobeDelegate)})
     * @param clickHandler Handler to invoke with the location of the globe click
     * @param deactivationHandler Optional runnable to be executed when the operation deactivates
     * @throws IllegalStateException If an operation is already active
     */
    default void activate(@Nullable IGlobeDelegate globe, @Nullable String message, @Nonnull Consumer<ILocation> clickHandler, @Nullable Runnable deactivationHandler)
            throws IllegalStateException
    {
        activate(globe, message, clickHandler, new PointDropperConfiguration.OffGlobeDeactivator(), deactivationHandler);
    }

    /**
     * Activate the service to detect when the user clicks on the globe.  Once activated, mouse events will be given
     * to the specified {@code deactivator} to determine if the operation should be deactivated.
     *
     * @param globe The globe on which to listen for mouse clicks, {@code null} to listen on all globes
     * @param message Optional message to display at the top of the globe (similar to {@link IGlobeManager#showGlobeMessage(String, IGlobeDelegate)})
     * @param clickHandler Handler to invoke with the location of the globe click
     * @param deactivator Deactivator to process AWT Mouse Pressed events to determine when to deactivate
     * @param deactivationHandler Optional runnable to be executed when the operation deactivates
     * @throws IllegalStateException If an operation is already active
     */
    void activate(@Nullable IGlobeDelegate globe, @Nullable String message, @Nonnull Consumer<ILocation> clickHandler,
                  @Nonnull PointDropperConfiguration.IDeactivator deactivator, @Nullable Runnable deactivationHandler)
            throws IllegalStateException;

    /**
     * Deactivate any current service request. If one is not active, this call will be ignored.
     */
    void deactivate();
}
