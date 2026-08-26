package gov.takx.api.event;

/**
 * CDI Event fired when the project-level background services should start to spin up.  Observers are prioritized
 * to ensure that services start in the proper order.
 * <p/>
 * The background services are those that should not block the main service start up, and can safely be done in
 * the background, allowing the UI to be utilized by the user more quickly.
 *
 * @since 5.2
 */
public record StartBackgroundProjectServices()
{
}
