package gov.takx.core.view.gui.propertiespanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.ui.DevicePanelMetadataKeys;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.core.mapobject.gui.primarydialog.MapObjectDialogUtils;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.MapObjectHeaderPanel;
import gov.takx.core.view.gui.maptools.AnnotationPanel;
import gov.takx.core.view.gui.maptools.BullseyeEditorPanel;
import gov.takx.core.view.gui.maptools.FlatAnnotationPanel;
import gov.takx.core.view.gui.maptools.IconPropertiesEditorPanel;
import gov.takx.core.view.gui.maptools.RangeFanEditorPanel;
import gov.takx.core.view.gui.maptools.ShapePropertiesEditorPanel;
import jakarta.inject.Inject;

import javax.swing.JPanel;

import java.awt.Component;

/**
 * Only used by {@link PropertiesPanel}.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectPropertiesItem extends APropertiesItem
{
    private final IMapObjectController controller;

    private IRaptorPanel primaryPanel;
    private MapObjectHeaderPanel headerPanel;
    private IResourceCleaner editorPanel;

    @Inject
    private MapObjectDialogUtils mapObjectDialogUtils;

    public MapObjectPropertiesItem(IMapObjectController controller)
    {
        super("Map Object Properties Item");
        this.controller = controller;
    }

    @Override
    public void releaseResources()
    {
        if (primaryPanel instanceof IResourceCleaner resourceCleaner) resourceCleaner.releaseResources();
        if (null != headerPanel) headerPanel.releaseResources();
        if (null != editorPanel) editorPanel.releaseResources();
    }

    @Override
    public JPanel getPanel()
    {
        JPanel propertiesPanel = new JPanel();
        propertiesPanel.setLayout(new FormLayout(
                "fill:default:grow",
                "default, $lgap, fill:default, $lgap, fill:default"));
        CellConstraints cc = new CellConstraints();

        if (controller.getMapObject().getMetaBoolean(DevicePanelMetadataKeys.SHOW_HEADER, true))
        {
            headerPanel = new MapObjectHeaderPanel(controller);
            propertiesPanel.add(headerPanel, cc.xy(1, 1));
            if (!controller.getMapObject().getMetaBoolean(DevicePanelMetadataKeys.SHOW_RANGE_AND_BEARING, true))
            {
                headerPanel.getFromYouField().setVisible(false);
                FormLayoutUtils.hideRow(headerPanel.getFromYouField());
            }
        }

        primaryPanel = mapObjectDialogUtils.createPrimaryPanel(controller);
        propertiesPanel.add(primaryPanel.getPanel(), cc.xy(1, 5));

        IMapObject mapObject = controller.getMapObject();

        if (mapObject instanceof IMutableShape shape)
        {
            if (mapObject instanceof IShapeRangeFanMapObject rangeFan)
            {
                editorPanel = new RangeFanEditorPanel(rangeFan);
            } else if (mapObject instanceof IShapeBullseyeMapObject bullseye)
            {
                editorPanel = new BullseyeEditorPanel(bullseye);
            } else
            {
                editorPanel = new ShapePropertiesEditorPanel(shape);
            }
        } else if (mapObject instanceof IAnnotationMapObject annotation)
        {
            editorPanel = new AnnotationPanel(annotation);
        } else if (mapObject instanceof IFlatAnnotationMapObject flatAnnotation)
        {
            editorPanel = new FlatAnnotationPanel(flatAnnotation);
        } else if (mapObject instanceof IIconMapObject icon)
        {
            if (mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_ICON_OPTIONS, true))
            {
                editorPanel = new IconPropertiesEditorPanel(icon);
            }

        }

        if (editorPanel != null) propertiesPanel.add((Component) editorPanel, cc.xy(1, 3));

        return propertiesPanel;
    }

    @Override
    public String getLabelText()
    {
        return mapObjectDialogUtils.createTitleForMapObjectController(controller);
    }

    @Override
    public Object getIdentityObject()
    {
        return controller;
    }

    @Override
    public boolean isDialogOpen()
    {
        return controller.isDialogOpen();
    }

    @Override
    public boolean supportsAttachments()
    {
        return true;
    }
}
