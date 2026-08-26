package gov.takx.api.pointdropper;

import gov.tak.api.icons.IIconSetManager;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract for the service that allows users to drop points on the globe by either clicking on a location on the globe
 * or specifying location coordinates manually.
 *
 * @see PointDropperConfiguration
 * @since 5.1
 */
@SuppressWarnings("removal")
@InjectableService
@NoPluginImplementations
@Experimental
public interface IPointDropperService
{
    /**
     * Activates the point dropper. When the user clicks the globe, an icon will be dropped, initialized from the
     * given configuration.
     *
     * @param configuration Configuration controlling how Map Objects are created by the service
     */
    void activatePointDropper(@Nonnull PointDropperConfiguration configuration);

    /**
     * Activates the point dropper. When the user clicks the globe, the icon specified by iconSet/iconName will be
     * dropped on the globe.
     * <p/>
     * <b>Partial Deprecation Note:</b> As of 5.2, this method will interpret its parameters differently:
     * <ul>
     *     <li>
     *         The first parameter, renamed from {@code group} to {@code iconSet}, is now expected to
     *         be the name or UID of an {@link gov.tak.api.icons.IUserIconSet icon set}, as managed by the
     *         {@link gov.tak.api.icons.IIconSetManager Icon Set Manager}.
     *     </li>
     *     <li>
     *         The second parameter, renamed from {@code type} to {@code iconName}, is now expected to
     *         be the name of an icon within the identified {@code iconSet}.
     *     </li>
     * </ul>
     *
     * @param iconSet The name or UID of an icon set known to the {@link IIconSetManager}
     * @param iconName The name of the icon within the icon set
     * @see gov.tak.api.icons.IIconSetManager
     * @see gov.tak.api.icons.IUserIconSet
     * @throws IllegalArgumentException If no icon can be located using the given inputs
     */
    default void activatePointDropper(@Nonnull String iconSet, @Nonnull String iconName) throws IllegalArgumentException
    {
        activatePointDropper(PointDropperConfiguration.builder(iconSet, iconName).build());
    }

    /**
     * Activates the point dropper. When the user clicks the globe, the specified pre-defined CoT icon will be dropped.
     *
     * @param icon The {@link CotIcon} to use
     */
    default void activateCotPointDropper(@Nonnull CotIcon icon)
    {
        activatePointDropper(PointDropperConfiguration.builder(icon).build());
    }

    /**
     * Activates the point dropper. When the user clicks the globe, the icon that matches the specified CoT type will be
     * dropped.  If the CoT type string cannot be interpreted, then the icon defined by {@link CotIcon#UnknownGround}
     * will be used.
     *
     * @param cotType The CoT type
     */
    default void activateCotPointDropper(@Nonnull String cotType)
    {
        activatePointDropper(PointDropperConfiguration.builder(cotType).build());
    }

    /**
     * Creates a point at the specified location using the currently active icon type.
     *
     * @param location The location to place the point
     * @return The map object representing the new point or {@code null} if the point dropper is not active and no map
     * object was created
     */
    @Nullable
    IMapObject dropPoint(@Nonnull ILocation location);

    /**
     * Drop a single point, as defined in the given {@code configuration}.  The service will be activated, the
     * point dropped, and then the service is deactivated. None of the normal interactive user setup is performed,
     * meaning no globe message is presented, and key/mouse listeners are not installed.
     * Roughly equivalent to this code sequence:
     * <pre>{@code
     *     pointDropperService.activatePointDropper(configuration);
     *     pointDropperService.dropPoint(location);
     *     pointDropperService.deactivatePointDropper();
     * }</pre>
     *
     * @param configuration Configuration controlling how Map Objects are created by the service
     * @param location The location to place the point
     * @return The map object representing the new point
     * @since 5.3
     */
    @Nonnull
    IMapObject dropPoint(@Nonnull PointDropperConfiguration configuration, @Nonnull ILocation location);

    /**
     * Deactivates the point dropper.
     */
    void deactivatePointDropper();

    /**
     * @return {@code true} if the point dropper is active, {@code false} otherwise
     */
    boolean isActive();

    /**
     * @return The current configuration
     * @since 5.2
     */
    PointDropperConfiguration getConfiguration();
}
