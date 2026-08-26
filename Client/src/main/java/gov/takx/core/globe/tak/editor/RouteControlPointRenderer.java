package gov.takx.core.globe.tak.editor;

import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLLabelManager.HorizontalAlignment;
import com.atakmap.map.opengl.GLLabelManager.VerticalAlignment;
import gov.takx.core.globe.tak.features.FrameFactory;
import gov.takx.platform.globe.GlobeTextUtils;

import java.util.ArrayList;

/**
 * An implementation of {@link IShapeEditorRenderer} that renders route control points using the TAK feature renderer.
 *
 * @since 5.4
 */
public class RouteControlPointRenderer extends ControlPointRenderer
{
    @Override
    protected Style createStyle(IControlPoint controlPoint)
    {
        ArrayList<Style> styles = new ArrayList<>();
        boolean isWaypoint = controlPoint.getControlPointType().startsWith(RouteControlPointController.CONTROL_POINT_WAYPOINT_INDEX);
        FrameFactory.ShapeType graphicShape = isWaypoint ? FrameFactory.ShapeType.Ellipse : FrameFactory.ShapeType.Rectangle;
        int size = controlPoint.getPointSize() / 4;
        String uri = getGraphicShapeUrl(graphicShape, size, size);
        int color = controlPoint.isSelected() ? controlPoint.getHighlightColor() : controlPoint.getColor();
        IconPointStyle iconPointStyle = new IconPointStyle(color, uri, size, size, 0, 0, 0, 0, 0, true);
        styles.add(iconPointStyle);
        return new CompositeStyle(styles.toArray(Style[]::new));
    }

    @Override
    protected void applyToLabel(IControlPoint controlPoint, GLLabelManager labelManager, int labelId)
    {
        super.applyToLabel(controlPoint, labelManager, labelId);
        labelManager.setTextFormat(labelId, GlobeTextUtils.DEFAULT_GLOBE_FONT.getFontName(), 14, false, false, false, false);
        labelManager.setDesiredOffset(labelId, -32, 32, 0);
        labelManager.setVerticalAlignment(labelId, VerticalAlignment.Bottom);
        labelManager.setHorizontalAlignment(labelId, HorizontalAlignment.Right);
        labelManager.setRotation(labelId, 0, false);
    }
}
