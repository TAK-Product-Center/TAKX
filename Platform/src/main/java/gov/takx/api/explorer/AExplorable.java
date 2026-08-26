package gov.takx.api.explorer;

import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JPopupMenu;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Abstract implementation of IExplorable.  Implementors of {@link IExplorable} are encouraged
 * to extend this class (or {@link Explorable}) as the basis for their custom explorables.
 *
 * @since 1.48
 */
public abstract class AExplorable extends AExplorerEventSupport implements IExplorable
{
    // We keep the children collection sorted by their unique key so that operations like contains, indexOf, and remove
    // can be optimized using a binary search instead of a linear operation.
    private static final Comparator<IExplorable> CHILD_ORDER = Comparator.comparing(IExplorable::getUniqueKey);

    /**
     * The owner of this explorable.  Explorables are typically wrappers around other types of objects, so the owner
     * would be the originating object.
     */
    protected final Object owner;

    /**
     * List of strings representing the explorable's parent path in a tree.
     */
    protected final List<String> parentPath;

    /**
     * Groups that this explorable is a member of.
     * @since 5.3
     */
    protected final List<IExplorable> groups = new ArrayList<>();

    /**
     * Children of this explorable.
     * @since 5.3
     */
    private final List<IExplorable> children = new ArrayList<>();

    /**
     * The primary parent.
     * @since 5.3
     */
    protected IExplorable parent;

    /**
     * Depth of this explorable in the tree.
     *
     * @since 2.36
     */
    protected final int depth;

    /**
     * An immutable unique key for this explorable.  This must be non-null and non-empty.  A typical method for
     * creating a unique key is by creating a flat string out of the parent path and concatenating the display name.
     * This may be achieved using the utility
     * {@link ExplorerUtils#createDefaultKey(String, java.util.List)} method.
     */
    protected final String uniqueKey;

    /**
     * The full path of this explorable.
     *
     * @since 2.36
     */
    protected List<String> path;

    /**
     * Flag for visibility of explorer object on the globe.
     */
    protected AtomicBoolean visible = new AtomicBoolean(true);

    /**
     * Flag for visibility of explorer object in the explorer UI panel;
     * @since 5.3
     */
    protected AtomicBoolean visibleInExplorer = new AtomicBoolean(true);

    /**
     * Optional unique key of a map object linked to this explorable.
     *
     * @see IExplorable#setLinkedMapObjectUniqueKey(String)
     * @since 2.31.1
     */
    protected String linkedMapObjectUniqueKey;

    /**
     * Flag determining whether this explorable is considered a group.
     */
    protected final boolean group;

    /**
     * Flag for determining if an explorable supports being deleted.
     * @since 5.3
     */
    protected boolean supportsDeletion = true;

    /**
     * See {@link IExplorable#supportsSilentDeletion()}.
     * 
     * @since 5.7
     */
    protected boolean supportsSilentDeletion;

    /**
     * Flag for determining if an explorable supports being marked invisible.
     * @since 5.3
     */
    protected boolean supportsVisibility = true;

    /**
     * Allow setting of a default popup menu, useful for when nodes are created automatically, but you still want
     * menus attached.
     *
     * @since 2.37
     */
    private Supplier<JPopupMenu> defaultPopupMenuSupplier = () -> null;

    protected AExplorable(@Nullable Object owner, @Nullable List<String> parentPath, @Nullable String uniqueKey, boolean group)
    {
        this.owner = owner;
        this.parentPath = parentPath != null ? List.copyOf(parentPath) : Collections.emptyList();
        this.uniqueKey = uniqueKey;
        this.group = group;
        depth = this.parentPath.size() + 1;
    }

    @Override
    public List<String> getPath()
    {
        if (path == null)
        {
            final ArrayList<String> tempPath = new ArrayList<>(parentPath);
            tempPath.add(getPathName());
            path = Collections.unmodifiableList(tempPath);
        }

        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return path;
    }

    @Override
    public Collection<String> getFilterableKeys()
    {
        return Collections.emptyList();
    }

    @Override
    public Object getOwner()
    {
        return owner;
    }

    @Override
    public String getUniqueKey()
    {
        return uniqueKey;
    }

    @Override
    public ILocation getLocation()
    {
        return null;
    }

