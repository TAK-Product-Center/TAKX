package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This dialog allows the user to select which map objects' tracks to view after double-clicking on a group of tracks
 * owned by more than one map object.
 *
 * @since 1.17
 */
public class MapObjectTrackSelectionDialog extends AStandardDialog implements ItemListener
{
    private static final String DESCRIPTION = "<html>The area you selected contains tracks belonging to multiple map " +
            "objects. Please select which map object's tracks you would like to view.</html>";

    /**
     * The button group for the map object radio buttons added to this dialog.
     */
    private final ButtonGroup buttonGroup;

    /**
     * A mapping of each radio button to the map object it corresponds to.
     */
    private final Map<JRadioButton, IMapObject> buttonToMapObjectMap;

    /**
     * A mapping from each label containing the map object name and icon to its button.
     */
    private final Map<JPanel, JRadioButton> panelToButtonMap;

    /**
     * Listens for clicks on the label associated with a radio button.
     */
    private final MouseListener labelMouseListener;

    /**
     * Indicates if the user has cancelled out of this dialog via the 'X' or the cancel button.
     */
    private boolean cancelled = true;

    /**
     * @param mapObjectToRDMs Map of MO to RDMs
     */
    public MapObjectTrackSelectionDialog(Map<IMapObject, List<IRaptorDataMessage>> mapObjectToRDMs)
    {
        initComponents();
        dialogHeaderPanel.setDescription(DESCRIPTION);
        dialogHeaderPanel.setIcon("tracks.png");

        buttonGroup = new ButtonGroup();
        buttonToMapObjectMap = new HashMap<>();
        panelToButtonMap = new HashMap<>();
        labelMouseListener = new LabelMouseListener();

        // Add the map objects.
        List<IMapObject> sortedList = getSortedList(mapObjectToRDMs.keySet());
        for (IMapObject mapObject : sortedList)
        {
            addMapObject(mapObject, mapObjectToRDMs.get(mapObject).size());
        }

        buttonOk.setEnabled(false);
        pack();
    }

    /**
     * @return was the dialog cancelled (closed any other way than the "Ok" button)
     */
    public boolean cancelled()
    {
        return cancelled;
    }

