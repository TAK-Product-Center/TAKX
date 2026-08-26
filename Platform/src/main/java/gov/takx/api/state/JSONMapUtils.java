package gov.takx.api.state;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for handling instances of the JSON-implementation of IRestorableMap.
 *
 * @since 1.48
 */
public class JSONMapUtils
{
    /**
     * Create a new (JSONMap implementation of a) {@link IRestorableMap} instance.
     *
     * @return a new IRestorableMap instance.
     */
    public static IRestorableMap createNewRestorableMap()
    {
        return new JSONMap();
    }

    /**
     * Restore a (JSONMap implementation of a) {@link IRestorableMap} from a JSON String.
     *
     * @param jsonString a JSON String representation of an IRestorableMap.
     * @return restored IRestorableMap.
     */
    public static IRestorableMap restoreMap(String jsonString)
    {
        return new JSONMap(jsonString);
    }

    /**
     * Restore a (JSONMap implementation of a) {@link IRestorableMap} from a JSON file.
     *
     * @param jsonFile a file containing a JSON representation of an IRestorableMap.
     * @return restored IRestorableMap.
     * @throws Exception if the file does not exist or is unreadable.
     */
    public static IRestorableMap restoreMap(File jsonFile) throws Exception
    {
        return new JSONMap(jsonFile);
    }

    /**
     * A convenience method to create a List of IRestoreMaps from an Iterable of IRestorableMapSupport objects.
     *
     * @param restorables an Iterable of IRestorableMapSupport objects.
     * @return a List of IRestoreMaps created from the IRestorableMapSupport objects.
     */
    public static List<IRestorableMap> createNewRestoreMapList(Iterable<? extends IRestorableMapSupport> restorables)
    {
        List<IRestorableMap> stateList = new ArrayList<>();
        for (IRestorableMapSupport persistable : restorables)
        {
            IRestorableMap state = new JSONMap();
            persistable.store(state);
            stateList.add(state);
        }
        return stateList;
    }
}