    @Override
    public boolean isGeolocated()
    {
        return getLocation() != null;
    }

    @Override
    public Color getDisplayNameColor()
    {
        return null;
    }

    @Override
    public BufferedImage getIconImage()
    {
        return null;
    }

    @Override
    public void setIconImage(BufferedImage iconImage)
    {
        // Do nothing.
    }

    @Override
    public Collection<String> getKeys()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean hasKey(String key)
    {
        return false;
    }

    @Override
    public Object getValue(String key)
    {
        return null;
    }

    @Override
    public <T> T getValue(String key, Class<T> type, T defaultValue)
    {
        Object value = getValue(key);
        return (type != null && type.isInstance(value)) ? type.cast(value) : defaultValue;
    }

    @Override
    public String getStringValue(String key, String defaultValue)
    {
        Object value = getValue(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    @Override
    public Integer getIntValue(String key, Integer defaultValue)
    {
        Object value = getValue(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    @Override
    public Double getDoubleValue(String key, Double defaultValue)
    {
        Object value = getValue(key);
        return value instanceof Double ? (Double) value : defaultValue;
    }

    @Override
    public Float getFloatValue(String key, Float defaultValue)
    {
        Object value = getValue(key);
        return value instanceof Float ? (Float) value : defaultValue;
    }

    @Override
    public Boolean getBooleanValue(String key, Boolean defaultValue)
    {
        Object value = getValue(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    @Override
    public Long getLongValue(String key, Long defaultValue)
    {
        Object value = getValue(key);
        return value instanceof Long ? (Long) value : defaultValue;
    }

    @Override
    public Byte getByteValue(String key, Byte defaultValue)
    {
        Object value = getValue(key);
        return value instanceof Byte ? (Byte) value : defaultValue;
    }

    @Override
    public Short getShortValue(String key, Short defaultValue)
    {
        Object value = getValue(key);
        return value instanceof Short ? (Short) value : defaultValue;
    }

    @Override
    public void dispose()
    {
        firePropertyChange(DISPOSED, false, true);
    }

    @Override
    public boolean isGroup()
    {
        return group;
    }

    @Override
    public List<String> getParentPath()
    {
        return parentPath;
    }

    @Override
    public int getDepth()
    {
        return depth;
    }

    @Override
    public boolean isVisible()
    {
        return visible.get();
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (this.visible.getAndSet(visible) != visible)
        {
            firePropertyChange(VISIBLE, !visible, visible);
        }
    }

    @Override
    public boolean isVisibleInExplorer()
    {
        return visibleInExplorer.get();
    }

    @Override
    public void setVisibleInExplorer(boolean visibleInExplorer)
    {
        if (this.visibleInExplorer.getAndSet(visibleInExplorer) != visibleInExplorer)
        {
            firePropertyChange(VISIBLE_IN_EXPLORER, !visibleInExplorer, visible);
        }
    }

    @Override
    public boolean supportsVisibility()
    {
        return supportsVisibility;
    }

    /**
     * @param supportsDeletion {@code true} if this explorable should support deletion, otherwise {@code false}
     * @since 5.3
     */
    public void setSupportsDeletion(boolean supportsDeletion)
    {
        this.supportsDeletion = supportsDeletion;
    }

    @Override
    public boolean supportsDeletion()
    {
        return supportsDeletion;
    }

    /**
     * @param supportsSilentDeletion {@code true} if this explorable should support silent deletion, otherwise {@code false}
     * @since 5.7
     */
    public void setSupportsSilentDeletion(boolean supportsSilentDeletion)
    {
        this.supportsSilentDeletion = supportsSilentDeletion;
    }

    @Override
    public boolean supportsSilentDeletion()
    {
        return supportsSilentDeletion;
    }

    /**
     * @param supportsVisibility {@code true} if this explorable should support visibility, otherwise {@code false}
     * @since 5.3
     */
    public void setSupportsVisibility(boolean supportsVisibility)
    {
        this.supportsVisibility = supportsVisibility;
    }

    @Override
    public boolean addGroup(@Nonnull IExplorable group)
    {
        synchronized (ExplorerUtils.getExplorerTreeLock(this))
        {
            if (!groups.contains(group))
            {
                groups.add(group);
                if (group.getChildIndex(this) >= 0 || group.addChild(this)) // Make sure we can add this as a group on the child
                {
                    if (groups.size() == 1) setParent(group); // If this was the first group then it becomes the default parent
                    firePropertyChange(GROUP, null, group);
                    return true;
                } else
                {
                    groups.remove(group); // We could not add this child to the group, so revert
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeGroup(@Nonnull IExplorable group)
    {
        synchronized (ExplorerUtils.getExplorerTreeLock(this)) // We cannot un-set the parent by removing the group, because there is no logic to detect a new parent
        {
            if (groups.contains(group) && !group.equals(parent))
            {
                groups.remove(group);
                group.removeChild(this);
                firePropertyChange(GROUP, group, null);
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public List<IExplorable> getGroups()
    {
        return List.copyOf(groups);
    }

    @Override
    @Nullable
    public IExplorable getParent()
    {
        return parent;
    }

    @Override
    public void setParent(@Nullable IExplorable newParent)
    {
        synchronized (ExplorerUtils.getExplorerTreeLock(this))
        {
            if (!Objects.equals(newParent, parent))
            {
                IExplorable oldParent = parent;
                parent = newParent;
                if (oldParent != null)
                {
                    removeGroup(oldParent);
                    oldParent.removeChild(this);
                }
                if (newParent != null) addGroup(newParent);
                firePropertyChange(PARENT, oldParent, newParent);
            }
        }
    }

    @Override
    public boolean addChild(@Nonnull IExplorable child)
    {
        synchronized (ExplorerUtils.getExplorerTreeLock(this))
        {
            final int insertionIndex = getChildIndex(child);
            if (insertionIndex < 0) // Must not already exist
            {
                children.add(-insertionIndex - 1, child); // Keep sorted on unique key
                if (child.getGroups().contains(this) || child.addGroup(this)) // Make sure we can add this as a group on the child
                {
                    firePropertyChange(CHILD, null, child);
                    return true;
                } else
                {
                    children.remove(child); // For some reason we could not add the group to the child, so revert
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeChild(@Nonnull IExplorable child)
    {
        synchronized (ExplorerUtils.getExplorerTreeLock(this))
        {
            final int insertionIndex = getChildIndex(child);
            if (insertionIndex >= 0)
            {
                children.remove(insertionIndex);
                child.removeGroup(this);
                firePropertyChange(CHILD, child, null);
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public List<IExplorable> getChildren()
    {
        return List.copyOf(children);
    }

    @Nonnull
    @Override
    public Stream<IExplorable> getChildrenStream()
    {
        return children.stream();
    }

    @Override
    public int getChildrenCount()
    {
        return children.size();
    }

    @Nonnull
    @Override
    public IExplorable getChild(int index)
    {
        return children.get(index);
    }

    @Override
    public int getChildIndex(@Nonnull IExplorable child)
    {
        return Collections.binarySearch(children, child, CHILD_ORDER);
    }

    @Override
    public void setLinkedMapObjectUniqueKey(String linkedMapObjectUniqueKey)
    {
        if (linkedMapObjectUniqueKey != null && linkedMapObjectUniqueKey.trim().isEmpty())
        {
            throw new IllegalArgumentException("Map object unique key cannot be empty or contain all whitespace characters.");
        }
        this.linkedMapObjectUniqueKey = linkedMapObjectUniqueKey;
    }

    @Override
    public String getLinkedMapObjectUniqueKey()
    {
        return linkedMapObjectUniqueKey;
    }

    @Override
    public void showDialog()
    {
        // Do nothing.
    }

    @Override
    public void setDefaultPopupMenu(@Nonnull Supplier<JPopupMenu> menuSupplier)
    {
        defaultPopupMenuSupplier = menuSupplier;
    }

    @Override
    public JPopupMenu createPopupMenu(@Nonnull MouseEvent mouseEvent)
    {
        return defaultPopupMenuSupplier.get();
    }

    @Override
    public String toString()
    {
        return "AExplorable{" +
                "key=" + getUniqueKey() +
                ", displayName=" + getDisplayName() +
                ", parentPath=" + parentPath +
                ", visible=" + visible +
                '}';
    }
}
