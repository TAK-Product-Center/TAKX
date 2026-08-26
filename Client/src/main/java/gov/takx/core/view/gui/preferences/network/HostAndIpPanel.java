package gov.takx.core.view.gui.preferences.network;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Panel to display the hostname and IP address, with buttons to copy their values to the clipboard.
 * @since 5.4
 */
public class HostAndIpPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Dimension ICON_SIZE = new Dimension(SMALL_ICON_WIDTH - 2, SMALL_ICON_HEIGHT - 2);
    private static final ImageIcon COPY_ICON = UIResources.getSizedIcon(TAK_ASSETS_PNG, "Copy_32x32.png", ICON_SIZE, true);

    private static final HostAndIp hostAndIp = getHostAndIp();

    public HostAndIpPanel()
    {
        initComponents();

        hostnameValueLabel.setText(hostAndIp.hostname);
        copyHostnameButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        copyHostnameButton.setIcon(COPY_ICON);

        ipAddressValueLabel.setText(hostAndIp.ipAddress);
        copyIpButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        copyIpButton.setIcon(COPY_ICON);
    }

    private void copyHostnameToClipboard()
    {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(hostAndIp.hostname), null);
    }

    private void copyIpAddressToClipboard()
    {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(hostAndIp.ipAddress), null);
    }

    private static HostAndIp getHostAndIp()
    {
        try
        {
            final var localHost = InetAddress.getLocalHost();
            return new HostAndIp(localHost.getHostName(), localHost.getHostAddress());
        } catch (UnknownHostException e)
        {
            logger.error("Error encountered while trying to obtain IP address.", e);
            return new HostAndIp("(unknown)", "(unknown)");
        }
    }

    private record HostAndIp(@Nonnull String hostname, @Nonnull String ipAddress)
    {
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var HostAndIpPanel = new JPanel();
        var hostnameLabel = new JLabel();
        hostnameValueLabel = new JLabel();
        copyHostnameButton = new JButton();
        var ipAddressLabel = new JLabel();
        ipAddressValueLabel = new JLabel();
        copyIpButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== HostAndIpPanel ========
        {
            HostAndIpPanel.setName("HostAndIpPanel");
            HostAndIpPanel.setLayout(new FormLayout(
                    "default, default:grow, $lcgap, default",
                    "default, $lgap, default"));

            //---- hostnameLabel ----
            hostnameLabel.setText("Device Hostname");
            hostnameLabel.setFont(hostnameLabel.getFont().deriveFont(hostnameLabel.getFont().getSize() + 1f));
            hostnameLabel.setName("hostnameLabel");
            HostAndIpPanel.add(hostnameLabel, cc.xy(1, 1));

            //---- hostnameValueLabel ----
            hostnameValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            hostnameValueLabel.setFont(hostnameValueLabel.getFont().deriveFont(hostnameValueLabel.getFont().getSize() + 1f));
            hostnameValueLabel.setName("hostnameValueLabel");
            HostAndIpPanel.add(hostnameValueLabel, cc.xy(2, 1));

            //---- copyHostnameButton ----
            copyHostnameButton.setFocusable(false);
            copyHostnameButton.setToolTipText("Copy Hostname");
            copyHostnameButton.setName("copyHostnameButton");
            copyHostnameButton.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    copyHostnameToClipboard();
                }
            });
            HostAndIpPanel.add(copyHostnameButton, cc.xy(4, 1));

            //---- ipAddressLabel ----
            ipAddressLabel.setText("Primary IP Address");
            ipAddressLabel.setFont(ipAddressLabel.getFont().deriveFont(ipAddressLabel.getFont().getSize() + 1f));
            ipAddressLabel.setName("ipAddressLabel");
            HostAndIpPanel.add(ipAddressLabel, cc.xy(1, 3));

            //---- ipAddressValueLabel ----
            ipAddressValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            ipAddressValueLabel.setFont(ipAddressValueLabel.getFont().deriveFont(ipAddressValueLabel.getFont().getSize() + 1f));
            ipAddressValueLabel.setName("ipAddressValueLabel");
            HostAndIpPanel.add(ipAddressValueLabel, cc.xy(2, 3));

            //---- copyIpButton ----
            copyIpButton.setFocusable(false);
            copyIpButton.setToolTipText("Copy IP Address");
            copyIpButton.setName("copyIpButton");
            copyIpButton.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    copyIpAddressToClipboard();
                }
            });
            HostAndIpPanel.add(copyIpButton, cc.xy(4, 3));
        }
        add(HostAndIpPanel, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel hostnameValueLabel;
    private JButton copyHostnameButton;
    private JLabel ipAddressValueLabel;
    private JButton copyIpButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
