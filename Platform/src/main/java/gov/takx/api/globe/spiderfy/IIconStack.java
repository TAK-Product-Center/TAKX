package gov.takx.api.globe.spiderfy;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.WWIcon;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.stream.Stream;

/**
 * An icon that can be expanded to reveal additional icons. Custom implementations of this interface are not
 * currently supported but support is planned to be added in future versions.
 *
 * @since 2.26
 */
@Experimental
@NoPluginImplementations
public interface IIconStack extends WWIcon
{
    /**
     * Adds {@code iconStack} as a linked descendant of this and fires a {@link IconStackPropertyConstants#CHILD_ADDED}
     * property change event. Descendants of this become visible when an {@code IconStack} is expanded and become
     * hidden when an {@code IconStack} is collapsed. An {@code IconStack} can be expanded / collapsed through user
     * mouse events or by invoking
     * {@link IIconStackController#expandIconStack(IIconStack, boolean)} and
     * {@link IIconStackController#collapseIconStack(IIconStack)}. If the parameter is
     * already a child of another {@code IconStack} the parameter will be unlinked from its former parent before being
     * linked as a descendant of this.
     */
    void addChildIconStack(@Nonnull IIconStack iconStack);

    /**
     * Removes the parameter {@code iconStack} as a linked descendant of this, clears the parameter's parent, and fires
     * a {@link IconStackPropertyConstants#CHILD_REMOVED} property change event.
     *
     * @param iconStack The icon to unlink from this.
     */
    void removeChildIconStack(@Nonnull IIconStack iconStack);

    /**
     * Gets the nested icons that are only shown when this is expanded.
     *
     * @return The nested icons that are only shown when this is expanded.
     */
    Stream<IIconStack> getChildIconStacks();

    /**
     * Gets the shape that nested icons will be expanded into.
     *
     * @return The shape that nested icons will be expanded into.
     */
    IconStackExpansionShape getExpansionShape();

    /**
     * Sets the shape that nested icons will be expanded into.
     *
     * @param iconStackExpansionShape The shape that nested icons will be expanded into.
     */
    void setExpansionShape(IconStackExpansionShape iconStackExpansionShape);

    /**
     * Gets the icon containing this icon.
     *
     * @return The ancestor of this if one exists; otherwise, {@code null}.
     */
    @Nullable
    IIconStack getParentIconStack();

    /**
     * Sets the parent of this icon stack and adds this as a child of {@code parentIconStack}.
     *
     * @param parentIconStack The icon stack which this is a child of.
     */
    void setParentIconStack(@Nullable IIconStack parentIconStack);

    /**
     * Sets the location where this icon will be displayed and fires a {@link IconStackPropertyConstants#POSITION}
     * property change event. To update the location of all child {@code IconStack}, use
     * {@link #setAllPositions(Position)}.
     *
     * @param position The location where this icon will be displayed.
     */
    void setPosition(Position position);

    /**
     * Sets the location of this icon to {@code rootPosition} and fires a {@link IconStackPropertyConstants#ALL_POSITIONS}
     * property change event to indicate that all child {@code IconStack}s should have their positions updated as well.
     * To set just the location of this icon use {@link #setPosition(Position)}.
     *
     * @param newRootIconStackPosition The new location where the root icon should be displayed.
     */
    void setAllPositions(Position newRootIconStackPosition);

    /**
     * Determines if the {@code IconStack}s nested within this are currently shown.
     *
     * @return True if the {@code IconStack}s nested within this are currently shown.
     */
    boolean isExpanded();
}
