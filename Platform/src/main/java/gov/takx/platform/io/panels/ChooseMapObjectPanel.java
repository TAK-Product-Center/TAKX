package gov.takx.platform.io.panels;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.commons.wizard.IWizardModel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.util.Comparator;

/**
 */
@PseudoBean
public class ChooseMapObjectPanel extends JPanel
{
    private final DefaultListModel<MapObjectWrapper> model;
    private final IWizardModel wizardModel;

    @Inject
    private IMapObjectManager mapObjectManager;

    /**
     * Constructor.
     *
     * @param wizardModel Model for storing wizard data
     * @since 3.0 (delegate parameter removed)
     */
    public ChooseMapObjectPanel(IWizardModel wizardModel)
    {
        this.wizardModel = wizardModel;

        initComponents();

        model = new DefaultListModel<>();
    }

    @PostConstruct
    private void postConstruct()
    {
        mapObjectManager.getMapObjectControllers().stream()
                .map(IMapObjectController::getMapObject)
                .sorted(Comparator.comparing(IMapObject::getName))
                .forEach(mo -> model.addElement(new MapObjectWrapper(mo)));

        mapObjectList.setModel(model);
        mapObjectList.setSelectedIndex(0);
    }

    public void updateModel()
    {
        MapObjectWrapper wrapper = mapObjectList.getSelectedValue();

        if (wrapper != null)
        {
            IMapObject mapObject = wrapper.mapObject;
            wizardModel.setProperty(CommonWizardSettings.MAP_OBJECT_FAMILY, mapObject.getFamily());
            wizardModel.setProperty(CommonWizardSettings.MAP_OBJECT_TYPE, mapObject.getType());
            wizardModel.setProperty(CommonWizardSettings.UNIT_ID, mapObject.getUnitID());
        }
    }

    /**
     * Manually select a map object from the list.
     *
     * @param family The map object's family.
     * @param type   The map object's type.
     * @param unitId The map object's unit id.
     * @return {@code true} iff the map object was found and selected.
     */
    public boolean selectMapObject(String family, String type, String unitId)
    {
        for (int i = 0; i < model.size(); i++)
        {
            IMapObject mapObject = model.elementAt(i).mapObject;

            if (mapObject.getFamily().equals(family)
                    && mapObject.getType().equals(type)
                    && mapObject.getUnitID().equals(unitId))
            {
                mapObjectList.setSelectedIndex(i);
                return true;
            }
        }

        return false;
    }

    private static class MapObjectWrapper
    {
        public IMapObject mapObject;

        public MapObjectWrapper(IMapObject mapObject)
        {
            this.mapObject = mapObject;
        }

        public String toString()
        {
            return mapObject.getName();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JLabel label1 = new JLabel();
        JScrollPane scrollPane1 = new JScrollPane();
        mapObjectList = new JList<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setLayout(new FormLayout(
                "default:grow",
                "default, $rgap, fill:default:grow"));

        //---- label1 ----
        label1.setText("Select a map object to import data into:");
        add(label1, cc.xy(1, 1));

        //======== scrollPane1 ========
        {

            //---- mapObjectList ----
            mapObjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            scrollPane1.setViewportView(mapObjectList);
        }
        add(scrollPane1, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JList<MapObjectWrapper> mapObjectList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
