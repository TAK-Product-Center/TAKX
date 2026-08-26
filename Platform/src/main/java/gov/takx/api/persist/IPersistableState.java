package gov.takx.api.persist;

/**
 * Defines a mechanism for allowing a plugin to store its state and have its previous state injected back into it at
 * startup.
 *
 * @since 1.3
 */
public interface IPersistableState
{
    /**
     * Stores the state of the plugin instance.  A good place to create an XML string with custom
     * state of the plugin.  This method is called prior to shutdown.
     *
     * @return state to persist, or return {@code null} if there is no state.
     */
    String store();

    /**
     * Previous state handed back to the client plug-in controller. This call is guaranteed to only be invoked when
     * there is non-null state to restore. In other words, the developer is assured that the state will never be
     * {@code null}, and that this method will never be called if there is no persisted state. The ramification of this
     * is that developers must not treat this method as a lifecycle event since it is guaranteed <b>not</b> to fire when
     * there is no state to restore.
     *
     * @param state State being restored.
     */
    void restore(String state);
}
