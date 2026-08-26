package gov.takx.core.view.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.WrappingLabel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import java.awt.image.BufferedImage;
import java.net.URL;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.FEATURE_IMAGE_SIZE;

/**
 * A header for adding an icon and description to the top of dialogs. Dialogs should use this panel instead of creating
 * their own since it has an appropriate preferred width set to avoid the dialog expanding to the width of the
 * description.
 *
 */
public class DialogHeaderPanel extends JPanel
{
    public DialogHeaderPanel()
    {
        initComponents();
    }

    /**
     * Set the image on the panel, sizing it to the {@link UIResources#FEATURE_IMAGE_SIZE}.
     *
     * @param image The image to use for the icon
     * @since 3.3
     */
    public void setIcon(@Nonnull BufferedImage image)
    {
        iconAndDescriptionLabel.setIcon(new ImageIcon(ImageUtils.getScaledInstance(image, FEATURE_IMAGE_SIZE.width)));
    }

    /**
     * Set the icon on the panel, sizing it to the {@link UIResources#FEATURE_IMAGE_SIZE}. If the icon doesn't exist at
     * the provided URL, none will be added.
     *
     * @param iconUrl The URL of the icon file
     * @since 3.4
     */
    public void setIcon(@Nonnull URL iconUrl)
    {
        iconAndDescriptionLabel.setIcon(UIResources.getSizedIcon(iconUrl, FEATURE_IMAGE_SIZE, false));
    }

    /**
     * Set the icon on the panel, sizing it to the {@link UIResources#FEATURE_IMAGE_SIZE}. If the icon doesn't exist at
     * the provided file name, none will be added.
     *
     * @param iconFileName The name of the icon file within {@link gov.takx.platform.ui.ResourceDirectory#PLATFORM_ICONS}
     * @since 3.3
     */
    public void setIcon(@Nonnull String iconFileName)
    {
        iconAndDescriptionLabel.setIcon(UIResources.getFeatureIcon(PLATFORM_ICONS, iconFileName));
    }

    /**
     * Set the description on the panel, adding HTML start and end tags if they aren't already present. It also adjusts
     * the row size for the layout so that when the text wraps, the containing dialog will be the right size to
     * accommodate the label's height without cutting off other components.
     *
     * @param description The description to add (may or may not be formatted as HTML)
     */
    public void setDescription(@Nonnull String description)
    {
        iconAndDescriptionLabel.setText(description);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        iconAndDescriptionLabel = new WrappingLabel();
        var separator = new JSeparator();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "[pref,200dlu]:grow",
                "2*(default, $rgap)"));

        //---- iconAndDescriptionLabel ----
        iconAndDescriptionLabel.setName("iconAndDescriptionLabel");
        add(iconAndDescriptionLabel, cc.xy(1, 1));

        //---- separator ----
        separator.setName("separator");
        add(separator, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private WrappingLabel iconAndDescriptionLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
