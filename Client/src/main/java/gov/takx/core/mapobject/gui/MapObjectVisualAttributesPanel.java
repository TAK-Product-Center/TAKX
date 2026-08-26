package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.location.LocationTracker;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectImageUtils;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.MilSymIconScaling;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.SwingFileFilter;
import gov.takx.core.view.gui.maptools.tacticalgraphics.TacticalGraphicsEditorPanel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileFilter;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectVisualAttributesPanel extends MapObjectMainPreferencesPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PNG_SUFFIX = "png";
    private static final FileFilter PNG_FILE_FILTER = new SwingFileFilter(PNG_SUFFIX);

    @Inject
    protected IMapObjectImageUtils mapObjectImageUtils;

    @Inject
    protected IMilStd2525ServiceProvider milStd2525ServiceProvider;

    @Inject
    protected LocationTracker locationTracker;

    private TacticalGraphicsEditorPanel tacticalGraphicsEditorPanel;
    private String newSidc;
    private URI imageURI;
    private boolean iconChanged;
    private boolean resetIcon;
    private File iconFile;

    public static final String TITLE = "Icon";

    public MapObjectVisualAttributesPanel(IMapObjectController mapObjectController, IPanelStateListener panelListener)
    {
        super(TITLE, mapObjectController, panelListener);

        initComponents();

        smallIconLabel.setIcon(UIResources.DEVICE_ICON);
        largeIconLabel.setIcon(UIResources.DEVICE_ACTION_BUTTON_ICON);

        if (isShapeMapObject())
        {
            iconSizePanel.setVisible(false);
        } else
        {
            final var min = (int) (IMapObject.MIN_ICON_SCALE * 100);
            final var max = (int) (IMapObject.MAX_ICON_SCALE * 100);

            iconScaleSlider.setMinimum(min);
            iconScaleSlider.setMaximum(max);
            setIconSizeSlider();

            final var spinnerModel = (SpinnerNumberModel) iconScaleSpinner.getModel();
            spinnerModel.setMinimum(min);
            spinnerModel.setMaximum(max);
        }
    }

    @PostConstruct
    private void postConstruct()
    {
        setIconFromMapObject();

        addProperties(List.of(IMapObject.ICON));

        // Don't allow the user to change the following map object types to a mil-sym rendered marker:
        // - self location marker
        // - files
        // - map objects with an icon from an icon-set applied
        IMapObject mo = getMapObject();
        if (mo.isSelfMarker()
                || mo instanceof IFileMapObject
                || !mo.getMetaString(StandardAttributeNames.ICONSET_PATH, "").isBlank())
        {
            milSymPanel.setVisible(false);
        } else
        {
            tacticalGraphicsEditorPanel = new TacticalGraphicsEditorPanel(e -> on2525AttributeUpdate(), () -> false, false);
            tacticalGraphicsEditorPanel.setMapObject(mapObject);
            milSymPanel.add(tacticalGraphicsEditorPanel, new CellConstraints(1, 1));
        }
    }

    /**
     * Updates our state and preview to match the current values of the {@link #tacticalGraphicsEditorPanel}.
     *
     * @since 5.5
     */
    private void on2525AttributeUpdate()
    {
        // Enable the 'apply' button
        setPanelDirty();

        // Update local state from changes in the 2525 editor panel
        iconChanged = true;
        imageURI = null;
        newSidc = tacticalGraphicsEditorPanel.getSymbolCode();

        // Update the preview image button's content
        if (newSidc != null)
        {
            BufferedImage newImage = milStd2525ServiceProvider.get2525ServiceForSymbol(newSidc).newSymbolIconBuilder()
                    .setCode(newSidc)
                    .addAttributes(tacticalGraphicsEditorPanel.getCurrentAttributeValues())
                    .setResolution(ImageUtils.DEFAULT_ICON_WIDTH)
                    .setResolutionScaling(MilSymIconScaling.SCALE_TO_MIN_DIMENSION)
                    .render();
            changeIconImageButton.setIcon(new ImageIcon(newImage));
        } else
        {
            setIconFromMapObject();
        }
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "icon_library.png");
    }

    @Override
    protected void applyActionPerformed()
    {
        // update icon image (if it has been changed)
        if (iconChanged)
        {
            if (resetIcon)
            {
                mapObject.resetIconImage();
            } else
            {
                if (iconFile != null)
                {
                    try
                    {
                        mapObject.setExternalIconImage(ImageIO.read(iconFile));
                    } catch (IOException e)
                    {
                        logger.error("Failed to read icon image: {}", iconFile.getAbsolutePath(), e);
                    }
                } else if (newSidc != null)
                {
                    applyTacticalGraphics(true);
                }
            }

            iconChanged = false;
            resetIcon = false;
        }

        if (!isShapeMapObject())
        {
            //update icon size
            double scale = iconScaleSlider.getValue() / 100.00;
            if (scale >= IMapObject.MIN_ICON_SCALE && scale <= IMapObject.MAX_ICON_SCALE)
            {
                mapObject.setIconScale(scale);
            }
        }
    }

    /**
     * Delegates commit action to the tactical graphics panel.
     *
     * @param applyName {@code true} to also update the map object name to match the symbol.
     *
     * @since 5.8
     */
    private void applyTacticalGraphics(boolean applyName)
    {
        if (tacticalGraphicsEditorPanel == null) return;

        // Delegate 2525 updates to the 2525 editor panel
        // This will update the symbol and apply modifiers to the map object.
        if (tacticalGraphicsEditorPanel.isVisible()) tacticalGraphicsEditorPanel.commit();

        // Update the name to match the newly selected symbol
        if (applyName)
        {
            final var service = milStd2525ServiceProvider.get2525ServiceForSymbol(newSidc);
            final var symbol = service.getSymbol(newSidc);
            if (symbol != null) mapObject.setName(symbol.getName());
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent)
    {
        if (IMapObject.ICON.equals(propertyChangeEvent.getPropertyName())) setIconFromMapObject();
    }

    /**
     * Sets the icon button image to the map object's current icon.
     *
     * @since 5.1
     */
    private void setIconFromMapObject()
    {
        if (milStd2525ServiceProvider.get2525ServiceForMapObject(mapObject).getSymbolCode(mapObject) != null)
        {
            // MilSym icons are not always perfect squares and trying to make them squares squishes them whenever modifiers are present.
            changeIconImageButton.setIcon(new ImageIcon(ImageUtils.getScaledInstanceToMinSize(mapObject.getIconImage(), ImageUtils.DEFAULT_ICON_WIDTH)));
        } else
        {
            changeIconImageButton.setIcon(mapObject.getSizedIcon(ImageUtils.DEFAULT_ICON_WIDTH));
        }
    }

    private boolean isShapeMapObject()
    {
        return mapObject instanceof IShapeMapObject;
    }

    private void setIconSizeSlider()
    {
        double scale = mapObject.getIconScale();
        if (scale >= IMapObject.MIN_ICON_SCALE && scale <= IMapObject.MAX_ICON_SCALE)
        {
            iconScaleSlider.setValue((int) (scale * 100));
        } else
        {
            iconScaleSlider.setValue(100);
        }
    }

    private boolean setIconImageButton()
    {
        if (imageURI != null)
        {
            logger.debug("Image URI: {}", imageURI);

            // We are loading a custom icon, likely from a file picked by the user
            try
            {
                // Windows file names with spaces are automatically URL-encoded to %20, so we need to decode
                URL url = new URL(URLDecoder.decode(imageURI.toString(), StandardCharsets.UTF_8));
                iconFile = new File(url.getFile());
                final ImageIcon scaledIcon = ImageUtils.getScaledIcon(iconFile, ImageUtils.DEFAULT_ICON_WIDTH, ImageUtils.DEFAULT_ICON_HEIGHT, false);
                if (scaledIcon != null)
                {
                    changeIconImageButton.setIcon(scaledIcon);
                    return true;
                }
            } catch (MalformedURLException e)
            {
                logger.error("Can not open file URI {} as icon image", imageURI, e);
            }
        } else
        {
            // NULL uri means we need to reset the icon to its original value, so obtain what that would be and
            // display it in the icon button. For 2525 tactical graphics, this would be their plain non-modifier
            // symbol.

            if (newSidc != null)
            {
                applyTacticalGraphics(false);
                return true;
            }

            final BufferedImage image;

            if (mapObject instanceof IFileMapObject fileMapObject)
            {
                final File file = fileMapObject.getVfsFile().getFile();
                image = file != null
                        ? FileUtils.getSystemIconImage(file)
                        : ImageUtils.getDefaultImage();
            } else
            {
                // Get the Icon from the plugin definition, if we can find one
                final var pluginDefinition = mapObject.getPluginDefinition();
                image = pluginDefinition == null || mapObjectImageUtils.isSyntheticDefaultIcon(pluginDefinition)
                        ? null
                        : pluginDefinition.getIcon();

                // Check for other edge cases, like the self location marker
                if (mapObject == locationTracker.getMarkerMapObject()) locationTracker.setDefaultTrackerAttributes();
            }

            if (image != null)
            {
                changeIconImageButton.setIcon(ImageUtils.getScaledIcon(image, ImageUtils.DEFAULT_ICON_WIDTH, ImageUtils.DEFAULT_ICON_HEIGHT));
                return true;
            }
        }
        return false;
    }

    private void restoreDefaultsActionPerformed()
    {
        iconScaleSlider.setValue(100);
        newSidc = null;
        imageURI = null;

        // For 2525 markers (indicated by the panel being visible), icon resets are handled in the tactical graphics panel.
        if (tacticalGraphicsEditorPanel != null)
        {
            tacticalGraphicsEditorPanel.restoreDefaults();
            newSidc = tacticalGraphicsEditorPanel.getSymbolCode();

            resetIcon = !tacticalGraphicsEditorPanel.isVisible() || newSidc == null;
            if (newSidc != null) applyTacticalGraphics(false);
        } else
        {
            resetIcon = true;
        }
        if (resetIcon) iconChanged = setIconImageButton();

        setPanelDirty();
    }

    private void changeIconImageButtonActionPerformed()
    {
        final AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);

        chooser.resetChoosableFileFilters();
        chooser.addChoosableFileFilter(PNG_FILE_FILTER); // Add in our file filter (PNG only)

        int returnVal = chooser.showDialog(this, "Open Icon Image");
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();

            // accept correct imageType
            if (file != null && FileUtils.getExtension(file).equalsIgnoreCase(PNG_SUFFIX))
            {
                newSidc = null;
                imageURI = file.toURI();
                iconChanged = setIconImageButton();

                if (iconChanged) setPanelDirty();
            }
        }
    }

    private void iconScaleSliderStateChanged()
    {
        iconScaleSpinner.setValue(iconScaleSlider.getValue());
        setPanelDirty();
    }

    private void iconScaleSpinnerStateChanged()
    {
        iconScaleSlider.setValue((Integer) iconScaleSpinner.getValue());
        setPanelDirty();
    }

    @Override
    public String validateContents()
    {
        return null;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var iconImagePanel = new JPanel();
        var iconLabel = new JLabel();
        changeIconImageButton = new JButton();
        var iconSizeLabel = new JLabel();
        iconSizePanel = new JPanel();
        smallIconLabel = new JLabel();
        iconScaleSlider = new JSlider();
        largeIconLabel = new JLabel();
        iconScaleSpinner = new JSpinner();
        var restoreDefaults = new JButton();
        separator1 = new JSeparator();
        milSymPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default, $lgap), fill:default:grow"));

        //======== iconImagePanel ========
        {
            iconImagePanel.setName("iconImagePanel");
            iconImagePanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, $lcgap, 45dlu, $lcgap, 35dlu",
                    "default, $lgap, default, $ugap, default"));

            //---- iconLabel ----
            iconLabel.setText("Icon");
            iconLabel.setName("iconLabel");
            iconImagePanel.add(iconLabel, cc.xy(1, 1));

            //---- changeIconImageButton ----
            changeIconImageButton.setName("changeIconImageButton");
            changeIconImageButton.addActionListener(e -> changeIconImageButtonActionPerformed());
            iconImagePanel.add(changeIconImageButton, cc.xywh(5, 1, 3, 1));

            //---- iconSizeLabel ----
            iconSizeLabel.setText("Icon Size");
            iconSizeLabel.setName("iconSizeLabel");
            iconImagePanel.add(iconSizeLabel, cc.xy(1, 3));

            //======== iconSizePanel ========
            {
                iconSizePanel.setName("iconSizePanel");
                iconSizePanel.setLayout(new FormLayout(
                        "default, default:grow, default",
                        "default"));

                //---- smallIconLabel ----
                smallIconLabel.setName("smallIconLabel");
                iconSizePanel.add(smallIconLabel, cc.xy(1, 1));

                //---- iconScaleSlider ----
                iconScaleSlider.setMaximum(200);
                iconScaleSlider.setValue(100);
                iconScaleSlider.setMajorTickSpacing(10);
                iconScaleSlider.setMinimum(20);
                iconScaleSlider.setName("iconScaleSlider");
                iconScaleSlider.addChangeListener(e -> iconScaleSliderStateChanged());
                iconSizePanel.add(iconScaleSlider, cc.xy(2, 1));

                //---- largeIconLabel ----
                largeIconLabel.setName("largeIconLabel");
                iconSizePanel.add(largeIconLabel, cc.xy(3, 1));
            }
            iconImagePanel.add(iconSizePanel, cc.xywh(3, 3, 3, 1));

            //---- iconScaleSpinner ----
            iconScaleSpinner.setModel(new SpinnerNumberModel(100, 20, 200, 1));
            iconScaleSpinner.setToolTipText("Percent");
            iconScaleSpinner.setName("iconScaleSpinner");
            iconScaleSpinner.addChangeListener(e -> iconScaleSpinnerStateChanged());
            iconImagePanel.add(iconScaleSpinner, cc.xy(7, 3));

            //---- restoreDefaults ----
            restoreDefaults.setText("Restore Icon Defaults");
            restoreDefaults.setName("restoreDefaults");
            restoreDefaults.addActionListener(e -> restoreDefaultsActionPerformed());
            iconImagePanel.add(restoreDefaults, cc.xywh(5, 5, 3, 1));
        }
        add(iconImagePanel, cc.xy(1, 1));

        //---- separator1 ----
        separator1.setName("separator1");
        add(separator1, cc.xy(1, 3));

        //======== milSymPanel ========
        {
            milSymPanel.setBorder(null);
            milSymPanel.setName("milSymPanel");
            milSymPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));
        }
        add(milSymPanel, cc.xy(1, 5));
// JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton changeIconImageButton;
    private JPanel iconSizePanel;
    private JLabel smallIconLabel;
    private JSlider iconScaleSlider;
    private JLabel largeIconLabel;
    private JSpinner iconScaleSpinner;
    private JSeparator separator1;
    private JPanel milSymPanel;
// JFormDesigner - End of variables declaration  //GEN-END:variables
}
