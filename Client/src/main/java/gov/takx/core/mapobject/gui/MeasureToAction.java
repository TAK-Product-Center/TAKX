package gov.takx.core.mapobject.gui;

import gov.tak.core.map.layer.IPickedObjectManager;
import gov.tak.core.map.layer.PickedObject;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.ShapeType;
import gov.takx.core.globe.GlobeClickMouseListener;
import gov.takx.core.mapobject.MapObjectManager;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Measures distance from selected map object to the point clicked.
 *
 * @since 5.1.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MeasureToAction extends AbstractAction
{
    private static final String MEASURE_TO_GLOBE_MESSAGE = "Click the point to measure range to.";

    @Inject
    private IGlobeManager globeManager;
    @Inject
    private MapObjectManager mapObjectManager;
    @Inject
    private IPickedObjectManager pickedObjectManager;

    private GlobeClickMouseListener globeClickMouseListener;
    private final IMapObject anchorMapObject;

    /**
     * Sets anchor of the to-be-drawn arrow, and the name of the action.
     *
     * @param name Name of the action.
     * @param anchorMapObject The map object to act as the base of the arrow.
     */
    public MeasureToAction(@Nonnull String name, @Nonnull IMapObject anchorMapObject)
    {
        super(name);
        this.anchorMapObject = anchorMapObject;
    }

    /**
     * Starts the listener for clicking the end point of the arrow.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        globeManager.showGlobeMessage(MEASURE_TO_GLOBE_MESSAGE);

        globeClickMouseListener = new GlobeClickMouseListener(this::mousePressed);
        globeManager.getAllGlobeDelegates().forEach(globeDelegate -> globeDelegate.getWorldWindow().getInputHandler().addMouseListener(globeClickMouseListener));
        globeClickMouseListener.setArmed(true);
    }

    /**
     * Creates an arrow from the selected map object to the location of the mouse press.
     *
     * @param location Location of the mouse press.
     */
    private void mousePressed(ILocation location)
    {
        final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, ShapeType.ARROW.getShapeType())
                .initializer(mapObject -> {
                    final var arrow = (IShapeArrowMapObject) mapObject;
                    arrow.setAllowDeviceToBeContainer(false);
                    arrow.setStartAnchor(anchorMapObject);

                    pickedObjectManager.getPickedObjectList().stream()
                            .map(PickedObject::userObject)
                            .filter(IGlobeMapObject.class::isInstance)
                            .map(IGlobeMapObject.class::cast)
                            .findFirst()
                            .ifPresentOrElse(globeMapObject -> arrow.setRangeAnchor(globeMapObject.getMapObject()),
                                    () -> arrow.setVertices(List.of(anchorMapObject.getLocation(), location)));
                }).build();
        mapObjectManager.getOrCreateMapObject(spec);

        globeClickMouseListener.setArmed(false);
        globeManager.getAllGlobeDelegates().forEach(globeDelegate -> globeDelegate.getWorldWindow().getInputHandler().removeMouseListener(globeClickMouseListener));
        globeManager.hideGlobeMessage();
    }
}
