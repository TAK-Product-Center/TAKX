package gov.takx.core.globe.layers;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.SurfaceImageLayerResizeEvent;
import gov.takx.api.event.SurfaceImageLayerResizeEvent.EventType;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.ui.event.GlobeCreatedEvent;
import gov.takx.core.globe.tak.editor.ControlPointRenderer;
import gov.takx.core.globe.tak.editor.CornerLockedControlPointController;
import gov.takx.core.globe.tak.editor.IControlPoint;
import gov.takx.core.globe.tak.editor.IShapeEditor;
import gov.takx.core.globe.tak.editor.IShapeEditorSource;
import gov.takx.core.globe.tak.editor.ShapeEditor;
import gov.takx.core.globe.tak.editor.SurfaceImageEditorSource;
import gov.takx.core.view.gui.layers.GeoRectifyDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Selection listener to handle editing surface imagery layers.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SurfaceImageController extends MouseAdapter implements SelectListener
{
    private final SurfaceImage image;
    private final SurfaceImageLayer layer;
    private IShapeEditor editor; // Active editor if non-null

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IRaptorLayerManager raptorLayerManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private EventBus eventBus;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    public SurfaceImageController(SurfaceImageLayer layer, SurfaceImage image)
    {
        if (image == null) throw new IllegalArgumentException("image is null");
        if (layer == null) throw new IllegalArgumentException("layer is null");

        this.image = image;
        this.layer = layer;
    }

    @PostConstruct
    private void postConstruct()
    {
        // Now we need to register as a mouse and select listener on all globes
        for (IApplicationGlobe globe : globeManager.getAllGlobeDelegates())
        {
            globe.getWorldWindow().addSelectListener(this);
            globe.addMouseListener(this);
        }

        // This class only listens for events on the UI event bus
        uiEventBus.register(this);
    }

    /**
     * Dispose of this controller, free all resources.
     */
    public void dispose()
    {
        uiEventBus.unregister(this);
        if (editor != null) editor.dispose();
        for (IApplicationGlobe globe : globeManager.getAllGlobeDelegates())
        {
            globe.getWorldWindow().removeSelectListener(this);
            globe.removeMouseListener(this);
        }
    }

    /**
     * Handle the creation of a new globe.  Add a mouse and select listeners.
     *
     * @param e The globe creation event
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onGlobeCreated(GlobeCreatedEvent e)
    {
        final IApplicationGlobe globe = e.getGlobe();

        globe.getWorldWindow().addSelectListener(this);
        globe.addMouseListener(this);
    }

    /**
     * Set the corners on the image to the given list of locations.  Ensure that the editor's control points are also
     * updated.
     *
     * @param corners New locations to install
     */
    public void setCorners(List<LatLon> corners)
    {
        image.setCorners(corners);
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent)
    {
        final WorldWindow wwd = (WorldWindow) mouseEvent.getSource();

        // If terrain is the only objects at current position, disarm the editor
        if (wwd.getObjectsAtCurrentPosition() != null && wwd.getObjectsAtCurrentPosition().size() < 2)
        {
            setArmed(false);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        final WorldWindow wwd = (WorldWindow) e.getSource();
        if (e.getButton() == MouseEvent.BUTTON1
                && e.getClickCount() == 2
                && wwd.getObjectsAtCurrentPosition() != null
                && wwd.getObjectsAtCurrentPosition().getTopObject() == image)
        {
            GeoRectifyDialog.showGeoRectifyDialog(layer.getRaptorLayer());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // This will catch when the user drags the image to a new location.
        // The seemingly more appropriate "mouseDragged" event isn't fired.
        if (editor != null && editor.isArmed()) layer.updateSector();
    }

    @Override
    public void selected(SelectEvent e)
    {
        PickedObject topObject = e.getTopPickedObject();

        if (e.getEventAction().equals(SelectEvent.LEFT_PRESS) && !topObject.isTerrain() && (topObject.getObject() != null))
        {
            // If the selected object is our image, arm the editor.
            // If it is not our image, and not a Marker object, disarm the editor. Markers are considered an
            // exception so that selecting one of the image's own Marker control points won't deselect the image.
            // These means that clicking on a track point or other Marker object will not disarm the editor.
            if (topObject.getObject() == image)
            {
                if (editor == null) editor = new ShapeEditor(new SurfaceImageEditorSource(image), new CornerLockedControlPointController(), new ControlPointRenderer());
                editor.getShapeEditorSource().addShapeEditorSourceListener(new IShapeEditorSource.ShapeEditorSourceListener()
                {
                    @Override
                    public <T> void onSourceUpdated(T source)
                    {
                        var event = new SurfaceImageLayerResizeEvent(SurfaceImageController.this, EventType.Resized, image.getCorners());
                        eventBus.post(event);
                        layer.updateSector();
                    }
                });

                setArmed(true);
                mapObjectManager.setSelected(null); // Clear any Map Object selection (disabling any active shape editors)
                raptorLayerManager.selectLayer(layer.getRaptorLayer());
            } else if (!(topObject.getObject() instanceof IControlPoint))
            {
                setArmed(false);
                raptorLayerManager.selectLayer(null);
            }
        }
    }

    void setArmed(boolean armed)
    {
        // Notify listeners of arm/disarm
        final SurfaceImageLayerResizeEvent event = new SurfaceImageLayerResizeEvent(this,
                armed ? EventType.Armed : EventType.Disarmed, image.getCorners());

        eventBus.post(event);

        if (editor != null)
        {
            editor.setEnabled(armed);
            image.setPickEnabled(!armed);
        }
        if (!armed) editor = null;
    }
}
