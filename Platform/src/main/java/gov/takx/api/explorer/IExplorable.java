package gov.takx.api.explorer;

import com.google.common.base.Predicates;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.text.html.CSS;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Base interface for objects and groups to be managed and displayed in the Explorer.
 *
 * @since 1.48
 */
@SuppressWarnings("SameParameterValue")
public interface IExplorable extends IExplorerEventSupport
{
    /**
     * Property name for parent change events.
     * @since 5.3
     */
    String PARENT = "Explorable Parent";

    /**
     * Property name for group added events.
     * @since 5.3
     */
    String GROUP = "Explorable Group";

    /**
     * Property name for child removal / addition
     * @since 5.3
     */
    String CHILD = "Explorable Child";

    /**
     * Property name for display name change events.
     */
    String DISPLAY_NAME = "Explorable Display Name";

    /**
     * Property name for summary text change events.
     * @since 5.3
     */
    String SUMMARY_TEXT = "Explorable Summary Text";

    /**
     * Property name for change events indicating an explorable's display name has changed color.
     *
     * @since 2.13
     */
    String DISPLAY_NAME_COLOR = "Explorable Display Name Color";

    /**
     * Property name for disposed change event. See {@link #dispose()} for more information on explorer object
     * disposal.
     */
    String DISPOSED = "Explorable Disposed";

    /**
     * Property name for icon image change events.
     */
    String ICON_IMAGE = "Explorable Icon Image";

    /**
     * Property name for visibility change events associated with this explorable object.
     */
    String VISIBLE = "Explorable Visible";

    /**
     * Property name for visibility change events associated with this explorable object.
     * @since 5.3
     */
    String VISIBLE_IN_EXPLORER = "Explorable Visible In Explorer";

    /**
     * Property name for key change events regarding the key/value pairs associated with this explorer object.
     */
    String KEYS = "Explorable Keys";

    /**
     * Property name for key change events regarding the filterable keys associated with this explorer object.  See
     * {@link #getFilterableKeys()} for more information on filterable keys.
     */
    String FILTERABLE_KEYS = "Explorable Filterable Keys";

    /**
     * Key name for data monitors list (if the implementation supports this feature).
     */
    String DATA_MONITORS_KEY = "Data Monitors Key";

    /**
     * Key name for highest alarm state (if the implementation supports this feature).
     */
    String HIGHEST_ALARM_STATE_KEY = "Highest Alarm State Key";

    /**
     * Return true for check of this key AND get boolean value to denote that your explorable doesn't care about
     * the visibility of its children.  User must explicitly click the checkbox of this one to toggle (or change
     * parent visibility).  Can also override getValue(String key)
     *
     * @see #hasKey(String)
     * @see #getBooleanValue(String, Boolean)
     * @since 2.5
     */
    String IGNORE_CHILD_VISIBILITY_KEY = "Ignore Child Visibility Key";

    /**
     * Return true for check of this key AND get boolean value to denote that your explorable doesn't care about
     * the visibility of its parent node.  User must explicitly click the checkbox of this Explorable to toggle
     * visibility (or toggle child visibility).
     *
     * @see #hasKey(String)
     * @see #getBooleanValue(String, Boolean)
     * @since 2.5
     */
    String IGNORE_PARENT_VISIBILITY_KEY = "Ignore Parent Visibility Key";

    /**
     * Return {@code false} for check of this key to hide the checkbox associated with an explorable.  Default check
     * when rendering Explorable node is {@code true}
     *
     * @see #hasKey(String)
     * @see #getBooleanValue(String, Boolean)
     * @since 2.13
     */
    String CHECKBOX_VISIBLE_KEY = "Checkbox Visible Key";

    /**
     * Return integer number of pixels to use for the explorer node text label.  In general, this should not be set
     * unless the Explorable also returns {@code false} for {@link #CHECKBOX_VISIBLE_KEY}.  In that case, the
     * default behavior of the tree is to provide a margin equal to the width of the invisible checkbox to keep
     * tree nodes lined up.  If you would rather not use this indent, or use some other value, return the integer
     * number of pixels for this property key.
     *
     * @see #hasKey(String)
     * @see #getIntValue(String, Integer)
     * @since 2.26
     */
    String TEXT_MARGIN_KEY = "Text Margin Key";

