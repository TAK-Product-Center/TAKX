package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;

/**
 * Dialog displaying the contents of the data package transfer log.
 *
 * @see IExtendedDataPackageManager#getTransferLog()
 * @since 5.0
 */
@PseudoBean(singletonObserver = true)
public class DataPackageTransferHistoryDialog extends AStandardDialog implements IExtendedDataPackageManager.ITransferListener
{
    private final DefaultListModel<IExtendedDataPackageManager.TransferLogItem> logModel = new DefaultListModel<>();

    @Inject
    private IExtendedDataPackageManager dataPackageManager;

    public DataPackageTransferHistoryDialog(@Nullable Window owner)
    {
        super(owner);

        initComponents();

        logList.setCellRenderer(new DataPackageTransferItemPanel());
    }

    @PostConstruct
    private void postConstruct()
    {
        initializeModel();

        // Reminder, we're only used effectively as a single instance, so we do not need to worry about cleanup here.
        dataPackageManager.addTransferListener(this);
    }

    @Override
    public void onNewTransfer(@Nonnull IExtendedDataPackageManager.TransferLogItem item)
    {
        addModelItem(item);
    }

    @Override
    public void onTransfersCleared()
    {
        clearModel();
    }

    /**
     * Called when the delete button is pressed.
     * This will indirectly call {@link #clearModel()} via {@link #onTransfersCleared()}.
     */
    private void onDelete()
    {
        // The listener callback will handle clearing the list
        dataPackageManager.clearTransferLog();
    }

    /**
     * Sets up the initial UI model with contents from the data package manager.
     */
    @InvokeOnEDT
    private void initializeModel()
    {
        logModel.addAll(dataPackageManager.getTransferLog());
        logList.setModel(logModel);
        logList.repaint();
    }

    /**
     * Clears the UI model.
     */
    @InvokeOnEDT
    private void clearModel()
    {
        logModel.clear();
        logList.repaint();
    }

    /**
     * Adds an item to the UI model.
     *
     * @param item Item to add.
     */
    @InvokeOnEDT
    private void addModelItem(@Nonnull IExtendedDataPackageManager.TransferLogItem item)
    {
        logModel.addElement(item);
        logList.repaint();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        deleteButton = new JButton();
        listScrollWrapper = new JScrollPane();
        logList = new JList<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("File transfer log");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow, $lcgap, default",
                        "default, $lgap, fill:default:grow"));

                //---- deleteButton ----
                deleteButton.setText("Delete");
                deleteButton.setName("deleteButton");
                deleteButton.addActionListener(e -> onDelete());
                contentPanel.add(deleteButton, cc.xy(3, 1));

                //======== listScrollWrapper ========
                {
                    listScrollWrapper.setName("listScrollWrapper");

                    //---- logList ----
                    logList.setBackground(new Color(20, 20, 20));
                    logList.setName("logList");
                    listScrollWrapper.setViewportView(logList);
                }
                contentPanel.add(listScrollWrapper, cc.xywh(1, 3, 3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JButton deleteButton;
    private JScrollPane listScrollWrapper;
    private JList<IExtendedDataPackageManager.TransferLogItem> logList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
