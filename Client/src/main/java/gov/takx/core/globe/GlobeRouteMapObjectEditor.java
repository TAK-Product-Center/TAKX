package gov.takx.core.globe;

import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.event.EditorDisarmedEvent;
import gov.takx.api.event.EditorFailedEvent;
import gov.takx.api.event.FinalizeShapeEvent;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.shapes.IRoute;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.dataservices.shapes.Route;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * GlobeRouteMapObjectEditor is an extension of {@link GlobeShapeMapObjectEditor} specifically designed to handle
 * {@link Route} objects.
 * <p/>
 * Implementation note: this uses an internal {@link gov.takx.core.globe.tak.editor.IShapeEditor} for the presentation and
 * handling of a user-interface based editor components.
 *
 * @since 3.3
 */
public class GlobeRouteMapObjectEditor extends GlobeShapeMapObjectEditor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private List<GeoPointMetaData> originalPoints;

    public GlobeRouteMapObjectEditor()
    {
        super(false);
    }

    @Override
    public void setGlobeMapObject(IGlobeMapObject globeMapObject, boolean creatingNewShape)
    {
        super.setGlobeMapObject(globeMapObject, creatingNewShape);

        // Store a copy of the original points, if any, in case we need to cancel when editing later
        final IRoute route = getGlobeMapObject().getShapeModel();
        final var controlPoints = route == null ? null : route.getControlPoints();
        originalPoints = controlPoints == null ? null : controlPoints.stream().map(GeoPointMetaData::new).toList();
    }

    @Override
    protected boolean applyUpdatesToModel()
    {
        if (!shapeEditor.applyChangesToSource())
        {
            disposeEditor();
            showIncompletePolyShapeDialog();
            restoreOrFail();
            return false;
        } else
        {
            uiEventBus.post(new FinalizeShapeEvent());
            return true;
        }
    }

    @Override
    protected void createEditor()
    {
        shapeEditor = createShapeEditor();
        shapeEditor.setEnabled(true);
        shapeEditor.setArmed(creatingNewShape);
    }

    @Override
    public void disarm(boolean noUpdate)
    {
        super.disarm(noUpdate);
        uiEventBus.post(new EditorDisarmedEvent());
    }

    private void restoreOrFail()
    {
        final IRoute route = getGlobeMapObject().getShapeModel();

        // Note that 'route' can be null in  a couple of situations:
        //   1. The user initiates the creation, and then hits 'cancel' in the "Routes" panel without ever clicking on the globe
        //   2. The user clicks on the globe panel, but not on the globe (in space) and then hits ESC to cancel the edit

        if (route == null || route.getShapeMapObject() == null || originalPoints == null)
        {
            // We don't have a model or original points, meaning we're creating a new route, so fire a failed event
            uiEventBus.post(new EditorFailedEvent(route == null ? null : route.getKey()));
        } else
        {
            // Restore original state on a cancel
            final IRouteMapObject routeMapObject = route.getShapeMapObject();
            routeMapObject.setPoints(originalPoints);
        }
    }

    private GlobeRouteMapObject getGlobeRouteMapObject()
    {
        return (globeMapObject instanceof GlobeRouteMapObject globeRouteMapObject) ? globeRouteMapObject : null;
    }

    /**
     * Allows the currently selected object to be edited as if it were new, which means adding new points to the route.
     */
    public void enableEditing()
    {
        IRoute route = getRouteModel();
        if (route.isEditable())
        {
            creatingNewShape = true;
            arm();
        } else
        {
            final String message = "Unable to edit route " + route.getName() + ", because it is currently not editable.";
            logger.warn(message);
            AlwaysOnTopJOptionPane.showErrorDialog(message, "Invalid Shape");
        }
    }

    /**
     * Cancel creation of the route currently in progress
     */
    public void cancelCreation()
    {
        if (isArmed())
        {
            disposeEditor();
            restoreOrFail();
            uiEventBus.post(new EditorDisarmedEvent());
        }
    }

    @Nonnull
    private IRoute getRouteModel()
    {
        return getGlobeMapObject().requireShapeModel();
    }
}