    /**
     * Return true for check of this key AND get boolean value to denote that your explorable should be copied to the
     * default explorer in addition to the explorer to which it was explicitly added.  This will result in the
     * explorable appearing in both explorers.
     *
     * @see #hasKey(String)
     * @see #getBooleanValue(String, Boolean)
     * @since 2.7
     */
    String COPY_TO_DEFAULT_EXPLORER = "Copy To Default Explorer";

    /**
     * Return value for check of this key AND get string value to denote that your explorable should be have the
     * provided value prepended to the parent path when the explorable is copied to the default explorer.  Note: this
     * key is only checked when {@link #COPY_TO_DEFAULT_EXPLORER} is set to true.
     *
     * @see #hasKey(String)
     * @see #getStringValue(String, String)
     * @since 2.7
     */
    String PARENT_TO_PREPEND_TO_PATH_ON_COPY = "Parent to Prepend to Path on Copy";

    /**
     * Get a set of keys for which this object has associated values (key/value pairs)
     *
     * @return set of keys for key/value pairs.
     */
    Collection<String> getKeys();

    /**
     * An optional set of keys for which this object has associated values (key/value pairs) AND the Explorer may
     * filter its view according to the values associated with these keys.
     *
     * @return an optional set of keys for Explorer filtering.
     */
    Collection<String> getFilterableKeys();

    /**
     * Determine whether a this object has a value associated with a key.
     *
     * @param key key of interest.
     * @return {@code true} if the object has a key/value pair for the key, or {@code false} otherwise.
     */
    boolean hasKey(String key);

    /**
     * Get the value associated with a key (if one exists).  The value must also be of the type specified in the
     * parameters.  If the value is {@code null} or of a different type than expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param type         expected value type.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return value associated with the key, or the default value if the retrieved value is {@code null} or of an unexpected
     * type.
     */
    <T> T getValue(String key, Class<T> type, T defaultValue);

    /**
     * Get the String value associated with a key (if one exists).  If the value is {@code null} or of a different type than
     * expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return String value associated with the key, or the default value if the retrieved value is {@code null} or of an
     * unexpected type.
     */
    String getStringValue(String key, String defaultValue);

    /**
     * Get the Long value associated with a key (if one exists).  If the value is {@code null} or of a different type than
     * expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return Long value associated with the key, or the default value if the retrieved value is {@code null} or of an
     * unexpected type.
     */
    Long getLongValue(String key, Long defaultValue);

    /**
     * Get the Integer value associated with a key (if one exists).  If the value is {@code null} or of a different type than
     * expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return Integer value associated with the key, or the default value if the retrieved value is {@code null} or of an
     * unexpected type.
     */
    Integer getIntValue(String key, Integer defaultValue);

    /**
     * Get the Short value associated with a key (if one exists).  If the value is {@code null} or of a different type than
     * expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return Short value associated with the key, or the default value if the retrieved value is {@code null} or of an
     * unexpected type.
     */
    Short getShortValue(String key, Short defaultValue);

    /**
     * Get the Byte value associated with a key (if one exists).  If the value is {@code null} or of a different type than
     * expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return Byte value associated with the key, or the default value if the retrieved value is {@code null} or of an
     * unexpected type.
     */
    Byte getByteValue(String key, Byte defaultValue);

    /**
     * Get the Double value associated with a key (if one exists).  If the value is {@code null} or of a different type than
     * expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return Double value associated with the key, or the default value if the retrieved value is {@code null} or of an
     * unexpected type.
     */
    Double getDoubleValue(String key, Double defaultValue);

    /**
     * Get the Float value associated with a key (if one exists).  If the value is {@code null} or of a different type than
     * expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return Float value associated with the key, or the default value if the retrieved value is {@code null} or of an
     * unexpected type.
     */
    Float getFloatValue(String key, Float defaultValue);

