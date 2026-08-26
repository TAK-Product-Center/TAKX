package gov.takx.core.explorer.controller;

import gov.takx.api.explorer.AExplorerEventSupport;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.explorer.IManualExplorableGroup;
import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.api.state.IRestorableMap;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.explorer.common.ExplorerStateKeys;
import gov.takx.core.explorer.common.ManualExplorableGroup;
import gov.takx.core.explorer.model.IExplorablePersistor;
import jakarta.annotation.Nonnull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of {@link IExplorablePersistor} that knows how to store/restore instances of {@link ManualExplorableGroup}
 *
 * TODO DCW TAKX-3283 switch this to use the single explorable methods instead of bulk restore. But keep in mind that we need to retain the original RESTORE code so that we dont lose info when users open for the first time
 * TODO DCW TAKX-3282 If we want to restore path groups we need to write another persistor, but can probably share a base class with this one
 * @since 5.3
 */
class ManualExplorableGroupPersistor extends AExplorerEventSupport implements IExplorablePersistor
{

    public static final String RESTORED_KEY_TO_GROUP_ADDED = "Restored Key to Group Added";
    public static final String RESTORED_KEY_TO_GROUP_REMOVED = "Restored Key to Group Removed";
    public static final String RESTORED_KEYS_TO_GROUPS_CLEARED = "Restored Keys to Groups Cleared";

    private final IExplorerController controller;

    private final Map<String, Set<IManualExplorableGroup>> restoredGroupContents = new ConcurrentHashMap<>();

    /**
     * Construct a new instance.
     *
     * @param controller the controller
     */
    ManualExplorableGroupPersistor(@Nonnull IExplorerController controller)
    {
        this.controller = controller;
    }

    @Override
    public void store(@Nonnull IRestorableMap map)
    {
        Object o = map.getTransientValue(ExplorerStateKeys.ICON_FOLDER_LOCATION_KEY);
        String iconsDirectory = o instanceof String ? (String) o : null;

        // Create a new restorable map list - this is going to hold data for the standard groups.
        List<IRestorableMap> groupsMapList = new ArrayList<>();
        List<ManualExplorableGroup> manualGroups = controller.getExplorables()
                .stream()
                .filter(ManualExplorableGroup.class::isInstance)
                .map(ManualExplorableGroup.class::cast)
                .filter(IUserExplorableGroup::isPersist)
                .toList();
        for (ManualExplorableGroup explorableGroup : manualGroups)
        {
            // Iterate through the available explorables, looking for standard explorable groups.
            // Create a restorable map for each group.
            IRestorableMap restorableMap = map.createNewRestorableMap();
            groupsMapList.add(restorableMap);

            // IExplorables are not required to support persistence.  However, standard groups need a way to know
            // information about explorables they contain, which is available in this class.
            store(explorableGroup, restorableMap);

            // Store the custom icon for the group (if one exists).
            BufferedImage iconImage = explorableGroup.getIconImage();
            if (iconsDirectory != null && iconImage != null && iconImage != ExplorerController.GROUP_ICON)
            {
                // Create a name based upon the hash of the image data.  This ensures that any images shared
                // between groups will have the same name, and we won't waste disk space.
                String iconName = ImageUtils.generateHexMD5HashFromImage(iconImage) + ".png";
                File imageFile = new File(iconsDirectory, iconName);

                // It's possible the image already exists - so check before writing to disk.
                if (!imageFile.exists()) ImageUtils.writeBufferedImageToPNGFile(iconImage, new File(iconsDirectory, iconName));

                // Finally, put the icon name in the restorable map.
                restorableMap.setString(ExplorerStateKeys.ICON_IMAGE_KEY, iconName);
            }
        }
        map.setRestorableMapList(ExplorerStateKeys.STANDARD_GROUPS_KEY, groupsMapList);
    }

