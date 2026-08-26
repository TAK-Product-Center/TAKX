package gov.takx.api.plugin.app;

import gov.takx.api.persist.IPersistableState;
import gov.takx.api.plugin.IAnnotatedPlugin;
import gov.takx.api.plugin.IPluginDescriptor;
import gov.takx.api.plugin.annotations.AppPlugin;

/**
 * Contract for an App plugin.  This interface provides default implementations of most of the simple
 * getters for values that can be declared on the {@link AppPlugin} annotation.  This relieves the plugin
 * developer of the need to declare and implement the methods in {@link IPluginDescriptor} for example.  Instead,
 * they just declare the appropriate values in the annotation and these default implementations do the rest.
 *
 * @since 2.0.0
 */
public interface IAppPlugin extends IAnnotatedPlugin<AppPlugin>, IPersistableState
{
    /**
     * Called when the app is started. The app may be started when TAKX starts if it is configured to auto-start,
     * or may be started later by the operator. The app may be started and stopped an arbitrary number of times during
     * a program run.  At this point, the IAppPlugin will have a valid delegate and will start receiving
     * project lifecycle events.
     *
     * @throws Exception on any error
     */
    void start() throws Exception;

    /**
     * Called when the app is stopped. The app will be stopped if the project is closed, TAKX is shutdown, or the
     * user chooses to stop the app. The app may be started and stopped an arbitrary number of times during a
     * program run.  Note:  After the stop notification, the delegate will be disposed and should no longer be
     * used by the IAppPlugin.  Also, there will be no more notifications of project lifecycle events.
     *
     * @throws Exception on any error
     */
    void stop() throws Exception;

    /**
     * Called when the app is shutdown because TAKX is shutting down. The app will <b>not</b> be started again
     * after this method is called.
     *
     * @throws Exception on any error
     */
    void shutdown() throws Exception;

    /**
     * The app framework will call this method and pass in a delegate that the app can use to interact with
     * the rest of TAKX.
     *
     * @param delegate The app delegate.
     */
    void setAppDelegate(IAppPluginDelegate delegate);

    /**
     * If a string is returned, when the operator presses the stop button to stop the application,
     * the warning will be displayed in a popup dialog also giving the user a chance to cancel.
     *
     * @return warning message
     */
    String displayWarningOnStop();
}
