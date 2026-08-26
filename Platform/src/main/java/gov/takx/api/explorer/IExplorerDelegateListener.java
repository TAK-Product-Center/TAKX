package gov.takx.api.explorer;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * A general-purpose event listener interface for plugins interested in receiving Explorer Delegate events.
 *
 * @since 1.49
 */
public interface IExplorerDelegateListener
{
    /**
     * Event name for newly-added explorable events.
     */
    String EXPLORABLE_ADDED_EVENT = "Explorer Delegate Explorable Added Event";

    /**
     * Event name for newly-removed explorable events.
     */
    String EXPLORABLE_REMOVED_EVENT = "Explorer Delegate Explorable Removed Event";

    /**
     * Event name for alarm state change events.
     */
    String ALARMS_EVENT = "Explorer Delegate Alarms Event";

    /**
     * Event Name for explorable selection change events.
     */
    String SELECTION_EVENT = "Explorer Delegate Selection Event";

    /**
     * Event name for explorable selection change events, with old and new selection state.
     *
     * @since 2.29
     */
    String SELECTION_CHANGED_EVENT = "Explorer Delegate Selection Changed Event";

    /**
     * Event name for explorable visibility change events.
     */
    String VISIBILITY_EVENT = "Explorer Delegate Visibility Event";

    /**
     * Event name for explorer delegate load-state events.
     */
    String LOAD_STATE_EVENT = "Explorer Delegate Load State Event";

    /**
     * Event name for explorer delegate clear-state events.
     */
    String CLEAR_STATE_EVENT = "Explorer Delegate Clear State Event";

    /**
     * Event name for explorer delegate save-state events.
     */
    String SAVE_STATE_EVENT = "Explorer Delegate Save State Event";

    /**
     * A general-purpose method for Explorer Delegate listeners.
     *
     * @param eventName          Name of the event.  This string will always be non-null and non-empty.
     * @param affectedExplorable the affect explorable (if one was affected).  If no specific explorable was affected
     *                           by this event, this value will be {@code null}.
     * @param oldValue           The 'old' value replaced by this event.  Depending on the event, this may be {@code null}.
     * @param newValue           The 'new' replacement value for this event.  Depending on the event, this may be {@code null}.
     * @param isGroupTreeSource  Always {@code false}
     * @deprecated {@code isGroupTreeSource} parameter is no longer used. Use {@link #onExplorerEvent(String, IExplorable, Object, Object)}
     */
    @Deprecated(since = "5.8", forRemoval = true)
    @DeprecatedApi(since = "5.8", forRemoval = true, removeAt = "5.11")
    void onExplorerEvent(String eventName, IExplorable affectedExplorable, Object oldValue, Object newValue, boolean isGroupTreeSource);

    /**
     * A general-purpose method for Explorer Delegate listeners.
     *
     * @param eventName Name of the event.  This string will always be non-null and non-empty.
     * @param affectedExplorable the affect explorable (if one was affected).  If no specific explorable was affected by this
     * event, this value will be {@code null}.
     * @param oldValue The 'old' value replaced by this event.  Depending on the event, this may be {@code null}.
     * @param newValue The 'new' replacement value for this event.  Depending on the event, this may be {@code null}.
     * @since 5.8
     */
    default void onExplorerEvent(String eventName, IExplorable affectedExplorable, Object oldValue, Object newValue)
    {
        onExplorerEvent(eventName, affectedExplorable, oldValue, newValue, false);
    }

    /**
     * The selection state of an explorer node in an explorer event.
     *
     * @since 2.29
     */
    enum SelectionState
    {
        SELECTED, UNSELECTED;

        /**
         * Util method to convert boolean to proper enum value.
         * @param selected the selection state
         * @return {@link #SELECTED} if parameter is {@code true}, otherwise {@link #UNSELECTED}
         * @since 5.3
         */
        public static SelectionState valueOf(boolean selected)
        {
            return selected ? SELECTED : UNSELECTED;
        }
    }
}