    @Override
    public void restore(@Nonnull IRestorableMap map)
    {

        // Restore standard groups.
        List<IRestorableMap> standardGroupsList = map.getRestorableMapList(ExplorerStateKeys.STANDARD_GROUPS_KEY);
        if (standardGroupsList != null)
        {
            // Get the icon folder location - this should have been put in the map by the explorer manager before this
            // method was called.
            Object o = map.getTransientValue(ExplorerStateKeys.ICON_FOLDER_LOCATION_KEY);
            String iconsDirectory = o instanceof String ? (String) o : null;

            // Iterate through restorable maps for each standard group.
            for (IRestorableMap standardGroupState : standardGroupsList)
            {
                // Restore the name of the group, as well as its custom icon (if one exists).
                String displayName = standardGroupState.getString(ExplorerStateKeys.DISPLAY_NAME_KEY, null);
                String iconName = standardGroupState.getString(ExplorerStateKeys.ICON_IMAGE_KEY, null);
                String uniqueKey = standardGroupState.getString(ExplorerStateKeys.UNIQUE_KEY_KEY, null);
                BufferedImage iconImage = null;
                if (iconsDirectory != null && iconName != null) iconImage = ImageUtils.getImage(new File(iconsDirectory, iconName), false);

                if (displayName != null)
                {
                    // Instantiate the standard group with the name and custom icon.
                    IManualExplorableGroup group = controller.createManualGroup(displayName, uniqueKey, iconImage);
                    restore(group, standardGroupState);
                }
            }
        }
    }

    private void store(@Nonnull IExplorableGroup group, @Nonnull IRestorableMap map)
    {
        map.setString(ExplorerStateKeys.DISPLAY_NAME_KEY, group.getDisplayName());
        map.setStringList(ExplorerStateKeys.PATH_KEY, group.getParentPath());
        List<String> uniqueKeys = group.getDescendantsStream().map(IExplorable::getUniqueKey).toList();
        map.setStringList(ExplorerStateKeys.UNIQUE_KEYS_KEY, uniqueKeys);
        map.setString(ExplorerStateKeys.UNIQUE_KEY_KEY, group.getUniqueKey());
    }

    private void restore(@Nonnull IManualExplorableGroup group, @Nonnull IRestorableMap map)
    {
        clearRestoreKeysToGroups();
        for (String uniqueKey : map.getStringList(ExplorerStateKeys.UNIQUE_KEYS_KEY, Collections.emptyList()))
        {
            IExplorable explorable = controller.getExplorable(uniqueKey);
            if (explorable != null)
            {
                group.addChild(explorable);
            } else
            {
                // This section will be reached if the explorable referred to by the uniqueKey does not yet exist.
                // Put the key in the model in the hopes that the explorable in question will appear some time in
                // the future.  At that point, we can restore the explorable to this group.
                addRestoredKeyToGroup(uniqueKey, group);
            }
        }
    }

    @Override
    public IRestorableMap storeExplorable(@Nonnull IExplorable explorableGroup, @Nonnull IRestorableMap map)
    {
        return null;
    }

    @Override
    public IExplorable restoreExplorable(@Nonnull IRestorableMap map)
    {
        return null;
    }

    private void addRestoredKeyToGroup(@Nonnull String explorableKey, @Nonnull IManualExplorableGroup group)
    {
        final Set<IManualExplorableGroup> groups = restoredGroupContents.computeIfAbsent(explorableKey,
                k -> ConcurrentHashMap.newKeySet());

        if (groups.add(group)) pcs.firePropertyChange(RESTORED_KEY_TO_GROUP_ADDED, null, explorableKey);
    }

    private void clearRestoreKeysToGroups()
    {
        final int oldSize = restoredGroupContents.size();

        if (oldSize != 0) pcs.firePropertyChange(RESTORED_KEYS_TO_GROUPS_CLEARED, oldSize, restoredGroupContents.size());
    }

    private Set<IManualExplorableGroup> getRestoredKeysForGroup(@Nonnull String explorableKey, boolean remove)
    {
        final Set<IManualExplorableGroup> groups;

        if (remove)
        {
            groups = restoredGroupContents.remove(explorableKey);

            if (groups != null) pcs.firePropertyChange(RESTORED_KEY_TO_GROUP_REMOVED, explorableKey, null);
        } else
        {
            groups = restoredGroupContents.get(explorableKey);
        }

        return groups != null ? groups : Collections.emptySet();
    }

    /**
     * Called whenever a new explorable is added to the project.  It is possible that the explorable was actually
     * restored, and in this case, the explorable should be also added to all of the standard groups to which it was
     * previously associated.
     *
     * @param explorable explorable of interest.
     */
    void restoreExplorableToGroup(@Nonnull IExplorable explorable)
    {
        for (IExplorableGroup group : getRestoredKeysForGroup(explorable.getUniqueKey(), true)) group.addChild(explorable);
    }
}