package gov.takx.core.globe.tak.editor;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.tak.api.engine.map.MapRenderer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.view.gui.EnterLocationDialog;
import jakarta.inject.Inject;

import java.awt.Point;
import java.util.Map;

/**
 * An implementation of {@link IControlPointController} for multipoint source objects. Provides:
 * <ul>
 *     <li>{@link SelectEvent} monitoring for control points</li>
 *     <li>Control point hover handling</li>
 *     <li>Control point drag handling</li>
 *     <li>Control point click-to-add handling</li>
 *     <li>Control point double-click-to-move handling</li>
 *     <li>Control point ctrl-click-to-remove handling</li>
 * </ul>
 *
 * @since 5.4
 */
@PseudoBean
public class MultipointControlPointController extends AControlPointController implements SelectListener
{
    private static final Map<String, Object> METADATA_MODIFIER_CTRL = Map.of(IControlPoint.MODIFIER_CTRL, true);

    @Inject
    private ApplicationGlobeManager globeManager;

    private IControlPoint hoveredControlPoint;
    private String dragControlPointType;
    private boolean armed;

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        if (enabled)
        {
            globeManager.getAllGlobeDelegates().forEach(g -> g.getWorldWindow().addSelectListener(this));
        } else
        {
            globeManager.getAllGlobeDelegates().forEach(g -> g.getWorldWindow().removeSelectListener(this));
        }
    }

    @Override
    public void setArmed(boolean armed)
    {
        this.armed = armed;
    }

    @Override
    public void selected(SelectEvent event)
    {
        IControlPoint controlPoint = event.getTopObject() instanceof IControlPoint cp ? cp : null;
        if (event.isRollover() && dragControlPointType == null)
        {
            if (controlPoint != hoveredControlPoint && hoveredControlPoint != null)
            {
                hoveredControlPoint.setSelected(false);
                hoveredControlPoint = null;
            }
            if (controlPoint != null)
            {
                controlPoint.setSelected(true);
                hoveredControlPoint = controlPoint;
            }
        } else if (event.isDrag())
        {
            if (controlPoint != null && controlPoint.isDraggable())
            {
                Point pickPoint = event.getPickPoint();
                MapRenderer<?> mapRenderer = getGlobeFromEvent(event).getPortableMapRenderer();
                ILocation location = inverse(pickPoint, mapRenderer, controlPoint);
                if (location != null)
                {
                    dragControlPointType = controlPoint.getControlPointType();
                    onControlPointDragged(dragControlPointType, location);
                    IControlPoint controlPointByType = IControlPoint.getControlPointByType(getControlPoints(), dragControlPointType);
                    if (controlPointByType != null) controlPointByType.setSelected(true);
                    event.consume();
                }
            }
        } else if (event.isDragEnd())
        {
            if (dragControlPointType != null)
            {
                IControlPoint dragControlPoint = IControlPoint.getControlPointByType(getControlPoints(), dragControlPointType);
                if (dragControlPoint != null) dragControlPoint.setDragging(false);
            }
            dragControlPointType = null;
        } else if (event.isLeftClick())
        {
            if (event.getMouseEvent().isControlDown())
            {
                if (controlPoint != null)
                {
                    onControlPointRemoved(controlPoint);
                } else
                {
                    Point pickPoint = event.getPickPoint();
                    MapRenderer<?> mapRenderer = getGlobeFromEvent(event).getPortableMapRenderer();
                    ILocation location = inverse(pickPoint, mapRenderer);
                    if (location != null)
                    {
                        onControlPointAdded(location, METADATA_MODIFIER_CTRL);
                    }
                }
            }
        } else if (event.isLeftPress())
        {
            if (hoveredControlPoint != null)
            {
                dragControlPointType = hoveredControlPoint.getControlPointType();
                hoveredControlPoint.setDragging(true);
            }
        }
        // PointAndClickControlPointController will consume the double click mouse event that ends the shape
        // creation.  We use that to avoid showing the enter location dialog on that double click.
        else if (event.isLeftDoubleClick() && controlPoint != null && controlPoint.isDraggable() && !armed && !event.getMouseEvent().isConsumed())
        {
            final var dialog = new EnterLocationDialog();

            dialog.setDescription("<html>Enter the coordinates that you would like to set on this control point</html>");
            dialog.setLocation(controlPoint.getLocation());
            dialog.setVisible(true);

            if (!dialog.wasCanceled())
            {
                final var location = dialog.getEnteredLocation();
                if (location != null) onControlPointDragged(controlPoint.getControlPointType(), location);
            }
        }
    }

    private IApplicationGlobe getGlobeFromEvent(SelectEvent e)
    {
        IApplicationGlobe globe = null;
        if (e.getSource() instanceof WorldWindow worldWindow)
        {
            globe = globeManager.getGlobeForComponent(worldWindow);
        }
        return globe != null ? globe : globeManager.getLastFocusedGlobe();
    }
}
