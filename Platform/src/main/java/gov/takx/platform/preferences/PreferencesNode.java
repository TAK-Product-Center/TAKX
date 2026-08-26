package gov.takx.platform.preferences;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper for operations on a Preferences node.  Can stand alone for simple cases, or subclasses can provide more
 * specific conversions/operations.
 * <p>
 * The primary intent for this class is to isolate any direct {@link Preferences} usage within the
 * code base at large. Abstracting it here will allow for an easier migration path in the future when we pick
 * a new preferences/configuration implementation.
 * <p>
 * Even though the Preferences API requires us to handle {@link BackingStoreException}, they can never actually
 * be thrown given our custom Preferences implementation.  So, this class hides those exceptions from its exposed API.
 *
 * @since 2.35
 */
public class PreferencesNode
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final Preferences rootNode;

    /**
     * Construct on the given Preferences node.
     */
    public PreferencesNode(@Nullable Preferences rootNode)
    {
        this.rootNode = rootNode;
    }

    /**
     * Copy constructor, just unwraps rootNode in the given PreferencesNode.
     */
    public PreferencesNode(@Nullable PreferencesNode preferencesNode)
    {
        rootNode = preferencesNode != null ? preferencesNode.rootNode : null;
    }

    /**
     * @return {@code true} if the root node is not {@code null}
     */
    public boolean exists()
    {
        return rootNode != null;
    }

    /**
     * @return Name from root node
     */
    @Nullable
    public String name()
    {
        return rootNode != null ? rootNode.name() : null;
    }

    /**
     * @return The absolute path to our root node
     * @since 5.1
     */
    @Nullable
    public String absolutePath()
    {
        return rootNode != null ? rootNode.absolutePath() : null;
    }

    /**
     * Determine if the named node exists within the root node.
     *
     * @param pathName Node path to test (relative to root node)
     * @return {@code true} if the node exists
     */
    public boolean nodeExists(@Nonnull String pathName)
    {
        try
        {
            return rootNode != null && rootNode.nodeExists(pathName);
        } catch (BackingStoreException e)
        {
            logger.error("Error accessing preference node: {}", rootNode, e);
            return false;
        }
    }

    /**
     * @return Preferences node for the given path name
     */
    @Nullable
    public PreferencesNode node(@Nonnull String pathName)
    {
        return rootNode != null ? new PreferencesNode(rootNode.node(pathName)) : null;
    }

    /**
     * @return List of all keys defined on the root node
     */
    @Nonnull
    public List<String> keys()
    {
        try
        {
            return rootNode != null ? Arrays.asList(rootNode.keys()) : Collections.emptyList();
        } catch (BackingStoreException e)
        {
            logger.error("Error accessing preference keys: {}", rootNode, e);
            return Collections.emptyList();
        }
    }

    /**
     * @return List of the names of the child nodes
     */
    @Nonnull
    public List<String> childrenNames()
    {
        try
        {
            return rootNode != null ? Arrays.asList(rootNode.childrenNames()) : Collections.emptyList();
        } catch (BackingStoreException e)
        {
            logger.error("Error accessing preference children: {}", rootNode, e);
        }
        return Collections.emptyList();
    }

    /**
     * @return List of all values of each key defined on the root node
     */
    @Nonnull
    public List<String> values()
    {
        return keys().stream()
                .map(key -> rootNode.get(key, null))
                .collect(Collectors.toList());
    }

    /**
     * Convert all the key/value pairs according to a given function and return them as a list.
     *
     * @param converter Conversion function, receives key and value
     * @param <T>       Inferred result type
     * @return List of converted values
     */
    @Nonnull
    public <T> List<T> keyValuesAs(@Nonnull BiFunction<String, String, T> converter)
    {
        return keys().stream()
                .map(key -> converter.apply(key, rootNode.get(key, null)))
                .collect(Collectors.toList());
    }

    /**
     * @return A map from key to value for every key defined on the root node
     */
    @Nonnull
    public Map<String, String> keyValuesAsMap()
    {
        return keys().stream()
                .collect(Collectors.toMap(key -> key, key -> rootNode.get(key, null)));
    }

    /**
     * Create a map from key to the result of a function applied to the value of that key.
     *
     * @param valueMapper Mapping function accepting the key's value
     * @param <T>         Inferred value type
     * @return A map from key to a converted value for every key defined on the root node
     */
    @Nonnull
    public <T> Map<String, T> keyValuesAsMap(@Nonnull Function<String, T> valueMapper)
    {
        return keys().stream()
                .collect(Collectors.toMap(key -> key, key -> valueMapper.apply(rootNode.get(key, null))));
    }

    /**
     * Create a map from key to int value for every key defined on the root node.
     *
     * @param keyFilter    Predicate to filter keys, return {@code true} to accept the key
     * @param defaultValue Default value to use if conversion to int value fails
     * @return map from key to int value
     * @see #getInt(String, int)
     */
    @Nonnull
    public Map<String, Integer> keyValuesAsIntMap(@Nonnull Predicate<String> keyFilter, int defaultValue)
    {
        return keys().stream()
                .filter(keyFilter)
                .collect(Collectors.toMap(key -> key, key -> rootNode.getInt(key, defaultValue)));
    }

    /**
     * Collect a list of all the values for the given key on the child nodes of the root node.
     *
     * @param key Key to extract from each child node
     * @return List of extracted values
     */
    @Nonnull
    public List<String> valuesFromChildren(@Nonnull String key)
    {
        return childrenAsStream()
                .map(node -> node.get(key, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Create a map from child name to the value of a function applied to the child node.
     *
     * @param valueMapper Function to apply to node to construct value for map
     * @param <T>         Inferred result/value type
     * @return Map from child node name to converted value
     */
    @Nonnull
    public <T> Map<String, T> childrenToMap(@Nonnull Function<PreferencesNode, T> valueMapper)
    {
        return childrenToMap(child -> true, valueMapper);
    }

    /**
     * Create a map from child name to the value of a function applied to the child node.  Only child nodes that
     * pass the given filter are included in the resulting map.
     *
     * @param filter      Filter to apply to child nodes
     * @param valueMapper Function to apply to node to construct value for map
     * @param <T>         Inferred result/value type
     * @return Map from child node name to converted value
     */
    @Nonnull
    public <T> Map<String, T> childrenToMap(@Nonnull Predicate<PreferencesNode> filter, @Nonnull Function<PreferencesNode, T> valueMapper)
    {
        return childrenAsStream()
                .filter(filter)
                .collect(Collectors.toMap(PreferencesNode::name, valueMapper));
    }

    /**
     * Get the value of the given key from the root node.
     *
     * @param key          Key to retrieve
     * @param defaultValue Default value to return if the root node is {@code null} or the key is not present
     * @return Value of key or default value
     */
    @Nullable
    public String get(@Nonnull String key, @Nullable String defaultValue)
    {
        return rootNode != null ? rootNode.get(key, defaultValue) : defaultValue;
    }

    /**
     * Get the value of the given key as an {@code int} from the root node.
     *
     * @param key          Key to retrieve
     * @param defaultValue Default value to return if the root node is {@code null} or the key is not present
     * @return Value of key or default value
     */
    public int getInt(@Nonnull String key, int defaultValue)
    {
        return rootNode != null ? rootNode.getInt(key, defaultValue) : defaultValue;
    }

    /**
     * Get the value of the given key as a {@code long} from the root node.
     *
     * @param key          Key to retrieve
     * @param defaultValue Default value to return if the root node is {@code null} or the key is not present
     * @return Value of key or default value
     * @since 5.5.1
     */
    public long getLong(@Nonnull String key, long defaultValue)
    {
        return rootNode != null ? rootNode.getLong(key, defaultValue) : defaultValue;
    }

    /**
     * Get the value of the given key as a {@code boolean} from the root node.
     *
     * @param key          Key to retrieve
     * @param defaultValue Default value to return if the root node is {@code null} or the key is not present
     * @return Value of key or default value
     */
    public boolean getBoolean(@Nonnull String key, boolean defaultValue)
    {
        return rootNode != null ? rootNode.getBoolean(key, defaultValue) : defaultValue;
    }

    /**
     * Get the value of the given key as a {@code double} from the root node.
     *
     * @param key          Key to retrieve
     * @param defaultValue Default value to return if the root node is {@code null} or the key is not present
     * @return Value of key or default value
     */
    public double getDouble(@Nonnull String key, double defaultValue)
    {
        return rootNode != null ? rootNode.getDouble(key, defaultValue) : defaultValue;
    }

    /**
     * Removes the value associated with the specified key in this preference node, if any.
     *
     * @param key key whose mapping is to be removed from the preference node
     */
    public void remove(@Nonnull String key)
    {
        if (rootNode != null) rootNode.remove(key);
    }

    /**
     * Remove THIS node (rootNode) from the preferences.
     */
    public void removeNode()
    {
        try
        {
            if (rootNode != null) rootNode.removeNode();
        } catch (BackingStoreException e)
        {
            logger.error("Error removing root preferences node: {}", rootNode, e);
        }
    }

    /**
     * Remove the named child node from the root node.
     *
     * @param pathName Name of child node to remove
     */
    public void removeChildNode(@Nonnull String pathName)
    {
        try
        {
            if (rootNode != null && rootNode.nodeExists(pathName)) rootNode.node(pathName).removeNode();
        } catch (BackingStoreException e)
        {
            logger.error("Error removing preferences node: {}", pathName, e);
        }
    }

    /**
     * Clear all key/value mappings from the root node.
     */
    public void clear()
    {
        try
        {
            if (rootNode != null) rootNode.clear();
        } catch (BackingStoreException e)
        {
            logger.error("Error clearing preferences: {}", rootNode, e);
        }
    }

    /**
     * Put a String value on the root node.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public void put(@Nonnull String key, @Nonnull String value)
    {
        if (rootNode != null) rootNode.put(key, value);
    }

    /**
     * Put a boolean value on the root node.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public void put(@Nonnull String key, boolean value)
    {
        if (rootNode != null) rootNode.putBoolean(key, value);
    }

    /**
     * Put a numeric value on the root node.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public void put(@Nonnull String key, @Nonnull Number value)
    {
        if (rootNode != null) rootNode.put(key, String.valueOf(value));
    }

    /**
     * Store the given collection as numbered key/value pairs.  This is typically used for storing a list/set of
     * values where the keys are not relevant.  But Preferences doesn't offer the concept of an array/list type,
     * so we use an integer counter as the key.
     * <p>
     * Note that the node will be cleared before the collection is stored.
     *
     * @param values      collection of values to store
     * @param valueMapper Function to convert collection values to String to be stored
     * @see #clear()
     */
    public <T> void putAsValues(@Nonnull Collection<T> values, @Nonnull Function<T, String> valueMapper)
    {
        clear();

        int i = 0;
        for (T value : values)
        {
            put(Integer.toString(i++), valueMapper.apply(value));
        }
    }

    /**
     * Specialized form of {@link #putAsValues(Collection, Function)} handling collections of String.
     *
     * @param values collection of values to store
     * @see #putAsValues(Collection, Function)
     */
    public void putAsValues(@Nonnull Collection<String> values)
    {
        putAsValues(values, s -> s);
    }

    /**
     * @return Stream of child nodes
     */
    @Nonnull
    private Stream<PreferencesNode> childrenAsStream()
    {
        return childrenNames().stream().map(pathName -> new PreferencesNode(rootNode.node(pathName)));
    }

    public void dump(@Nonnull StringBuilder dest)
    {
        dest.append("Name '").append(name()).append("':\n");
        keys().forEach(
                key -> dest.append("   ").append(key).append(" = ").append(rootNode.get(key, null)).append('\n'));
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return rootNode == ((PreferencesNode) o).rootNode;
    }

    @Override
    public int hashCode()
    {
        return rootNode != null ? rootNode.hashCode() : 0;
    }
}
