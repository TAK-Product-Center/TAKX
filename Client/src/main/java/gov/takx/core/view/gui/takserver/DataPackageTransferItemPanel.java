package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.SizeFormatter;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.awt.Component;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Panel showing the contents of a single data package transfer.
 *
 * @see DataPackageTransferHistoryDialog Containing dialog.
 * @since 5.0
 */
public class DataPackageTransferItemPanel extends JPanel implements ListCellRenderer<IExtendedDataPackageManager.TransferLogItem>
{
    public DataPackageTransferItemPanel()
    {
        initComponents();
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends IExtendedDataPackageManager.TransferLogItem> list,
                                                  IExtendedDataPackageManager.TransferLogItem transfer, int index, boolean isSelected, boolean cellHasFocus)
    {
        eventLabel.setText(transfer.isReceive() ? "RECV" : "SEND");
        packageNameLabel.setText(FileUtils.getNameWithoutExtension(transfer.packageFile()));
        packageSizeLabel.setText(SizeFormatter.formatEstimate(transfer.fileSize()));
        timeLabel.setText(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss ZZZ").withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(transfer.lastModified())));
        String verb = transfer.isReceive() ? "received from " : "sent to ";
        String callsign = transfer.getOtherCallsign();
        messageLabel.setText("Data Package " + verb + callsign);
        return this;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        eventLabel = new JLabel();
        packageNameLabel = new JLabel();
        packageSizeLabel = new JLabel();
        messageLabel = new JLabel();
        timeLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new CompoundBorder(
                new EmptyBorder(5, 3, 5, 3),
                new CompoundBorder(
                        new LineBorder(Color.darkGray, 1, true),
                        new EmptyBorder(4, 4, 4, 4))));
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, center:default:grow, $lcgap, default",
                "2*(default, $lgap), default"));

        //---- eventLabel ----
        eventLabel.setText("EVENT");
        eventLabel.setName("eventLabel");
        add(eventLabel, cc.xy(1, 1));

        //---- packageNameLabel ----
        packageNameLabel.setText("FILE_NAME");
        packageNameLabel.setName("packageNameLabel");
        add(packageNameLabel, cc.xy(3, 1));

        //---- packageSizeLabel ----
        packageSizeLabel.setText("SIZE");
        packageSizeLabel.setName("packageSizeLabel");
        add(packageSizeLabel, cc.xy(5, 1));

        //---- messageLabel ----
        messageLabel.setText("MESSAGE");
        messageLabel.setName("messageLabel");
        add(messageLabel, cc.xywh(1, 3, 5, 1));

        //---- timeLabel ----
        timeLabel.setText("DATE");
        timeLabel.setName("timeLabel");
        add(timeLabel, cc.xywh(1, 5, 5, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel eventLabel;
    private JLabel packageNameLabel;
    private JLabel packageSizeLabel;
    private JLabel messageLabel;
    private JLabel timeLabel;

    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
