package gov.takx.api.globe.spiderfy;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Manages the expansion and collapsing of {@code IIconStack}. IconStacks can be expanded to reveal nested icons at
 * their actual locations or in a shape around the parent icon. If icons are expanded into a shape then
 * zooming/panning will cause the expanded icons to visually shift to maintain the shape and avoid overlap. If icons
 * are expanded to their true locations then zooming/panning will have no affect.
 *
 * @since 2.26
 */
@InjectableService
@Experimental
@NoPluginImplementations
public interface IIconStackController
{
    /**
     * Creates an {@link IIconStack} that can be added to the icon layer using {@link #addIconStackToGlobe(IIconStack)}
     * or can be nested within an {@code IIconStack} using {@link IIconStack#addChildIconStack(IIconStack)}
     *
     * @param imageSource  A URL or image that will be shown at the icon's location.
     * @param iconPosition The geographical location where the icon should be displayed.
     * @return The {@code IconStack} that was created.
     */
    IIconStack createIconStack(@Nonnull Object imageSource, @Nonnull Position iconPosition);

    /**
     * Makes the {@code iconStack} visible, hides all of its child icons, and adds the icon to the globe.
     *
     * @param iconStack The icon stack to add directly to the globe.
     */
    void addIconStackToGlobe(@Nonnull IIconStack iconStack);

    /**
     * Removes {@code iconStack} and all of its nested icons from the layer.
     *
     * @param iconStack The root icon stack to remove from the icon layer.
     */
    void removeIconStackFromGlobe(@Nonnull IIconStack iconStack);

    /**
     * Reveals all nested icons within an {@code iconStack} in a shape around the {@code iconStack}. The primary globe's
     * zoom level is used to determine the radius of the shape of the icons if an {@link IconStackExpansionShape} has
     * been specified.
     *
     * @param iconStack       The icon to expand.
     * @param enableAnimation True if the nested icons should animate while expanding to their final position.
     */
    void expandIconStack(@Nonnull IIconStack iconStack, boolean enableAnimation);

    /**
     * Reveals the nested icons within an {@code iconStack} in a shape around the {@code iconStack}.
     * The {@code worldWindow}'s zoom level is used to determine the radius of the shape of the icons if an
     * {@link IconStackExpansionShape} has been specified.
     *
     * @param iconStack       The icon to expand.
     * @param worldWindow     The globe model where the icon's zoom level will be calculated from.
     * @param enableAnimation True if the nested icons should animate while expanding to their final position; otherwise, false.
     */
    void expandIconStack(@Nonnull IIconStack iconStack, @Nonnull WorldWindow worldWindow, boolean enableAnimation);

    /**
     * Hides all nested icons within the {@code iconStack} on the primary globe.
     *
     * @param iconStack The icon stack whose nested icons should be hidden.
     */
    void collapseIconStack(@Nonnull IIconStack iconStack);

    /**
     * Registers {@code expansionListener} for notifications when an icon stack is expanded or collapsed.
     *
     * @param expansionListener The listener to notify of expansion events.
     */
    void addIconStackExpansionListener(@Nonnull IIconStackExpansionListener expansionListener);

    /**
     * Unregisters a listener from notifications about when an icon stack is expanded or collapsed.
     *
     * @param expansionListener The listener to unregister from expansion events.
     */
    void removeIconStackExpansionListener(@Nonnull IIconStackExpansionListener expansionListener);
}
