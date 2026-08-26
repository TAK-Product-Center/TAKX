/**
 * Events which are fired using CDI.
 * <p>
 * To observe one of these events, classes participating in CDI can define a method with an
 * {@link jakarta.enterprise.event.Observes} annotation on a parameter of the desired type. Here is an example of
 * observing the {@link gov.takx.api.event.app.AppInstalled} event:
 * <pre>
 * private void onAppInstalled(@Observes AppInstalled event)
 * {
 *     // Handle the event...
 * }
 * </pre>
 *
 * @since 2.0
 */
package gov.takx.api.event;
