package gov.takx.core.mapobject.gui.primarydialog;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.AnnotationMapObjectPrimaryPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.DeviceMapObjectPrimaryPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.FileMapObjectPrimaryPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.FlatAnnotationMapObjectPrimaryPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.MarkerMapObjectPrimaryPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.MultiPolylineMapObjectPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.RouteMapObjectPrimaryPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.ShapeMapObjectPrimaryPanel;
import gov.takx.dataservices.persist.AnnotationMapObject;
import gov.takx.dataservices.persist.FileMapObject;
import gov.takx.dataservices.persist.FlatAnnotationMapObject;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.dataservices.persist.MultiPolylineMapObject;
import gov.takx.dataservices.persist.RouteMapObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

/**
 */
@ApplicationScoped
public class MapObjectDialogUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    MapObjectDialogUtils()
    {
    }

    public IMapObjectPrimaryDialog<?> createPrimaryDialog(IMapObjectController controller)
    {
        final IMapObject mo = controller.getMapObject();
        String multiPointLocationLabel = "Start Loc:";

        if (mo instanceof FileMapObject)
        {
            return new MapObjectPrimaryDialog<>(controller, new FileMapObjectPrimaryPanel(controller));
        } else if (mo instanceof IconMapObject)
        {
            return new MapObjectPrimaryDialog<>(controller, new DeviceMapObjectPrimaryPanel(controller, true));
        } else if (mo instanceof MarkerMapObject)
        {
            return new MapObjectPrimaryDialog<>(controller, new MarkerMapObjectPrimaryPanel(controller));
        } else if (mo.isDevice())
        {
            return new MapObjectPrimaryDialog<>(controller, new DeviceMapObjectPrimaryPanel(controller, true));
        } else if (mo instanceof MultiPolylineMapObject)
        {
            var mapObjectPrimaryDialog = new MapObjectPrimaryDialog<>(controller, new MultiPolylineMapObjectPanel(controller));
            mapObjectPrimaryDialog.setLocationLabelText(multiPointLocationLabel);
            return mapObjectPrimaryDialog;
        } else if (mo instanceof AnnotationMapObject)
        {
            return new MapObjectPrimaryDialog<>(controller, new AnnotationMapObjectPrimaryPanel(controller));
        } else if (mo instanceof FlatAnnotationMapObject)
        {
            return new MapObjectPrimaryDialog<>(controller, new FlatAnnotationMapObjectPrimaryPanel(controller));
        } else if (mo instanceof RouteMapObject)
        {
            var mapObjectPrimaryDialog = new MapObjectPrimaryDialog<>(controller, new RouteMapObjectPrimaryPanel(controller));
            mapObjectPrimaryDialog.setLocationLabelText(multiPointLocationLabel);
            return mapObjectPrimaryDialog;
        } else if (mo instanceof IShapeMapObject)
        {
            return new MapObjectPrimaryDialog<>(controller, new ShapeMapObjectPrimaryPanel(controller));
        } else
        {
            logger.error("Unexpected map object type while trying to create map object primary dialog. Map Object: {}", mo);
            throw new IllegalArgumentException("Unsupported MapObject type: " + mo.getClass().getSimpleName());
        }
    }

    public IRaptorPanel createPrimaryPanel(IMapObjectController controller)
    {
        IMapObject mo = controller.getMapObject();
        if (controller.getMapObject().isDevice())
        {
            return new DeviceMapObjectPrimaryPanel(controller);
        } else if (mo instanceof AnnotationMapObject)
        {
            return new AnnotationMapObjectPrimaryPanel(controller);
        } else if (mo instanceof FlatAnnotationMapObject)
        {
            return new FlatAnnotationMapObjectPrimaryPanel(controller);
        } else if (mo instanceof FileMapObject)
        {
            return new FileMapObjectPrimaryPanel(controller);
        } else if (mo instanceof IconMapObject)
        {
            return new DeviceMapObjectPrimaryPanel(controller);
        } else if (mo instanceof RouteMapObject)
        {
            return new RouteMapObjectPrimaryPanel(controller);
        } else if (mo instanceof IShapeMapObject)
        {
            return new ShapeMapObjectPrimaryPanel(controller);
        } else if (mo instanceof MarkerMapObject)
        {
            return new MarkerMapObjectPrimaryPanel(controller);
        } else if (mo instanceof MultiPolylineMapObject)
        {
            return new MultiPolylineMapObjectPanel(controller);
        } else
        {
            logger.error("Unexpected map object type while trying to create map object primary dialog. Map Object: {}", mo);
            throw new IllegalArgumentException("Unsupported MapObject type: " + mo.getClass().getSimpleName());
        }
    }

    /**
     * Create a title for a map object. type: id, type: id (display name), display name, or type (display name)
     *
     * @param controller controller to create a title for
     * @return title
     */
    public String createTitleForMapObjectController(IMapObjectController controller)
    {
        IMapObject mo = controller.getMapObject();
        StringBuilder title = new StringBuilder();

        String type = mo.getType();
        String id = mo.getUnitID();
        String name = mo.getName();

        // for devices, title will be type: id (display name) unless the display name is just the type and unit id, in which
        //case it will just be type:id. for any other type of map object it will be type (display name)
        if (mo.isDevice())
        {
            if (name.equals(type + " " + id))
            {
                title.append(type).append(": ").append(id);
            } else
            {
                try
                {
                    // If the unit id is parsable as a UUID, do not display it in the title
                    //noinspection ResultOfMethodCallIgnored
                    UUID.fromString(id);
                    title.append(type).append(": ").append(name);
                } catch (Exception e)
                {
                    // The unit id was not parsable as a UUID - so display it in the title
                    title.append(type).append(": ").append(id).append(" (").append(name).append(")");
                }
            }
        } else
        {
            // I'm so glad this bizarre logic is appropriately commented with examples as to why it's here....
            if (name.contains(type.subSequence(0, type.length() - 1)))
            {
                title.append(name);
            } else
            {
                title.append(type).append(" (").append(name).append(")");
            }
        }
        return title.toString();
    }
}
