package gov.takx.core.view.gui.pointdropper;

import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.icons.IUserIcon;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.view.gui.pointdropper.PointDropperPanel.PointDropperSelection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

/**
 * Default icon selection panel which displays all the icons available for selection as a wrapped list of toggle buttons.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DefaultIconSelectionPanel extends APointDropperIconSelectionPanel
{
    @Inject
    private IIconSetManager iconSetManager;

    /**
     * @param selectionConsumer Consumer to handle the user making an icon selection
     * @since 5.2
     */
    DefaultIconSelectionPanel(@Nonnull Consumer<PointDropperSelection> selectionConsumer)
    {
        super(selectionConsumer);
    }

    /**
     * @param userIcons Icons to add to this panel, any previous icons are cleared
     * @since 5.2
     */
    public void setIcons(@Nonnull List<IUserIcon> userIcons)
    {
        setIcons(userIcons, null);
    }

    /**
     * @param userIcons Icons to add to this panel, any previous icons are cleared
     * @param color Optional color to colorize the icons with. Can be {@code null} for no visual filtering
     * @since 5.3
     */
    public void setIcons(@Nonnull List<IUserIcon> userIcons, @Nullable Color color)
    {
        if (iconButtonGroup.getSelection() != null) applyEmptySelection();

        iconButtonGroup.removeAll();
        removeAll();

        // Create buttons from each user icon
        for (var userIcon : userIcons)
        {
            final var button = new JToggleButton();
            button.setFocusable(false);
            button.setName(userIcon.getFileName());
            BufferedImage image = iconSetManager.getIconImage(userIcon);
            if (color != null && color != Color.WHITE) image = GraphicsUtils.colorImage(image, color);
            button.setIcon(new ImageIcon(image));
            button.addActionListener(e -> toggleButtonActionPerformed(button, userIcon));
            button.setToolTipText(FileUtils.getNameWithoutExtension(userIcon.getFileName()));

            addButtonToGroup(button);
            add(button);
        }

        revalidate();
        repaint();
    }

    /**
     * @param button   The button on which this action was invoked
     * @param userIcon User icon represented by the given button
     * @since 5.1 (signature change in 5.2)
     */
    protected void toggleButtonActionPerformed(@Nonnull JToggleButton button, @Nonnull IUserIcon userIcon)
    {
        final ISyntheticPluginDefinition selectedIconDefinition = button.isSelected()
                ? iconSetManager.getOrCreatePluginDefinition(userIcon)
                : null;

        final String initialCotType = userIcon.get2525cType();

        // If we have no CoT type associated with the selected icon, use a default, otherwise WinTAK won't
        // process the marker and won't show it on the map when it is broadcast.
        final String cotType = StringUtils.isEmpty(initialCotType) ? CotIcon.UnknownGround.getCotType() : initialCotType;
        applySelection(new PointDropperSelection(selectedIconDefinition, cotType, null));
    }
}
