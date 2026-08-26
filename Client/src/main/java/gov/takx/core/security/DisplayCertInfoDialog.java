package gov.takx.core.security;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.FontUtils;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Displays a certificate's information, and allows the user to select to install the cert permanently or
 * to cancel out.  Some improvements might be a button to allow the user to install the cert
 * temporarily (as opposed to permanently) which would be fairly easy to do.
 *
 */
public class DisplayCertInfoDialog extends AStandardDialog
{
    private boolean okay = false;

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static
    {
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public DisplayCertInfoDialog(X509Certificate cert, MessageDigest sha512)
    {
        initComponents();

        String subject = cert.getSubjectDN().toString();
        String[] parts = subject.split(",");
        if (parts.length > 0)
        {
            int index = parts[0].indexOf("CN=");
            if (index != -1)
            {
                issuedToCN.setText(parts[0].substring(index + 3));
            }
        }
        if (parts.length > 1)
        {
            int index = parts[1].indexOf("OU=");
            if (index != -1)
            {
                issuedToOU.setText(parts[1].substring(index + 3));
            }
        }
        if (parts.length > 2)
        {
            int index = parts[2].indexOf("O=");
            if (index != -1)
            {
                issuedToO.setText(parts[2].substring(index + 2));
            }
        }

        String issuer = cert.getIssuerDN().toString();
        parts = issuer.split(",");
        if (parts.length > 0)
        {
            int index = parts[0].indexOf("CN=");
            if (index != -1)
            {
                issuedByCN.setText(parts[0].substring(index + 3));
            }
        }
        if (parts.length > 1)
        {
            int index = parts[1].indexOf("OU=");
            if (index != -1)
            {
                issuedByOU.setText(parts[1].substring(index + 3));
            }
        }
        if (parts.length > 2)
        {
            int index = parts[2].indexOf("O=");
            if (index != -1)
            {
                issuedByO.setText(parts[2].substring(index + 2));
            }
        }

        issuedToAlgorithm.setText(cert.getSigAlgName() + ", OID = " + cert.getSigAlgOID());
        issuedToSN.setText(CertUtils.toHexString(cert.getSerialNumber().toByteArray()));

        sha512Fingerprint.setText(CertUtils.toHexString(sha512.digest()));

        validityIssued.setText(formatter.format(cert.getNotBefore()));
        validityExpires.setText(formatter.format(cert.getNotAfter()));

        // Make title borders bold (can't do it directly in JFD and keep the size relative)
        FontUtils.updateTitledBorderFont(issuedToPanel, Font.BOLD);
        FontUtils.updateTitledBorderFont(issuedByPanel, Font.BOLD);
        FontUtils.updateTitledBorderFont(validityPanel, Font.BOLD);
        FontUtils.updateTitledBorderFont(fingerprintPanel, Font.BOLD);

        pack();
    }

    public boolean isOkay()
    {
        return okay;
    }

    private void okButtonActionPerformed()
    {
        okay = true;
        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        label1 = new JLabel();
        separator1 = new JSeparator();
        issuedToPanel = new JPanel();
        label2 = new JLabel();
        issuedToCN = new JLabel();
        label3 = new JLabel();
        issuedToO = new JLabel();
        label4 = new JLabel();
        issuedToOU = new JLabel();
        label14 = new JLabel();
        issuedToAlgorithm = new JLabel();
        label5 = new JLabel();
        issuedToSN = new JLabel();
        issuedByPanel = new JPanel();
        label6 = new JLabel();
        issuedByCN = new JLabel();
        label7 = new JLabel();
        issuedByO = new JLabel();
        label8 = new JLabel();
        issuedByOU = new JLabel();
        validityPanel = new JPanel();
        label9 = new JLabel();
        validityIssued = new JLabel();
        label10 = new JLabel();
        validityExpires = new JLabel();
        fingerprintPanel = new JPanel();
        label11 = new JLabel();
        scrollPane1 = new JScrollPane();
        sha512Fingerprint = new JTextArea();
        buttonBar = new JPanel();
        label13 = new JLabel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
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
                        "2*(default, $lgap), 3*(default, $ugap), default"));

                //---- label1 ----
                label1.setText("Could not verify this certificate because the issuer is not trusted.");
                label1.putClientProperty("FlatLaf.styleClass", "large");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //---- separator1 ----
                separator1.setName("separator1");
                contentPanel.add(separator1, cc.xy(1, 3));