    /**
     * Get the Boolean value associated with a key (if one exists).  If the value is {@code null} or of a different type than
     * expected, the default value is returned.
     *
     * @param key          key of interest.
     * @param defaultValue default value to return if the retrieved value is {@code null} or of an unexpected type.
     * @return Boolean value associated with the key, or the default value if the retrieved value is {@code null} or of an
     * unexpected type.
     */
    Boolean getBooleanValue(String key, Boolean defaultValue);

    /**
     * Get the value associated with a key (if one exists).
     *
     * @param key key of interest.
     * @return value associated with the key, or {@code null} if none exist.
     */
    Object getValue(String key);

    /**
     * Get the owner object.  Generally, explorer objects are intended to be containers for other objects.  Explorer
     * objects act as a wrapper for owning objects to enable a representation of the owning object on the explorer
     * view.
     * <p/>
     * Alternatively, the owner object may implement the methods in this class, so that the owner itself is displayed
     * in the explorer view.  In such cases, this method would return a self-reference.
     *
     * @return owner object.
     */
    Object getOwner();

    /**
     * Get this object's current location on the globe.  If this object does not have a location, this method may
     * return {@code null}.
     *
     * @return this object's location on the globe if it exists, or {@code null} otherwise.
     */
    ILocation getLocation();

    /**
     * Determine whether this explorable is geolocated.
     *
     * @return {@code true} if this explorable is geolocated, or {@code false} otherwise.
     */
    boolean isGeolocated();

    /**
     * Get the display name for this explorer object.
     * <p/>
     * This value must not be {@code null}.
     *
     * @return the display name.
     */
    @Nonnull
    String getDisplayName();

    /**
     * Returns a custom block of html that should be used, when possible, as the display name for the explorable. Note
     * that care should be taken before overriding this because it may interfere with application rendering. Explorables
     * that return values from this method should still return a raw text version from {@link #getDisplayName()}, which
     * will be used for sorting and filtering.
     * <br/>
     * Note: The return value should <b>not</b> include the enclosing {@code <html>} tag.<br/>
     * For styles supported see: {@link CSS}
     *
     * @return the html to display for this explorable, <i>not including the enclosing {@code <html>} tag</i>
     * @since 5.3
     */
    @Nullable
    default String getDisplayNameHtml()
    {
        return null;
    }

    /**
     * Get the display name's color for this explorer object.
     * <p/>
     * If {@code null} is returned, then the default color will be employed.
     *
     * @return The color to use for the display name of this explorer object, or {@code null} if the default color should be
     * employed.
     * @since 2.13
     */
    Color getDisplayNameColor();

    /**
     * Set the display name's color for this explorer object.
     * <p>
     * If a {@code null} Color is provided, then the default color will be employed.
     * <p>
     * Note that by setting the color here, both the selected and unselected font colors are replaced by this one color.
     * Therefore, it is wise to choose a color that works for both the selected and unselected states.
     *
     * @param color The color to use for the display name of this explorer object, or {@code null} if the default color should be
     *              employed.
     * @since 2.13
     */
    default void setDisplayNameColor(@Nullable Color color)
    {
        // Do nothing.
    }

    /**
     * Get the summary text for this explorer object. This text is used by the Explorer UI to give context to this instance
     * when possible.
     *
     * @return the summary text.
     * @since 5.3
     */
    @Nullable
    default String getDetailsText()
    {
        return null;
    }

    /**
     * Returns a custom block of html that should be used, when possible, as the summary/description for the explorable. Note
     * that care should be taken before overriding this because it may interfere with application rendering. Explorables
     * that return values from this method should still return a raw text version from {@link #getDetailsText()}, which
     * will be used for sorting and filtering.
     * <br/>
     * Note: The return value should <b>not</b> include the enclosing {@code <html>} tag.<br/>
     * For styles supported see: {@link CSS}
     *
     * @return the html to display for this explorable, <i>not including the enclosing {@code <html>} tag</i>
     * @since 5.3
     */
    @Nullable
    default String getDetailsTextHtml()
    {
        return null;
    }

