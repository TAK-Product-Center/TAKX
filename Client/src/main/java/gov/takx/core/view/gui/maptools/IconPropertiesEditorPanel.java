package gov.takx.core.view.gui.maptools;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.core.mapobject.gui.MapObjectVisualAttributesPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;

import java.beans.PropertyChangeEvent;

/**
 * Displays properties for {@link IIconMapObject} items.
 *
 * @since 5.1
 */
@PseudoBean
public class IconPropertiesEditorPanel extends APropertiesEditorPanel<IIconMapObject> implements IPanelStateListener
{
    private final MapObjectVisualAttributesPanel visualAttributesPanel;

    /**
     * Constructor used when this panel is in the right-side properties panel to edit an existing shape.
     */
    public IconPropertiesEditorPanel(@Nonnull IIconMapObject icon)
    {
        this(icon, true, null);
    }

    /**
     * Constructor used when this panel is in the right-side properties panel for initial creation of a shape.
     */
    public IconPropertiesEditorPanel(@Nonnull IIconMapObject icon, boolean directActionMode, @Nullable IPanelStateListener panelListener)
    {
        super("Icon", icon, directActionMode, panelListener);

        initComponents();

        // Currently the only properties we need to display are visual attributes, so we can delegate the work
        // to another panel. The constructor will call back to the listener (passed as this) which requires
        // us to null-check usage of the panel in the method declarations in this class below.
        visualAttributesPanel = new MapObjectVisualAttributesPanel(icon.getController(), this);
        add(visualAttributesPanel, new CellConstraints(1, 1));
    }

    @PostConstruct
    private void postConstruct()
    {
        registerForEvents();
    }

    @Override
    public void setPanelDirty()
    {
        super.setPanelDirty();

        if (directActionMode) updateTargetProperties();
    }

    @Override
    public void setPanelClean()
    {
        super.setPanelClean();
    }

    @Override
    protected void updateTargetProperties()
    {
        if (visualAttributesPanel != null) visualAttributesPanel.applyAction();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (visualAttributesPanel != null) visualAttributesPanel.propertyChange(evt);
    }

    @Override
    public void releaseResources()
    {
        super.releaseResources();

        if (visualAttributesPanel != null) visualAttributesPanel.releaseResources();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
