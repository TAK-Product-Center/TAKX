package gov.takx.api.event;

/**
 * Event fired when a project is destroyed during integration testing.  This directs various components to reset
 * their state.
 *
 * @since 2.0.0
 */
public class TestProjectDestroyed implements TestEvent
{
}
