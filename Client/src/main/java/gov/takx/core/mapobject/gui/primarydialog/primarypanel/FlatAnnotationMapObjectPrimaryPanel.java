package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IMapObjectController;

/**
 */
public class FlatAnnotationMapObjectPrimaryPanel extends AMapObjectPanel
{
    public FlatAnnotationMapObjectPrimaryPanel(IMapObjectController controller)
    {
        super(controller);
        initComponents();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default",
                "2*(default, $lgap), default"));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
