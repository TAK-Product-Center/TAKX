package gov.takx.core.explorer.model;

import gov.takx.api.explorer.AExplorerEventSupport;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Implementation class for IExplorerModel.  For more information see documentation for
 * {@link IExplorerModel}.
 *
 * @since 1.48
 */
@SuppressWarnings("removal")
public class ExplorerModel extends AExplorerEventSupport implements IExplorerModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // Map of unique keys to explorables.
    private final Map<String, IExplorable> explorables = new ConcurrentHashMap<>();

    // All rule group controller explorables
    private final Set<IRuleGroupController> ruleGroupControllers = Collections.newSetFromMap(new IdentityHashMap<>());

    // The root path group.
    private IPathExplorableGroup rootGroup;
    private final PropertyChangeListener childChangedListener = this::explorableChildChanged;
    private final PropertyChangeListener propertyChangePropagateListener = this::firePropertyChange;

    @Override
    public void setRootGroup(@Nonnull IPathExplorableGroup root)
    {
        rootGroup = root;
        addListeners(rootGroup);
    }

    @Override
    public IExplorable getExplorable(@Nonnull String uniqueKey)
    {
        return explorables.get(uniqueKey);
    }

    @Override
    public boolean addExplorable(@Nonnull IExplorable explorable)
    {
        recordIfRuleGroupController(explorable, true);
        return rootGroup.addChild(explorable);
    }

    /**
     * If the given {@code explorable} is an instance of {@link IRuleGroupController}, then update the {@link #ruleGroupControllers}
     * collection by either adding or removing the explorable according to the{ @code add} parameter.
     *
     * @since 5.5.1
     */
    private void recordIfRuleGroupController(@Nonnull IExplorable explorable, boolean add)
    {
        if (explorable instanceof IRuleGroupController rgc)
        {
            synchronized (ruleGroupControllers)
            {
                if (add)
                {
                    ruleGroupControllers.add(rgc);
                } else
                {
                    ruleGroupControllers.remove(rgc);
                }
            }
        }
    }

    @Override
    public boolean hasExplorable(String key)
    {
        return explorables.containsKey(key) || getExplorable(key) != null;
    }

    @Override
    public boolean removeExplorable(IExplorable explorable)
    {
        if (explorables.containsKey(explorable.getUniqueKey()))
        {
            // Grab the parent before we remove the deleted explorable from its groups
            final var parent = explorable.getParent();

            if (parent == null) return false; // Should only happen during tests

            explorable.getGroups().forEach(explorable::removeGroup);
            if (explorable instanceof IExplorableGroup group) group.getChildren().forEach(group::removeChild);

            recordIfRuleGroupController(explorable, false);
            explorable.setParent(null);
            removeListeners(explorable);
            explorables.remove(explorable.getUniqueKey());
            explorable.dispose();
            firePropertyChange(EXPLORABLE_REMOVED, explorable, null);
            return true;
        } else
        {
            return false;
        }
    }

    @Nonnull
    @Override
    public IPathExplorableGroup getRoot()
    {
        return rootGroup;
    }

    @Override
    public Collection<IExplorable> getExplorables()
    {
        return List.copyOf(explorables.values());
    }

    @Override
    public Collection<IExplorable> getExplorables(Predicate<IExplorable> filter)
    {
        return explorables.values().stream().filter(filter).toList();
    }

    @Override
    @Nonnull
    public List<List<IExplorable>> getExplorablePaths(@Nonnull IExplorable explorable)
    {
        List<List<IExplorable>> paths = new ArrayList<>();
        List<IExplorable> groups = explorable.getGroups();
        if (groups.isEmpty())
        {
            paths.add(new ArrayList<>(List.of(explorable)));
        } else
        {
            groups.stream()
                    .flatMap(g -> getExplorablePaths(g).stream())
                    .filter(path -> path.get(0).equals(getRoot())) // Filter out any paths that don't start with the root, in case there are orphans
                    .peek(l -> l.add(explorable))
                    .forEach(paths::add);
        }
        return paths;
    }

    @Override
    @Nonnull
    public List<IRuleGroupController> getRuleGroupControllers()
    {
        return new ArrayList<>(ruleGroupControllers);
    }

    @Override
    public void dispose()
    {
        explorables.clear();
    }

    private void explorableChildChanged(PropertyChangeEvent event)
    {
        if (event.getNewValue() != null) explorableAdded(((IExplorable) event.getNewValue())); // Child was added, make sure its part of the model
    }

    private void explorableAdded(IExplorable explorable)
    {
        String key = explorable.getUniqueKey();
        if (!explorables.containsKey(key))
        {
            recordIfRuleGroupController(explorable, true);
            // Add to unique map
            explorables.put(key, explorable);
            addListeners(explorable);
            firePropertyChange(EXPLORABLE_ADDED, null, explorable);
            // Now make sure all children are added
            for (IExplorable child : explorable.getChildren()) explorableAdded(child);
        }
    }

    private void addListeners(IExplorable explorable)
    {
        explorable.addPropertyChangeListener(IExplorable.CHILD, childChangedListener); // Handle children added
        explorable.addPropertyChangeListener(propertyChangePropagateListener); // Propagate all changes out
    }

    private void removeListeners(IExplorable explorable)
    {
        explorable.removePropertyChangeListener(IExplorable.CHILD, childChangedListener);
        explorable.removePropertyChangeListener(propertyChangePropagateListener);
    }
}
