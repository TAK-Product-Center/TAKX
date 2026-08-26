package gov.takx.core.view.gui.pointdropper;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.icons.IUserIcon;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.core.view.gui.pointdropper.PointDropperPanel.PointDropperSelection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.JToggleButton;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

/**
 * 3D vehicle model selection panel showing vehicle model map icons.
 * 
 * @since 5.8
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class VehicleModelSelectionPanel extends DefaultIconSelectionPanel
{
    @Inject
    private IIconSetManager iconSetManager;

    private Color color = Color.WHITE;
    private boolean outlineOnly;

    /**
     * @param selectionConsumer Consumer to handle the user making an icon selection
     */
    VehicleModelSelectionPanel(@Nonnull Consumer<PointDropperSelection> selectionConsumer)
    {
        super(selectionConsumer);
    }

    @Override
    public void setIcons(@Nonnull List<IUserIcon> userIcons, @Nullable Color color)
    {
        this.color = color;
        super.setIcons(userIcons, color);
    }

    /**
     * @param outlineOnly Pass {@code true} if the model should be rendered as an outline
     */
    public void setOutlineOnly(boolean outlineOnly)
    {
        this.outlineOnly = outlineOnly;
    }

    @Override
    protected void toggleButtonActionPerformed(@Nonnull JToggleButton button, @Nonnull IUserIcon userIcon)
    {
        final ISyntheticPluginDefinition selectedIconDefinition = button.isSelected()
                ? iconSetManager.getOrCreatePluginDefinition(userIcon)
                : null;
        AttributeSet attributeSet = new AttributeSet();
        attributeSet.setAttribute(StandardAttributeNames.MODEL_NAME, userIcon.getFileName());
        attributeSet.setAttribute(StandardAttributeNames.MODEL_GROUP, userIcon.getGroup());
        attributeSet.setAttribute(StandardAttributeNames.MODEL_COLOR, color.getRGB());
        attributeSet.setAttribute(StandardAttributeNames.MODEL_OUTLINE_ENABLED, outlineOnly);
        applySelection(new PointDropperSelection(selectedIconDefinition, CotTypes.VEHICLE_MODEL, null, attributeSet, null));
    }
}
