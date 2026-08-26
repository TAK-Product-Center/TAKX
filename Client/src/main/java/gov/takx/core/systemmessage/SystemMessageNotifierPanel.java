package gov.takx.core.systemmessage;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.logging.SystemLogLevel;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.systemmessage.ISystemMessageManagerListener;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.themes.TakThemeColor;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.STANDARD_IMAGE_SIZE;

/**
 * This panel displays the current status of the system messages.  It shows the highest message level via an icon,
 * and the number of unacknowledged messages.  When the user clicks any portion of this display, the System Message
 * View panel is toggled.  Toggling the panel to visible resets the outstanding message count.
 *
 * @since 1.43
 */
@PseudoBean(singletonObserver = true)
public class SystemMessageNotifierPanel extends JPanel implements ISystemMessageManagerListener
{
    private static final Map<SystemLogLevel, Icon> levelIcons;

    static
    {
        final BufferedImage notificationImage = UIResources.getSizedImage(PLATFORM_ICONS, "notification_system.png", STANDARD_IMAGE_SIZE, true);

        //noinspection ConstantConditions - won't be null since the default will be returned
        final BufferedImage redNotificationImage = ImageUtils.getRecoloredInstance(notificationImage, TakThemeColor.RED.getColor());
        final BufferedImage yellowNotificationImage = ImageUtils.getRecoloredInstance(notificationImage, TakThemeColor.YELLOW.getColor());
        final BufferedImage greenNotificationImage = ImageUtils.getRecoloredInstance(notificationImage, TakThemeColor.GREEN.getColor());

        levelIcons = Map.of(
                SystemLogLevel.SYSTEM_ERROR, new ImageIcon(redNotificationImage),
                SystemLogLevel.SYSTEM_WARN, new ImageIcon(yellowNotificationImage),
                SystemLogLevel.SYSTEM_INFO, new ImageIcon(greenNotificationImage),
                SystemLogLevel.SYSTEM_ALL, new ImageIcon(notificationImage));
    }

    @Inject
    private ISystemMessageManager messageManager;

    @Inject
    private IExtendedUISpaceManager uiSpaceManager;

    public SystemMessageNotifierPanel()
    {
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        messageManager.addSystemMessageManagerListener(this);
        updateIconAndCount();
    }

    /**
     * @since 3.5
     */
    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        messageManager.resetOutstandingCount();
        updateIconAndCount();
    }

    @InvokeOnEDT
    @Override
    public void outstandingCountChanged()
    {
        updateIconAndCount();
    }

    /**
     * Toggle the system messages view.  Handles user click on our components.
     */
    private void toggleMessageView()
    {
        uiSpaceManager.toggleComponent(CoreComponentType.SYSTEM_MESSAGES_PANEL);
    }

    /**
     * Update the highest level indicator icon and the count of outstanding messages.
     */
    private void updateIconAndCount()
    {
        // Update the outstanding message count, clear it if 0
        final int count = messageManager.getOutstandingCount();
        lblMessageTypeAndCount.setText(count > 0 ? String.valueOf(count) : "");

        lblMessages.setText(count == 1 ? "System Message" : "System Messages");

        // Update the highest message level icon
        lblMessageTypeAndCount.setIcon(levelIcons.get(messageManager.getHighestOutstandingLevel()));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        lblMessageTypeAndCount = new JLabel();
        lblMessages = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                toggleMessageView();
            }
        });
        setLayout(new FormLayout(
                "2*(default, $lcgap), default",
                "default"));

        //---- lblMessageTypeAndCount ----
        lblMessageTypeAndCount.setText("1");
        lblMessageTypeAndCount.setIconTextGap(0);
        lblMessageTypeAndCount.setName("lblMessageTypeAndCount");
        lblMessageTypeAndCount.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                toggleMessageView();
            }
        });
        add(lblMessageTypeAndCount, cc.xy(1, 1));

        //---- lblMessages ----
        lblMessages.setText("System Messages  ");
        lblMessages.setEnabled(false);
        lblMessages.setName("lblMessages");
        lblMessages.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                toggleMessageView();
            }
        });
        add(lblMessages, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel lblMessageTypeAndCount;
    private JLabel lblMessages;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