    /**
     * Get an optional icon image representation of this object for display in the explorer view.
     *
     * @return icon image representation of this object.
     */
    BufferedImage getIconImage();

    /**
     * Set an icon image representation of this object for display in the explorer view.
     * <p/>
     * Note that the implementation is not required to do anything.
     *
     * @param iconImage icon image representation of this object.
     */
    void setIconImage(BufferedImage iconImage);

    /**
     * Get a unique String key for this object.  The key MUST remain constant throughout the existence of this object.
     * <p/>
     * This value must not be {@code null}.
     *
     * @return a unique String key for this object.
     */
    String getUniqueKey();

    /**
     * Get a list of Strings representing this object's parent path in the Explorer.  It should be in descending
     * order, beginning from base level name, leading to and ending with the parent of this object.
     * <p/>
     * For example, the parent path for a device map object of type Acme with name "Acme 1" may be represented with the
     * list ["Devices", "Acme"].  Note that "Acme 1", being the map object name, does not appear in the list.
     * <p/>
     * The parent path MUST remain constant throughout the existence of this object.  If you need to move an explorable
     * to a different path, remove the original explorable from the explorer and create another with the new path.
     *
     * @return a list of Strings representing this object's tree path in the Explorer.
     */
    List<String> getParentPath();

    /**
     * Get the String for this Explorable's path name.  This *must* be constant in order to reliably identify this
     * explorable.  The default value is derived from getDisplayName() but that is most definitely not constant and
     * thus this should be overridden.  (Using getDisplayName() is consistent with the documentation of
     * AExplorable.uniqueKey).
     *
     * @return A String with this Explorable's path name.
     */
    default String getPathName()
    {
        return getDisplayName();
    }

    /**
     * Get a list of Strings representing this object's path in the Explorer.  The list ends in the result produced
     * by getPathName().  See comments in getParentPath().
     *
     * @return a list of Strings representing this objects' full tree path in the Explorer.
     */
    default List<String> getPath()
    {
        return ExplorerUtils.extendPath(getParentPath(), getPathName());
    }

    /**
     * @return The depth of this node in the tree, starting at 1.
     * @since 2.36
     */
    default int getDepth()
    {
        return getParentPath().size() + 1;
    }

    /**
     * Returns {@code true} if this explorable should be visible in the explorer UI, otherwise {@code false}. The default
     * implementation always returns {@code true}.
     *
     * @return {@code true} if this explorable should be visible in the explorer UI, otherwise {@code false}
     * @since 5.3
     */
    default boolean isVisibleInExplorer()
    {
        return true;
    }

    /**
     * Sets whether the explorable should be visible in the explorer UI. The default implementation is a no-op.
     *
     * @param visible {@code true} if this explorable should be visible in the explorer UI, otherwise {@code false}
     * @since 5.3
     */
    default void setVisibleInExplorer(boolean visible)
    {
        // NO-OP
    }

    /**
     * Determine whether this object is visible.
     *
     * @return {@code true} if the object is visible or {@code false} otherwise.
     */
    boolean isVisible();

    /**
     * Set visibility of this object.
     *
     * @param visible {@code true} to enable visibility or {@code false} to hide it.
     */
    void setVisible(boolean visible);

    /**
     * Note: if an {@link IExplorableGroup} does not support visibility, this will currently interfere with the ability of users to
     * change the visibility of all descendants by clicking the visibility icon in the tree. TODO DCW TAKX-3282
     *
     * @return {@code true} if this explorable supports the visibility flag, otherwise {@code false}
     * @since 5.3
     */
    default boolean supportsVisibility()
    {
        return getBooleanValue(CHECKBOX_VISIBLE_KEY, true);
    }

    /**
     * @return {@code true} if any child returns {@code true} from {@link #isVisible()}, otherwise {@code false}
     * @since 5.3
     */
    default boolean isAnyChildVisible()
    {
        return getChildrenStream().anyMatch(IExplorable::isVisible);
    }