    /**
     * Return the map object selected by the user or {@code null} if "Select All" was chosen.
     *
     * @return The map object selected by the user or {@code null} if "Select All" was chosen.
     */
    public IMapObject getSelectedMapObject()
    {
        IMapObject result = null;

        if (!checkBoxShowAll.isSelected())
        {
            for (Map.Entry<JRadioButton, IMapObject> entry : buttonToMapObjectMap.entrySet())
            {
                if (entry.getKey().isSelected())
                {
                    result = entry.getValue();
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Add a map object to the UI in the form of a radio button displaying the map object name and message count.
     *
     * @param mapObject    The map object.
     * @param messageCount The map object's message count.
     */
    private void addMapObject(IMapObject mapObject, int messageCount)
    {
        // Prepare the radio button.
        JRadioButton button = new JRadioButton();
        button.addItemListener(this);
        buttonGroup.add(button);
        buttonToMapObjectMap.put(button, mapObject);

        // Prepare the label with the map object icon, name and message count.
        JLabel label = new JLabel(mapObject.getName() + " (" + messageCount + " tracks)", mapObject.getSizedIcon(ImageUtils.SMALL_ICON_WIDTH), JLabel.LEFT);

        // Prepare the panel that will hold the radio button and label.
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(button);
        panel.add(label);
        panel.addMouseListener(labelMouseListener);
        panelToButtonMap.put(panel, button);

        // Add the map object's panel to the container. Add a new row because we're using FormLayout.
        ((FormLayout) panelMapObjectOptions.getLayout()).appendRow(RowSpec.decode("default"));
        panelMapObjectOptions.add(panel, new CellConstraints(2, buttonToMapObjectMap.size()));
    }

    /**
     * Take a set of map objects and return them in a list sorted by map object name.
     *
     * @param mapObjects The unsorted set of map objects.
     * @return A sorted list of map objects.
     */
    private List<IMapObject> getSortedList(Set<IMapObject> mapObjects)
    {
        List<IMapObject> result = new ArrayList<>();

        for (IMapObject mapObject : mapObjects)
        {
            insertIntoSortedList(result, mapObject);
        }

        return result;
    }

    /**
     * Insert a map object into the provided list, sorted by map object name.
     *
     * @param list      The list of map objects.
     * @param mapObject The map object to add to the list in sorted order.
     */
    private void insertIntoSortedList(List<IMapObject> list, IMapObject mapObject)
    {
        for (int i = 0; i < list.size(); i++)
        {
            if (mapObject.getName().compareTo(list.get(i).getName()) < 0)
            {
                list.add(i, mapObject);
                return;
            }
        }

        list.add(mapObject);
    }

    /**
     * Enable the okay button if any map object is selected.
     *
     * @param e Event details.
     */
    @Override
    public void itemStateChanged(ItemEvent e)
    {
        buttonOk.setEnabled(true);
    }

    /**
     * Disable all radio buttons if the "Show All" checkbox is selected. Enable the "Okay" button
     * only if a map object or "Show All" is selected.
     */
    private void checkBoxShowAllItemStateChanged()
    {
        for (AbstractButton button : buttonToMapObjectMap.keySet())
        {
            button.setEnabled(!checkBoxShowAll.isSelected());
        }

        buttonOk.setEnabled(checkBoxShowAll.isSelected() || buttonGroup.getSelection() != null);
    }

    /**
     * Note that this dialog was not exited via a cancel option and release its modality control.
     */
    private void buttonOkayActionPerformed()
    {
        cancelled = false;
        setVisible(false);
    }

    /**
     * Hide this dialog and release its modality control.
     */
    private void buttonCancelActionPerformed()
    {
        setVisible(false);
    }

    private class LabelMouseListener extends MouseAdapter
    {
        @Override
        public void mousePressed(MouseEvent e)
        {
            JPanel source = (JPanel) e.getSource();
            panelToButtonMap.get(source).setSelected(true);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        var scrollPane1 = new JScrollPane();
        panelMapObjectOptions = new JPanel();
        checkBoxShowAll = new JCheckBox();
        var buttonBar = new JPanel();
        buttonOk = new JButton();
        var buttonCancel = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Select Map Object");
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "center:default:grow",
                        "default:grow, $rgap, default"));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //======== panelMapObjectOptions ========
                    {
                        panelMapObjectOptions.setName("panelMapObjectOptions");
                        panelMapObjectOptions.setLayout(new FormLayout(
                                "default:grow, left:default, default:grow",
                                ""));
                    }
                    scrollPane1.setViewportView(panelMapObjectOptions);
                }
                contentPanel.add(scrollPane1, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

                //---- checkBoxShowAll ----
                checkBoxShowAll.setText("Show Basic Details For All");
                checkBoxShowAll.setName("checkBoxShowAll");
                checkBoxShowAll.addItemListener(e -> checkBoxShowAllItemStateChanged());
                contentPanel.add(checkBoxShowAll, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "default"));

                //---- buttonOk ----
                buttonOk.setText("OK");
                buttonOk.setName("buttonOk");
                buttonOk.addActionListener(e -> buttonOkayActionPerformed());
                buttonBar.add(buttonOk, cc.xy(2, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                //---- buttonCancel ----
                buttonCancel.setText("Cancel");
                buttonCancel.setName("buttonCancel");
                buttonCancel.addActionListener(e -> buttonCancelActionPerformed());
                buttonBar.add(buttonCancel, cc.xy(4, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JPanel panelMapObjectOptions;
    private JCheckBox checkBoxShowAll;
    private JButton buttonOk;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
