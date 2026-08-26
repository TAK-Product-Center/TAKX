package gov.takx.core.explorer.controller;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.rules.HashtagRule;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Controller for dynamic hashtag groups within the explorer.
 * 
 * @since 5.8
 */
public class ExplorerHashtagController
{
    /**
     * Key prefix for groups that should be placed in the hashtags path node.
     */
    public static final String HASHTAG_GROUP_KEY_PREFIX = "EXPLORER_HASHTAG_GROUP_";
    /**
     * Properties and suppliers that are relevant for hashtag detection.
     */
    public static final Map<String, Function<IMapObject, String>> SUPPLIERS = Map.of(
            IMapObject.NAME, IMapObject::getName,
            IMapObject.REMARKS, IMapObject::getRemarks);
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("(?<!\\w)#(\\w+)");
    private static final BufferedImage HASHTAG_GROUP_ICON = UIResources.getImage(PLATFORM_ICONS, "folder_hashtag.svg");

    private final Map<String, IRuleGroupController> hashtagGroups = new HashMap<>();
    private final IExplorer explorer;

    /**
     * Construct a new controller for a given explorer.
     *
     * @param explorer the explorer to use
     */
    public ExplorerHashtagController(@Nonnull IExplorer explorer)
    {
        this.explorer = explorer;
    }

    /**
     * Create hashtag groups based on parsing values from a given property change event.
     *
     * @param event a property change event
     */
    public void createHashtagGroups(@Nonnull PropertyChangeEvent event)
    {
        if (event.getSource() instanceof IMapObjectExplorable mapObjectExplorable)
        {
            var object = mapObjectExplorable.getMapObject();
            var supplier = SUPPLIERS.get(event.getPropertyName());
            if (supplier != null) createHashtagGroups(supplier.apply(object));
        } else if (event.getNewValue() instanceof IMapObjectExplorable mapObjectExplorable)
        {
            var object = mapObjectExplorable.getMapObject();
            SUPPLIERS.values().stream()
                    .map(s -> s.apply(object))
                    .flatMap(text -> parseHashtagNames(text).stream())
                    .distinct()
                    .forEach(this::createHashtagGroup);
        }
    }

    private void createHashtagGroups(@Nullable String text)
    {
        parseHashtagNames(text).forEach(this::createHashtagGroup);
    }

    /**
     * @param name a valid hashtag name
     */
    private void createHashtagGroup(@Nonnull String name)
    {
        synchronized (hashtagGroups)
        {
            var hashtagGroup = hashtagGroups.get(name);
            if (hashtagGroup == null)
            {
                var controller = explorer.getController();
                var groupName = "#" + name;
                var key = HASHTAG_GROUP_KEY_PREFIX + name.toUpperCase();
                var newGroup = (RuleGroupController) controller.addRuleGroup(groupName, key, Collections.emptyList(), HASHTAG_GROUP_ICON);
                hashtagGroups.put(name, newGroup);
                // Automatically hide the group when it has no children
                newGroup.addPropertyChangeListener(IExplorable.CHILD, event -> explorer.getExplorerPanel().showOrHideExplorable(newGroup, newGroup.hasChildren()));
                newGroup.setPersist(false);
                newGroup.setEditable(false);
                newGroup.setSupportsDeletion(false);
                newGroup.setSupportsVisibility(true);
                var rule = new HashtagRule(name);
                newGroup.add(rule);
                controller.ruleGroupUpdated(newGroup);
            }
        }
    }

    /**
     * @param explorable an explorable to check
     * @param name a hashtag name
     * @return {@code true} if the given explorable contains the given hashtag in one of the relevant properties
     */
    public static boolean containsHashtag(@Nonnull IExplorable explorable, @Nonnull String name)
    {
        if (explorable instanceof IMapObjectExplorable mapObjectExplorable)
        {
            var object = mapObjectExplorable.getMapObject();
            var normalized = name.toLowerCase();
            // Check all relevant properties
            return SUPPLIERS.values().stream()
                    .map(s -> s.apply(object))
                    .map(ExplorerHashtagController::parseHashtagNames)
                    .anyMatch(tags -> tags.contains(normalized));
        }
        return false;
    }

    /**
     * @param character a character to test
     * @return {@code true} if the character is a valid hashtag name character, otherwise {@code false}
     */
    public static boolean isValidCharacter(char character)
    {
        return String.valueOf(character).matches("\\w");
    }

    /**
     * @param text text to parse
     * @return a Set of valid, lowercase, tag names found in the text
     */
    @Nonnull
    private static Set<String> parseHashtagNames(@Nullable String text)
    {
        if (text == null) return Collections.emptySet();
        var matcher = HASHTAG_PATTERN.matcher(text);
        var tags = new HashSet<String>();
        while (matcher.find())
        {
            var name = matcher.group(1).toLowerCase();
            tags.add(name);
        }
        return tags;
    }
}
