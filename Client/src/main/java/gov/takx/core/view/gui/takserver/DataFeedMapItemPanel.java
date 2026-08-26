package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.cot.event.CotEvent;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.MouseClickedListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Panel displaying a map item in {@link gov.takx.core.view.gui.takserver.DataFeedInfoPanel}.
 *
 * @since 5.3.0
 */
public class DataFeedMapItemPanel extends JPanel
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    /**
     * @param event Map item CoT model.
     * @param image Map item preview.
     * @param onClick Action to run when clicking the map item.
     */
    public DataFeedMapItemPanel(@Nonnull CotEvent event, @Nonnull BufferedImage image, @Nullable Runnable onClick)
    {
        initComponents();

        nameLabel.setText(event.getCallsign());
        iconLabel.setIcon(ImageUtils.getSquareIcon(image, ImageUtils.DEFAULT_ICON_WIDTH));
        createdDateLabel.setText(FORMATTER.format(Instant.ofEpochMilli(event.getTime().getMilliseconds())));

        if (onClick != null) addMouseListener((MouseClickedListener) e -> onClick.run());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        iconLabel = new JLabel();
        var infoPanel = new JPanel();
        nameLabel = new JLabel();
        createdDateLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new CompoundBorder(
                new EmptyBorder(3, 3, 3, 3),
                new LineBorder(UIManager.getColor("Component.disabledBorderColor"))));
        setName("this");
        setLayout(new FormLayout(
                "40px, $lcgap, 120px",
                "40px"));

        //---- iconLabel ----
        iconLabel.setName("iconLabel");
        add(iconLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.CENTER));

        //======== infoPanel ========
        {
            infoPanel.setName("infoPanel");
            infoPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, default"));

            //---- nameLabel ----
            nameLabel.setText("text");
            nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD));
            nameLabel.setName("nameLabel");
            infoPanel.add(nameLabel, cc.xy(1, 1));

            //---- createdDateLabel ----
            createdDateLabel.setText("yyyy-MM-dd");
            createdDateLabel.setName("createdDateLabel");
            infoPanel.add(createdDateLabel, cc.xy(1, 3));
        }
        add(infoPanel, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel createdDateLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
