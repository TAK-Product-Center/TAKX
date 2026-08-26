package gov.takx.core.view.gui.pointdropper;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.icons.IconSetConstants;
import gov.tak.api.icons.TakColor;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.pointdropper.PointDropperConfiguration.CountingNameGenerator;
import gov.takx.api.pointdropper.PointDropperConfiguration.INameGenerator;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.core.plugin.synthetic.SyntheticPluginDefinition;
import gov.takx.core.view.gui.pointdropper.PointDropperPanel.PointDropperSelection;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import static gov.takx.api.mapobject.MapObjectOptions.STANDARD_RESOURCE_OPTIONS;
import static gov.takx.commons.swing.ImageUtils.FEATURE_ICON_WIDTH;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Icon selection panel showing spot map icons.  Spot map icons are just colored circles, with an additional option for
 * a "label only" icon which shows its name on the globe, but not a colored icon.
 * 
 * @since 5.3
 */
public class SpotMapIconSelectionPanel extends APointDropperIconSelectionPanel
{
    // The reference point (spot) icon is 32x32, but the actual spot is a small portion of that.  So, we need to
    // resize the icon up to "feature" size.  For the label icon, it is also 32x32, but the image fills the icon, so we
    // don't need to resize it to make all the buttons have a consistent image size.
    private static final BufferedImage SPOT_IMAGE = UIResources.getFeatureSizedImage(TAK_ASSETS_ICONS, "reference_point.png");
    private static final BufferedImage LABEL_IMAGE = UIResources.getImage(ResourceDirectory.PLATFORM_ICONS, "spot_map_label.png");
    private static final Dimension BUTTON_SIZE = new Dimension(FEATURE_ICON_WIDTH + 2, FEATURE_ICON_WIDTH + 2);

    private static final List<SpotDefinition> SPOT_DEFINITIONS = List.of(
            new SpotDefinition(TakColor.WHITE, "W"),
            new SpotDefinition(TakColor.YELLOW, "Y"),
            new SpotDefinition(TakColor.ORANGE, "O"),
            new SpotDefinition(TakColor.BROWN, "Br"),
            new SpotDefinition(TakColor.RED, "R"),
            new SpotDefinition(TakColor.MAGENTA, "P"), // 'P', not 'M', for parity with ATAK and WinTAK
            new SpotDefinition(TakColor.BLUE, "B"),
            new SpotDefinition(TakColor.CYAN, "Cy"),
            new SpotDefinition(TakColor.GREEN, "G"),
            new SpotDefinition(TakColor.GRAY, "Gry"),
            new SpotDefinition(TakColor.BLACK, "Blk"),
            new SpotDefinition(TakColor.WHITE, IconSetConstants.LABEL, new ImageIcon(LABEL_IMAGE), "L"));

    private static final SyntheticPluginDefinition PLUGIN_DEFINITION =
            new SyntheticPluginDefinition(KeyUtils.RESOURCE_ICON_FAMILY + "_SPOT", "Spot Map", "Spot Map", null, null, STANDARD_RESOURCE_OPTIONS);

    /**
     * @param selectionConsumer Consumer to handle the user making an icon selection
     */
    SpotMapIconSelectionPanel(@Nonnull Consumer<PointDropperSelection> selectionConsumer)
    {
        super(selectionConsumer);

        // Create all the buttons for this panel.  One for each color in our palette plus one for "label only".
        for (var spot : SPOT_DEFINITIONS)
        {
            final var button = new SpotIconToggleButton(spot);

            button.addActionListener(e -> {
                final var definition = button.isSelected() ? PLUGIN_DEFINITION : null;
                applySelection(new PointDropperSelection(definition, CotTypes.SPOT_MARKER, null, button.attributeSet, button.nameGenerator));
            });

            addButtonToGroup(button);
            add(button);
        }

        revalidate();
        repaint();
    }

    /**
     * Toggle button extension to hold the attributes and name generator for dropped points.
     */
    private static class SpotIconToggleButton extends JToggleButton
    {
        private final AttributeSet attributeSet = new AttributeSet();
        private final INameGenerator nameGenerator;

        private SpotIconToggleButton(@Nonnull SpotDefinition spot)
        {
            final String iconName = spot.iconName;

            setName(iconName);
            setToolTipText(iconName);
            setIcon(spot.icon);
            setIconTextGap(0);
            setPreferredSize(BUTTON_SIZE);

            // Construct the iconset path and color attributes.
            // Path format is:
            //    "COT_MAPPING_SPOTMAP/b-m-p-s-m/<int color value>" for a color spot, or
            //    "COT_MAPPING_SPOTMAP/b-m-p-s-m/LABEL" for a label
            //
            final int colorValue = spot.takColor.colorValue;
            final String suffix = IconSetConstants.LABEL.equals(iconName) ? iconName : Integer.toString(colorValue);
            final String path = IconSetConstants.COT_MAPPING_SPOTMAP + "/" + CotTypes.SPOT_MARKER + "/" + suffix;
            attributeSet.setAttribute(StandardAttributeNames.ICONSET_PATH, path);
            attributeSet.setAttribute(StandardAttributeNames.COLOR, colorValue);

            nameGenerator = new CountingNameGenerator(spot.namePrefix, 1, 1);
        }
    }

    /**
     * Definition for a spot map icon, which can be a colored spot or a label-only.
     *
     * @param takColor The color to use for a spot icon
     * @param iconName Name for the icon
     * @param icon Image icon
     * @param namePrefix Prefix to use for generating names for dropped points
     */
    private record SpotDefinition(@Nonnull TakColor takColor, @Nonnull String iconName, @Nonnull ImageIcon icon, @Nonnull String namePrefix)
    {
        /** Constructor for a colored spot. */
        SpotDefinition(@Nonnull TakColor takColor, @Nonnull String namePrefix)
        {
            this(takColor, takColor.colorName, new ImageIcon(GraphicsUtils.colorImage(SPOT_IMAGE, new Color(takColor.colorValue))), namePrefix);
        }
    }
}