    /**
     * Returns {@code true} if visibility property for this explorable is currently locked from user changes, otherwise {@code false}.
     *
     * @return default implementation always returns {@code false}
     * @since 5.3
     */
    default boolean isVisibilityLocked()
    {
        return false;
    }

    /**
     * Add a {@link IExplorable} to this explorables list of groups.
     *
     * @param group the Group to add
     * @return {code true} if the group list changed, otherwise {@code false}
     * @since 5.3
     */
    default boolean addGroup(@Nonnull IExplorable group)
    {
        return false;
    }

    /**
     * Remove a {@link IExplorable} from this explorables list of groups.
     *
     * @param group the Group to remove
     * @return {code true} if the group list changed, otherwise {@code false}
     * @since 5.3
     */
    default boolean removeGroup(@Nonnull IExplorable group)
    {
        return false;
    }

    /**
     * Returns the current parent. A value of {@code null} usually means that the explorable has not been added to a model
     * yet.
     *
     * @return the primary parent of this explorable, or {@code null} if the explorable has no current parent
     * @since 5.3
     */
    @Nullable
    default IExplorable getParent()
    {
        return null;
    }

    /**
     * Sets the primary parent of this explorable.
     *
     * @param parent the group to set as the parent
     * @since 5.3
     */
    default void setParent(@Nullable IExplorable parent)
    {
        // NO-OP
    }

    /**
     * Add a child to this group.
     *
     * @param child the child to add.
     * @return {@code true} if the child list changed, otherwise {@code false}
     * @since 5.3
     */
    default boolean addChild(@Nonnull IExplorable child)
    {
        return false;
    }

    /**
     * Remove a child from this group.
     *
     * @param child the child to remove
     * @return {@code true} if the child list changed, otherwise {@code false}
     * @since 5.3
     */
    default boolean removeChild(@Nonnull IExplorable child)
    {
        return false;
    }

    /**
     * Set the list of children of this group.
     *
     * @param newChildren the new List of children
     * @return {@code true} if the child list changed, otherwise {@code false}
     * @since 5.3
     */
    default boolean setChildren(@Nonnull Collection<IExplorable> newChildren)
    {
        List<IExplorable> currentChildren = getChildren();
        boolean removeChanges = currentChildren
                .stream()
                .filter(Predicate.not(newChildren::contains))
                .map(this::removeChild)
                .reduce((a, b) -> a || b)
                .orElse(false);
        boolean addChanges = newChildren
                .stream()
                .filter(Predicate.not(currentChildren::contains))
                .map(this::addChild)
                .reduce((a, b) -> a || b)
                .orElse(false);
        return removeChanges || addChanges;
    }

    /**
     * Set the optional unique key of a map object linked to this explorable.
     * <p/>
     * A common use for this field is when an explorable is nested hierarchically underneath another explorable.  This
     * is not meant to be the unique key of a map object directly represented by this explorable (such as is contained
     * within {@link IMapObjectExplorable}, but instead of a map object that is connected to this explorable,
     * such as a parent-child relationship.
     *
     * @param linkedMapObjectUniqueKey Unique key of a map objected linked to this explorable.  A {@code null}
     *                                 value is allowed for the parameter, which represents that the explorable is not
     *                                 linked to a map object.
     * @throws IllegalArgumentException If the parameter is empty or is all whitespace.
     * @since 2.36.1
     */
    default void setLinkedMapObjectUniqueKey(String linkedMapObjectUniqueKey)
    {
    }

    /**
     * Get the unique key of the map object linked to this explorable.
     *
     * @return String representing unique key of a map object linked to this explorable or {@code null} if there
     * is no linked map object
     * @since 2.36.1
     */
    default String getLinkedMapObjectUniqueKey()
    {
        return null;
    }

    /**
     * Show the (optional) dialog for this explorable.
     */
    void showDialog();

