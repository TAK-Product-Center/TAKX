package gov.takx.api.state;

import jakarta.annotation.Nonnull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the {@link IRestorableMap} interface.  ExplorerState acts as a data
 * structure into which configuration parameters can be serialized to a JSON string, and stored/restored to/from a
 * file.
 *
 * @since 1.48
 */
public class JSONMap implements IRestorableMap
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String RESTORABLE_MAP_VERSION_KEY = "restorableMapVersion";
    private static final String RESTORABLE_MAP_VERSION = "1.0.0";
    private static final Set<String> RESERVED_KEYS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(RESTORABLE_MAP_VERSION_KEY)));

    private final JSONObject o;
    private final Map<String, Object> transientData;

    public JSONMap()
    {
        this(new JSONObject());
    }

    public JSONMap(String state)
    {
        this(new JSONObject(state));
    }

    public JSONMap(File stateFile) throws Exception
    {
        BufferedReader reader = new BufferedReader(new FileReader(stateFile));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            sb.append(line);
        }
        reader.close();
        o = new JSONObject(sb.toString());
        transientData = new HashMap<>();
        detectAndRemoveVersion();
    }

    public JSONMap(JSONObject o)
    {
        this.o = o;
        transientData = new HashMap<>();
        detectAndRemoveVersion();
    }

    @Override
    public String getVersion()
    {
        return RESTORABLE_MAP_VERSION;
    }

    @Override
    public Set<String> getKeys()
    {
        Set<String> keys = new HashSet<>();
        Iterator it = o.keys();
        while (it.hasNext())
        {
            Object key = it.next();
            if (key instanceof String)
            {
                keys.add((String) key);
            }
        }
        return keys;
    }

    @Override
    public boolean hasKey(String key)
    {
        return o.has(key);
    }

    @Override
    public boolean remove(String key)
    {
        return o.remove(key) != null;
    }

    @Override
    public void setString(String key, String value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setBoolean(String key, boolean value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setInt(String key, int value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setLong(String key, long value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setDouble(String key, double value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setStringList(String key, List<String> value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setBooleanList(String key, List<Boolean> value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setIntList(String key, List<Integer> value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setLongList(String key, List<Long> value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setDoubleList(String key, List<Double> value)
    {
        checkForReservedKey(key);
        o.put(key, value);
    }

    @Override
    public void setRestorableMap(String key, IRestorableMap value)
    {
        checkForReservedKey(key);
        o.put(key, ((JSONMap) value).getJSONObject());
    }

    @Override
    public void setRestorableMapList(String key, List<IRestorableMap> value)
    {
        checkForReservedKey(key);
        List<JSONObject> jsonObjects = new ArrayList<>();
        for (IRestorableMap state : value)
        {
            jsonObjects.add(((JSONMap) state).getJSONObject());
        }
        o.put(key, new JSONArray(jsonObjects));
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue)
    {
        return getBoolean(o, key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue)
    {
        return getInt(o, key, defaultValue);
    }

    @Override
    public long getLong(String key, long defaultValue)
    {
        return getLong(o, key, defaultValue);
    }

    @Override
    public double getDouble(String key, double defaultValue)
    {
        return getDouble(o, key, defaultValue);
    }

    @Override
    public String getString(String key, String defaultValue)
    {
        return getString(o, key, defaultValue);
    }

    @Override
    public List<String> getStringList(String key, List<String> defaultValue)
    {
        return getStringList(o, key, defaultValue);
    }

    @Override
    public List<Boolean> getBooleanList(String key, List<Boolean> defaultValue)
    {
        return getBooleanList(o, key, defaultValue);
    }

    @Override
    public List<Integer> getIntList(String key, List<Integer> defaultValue)
    {
        return getIntList(o, key, defaultValue);
    }

    @Override
    public List<Long> getLongList(String key, List<Long> defaultValue)
    {
        return getLongList(o, key, defaultValue);
    }

    @Override
    public List<Double> getDoubleList(String key, List<Double> defaultValue)
    {
        return getDoubleList(o, key, defaultValue);
    }

    @Override
    public IRestorableMap getRestorableMap(String key, IRestorableMap defaultValue)
    {
        JSONObject jsonObject = getJSONObject(o, key, null);
        return jsonObject != null ? new JSONMap(jsonObject) : defaultValue;
    }

    @Override
    @Nonnull
    public List<IRestorableMap> getRestorableMapList(String key)
    {
        List<IRestorableMap> stateList = new ArrayList<>();
        try
        {
            JSONArray jsonArray = o.getJSONArray(key);
            int length = jsonArray.length();
            for (int i = 0; i < length; i++)
            {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject != null)
                {
                    stateList.add(new JSONMap(jsonObject));
                }
            }
        } catch (JSONException json)
        {
            logger.warn("Json array for key {} not found", key, json);
        }
        return stateList;
    }

    @Override
    public String toString()
    {
        return o.toString(4);
    }

    @Override
    public boolean writeToFile(File file)
    {
        if (file == null)
        {
            return false;
        }

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(file));
            o.put(RESTORABLE_MAP_VERSION_KEY, RESTORABLE_MAP_VERSION);
            writer.write(o.toString(4));
            o.remove(RESTORABLE_MAP_VERSION_KEY);
        } catch (IOException e)
        {
            logger.error("Exception writing explorer state to file", e);
            return false;
        } finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                } catch (IOException e)
                {
                    // do nothing
                }
            }
        }
        return true;
    }

    @Override
    public void setTransientValue(String key, Object value)
    {
        transientData.put(key, value);
    }

    @Override
    public Object getTransientValue(String key)
    {
        return transientData.get(key);
    }

    @Override
    public boolean hasTransientKey(String key)
    {
        return transientData.containsKey(key);
    }

    @Override
    public Set<String> getTransientKeys()
    {
        return new HashSet<>(transientData.keySet());
    }

    @Override
    public IRestorableMap createNewRestorableMap()
    {
        return new JSONMap();
    }

    @SuppressWarnings("EqualsHashCode")
    @Override
    public boolean equals(Object o)
    {
        return o instanceof IRestorableMap && equals(this, o);
    }

    /**
     * Get the underlying JSONObject from this instance.
     *
     * @return the underlying JSONObject.
     */
    public JSONObject getJSONObject()
    {
        return o;
    }

    protected void checkForReservedKey(String key)
    {
        if (RESERVED_KEYS.contains(key))
        {
            throw new IllegalArgumentException("Key=" + key + " is reserved and may not be used");
        }
    }

    protected void detectAndRemoveVersion()
    {
        if (o.has(RESTORABLE_MAP_VERSION_KEY))
        {
            String restoredVersion = o.getString(RESTORABLE_MAP_VERSION_KEY);
            if (!RESTORABLE_MAP_VERSION.equals(restoredVersion))
            {
                logger.warn("Restored Map version={} does not match current version=" + RESTORABLE_MAP_VERSION + ", some data may be incorrectly restored.", restoredVersion);
            }
            o.remove(RESTORABLE_MAP_VERSION_KEY);
        }
    }

    protected boolean equals(Object o1, Object o2)
    {
        if (o1 == null && o2 == null)
        {
            return true;
        } else if (o1 == null || o2 == null)
        {
            return false;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("o1={}, class={}, o2={}, class={}", o1, o1.getClass(), o2, o2.getClass());
        }

        if (o1 instanceof JSONArray && o2 instanceof JSONArray)
        {
            return equals((JSONArray) o1, (JSONArray) o2);
        } else if (o1 instanceof JSONObject && o2 instanceof JSONObject)
        {
            return equals((JSONObject) o1, (JSONObject) o2);
        } else if (o1 instanceof JSONMap && o2 instanceof JSONMap)
        {
            return equals(((JSONMap) o1).getJSONObject(), ((JSONMap) o2).getJSONObject());
        } else if (o1 instanceof Number && o2 instanceof Number)
        {
            return ((Number) o1).doubleValue() == ((Number) o2).doubleValue();
        }
        return o1.equals(o2);
    }

    protected boolean equals(JSONArray a1, JSONArray a2)
    {
        int length = a1.length();
        if (length != a2.length())
        {
            logger.debug("mismatched array lengths");
            return false;
        }
        for (int i = 0; i < length; i++)
        {
            Object o1 = a1.get(i);
            Object o2 = a2.get(i);
            if (!equals(o1, o2))
            {
                logger.debug("mismatched array values: o1={}, o2={}", o1, o2);
                return false;
            }
        }
        return true;
    }

    protected boolean equals(JSONObject j1, JSONObject j2)
    {
        Set keys = j1.keySet();
        if (!keys.equals(j2.keySet()))
        {
            logger.debug("mismatched key sets");
            return false;
        }
        for (Object o : keys)
        {
            String key = (String) o;
            Object o1 = j1.get(key);
            Object o2 = j2.get(key);
            if (!equals(o1, o2))
            {
                logger.debug("mismatched values: o1={}, o2={}", o1, o2);
                return false;
            }
        }
        return true;
    }

    protected static JSONObject getJSONObject(JSONObject o, String key, JSONObject defaultValue)
    {
        try
        {
            return o.getJSONObject(key);
        } catch (Exception e)
        {
            logWarning("JSONObject", e);
        }
        return defaultValue;
    }

    protected static List<Integer> getIntList(JSONObject o, String key, List<Integer> defaultValue)
    {
        return getList(o, key, Integer.class, defaultValue);
    }

    protected static List<Long> getLongList(JSONObject o, String key, List<Long> defaultValue)
    {
        return getList(o, key, Long.class, defaultValue);
    }

    protected static List<Boolean> getBooleanList(JSONObject o, String key, List<Boolean> defaultValue)
    {
        return getList(o, key, Boolean.class, defaultValue);
    }

    protected static List<Double> getDoubleList(JSONObject o, String key, List<Double> defaultValue)
    {
        return getList(o, key, Double.class, defaultValue);
    }

    protected static List<String> getStringList(JSONObject o, String key, List<String> defaultValue)
    {
        return getList(o, key, String.class, defaultValue);
    }

    protected static String getString(JSONObject o, String key, String defaultValue)
    {
        try
        {
            return o.getString(key);
        } catch (Exception e)
        {
            logWarning("String", e);
        }
        return defaultValue;
    }

    protected static int getInt(JSONObject o, String key, int defaultValue)
    {
        try
        {
            return o.getInt(key);
        } catch (Exception e)
        {
            logWarning("int", e);
        }
        return defaultValue;
    }

    protected static long getLong(JSONObject o, String key, long defaultValue)
    {
        try
        {
            return o.getLong(key);
        } catch (Exception e)
        {
            logWarning("long", e);
        }
        return defaultValue;
    }

    protected static double getDouble(JSONObject o, String key, double defaultValue)
    {
        try
        {
            return o.getDouble(key);
        } catch (Exception e)
        {
            logWarning("double", e);
        }
        return defaultValue;
    }

    protected static float getFloat(JSONObject o, String key, float defaultValue)
    {
        try
        {
            Object object = o.get(key);
            return object instanceof Number ? ((Number) object).floatValue()
                    : Float.parseFloat((String) object);
        } catch (Exception e)
        {
            logWarning("float", e);
        }
        return defaultValue;
    }

    protected static boolean getBoolean(JSONObject o, String key, boolean defaultValue)
    {
        try
        {
            return o.getBoolean(key);
        } catch (Exception e)
        {
            logWarning("boolean", e);
        }
        return defaultValue;
    }

    protected static <T> List<T> getList(JSONObject o, String key, Class<T> type, List<T> defaultValue)
    {
        try
        {
            JSONArray array = o.getJSONArray(key);
            int length = array.length();
            List<T> l = new ArrayList<>();
            for (int i = 0; i < length; i++)
            {
                l.add(getItem(array, i, type));
            }
            return l;
        } catch (Exception e)
        {
            logWarning(type.getName() + " List", e);
        }
        return defaultValue;
    }

    protected static <T> T getItem(JSONArray array, int index, Class<T> type)
    {
        Object o = null;
        if (String.class.equals(type))
        {
            o = array.getString(index);
        } else if (Integer.class.equals(type))
        {
            o = array.getInt(index);
        } else if (Long.class.equals(type))
        {
            o = array.getLong(index);
        } else if (Double.class.equals(type))
        {
            o = array.getDouble(index);
        } else if (Boolean.class.equals(type))
        {
            o = array.getBoolean(index);
        }
        return o != null ? type.cast(o) : null;
    }

    protected static void logWarning(String type, Exception e)
    {
        logger.debug("Unable to get {} from JSONObject", type, e);
    }

    protected static boolean restore(IRestorableMap state, String key, List<? extends IRestorableMapSupport> restorables)
    {
        List<IRestorableMap> stateList = state.getRestorableMapList(key);
        if (stateList == null)
        {
            logger.error("Unable to restore state list with key={}", key);
            return false;
        }
        return restore(stateList, restorables);
    }

    protected static boolean restore(List<IRestorableMap> restoreMapList,
                                     List<? extends IRestorableMapSupport> restorables)
    {
        int size = restoreMapList.size();
        if (size != restorables.size())
        {
            logger.error("Unable to restore restorables from state list.  Mismatched sizes");
            return false;
        }

        for (int i = 0; i < size; i++)
        {
            restorables.get(i).restore(restoreMapList.get(i));
        }
        return true;
    }
}
