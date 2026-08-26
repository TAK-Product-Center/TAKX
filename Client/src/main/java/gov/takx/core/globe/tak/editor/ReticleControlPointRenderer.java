package gov.takx.core.globe.tak.editor;

import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.opengl.GLLabelManager;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.platform.ui.UIResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * An implementation of {@link IShapeEditorRenderer} that renders a small reticle control point when not dragging and a large reticle when dragging using the TAK feature renderer.
 *
 * @since 5.6
 */
public class ReticleControlPointRenderer extends ControlPointRenderer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static String reticleUrl;

    @Override
    protected Style createStyle(IControlPoint controlPoint)
    {
        ArrayList<Style> styles = new ArrayList<>();
        int size = controlPoint.isDragging() ? 80 : 48;
        String uri = FeatureUtility.sanitizeUri(controlPoint.isDragging() ? getReticleUrl() : UIResources.getUrl(PLATFORM_ICONS, "open_circle_small.png").toString());
        IconPointStyle iconPointStyle = new IconPointStyle(0x80FFFFFF, uri, size, size, 0, 0, 0, 0, 0, true);
        styles.add(iconPointStyle);
        if (IControlPoint.CONTROL_POINT_ORIENTATION.equals(controlPoint.getControlPointType()))
        {
            styles.add(new BasicStrokeStyle(0xAAFFFFFF, 6));
        }
        return new CompositeStyle(styles.toArray(Style[]::new));
    }

    @Override
    protected void applyToLabel(IControlPoint controlPoint, GLLabelManager labelManager, int labelId)
    {
        super.applyToLabel(controlPoint, labelManager, labelId);
        labelManager.setVisible(labelId, !controlPoint.isDragging());
    }

    private synchronized String getReticleUrl()
    {
        if (reticleUrl == null)
        {
            BufferedImage image;
            try
            {
                InputStream inputStream = UIResources.getUrl(PLATFORM_ICONS, "large_reticle_red.svg").openStream();
                image = UIResources.getSvg(null, inputStream, 80, 80, BufferedImage.TYPE_INT_ARGB, true);
            } catch (IOException e)
            {
                logger.error("Could not load reticle SVG.", e);
                image = ImageUtils.getDefaultImage();
            }
            reticleUrl = getBufferedImageUrl(image);
        }
        return reticleUrl;
    }
}