    /**
     * Get an (optional) popup menu for this explorable object.  The implementor should be aware that this method may
     * be called multiple times, and the explorer may make different modifications (i.e. new menu items added to
     * the menu) depending on where this explorer object is being used.  Because of this, each call to this method
     * should return a new popup menu instance.
     * <p/>
     * If the implementation returns {@code null}, a generic popup menu will be created for this object.
     *
     * @return a new popup menu instance for this explorer object.
     */
    JPopupMenu createPopupMenu(MouseEvent mouseEvent);

    /**
     * Set a default popup menu that will be displayed on a popup trigger. The implementor should be aware that supplier
     * may be called multiple times, and the explorer may make different modifications (i.e. new menu items added to
     * the menu) depending on where this explorer object is being used.  Because of this, each call to {@link Supplier#get()}
     * should return a new popup menu instance.
     * <p>
     * If {@link #createPopupMenu(MouseEvent)} is overridden, this supplier will be ignored.
     *
     * @param menuSupplier A menu producer that creates new menu instances. Must not be {@code null}.
     *                     To clear existing menu items return {@code null} as the result from {@link Supplier#get()}.
     * @since 2.37
     */
    void setDefaultPopupMenu(@Nonnull Supplier<JPopupMenu> menuSupplier);

    /**
     * Returns a list of {@link Action} instances that should be available to trigger when this explorable is rendered
     * in a table.
     *
     * @return a List of actions
     * @since 5.3
     */
    @Nonnull
    default List<Action> createQuickActions()
    {
        return Collections.emptyList();
    }

    /**
     * Release this object's resources in preparation for disposal.  After this method is called, this object should be
     * ready for garbage collection.  This method is called by the explorer, so it should not ordinarily be manually
     * called.
     */
    void dispose();

    /**
     * @return a deep copy of this Explorable, or null if it cannot be copied
     * @since 5.3
     */
    @Nullable
    default IExplorable copy()
    {
        return null;
    }

    /**
     * Get a flag determining whether this explorable is a group.  If an explorable is a group, it is intended to act
     * as a collection of child explorables rather than as an interesting explorable in its own right.  This flag
     * serves a purpose to explorable rules so that they may differentiate choose between ordinary explorables and
     * group explorables.  Most implementations of {@link IExplorable} are not groups, so it is
     * usually safe to return {@code false}.
     *
     * @return {@code true} if this explorable is intended to act as a group, or {@code false} otherwise.
     */
    default boolean isGroup()
    {
        return false;
    }

    /**
     * Called when the view requests deletion (removal) of an IExplorable.  This typically occurs when the user presses
     * the Delete key on an item in the Explorer.  Implementations of IExplorable are allowed to decide what action
     * (if any) should occur when this method is called.  The default implementation does nothing.
     */
    default void requestDelete()
    {
        // Do nothing.
    }

    /**
     * Called when an explorable is to be deleted without a confirm dialog. The primary use for this is the deletion
     * of multiple explorables at once.
     *
     * @since 2.38
     */
    default void deleteSilently()
    {
        // Do nothing
    }

    /**
     * @return {@code true} if this explorable can perform deletion without displaying a confirmation dialog, otherwise {@code false}.
     * @since 2.38
     */
    default boolean supportsSilentDeletion()
    {
        return false;
    }

    /**
     * @return {@code true} if this explorable supports the delete operation, otherwise {@code false}
     * @since 5.3
     */
    default boolean supportsDeletion()
    {
        return true;
    }

    /**
     * @return the List of groups that this explorable is in
     * @since 5.3
     */
    @Nonnull
    default List<IExplorable> getGroups()
    {
        return Collections.emptyList();
    }

    /**
     * @return the count of children of this explorable.
     * @since 5.3
     * @apiNote This is a fail-safe. Implementations that expect to have children should override this method with a more efficient implementation.
     */
    default int getChildrenCount()
    {
        return getChildren().size();
    }

    /**
     * @return {@code true} if this explorable has children, otherwise {@code false}
     * @since 5.3
     */
    default boolean hasChildren()
    {
        return getChildrenCount() > 0;
    }