                //======== issuedToPanel ========
                {
                    issuedToPanel.setBorder(new TitledBorder("Issued To"));
                    issuedToPanel.setName("issuedToPanel");
                    issuedToPanel.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "4*(default, $lgap), default"));

                    //---- label2 ----
                    label2.setText("Common Name (CN):");
                    label2.setName("label2");
                    issuedToPanel.add(label2, cc.xy(1, 1));

                    //---- issuedToCN ----
                    issuedToCN.setName("issuedToCN");
                    issuedToPanel.add(issuedToCN, cc.xy(3, 1));

                    //---- label3 ----
                    label3.setText("Organization (O):");
                    label3.setName("label3");
                    issuedToPanel.add(label3, cc.xy(1, 3));

                    //---- issuedToO ----
                    issuedToO.setName("issuedToO");
                    issuedToPanel.add(issuedToO, cc.xy(3, 3));

                    //---- label4 ----
                    label4.setText("Organizational Unit (OU):");
                    label4.setName("label4");
                    issuedToPanel.add(label4, cc.xy(1, 5));

                    //---- issuedToOU ----
                    issuedToOU.setName("issuedToOU");
                    issuedToPanel.add(issuedToOU, cc.xy(3, 5));

                    //---- label14 ----
                    label14.setText("Algorithm:");
                    label14.setName("label14");
                    issuedToPanel.add(label14, cc.xy(1, 7));

                    //---- issuedToAlgorithm ----
                    issuedToAlgorithm.setName("issuedToAlgorithm");
                    issuedToPanel.add(issuedToAlgorithm, cc.xy(3, 7));

                    //---- label5 ----
                    label5.setText("Serial Number:");
                    label5.setName("label5");
                    issuedToPanel.add(label5, cc.xy(1, 9));

                    //---- issuedToSN ----
                    issuedToSN.setName("issuedToSN");
                    issuedToPanel.add(issuedToSN, cc.xy(3, 9));
                }
                contentPanel.add(issuedToPanel, cc.xy(1, 5));

                //======== issuedByPanel ========
                {
                    issuedByPanel.setBorder(new TitledBorder("Issued By"));
                    issuedByPanel.setName("issuedByPanel");
                    issuedByPanel.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "2*(default, $lgap), default"));

                    //---- label6 ----
                    label6.setText("Common Name (CN):");
                    label6.setName("label6");
                    issuedByPanel.add(label6, cc.xy(1, 1));

                    //---- issuedByCN ----
                    issuedByCN.setName("issuedByCN");
                    issuedByPanel.add(issuedByCN, cc.xy(3, 1));

                    //---- label7 ----
                    label7.setText("Organization (O):");
                    label7.setName("label7");
                    issuedByPanel.add(label7, cc.xy(1, 3));

                    //---- issuedByO ----
                    issuedByO.setName("issuedByO");
                    issuedByPanel.add(issuedByO, cc.xy(3, 3));

                    //---- label8 ----
                    label8.setText("Organizational Unit (OU):");
                    label8.setName("label8");
                    issuedByPanel.add(label8, cc.xy(1, 5));

                    //---- issuedByOU ----
                    issuedByOU.setName("issuedByOU");
                    issuedByPanel.add(issuedByOU, cc.xy(3, 5));
                }
                contentPanel.add(issuedByPanel, cc.xy(1, 7));

                //======== validityPanel ========
                {
                    validityPanel.setBorder(new TitledBorder("Validity"));
                    validityPanel.setName("validityPanel");
                    validityPanel.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default, $lgap, default"));

                    //---- label9 ----
                    label9.setText("Issued On:");
                    label9.setName("label9");
                    validityPanel.add(label9, cc.xy(1, 1));

                    //---- validityIssued ----
                    validityIssued.setName("validityIssued");
                    validityPanel.add(validityIssued, cc.xy(3, 1));

                    //---- label10 ----
                    label10.setText("Expires On:");
                    label10.setName("label10");
                    validityPanel.add(label10, cc.xy(1, 3));

                    //---- validityExpires ----
                    validityExpires.setName("validityExpires");
                    validityPanel.add(validityExpires, cc.xy(3, 3));
                }
                contentPanel.add(validityPanel, cc.xy(1, 9));

                //======== fingerprintPanel ========
                {
                    fingerprintPanel.setBorder(new TitledBorder("Fingerprint"));
                    fingerprintPanel.setName("fingerprintPanel");
                    fingerprintPanel.setLayout(new FormLayout(
                            "default, $lcgap, default:grow",
                            "default:grow"));

                    //---- label11 ----
                    label11.setText("SHA-512 Fingerprint:");
                    label11.setName("label11");
                    fingerprintPanel.add(label11, cc.xy(1, 1));

                    //======== scrollPane1 ========
                    {
                        scrollPane1.setName("scrollPane1");

                        //---- sha512Fingerprint ----
                        sha512Fingerprint.setLineWrap(true);
                        sha512Fingerprint.setName("sha512Fingerprint");
                        scrollPane1.setViewportView(sha512Fingerprint);
                    }
                    fingerprintPanel.add(scrollPane1, cc.xy(3, 1));
                }
                contentPanel.add(fingerprintPanel, cc.xy(1, 11));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$lcgap, default, $glue, $button, $rgap, $button",
                        "pref"));

                //---- label13 ----
                label13.setText("Trust this Certificate?");
                label13.putClientProperty("FlatLaf.styleClass", "large");
                label13.setName("label13");
                buttonBar.add(label13, cc.xy(2, 1));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(4, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(6, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel label1;
    private JSeparator separator1;
    private JPanel issuedToPanel;
    private JLabel label2;
    private JLabel issuedToCN;
    private JLabel label3;
    private JLabel issuedToO;
    private JLabel label4;
    private JLabel issuedToOU;
    private JLabel label14;
    private JLabel issuedToAlgorithm;
    private JLabel label5;
    private JLabel issuedToSN;
    private JPanel issuedByPanel;
    private JLabel label6;
    private JLabel issuedByCN;
    private JLabel label7;
    private JLabel issuedByO;
    private JLabel label8;
    private JLabel issuedByOU;
    private JPanel validityPanel;
    private JLabel label9;
    private JLabel validityIssued;
    private JLabel label10;
    private JLabel validityExpires;
    private JPanel fingerprintPanel;
    private JLabel label11;
    private JScrollPane scrollPane1;
    private JTextArea sha512Fingerprint;
    private JPanel buttonBar;
    private JLabel label13;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
