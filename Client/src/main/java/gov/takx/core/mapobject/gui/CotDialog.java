package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.cot.CotFormatting;
import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.invoke.MethodHandles;

/**
 * Dialog to display a CoT event associated with a map object.
 * @since 3.2
 */
public class CotDialog extends AMapObjectDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructor.
     * @param controller The map object controller
     * @param event The CoT event to display in the dialog
     * @param title Optional title for the CoT dialog
     * @since 5.0 (title parameter added)
     */
    public CotDialog(IMapObjectController controller, @Nonnull CotEvent event, @Nullable String title)
    {
        super(controller);
        initComponents();

        if (!StringUtils.isEmpty(title)) setTitle(title);

        try
        {
            cotTextArea.setText(CotFormatting.formatEvent(event));
        } catch (IllegalStateException e)
        {
            logger.error("Error formatting CoT event", e);
        }
    }

    private void okActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var scrollPane = new JScrollPane();
        cotTextArea = new JTextArea();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Cursor on Target Source Data");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(500, 300));
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
                        "fill:default:grow"));

                //======== scrollPane ========
                {
                    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    scrollPane.setName("scrollPane");

                    //---- cotTextArea ----
                    cotTextArea.setLineWrap(true);
                    cotTextArea.setEditable(false);
                    cotTextArea.setName("cotTextArea");
                    scrollPane.setViewportView(cotTextArea);
                }
                contentPanel.add(scrollPane, cc.xy(1, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextArea cotTextArea;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
