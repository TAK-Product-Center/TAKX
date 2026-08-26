package gov.takx.core.globe.tak.ww.layer;

import com.atakmap.map.layer.feature.geometry.Point;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.GraticuleSupport;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GeographicText;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.tak.RenderableManager;
import gov.takx.platform.globe.GeometryAnnotation;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Implementation of {@link GraticuleSupport} that redirects text rendering to {@link RenderableManager}.
 *
 * @since 5.4
 */
@PseudoBean
public class TakGraticuleSupport extends GraticuleSupport
{
    @Inject
    private RenderableManager renderableManager;

    private static final java.awt.Point ZERO_POINT = new java.awt.Point();

    private final List<GeometryAnnotation> annotationPool = new ArrayList<>();

    @Override
    protected void renderText(DrawContext dc, Collection<GeographicText> text)
    {
        Queue<GeometryAnnotation> annotationQueue = new LinkedList<>(this.annotationPool);
        for (GeographicText geographicText : text)
        {
            Position position = geographicText.getPosition();
            GeometryAnnotation annotation = annotationQueue.poll();
            if (annotation == null)
            {
                annotation = new GeometryAnnotation(new Point(position.longitude.degrees, position.latitude.degrees));
                AnnotationAttributes attributes = annotation.getAttributes();
                attributes.setDrawOffset(ZERO_POINT);
                attributes.setFrameShape(AVKey.SHAPE_NONE);
                annotationPool.add(annotation);
            } else
            {
                annotation.setGeometry(new Point(position.longitude.degrees, position.latitude.degrees));
            }
            annotation.setText(geographicText.getText().toString());
            AnnotationAttributes attr = annotation.getAttributes();
            attr.setTextColor(geographicText.getColor());
            attr.setBackgroundColor(geographicText.getBackgroundColor());
            attr.setFont(geographicText.getFont());
            renderableManager.offerRenderable(annotation, dc);
        }
    }
}
