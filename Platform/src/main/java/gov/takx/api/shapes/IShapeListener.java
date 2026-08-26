package gov.takx.api.shapes;

import java.beans.PropertyChangeListener;

/**
 * Empty extension of {@link PropertyChangeListener} so we can differentiate listeners specifically monitoring
 * shapes from other property change listeners.  When shape listeners are attached to a shape Map Object, it indicates
 * that the plugin is using the shape in some way, and the user experience is slightly modified to alert the user
 * to the fact that the shape is being monitored, and to query the user before allowing them to directly edit or
 * delete the Map Object.
 *
 * @since 1.4
 */
public interface IShapeListener extends PropertyChangeListener
{
}
