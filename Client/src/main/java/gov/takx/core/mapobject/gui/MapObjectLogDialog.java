package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.mapobject.MapEntityStructuresUpdated;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.query.IQueryService;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.mapobject.gui.devicelog.DeviceLogPanel;
import gov.takx.dataservices.persist.MapEntity;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Window;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Presents a GUI for working with a set of persisted {@link IRaptorDataMessage} instances associated with a particular MapObjectID.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public class MapObjectLogDialog extends AMapObjectDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DeviceLogPanel logPanel;

    /**
     * Tracks {@link MapEntity} changes.
     */
    private boolean structureListStale;

    /**
     * The data structures being tracked by this dialog.
     */
    private final Collection<IRaptorDataStructure> dataStructures;

    public MapObjectLogDialog(Window owner, IMapObjectController controller, Long minReceiveTime, Collection<IRaptorDataStructure> dataStructures, IRaptorDataStructure defaultFilter, IQueryService queryService, String vfsDir)
    {
        this(owner, controller, minReceiveTime, dataStructures, defaultFilter, queryService, vfsDir, null, null);
    }

    public MapObjectLogDialog(Window owner, IMapObjectController controller, Long minReceiveTime, Collection<IRaptorDataStructure> dataStructures, IRaptorDataStructure defaultFilter, IQueryService queryService, String vfsDir, ILocation minLocation, ILocation maxLocation)
    {
        super(owner, controller);

        initComponents();

        setDefaultCloseOperation(HIDE_ON_CLOSE); // So we retain user selections/sizing/customization

        final IMapObject mapObject = controller.getMapObject();
        final String mapObjectKey = controller.getKey();

        if (logger.isDebugEnabled()) logger.debug("Device Log initialized for map object ID {}", mapObjectKey);

        this.dataStructures = new ArrayList<>(dataStructures);

        final String mapObjectName = mapObject.getName();
        final String description = mapObject.getUnitID();

        logPanel = new DeviceLogPanel(mapObjectName, mapObjectKey, description, minReceiveTime, dataStructures, defaultFilter, queryService, vfsDir, minLocation, maxLocation);

        CellConstraints cc = new CellConstraints();
        contentPanel.add(logPanel, cc.xywh(1, 1, 1, 1, CellConstraints.FILL, CellConstraints.FILL));

        // Repack and reset relative location so that dialog
        // appears in the center of the application frame.
        pack();
        setLocationRelativeTo(getOwner());
    }

    /**
     * Structures have been re-defined for a Map Entity.  Check to see if our MOC is related to the ME, and if so we
     * will need to use the new structures.  Query the user if they want to do it right away, or postpone until the
     * next time this dialog is created.
     *
     * @param event the structure re-defined event
     * @since 5.5.1
     */
    private void onStructuresRedefined(@Observes MapEntityStructuresUpdated event)
    {
        final var controller = getMapObjectController();

        if (controller.getMapObject().getMapEntity() == event.mapEntity())
        {
            if (isVisible())
            {
                SwingUtilities.invokeLater(() -> {
                    // Prompt the user
                    int choice = AlwaysOnTopJOptionPane.showConfirmDialog(this,
                            "Would you like to reload the Device Log Dialog to include access to newly declared structures?",
                            "New Structures Available (" + controller.getKey() + ")",
                            JOptionPane.QUESTION_MESSAGE,
                            JOptionPane.YES_NO_OPTION);

                    if (JOptionPane.YES_OPTION == choice)
                    {
                        dispose();
                        controller.showDeviceLog();
                    } else
                    {
                        structureListStale = true;
                    }
                });
            } else
            {
                dispose();
            }
        }
    }

    public void receiveMessage(IRaptorDataMessage rdm)
    {
        logPanel.receiveMessage(rdm);
    }

    @Override
    protected void onWindowClosing()
    {
        logPanel.close(true);
        super.onWindowClosing();
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            logPanel.open();
        } else
        {
            logPanel.close(false);
        }

        super.setVisible(visible);
    }

    public void refreshData()
    {
        logPanel.refreshData();
    }

    private void okButtonActionPerformed()
    {
        setVisible(false);
    }

    public boolean isStructureListStale(Collection<IRaptorDataStructure> dataStructures)
    {
        return structureListStale || requestedDataStructuresChanged(dataStructures);
    }

    /**
     * Check to see if the collection of data structures has changed since this dialog was created.
     *
     * @param dataStructures The new collection of data structures.
     * @return {@code true} iff the collection has changed.
     * @since 2.12
     */
    private boolean requestedDataStructuresChanged(Collection<IRaptorDataStructure> dataStructures)
    {
        return dataStructures != null
                && (!this.dataStructures.containsAll(dataStructures) || !dataStructures.containsAll(this.dataStructures));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        contentPanel = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Device Log");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "top:default:grow"));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "default, $lcgap, default, $glue, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("Close");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(5, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contentPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
