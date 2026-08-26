package gov.takx.api.state;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * A data structure designed to store and restore configuration information in a key/value pair fashion.
 * <p/>
 * Data is stored as typical map-style key/value pairs, where keys are Strings, and values are one of a number
 * of primitive types or simple collections.  This interface also supports nested state data, where values can also be
 * other instances of IRestorableMap.
 * <p/>
 * Restorable maps can be stored and restored to/from files, the format of which is implementation-specific.
 *
 * @since 1.48
 */
public interface IRestorableMap
{
    /**
     * Get the available keys in this state instance.
     *
     * @return the keys in this instance.
     */
    Set<String> getKeys();

    /**
     * Determine whether this instance has a specific key.
     *
     * @param key key of interest.
     * @return {@code true} if this instance has the key, or {@code false} otherwise.
     */
    boolean hasKey(String key);

    /**
     * Get the version of this restorable map implementation.
     *
     * @return the version of this restorable map implementation.
     */
    String getVersion();

    /**
     * Remove a key/value pair from this instance.
     *
     * @param key key of pair to remove.
     * @return {@code true} if removal was successful, or {@code false} otherwise.
     */
    boolean remove(String key);

    /**
     * A factory method to create a new instance of this class.
     *
     * @return a new instance of this class.
     */
    IRestorableMap createNewRestorableMap();

    /**
     * Set a String value.
     *
     * @param key   key for key/value pair.
     * @param value String value for key/value pair.
     */
    void setString(String key, String value);

    /**
     * Set a boolean value.
     *
     * @param key   key for key/value pair.
     * @param value boolean value for key/value pair.
     */
    void setBoolean(String key, boolean value);

    /**
     * Set an int value.
     *
     * @param key   key for key/value pair.
     * @param value int value for key/value pair.
     */
    void setInt(String key, int value);

    /**
     * Set a long value.
     *
     * @param key   key for key/value pair.
     * @param value long value for key/value pair.
     */
    void setLong(String key, long value);

    /**
     * Set a double value.
     *
     * @param key   key for key/value pair.
     * @param value int value for key/value pair.
     */
    void setDouble(String key, double value);

    /**
     * Set an IExplorerState value.
     *
     * @param key   key for key/value pair.
     * @param value IExplorerState value for key/value pair.
     */
    void setRestorableMap(String key, IRestorableMap value);

    /**
     * Set a String List value.
     *
     * @param key   key for key/value pair.
     * @param value String List value for key/value pair.
     */
    void setStringList(String key, List<String> value);

    /**
     * Set a boolean List value.
     *
     * @param key   key for key/value pair.
     * @param value boolean List value for key/value pair.
     */
    void setBooleanList(String key, List<Boolean> value);

    /**
     * Set an int List value.
     *
     * @param key   key for key/value pair.
     * @param value int List value for key/value pair.
     */
    void setIntList(String key, List<Integer> value);

    /**
     * Set a long List value.
     *
     * @param key   key for key/value pair.
     * @param value long LIst value for key/value pair.
     */
    void setLongList(String key, List<Long> value);

    /**
     * Set a double List value.
     *
     * @param key   key for key/value pair.
     * @param value double List value for key/value pair.
     */
    void setDoubleList(String key, List<Double> value);

    /**
     * Set an IExplorerState List value.
     *
     * @param key   key for key/value pair.
     * @param value IJSONState List value for key/value pair.
     */
    void setRestorableMapList(String key, List<IRestorableMap> value);

    /**
     * Get a boolean value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default boolean value to return if key does not exist.
     * @return a boolean value from this instance, or the default value if the key does not exist.
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Get an int value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default int value to return if key does not exist.
     * @return an int value from this instance, or the default value if the key does not exist.
     */
    int getInt(String key, int defaultValue);

    /**
     * Get a long value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default long value to return if key does not exist.
     * @return a long value from this instance, or the default value if the key does not exist.
     */
    long getLong(String key, long defaultValue);

    /**
     * Get a double value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default double value to return if key does not exist.
     * @return a double value from this instance, or the default value if the key does not exist.
     */
    double getDouble(String key, double defaultValue);

    /**
     * Get a String value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default String value to return if key does not exist.
     * @return a String value from this instance, or the default value if the key does not exist.
     */
    String getString(String key, String defaultValue);

    /**
     * Get an IExplorerState value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default IExplorerState value to return if key does not exist.
     * @return an IExplorerState value from this instance, or the default value if the key does not exist.
     */
    IRestorableMap getRestorableMap(String key, IRestorableMap defaultValue);

    /**
     * Get a String List value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default String List value to return if key does not exist.
     * @return a String List value from this instance, or the default value if the key does not exist.
     */
    List<String> getStringList(String key, List<String> defaultValue);

    /**
     * Get a boolean List value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default boolean List value to return if key does not exist.
     * @return a boolean List value from this instance, or the default value if the key does not exist.
     */
    List<Boolean> getBooleanList(String key, List<Boolean> defaultValue);

    /**
     * Get an int List value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default int List value to return if key does not exist.
     * @return a int List value from this instance, or the default value if the key does not exist.
     */
    List<Integer> getIntList(String key, List<Integer> defaultValue);

    /**
     * Get a long List value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default long List value to return if key does not exist.
     * @return a long List value from this instance, or the default value if the key does not exist.
     */
    List<Long> getLongList(String key, List<Long> defaultValue);

    /**
     * Get a double List value.
     *
     * @param key          key for key/value pair.
     * @param defaultValue default double List value to return if key does not exist.
     * @return a double List value from this instance, or the default value if the key does not exist.
     */
    List<Double> getDoubleList(String key, List<Double> defaultValue);

    /**
     * Get an IJSONState List value.
     *
     * @param key key for key/value pair.
     * @return an IExplorerState List value from this instance, or the default value if the key does not exist.
     */
    List<IRestorableMap> getRestorableMapList(String key);

    /**
     * Write state information to a file.
     *
     * @param file file to which state information will be written.
     * @return {@code true} if the write was successful, or {@code false} otherwise.
     */
    boolean writeToFile(File file);

    /**
     * Put a transient value into this map.  Transient key/value pairs are *NOT* stored/restored by the map, but may be
     * useful in situations where a parent object needs to provide extra information to a child object during
     * store/restore.
     *
     * @param key   transient key.
     * @param value transient value.
     */
    void setTransientValue(String key, Object value);

    /**
     * Get a transient value from this map.  Transient key/value pairs are *NOT* stored/restored by the map, but may be
     * useful in situations where a parent object needs to provide extra information to a child object during
     * store/restore.
     *
     * @param key transient key.
     * @return transient value associated with the key.
     */
    Object getTransientValue(String key);

    /**
     * Determine whether a transient key exists in this map.  Transient key/value pairs are *NOT* stored/restored by
     * the map, but may be useful in situations where a parent object needs to provide extra information to a child
     * object during store/restore.
     *
     * @param key transient key of interest.
     * @return {@code true} if the transient key exists in this map, or {@code false} if it does not.
     */
    boolean hasTransientKey(String key);

    /**
     * Get a set of transient keys in this map.  Transient key/value pairs are *NOT* stored/restored by the map, but
     * may be useful in situations where a parent object needs to provide extra information to a child object during
     * store/restore.
     *
     * @return set of transient keys in this map.
     */
    Set<String> getTransientKeys();
}
