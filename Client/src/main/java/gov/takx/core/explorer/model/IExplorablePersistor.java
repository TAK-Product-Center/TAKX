package gov.takx.core.explorer.model;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.IRestorableMapSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Interface for customizing the store/restore logic for explorables within an {@link IExplorerModel}. Multiple persistors 
 * can be added to the model and will be given the chance to bulk load explorables as well as load individual records.
 * There are two possible methods for storing/restoring explorables: <br>
 * <ul>
 *     <li>Bulk - Bulk store/restore calls receive the model wide RestorableMap object, and are able to perform any operation
 *     on it necessary to store or restore their contents</li>
 *     <li>Individual - Individual calls receive a reference to a specific explorable, and accept/return a RestorableMap that should
 *     be used only for that single explorable. This allows for more modular control over storing and restoring.</li>
 * </ul>
 * The bulk store/restore calls are made to {@link IRestorableMapSupport#store(IRestorableMap)} and 
 * {@link IRestorableMapSupport#restore(IRestorableMap)}, and the individual calls are made to the methods in this subclass.
 *
 * @since 5.3
 */
public interface IExplorablePersistor extends IRestorableMapSupport
{
    /**
     * Attempt to create an {@link IRestorableMap} for an individual explorable, using the root map if necessary.
     * 
     * @param explorable the explorable to store
     * @param map the root restorable map for the controller
     * @return the map created for the explorable, or {@code null} if this persistor was unable to create one
     */
    @Nullable
    IRestorableMap storeExplorable(@Nonnull IExplorable explorable, @Nonnull IRestorableMap map);

    /**
     * Attempt to restore an individual {@link IExplorable} from the given {@link IRestorableMap}.
     *
     * @param map the map for an individual explorable
     * @return the restored explorable, or {@code null} if this persistor was unable to restore one
     */
    @Nullable
    IExplorable restoreExplorable(@Nonnull IRestorableMap map);
}