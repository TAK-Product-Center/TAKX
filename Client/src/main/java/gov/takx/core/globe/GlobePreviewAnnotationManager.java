package gov.takx.core.globe;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.mapobject.IPreviewProvider;
import gov.takx.commons.file.FileUtils;
import gov.takx.platform.utils.WWUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Manager for handling icon preview annotations.
 *
 */
class GlobePreviewAnnotationManager implements SelectListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final WorldWindow wwd;
    private AnnotationLayer previewLayer = null;
    private PreviewAnnotation previewAnnotation = null;
    private IPreviewProvider previewingObject = null;
    private Object rolloverObject;

    /**
     * Constructor.
     *
     * @param wwd The WorldWindow we're working on
     */
    public GlobePreviewAnnotationManager(WorldWindow wwd)
    {
        this.wwd = wwd;
        wwd.addSelectListener(this);
    }

    /**
     * Put up an annotation object representing the previewable object, if it has a preview image available.
     *
     * @param previewable object to show annotation for
     * @return {@code true} if an annotation was created, {@code false} otherwise
     */
    private boolean showPreviewAnnotation(IPreviewProvider previewable)
    {
        if (previewable.isPreviewImageAvailable())
        {
            final Position position = WWUtils.locationToPosition(previewable.getLocation());

            logger.trace("Showing preview annotation for: {} @ {}", previewable, position);

            previewingObject = previewable;
            if (previewAnnotation != null)
            {
                previewAnnotation.reconfigureAnnotation(previewable, position);
            } else
            {
                previewAnnotation = new PreviewAnnotation(previewable, position);
                getPreviewLayer().addAnnotation(previewAnnotation);
            }
            wwd.redraw();

            return true;
        }
        return false;
    }

    /**
     * Hide any existing preview annotation being shown.
     */
    private void hidePreviewAnnotation()
    {
        if (previewAnnotation != null)
        {
            logger.trace("hiding preview annotation for: {}", previewingObject);

            getPreviewLayer().removeAnnotation(previewAnnotation);

            previewAnnotation.dispose();
            previewAnnotation = null;
            previewingObject = null;
            rolloverObject = null;
            wwd.redraw();
        }
    }

    /**
     * Determine if the given object has a preview showing for it.
     *
     * @param topObject Object to test
     * @return {@code true} if a preview is being shown for that object
     * @since 3.1
     */
    public boolean isPreviewShowingFor(Object topObject)
    {
        return topObject != null && (topObject == rolloverObject || topObject == previewAnnotation);
    }

    /**
     * Show/hide the preview annotation when the mouse rolls on to/off of a previewable object.
     */
    @Override
    public void selected(SelectEvent event)
    {
        if (event.isRollover())
        {
            final Object topObject = event.getTopObject();
            if (isPreviewShowingFor(topObject)) return;
            hidePreviewAnnotation();

            // If it can show a preview, then create the preview annotation
            if (topObject instanceof IGlobeMapObject globeMapObject
                    && globeMapObject.getMapObject() instanceof IPreviewProvider previewProvider
                    && showPreviewAnnotation(previewProvider))
            {
                rolloverObject = topObject;
            }
        } else if (event.isLeftClick() && event.getTopObject() == previewAnnotation)
        {
            // Launch the file associated with this preview.
            if (previewingObject != null && previewingObject.isPreviewImageAvailable()) FileUtils.launchFile(previewingObject.getExternalFile());
        }
    }

    /**
     * Dispose of all resources.
     * @since 3.1
     */
    public void dispose()
    {
        hidePreviewAnnotation();    // Ensure that we have nothing showing
        disposePreviewLayer();
        wwd.removeSelectListener(this);
    }

    /**
     * Lazy instantiation of preview layer.
     *
     * @return get layer to hold annotation
     */
    private synchronized AnnotationLayer getPreviewLayer()
    {
        if (previewLayer == null)
        {
            previewLayer = new AnnotationLayer();
            wwd.getModel().getLayers().add(previewLayer);
        }
        return previewLayer;
    }

    /**
     * Dispose of the rendering layer and clear our reference.  Used during shutdown to free resources.
     */
    private synchronized void disposePreviewLayer()
    {
        if (previewLayer == null) return;
        previewLayer.dispose();
        previewLayer = null;
    }
}
