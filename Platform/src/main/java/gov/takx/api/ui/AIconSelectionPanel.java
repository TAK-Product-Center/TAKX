package gov.takx.api.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.IPluginDelegate;
import gov.takx.api.registry.IIconSelectionListener;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import java.awt.Dimension;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consistent way for plugins, typically App Plugins, to prompt the user to select one or more icons.
 * <p>
 * It is presently the responsibility of the plugin to define what images are used to populate the panel,
 * see {@link #addImage(String)}.
 *
 * @since 1.13
 */
@PseudoBean
public abstract class AIconSelectionPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected static final int IMAGE_WIDTH = 64;
    protected static final int IMAGE_HEIGHT = 64;
    protected IIconSelectionListener selectionListener;
    protected ImageIcon selectedImage;
    protected IPluginDelegate delegate;
    protected IconGroupSelectionButton groupButton;
    protected Map<String, DefaultListModel<ImageIcon>> groupToListModelMap;
    protected static final String ALL_GROUPS = "All Groups";
    protected static final String ADDED_ICONS = "Added Icons";

    protected AIconSelectionPanel(IPluginDelegate delegate)
    {
        initComponents();
        imageBrowseButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);

        iconList.setVisibleRowCount(-1);

        groupToListModelMap = new HashMap<>();
        groupButton = new IconGroupSelectionButton(this);
        groupSelectionPanel.add(groupButton, new CellConstraints(3, 1));
        groupSelectionPanel.setVisible(false);  //default to not show group selection button
        this.delegate = delegate;
    }

    /**
     * Need a no-args constructor to meet Swing Beans spec which is required by JFormDesigner
     */
    protected AIconSelectionPanel()
    {
        this(null);
    }

    public void setIconListSize(int width, int height)
    {
        iconScrollPane.setPreferredSize(new Dimension(width, height));
    }

    /**
     * Controls visibility of group selection button and panel.  These components are invisible by default.
     *
     * @param visible if {@code true} the group selection panel and button will be visible, otherwise the panel
     *                will be invisible.
     */
    public void setGroupSelectionButtonVisible(boolean visible)
    {
        groupSelectionPanel.setVisible(visible);
    }

    public void setCustomLabel(String text)
    {
        customLabel.setText(text);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        iconList.setEnabled(enabled);
        imageBrowseButton.setEnabled(enabled);
    }

    /**
     * Controls visibility of browse button and panel.  These components are visible by default.
     *
     * @param visible if {@code true} the browse button and panel will be visible, otherwise the panel will be
     *                invisible.
     */
    public void setBrowseButtonVisible(boolean visible)
    {
        panel1.setVisible(visible);
    }

    public void setIconSelectionListener(IIconSelectionListener selectionListener)
    {
        this.selectionListener = selectionListener;
    }

    public boolean hasIcons()
    {
        return !getScaledImagesAndAddresses().isEmpty();
    }

    protected abstract Map<String, ImageIcon> getScaledImagesAndAddresses();

    protected abstract Set<String> getAddedIconsGroup();

    /**
     * The bulk of the icon image management occurs in this method.  Assuming the images/paths are already set up,
     * this method will create a DefaultListModel for each icon group, along with models for "All Groups" and
     * "Added Icons".  The "All Groups" model is selected by default - so even if the groups selection button is
     * invisible, this panel is still navigable.
     */
    public void initIconList()
    {
        groupToListModelMap.clear();
        DefaultListModel<ImageIcon> allGroupsModel = new DefaultListModel<>();
        groupToListModelMap.put(ALL_GROUPS, allGroupsModel);

        // sort the icons by name
        List<Map.Entry<String, ImageIcon>> entryList = new ArrayList<>(getScaledImagesAndAddresses().entrySet());
        entryList.sort((o1, o2) -> {
            String name1 = new File(o1.getKey()).getName();
            String name2 = new File(o2.getKey()).getName();
            return name1.compareTo(name2);
        });

        // place icons in their respective groups
        entryList.forEach(imageEntry -> {
            final String imageKey = imageEntry.getKey();
            final String groupName = getAddedIconsGroup().contains(imageKey) ? ADDED_ICONS : extractGroupFromPath(imageKey);

            if (!ALL_GROUPS.equals(groupName))
            {
                groupToListModelMap.computeIfAbsent(groupName, k -> new DefaultListModel<>())
                        .addElement(imageEntry.getValue());
            }
        });

        List<String> groupList = new ArrayList<>();
        groupList.add(ALL_GROUPS); // force "All Groups" to the top of the list

        final List<String> otherGroups = groupToListModelMap.keySet().stream()
                .filter(n -> !ALL_GROUPS.equals(n))
                .sorted(Collections.reverseOrder())
                .toList();

        // add icons to all groups model - in reverse group ordering (this ordering is arbitrary, but it tends to put
        // the largest group (Flags) last
        otherGroups.forEach(groupName -> {
            final var elements = groupToListModelMap.get(groupName).elements();
            while (elements.hasMoreElements()) allGroupsModel.addElement(elements.nextElement());
        });

        groupList.addAll(otherGroups);
        groupButton.setGroups(groupList);
        selectGroupsModel(allGroupsModel);
        groupButton.setSelectedMenuItem(ALL_GROUPS);
    }

    public void selectIconGroupView(String groupName)
    {
        if (groupName != null && groupToListModelMap.containsKey(groupName))
        {
            selectGroupsModel(groupToListModelMap.get(groupName));
        }
    }

    private void selectGroupsModel(DefaultListModel<ImageIcon> groupsModel)
    {
        iconList.setModel(groupsModel);
        iconList.setSelectedIndex(0);
    }

    /**
     * Attempt to extract the group name from a path to an image.  If the given path has a parent directory, then return
     * that.  If not, then a default of "User Added" is returned.
     *
     * @param imagePath path to an image
     * @return group name
     */
    @Nonnull
    protected String extractGroupFromPath(@Nonnull String imagePath)
    {
        final File parent = new File(imagePath).getParentFile();
        return parent != null ? WordUtils.capitalize(parent.getName()) : "User Added";
    }

    private void imageBrowseButtonActionPerformed()
    {
        browseImages();
    }

    protected void browseImages()
    {
        JFileChooser openFileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        openFileChooser.setMultiSelectionEnabled(false);
        openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        openFileChooser.setDialogTitle("Choose an image");

        int retVal = openFileChooser.showOpenDialog(this);
        if (retVal != JFileChooser.APPROVE_OPTION)
        {
            return;
        }

        File file = openFileChooser.getSelectedFile();
        if (!file.exists())
        {
            JOptionPane.showMessageDialog(this, "Error reading image file : " + file);
        } else
        {
            addImage(file.getPath());
        }
    }

    public void selectIcon(ImageIcon icon)
    {
        ListModel<ImageIcon> lm = iconList.getModel();
        for (int i = 0; i < lm.getSize(); i++)
        {
            if (lm.getElementAt(i) == icon)
            {
                iconList.setSelectedIndex(i);
                return;
            }
        }
    }

    private void iconListValueChanged()
    {
        if (iconList.getSelectedValue() != null)
        {
            if (iconList != null && iconList.getSelectedValue() != null)
            {
                selectedImage = iconList.getSelectedValue();
            } else
            {
                selectedImage = null;
            }
            if (selectionListener != null)
            {
                selectionListener.iconSelected(getSelectedImagePath());
            }
        }
    }

    public String getSelectedImagePath()
    {
        for (Map.Entry<String, ImageIcon> stringImageIconEntry : getScaledImagesAndAddresses().entrySet())
        {
            if (stringImageIconEntry.getValue().equals(selectedImage))
            {
                return stringImageIconEntry.getKey();
            }
        }
        return null;
    }

    public void setSelectedIcon(String pathOfImageToSelect)
    {
        if (pathOfImageToSelect != null)
        {
            ImageIcon imageToSelect = getScaledImagesAndAddresses().get(pathOfImageToSelect);
            selectedImage = imageToSelect;
            if (imageToSelect != null)
            {
                iconList.setSelectedValue(imageToSelect, true);
            } else
            {
                addImage(pathOfImageToSelect);
                imageToSelect = getScaledImagesAndAddresses().get(pathOfImageToSelect);
                if (imageToSelect != null)
                {
                    iconList.setSelectedValue(imageToSelect, true);
                }
            }
        } else
        {
            logger.error("The default selected icon can not be null");
        }
    }

    /**
     * Use this method if you want to manage the imageIcons yourself. This method allows better performance since the
     * icons do not need to be recreated every time the panel is recreated.
     *
     * @param imageIcon - the icon that will be placed in the panel
     * @param imagePath - the path of the icon to be placed in the panel
     */
    public void addImage(ImageIcon imageIcon, String imagePath)
    {
        getScaledImagesAndAddresses().put(imagePath, imageIcon);
        getAddedIconsGroup().add(imagePath);
        initIconList();
        DefaultListModel<ImageIcon> lm = groupToListModelMap.get(ADDED_ICONS);
        selectGroupsModel(lm);
        if (!lm.isEmpty())
        {
            iconList.setSelectedIndex(lm.size() - 1);
        }
        groupButton.setText(ADDED_ICONS);
    }

    /**
     * The path is loaded as an ImageIcon and placed in the list of icons.
     *
     * @param imagePath path of the image to load
     * @return the ImageIcon that was created from the imagePath or {@code null} if an error occurred
     */
    public ImageIcon addImage(String imagePath)
    {
        if (imagePath.charAt(0) == '\\' || imagePath.charAt(0) == '/')
        {
            imagePath = imagePath.substring(1);
        }

        ImageIcon icon = ImageUtils.getScaledIcon(new File(imagePath), IMAGE_WIDTH, IMAGE_HEIGHT, false);
        if (icon != null)
        {
            getScaledImagesAndAddresses().put(imagePath, icon);
            initIconList();
            selectedImage = icon;
        }

        return icon;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel2 = new JPanel();
        groupSelectionPanel = new JPanel();
        customLabel = new JLabel();
        var showLabel = new JLabel();
        iconScrollPane = new JScrollPane();
        iconList = new JList<>();
        panel1 = new JPanel();
        var panel3 = new JPanel();
        imageBrowseButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "pref:grow, $lcgap, left:default",
                    "default, fill:default:grow"));

            //======== groupSelectionPanel ========
            {
                groupSelectionPanel.setName("groupSelectionPanel");
                groupSelectionPanel.setLayout(new FormLayout(
                        "default:grow, 2*(default)",
                        "default"));

                //---- customLabel ----
                customLabel.setName("customLabel");
                groupSelectionPanel.add(customLabel, cc.xy(1, 1));

                //---- showLabel ----
                showLabel.setText("Show:");
                showLabel.setName("showLabel");
                groupSelectionPanel.add(showLabel, cc.xy(2, 1));
            }
            panel2.add(groupSelectionPanel, cc.xy(1, 1));

            //======== iconScrollPane ========
            {
                iconScrollPane.setName("iconScrollPane");

                //---- iconList ----
                iconList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                iconList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                iconList.setVisibleRowCount(4);
                iconList.setName("iconList");
                iconList.addListSelectionListener(e -> iconListValueChanged());
                iconScrollPane.setViewportView(iconList);
            }
            panel2.add(iconScrollPane, cc.xy(1, 2));

            //======== panel1 ========
            {
                panel1.setName("panel1");
                panel1.setLayout(new FormLayout(
                        "pref",
                        "fill:default:grow"));

                //======== panel3 ========
                {
                    panel3.setName("panel3");
                    panel3.setLayout(new FormLayout(
                            "pref",
                            "top:default:grow"));

                    //---- imageBrowseButton ----
                    imageBrowseButton.setName("imageBrowseButton");
                    imageBrowseButton.addActionListener(e -> imageBrowseButtonActionPerformed());
                    panel3.add(imageBrowseButton, cc.xy(1, 1));
                }
                panel1.add(panel3, cc.xy(1, 1));
            }
            panel2.add(panel1, cc.xy(3, 2));
        }
        add(panel2, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel groupSelectionPanel;
    private JLabel customLabel;
    private JScrollPane iconScrollPane;
    private JList<ImageIcon> iconList;
    private JPanel panel1;
    private JButton imageBrowseButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