    /**
     * @return the List of children of this explorable.
     * @since 5.3
     */
    @Nonnull
    default List<IExplorable> getChildren()
    {
        return Collections.emptyList();
    }

    /**
     * @return the Stream of children of this explorable.
     * @since 5.3
     * @apiNote This is a fail-safe. Implementations that expect to have children should override this method with a more efficient implementation.
     */
    @Nonnull
    default Stream<IExplorable> getChildrenStream()
    {
        return getChildren().stream();
    }

    /**
     * Returns a child at the specific index in the child List.
     *
     * @param index the index
     * @return the child at the specified index in this explorables child list
     * @since 5.3
     * @apiNote This is a fail-safe. Implementations that expect to have children should override this method with a more efficient implementation.
     */
    @Nonnull
    default IExplorable getChild(int index)
    {
        return getChildren().get(index);
    }

    /**
     * @param child Child of interest
     * @return The index of the given child within the children of this explorable, or {@code < 0} if not a child of this explorable.
     * @since 5.5.1
     */
    default int getChildIndex(@Nonnull IExplorable child)
    {
        return getChildren().indexOf(child);
    }

    /**
     * @return the Stream of all descendants of this Explorable
     * @see #getDescendantsStream(Predicate)
     * @since 5.3
     */
    @Nonnull
    default Stream<IExplorable> getDescendantsStream()
    {
        return getDescendantsStream(null);
    }

    /**
     * @param filter the predicate to filter on
     * @return the Stream of all descendants of this Explorable, filtering <i>at each level</i> by the predicate
     * @since 5.3
     */
    @Nonnull
    default Stream<IExplorable> getDescendantsStream(@Nullable Predicate<IExplorable> filter)
    {
        // We create a copy here so that flat map at least matches the state of the child list
        List<IExplorable> children = getChildren();
        Predicate<IExplorable> p = filter == null ? Predicates.alwaysTrue() : filter;
        return Stream.concat(children.stream().filter(p), children.stream().filter(p).flatMap(s -> s.getDescendantsStream(p)));
    }

    /**
     * @return the List of all paths under this Explorable
     * @see #getDescendantPathsStream(Predicate)
     * @since 5.3
     */
    @Nonnull
    default Stream<List<IExplorable>> getDescendantPathsStream()
    {
        return getDescendantPathsStream(null);
    }

    /**
     * @param filter the predicate to filter on
     * @return the List of all paths under this Explorable where the path List matches the given predicate
     * @since 5.3
     */
    @Nonnull
    default Stream<List<IExplorable>> getDescendantPathsStream(@Nullable Predicate<IExplorable> filter)
    {
        // We create a copy here so that flat map at least matches the state of the child list
        List<IExplorable> children = getChildren();
        Predicate<IExplorable> childFilter = filter == null ? Predicates.alwaysTrue() : filter;
        Stream<List<IExplorable>> childrenDescendants = children.stream().filter(childFilter).map(child -> {
            List<IExplorable> path = new ArrayList<>();
            path.add(child);
            return path;
        });
        for (IExplorable child : children.stream().filter(childFilter).toList())
        {
            Stream<List<IExplorable>> childDesc = child.getDescendantPathsStream(childFilter);
            childrenDescendants = Stream.concat(childrenDescendants, childDesc);
        }
        // Now add self to each path
        childrenDescendants = childrenDescendants.peek(l -> l.add(0, this));
        return childrenDescendants;
    }

    /**
     * @return the List of all primary ancestors of this Explorable, according to the {@link #getParent()} method.
     * @since 5.3
     */
    @Nonnull
    default List<IExplorable> getAncestors()
    {
        return getParent() != null ? getParent().getExplorablePath() : new ArrayList<>();
    }

    /**
     * @return the List of all primary ancestors of this Explorable, according to the {@link #getParent()} method, and ending with this Explorable instance
     * @since 5.3
     */
    @Nonnull
    default List<IExplorable> getExplorablePath()
    {
        List<IExplorable> parentPath = getAncestors();
        parentPath.add(this);
        return parentPath;
    }
}
