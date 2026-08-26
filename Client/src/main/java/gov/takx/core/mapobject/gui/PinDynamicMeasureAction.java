package gov.takx.core.mapobject.gui;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.shapes.ShapeService;
import gov.takx.dataservices.persist.ShapeArrowMapObject;
import gov.takx.dataservices.shapes.Arrow;
import gov.takx.platform.utils.UnitIdHelper;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.lang.invoke.MethodHandles;

/**
 * Action pins a dynamic measure to the globe, that is, it creates a static arrow with the same range and bearing.
 *
 * @since 5.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PinDynamicMeasureAction extends AbstractAction
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private ShapeService shapeService;
    @Inject
    private UnitIdHelper unitIdHelper;

    private final IMapObject mapObject;

    public PinDynamicMeasureAction(IMapObject mapObject)
    {
        this.mapObject = mapObject;
    }

    /**
     * Creates a name based on the name of the dynamic measure being pinned.
     * @return Unique name
     */
    private String getUniqueName()
    {
        return mapObject.getName() + " " + unitIdHelper.getNextFriendlyUnitId(mapObject.getFamily(), mapObject.getType());
    }

    /**
     * Pins the dynamic measure to the globe, that is, it creates a static arrow with the same range and bearing.
     *
     * @param e the event to be processed
     * @since 5.0
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        ShapeArrowMapObject dynamicMeasure = (ShapeArrowMapObject) mapObject;
        try
        {
            shapeService.createArrowMapObject(new Arrow(getUniqueName(), dynamicMeasure.getLocation(), dynamicMeasure.getRange(), dynamicMeasure.getBearing()));
        } catch (ShapeAlreadyExistsException ex)
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Cannot draw dynamic measure: There is already one on the map.", "Error");
            logger.error("Arrow already exists: ", ex);
        }
    }
}
