package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.ui.AProgramPreferencesPanel;
import gov.takx.core.security.CertUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Panel to allow the user to select a specific PKI client cert to use.  Gets the list of PKI certs from
 * the Windows IE keystore.  Most of this is actually contained in the {@link CertUtils}
 * helper class.
 * <p/>
 * Note:  This only allows the CHOICE of a key, which will be stored to the user preferences.  Creating an SSLContext
 * to actually use the key is done elsewhere when the key is actually needed through CertUtils.  For example,
 * when a user selects to load a KML Overlay, the logic there should make sure to specify an SSLContext that uses
 * the user selected PKI key.
 *
 */
class PkiPreferencesPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NO_KEY_SELECTED = "None (" + PreferenceEntry.NAME.getStringValue() +
            " will attempt to pick)";

    private final AProgramPreferencesPanel parent;
    private final String originalKeyString;

    /**
     * Constructor.  Populates the combo box with human-readable versions of the key descriptions (again, through
     * CertUtils), and then selects the one currently in the user preferences.
     *
     * @param parent parent preferences panel.  {@code null} if not being instantiated through a preferences panel.
     */
    PkiPreferencesPanel(AProgramPreferencesPanel parent)
    {
        initComponents();
        this.parent = parent;
        keyComboBox.addItem(NO_KEY_SELECTED);

        try
        {
            populateKeys();
        } catch (Exception e)
        {
            logger.error("Error populating keys", e);
        }

        keyInfoTextPane.setText("No key selected");
        keyInfoTextPane.setLineWrap(true);

        String userKeyEntry = CertUtils.getUserSelectedKey();
        if (!userKeyEntry.isEmpty() && containsKey(userKeyEntry))
        {
            originalKeyString = userKeyEntry;
            try
            {
                X509Certificate x509Certificate = CertUtils.getKeyFromEntryString(userKeyEntry);
                if (x509Certificate != null)
                {
                    keyComboBox.setSelectedItem(userKeyEntry);
                    keyInfoTextPane.setText(x509Certificate.toString());
                }
            } catch (Exception e)
            {
                logger.error("Error restoring user preferred PKI key", e);
            }
        } else // key in preferences is no longer installed in windows keystore.
        {
            originalKeyString = userKeyEntry;
            keyComboBox.setSelectedIndex(0);
        }
    }

    /**
     * Method to store the selected user key into the user preferences
     */
    public void applyAction()
    {
        String entry = (String) keyComboBox.getSelectedItem();
        if (!originalKeyString.equals(entry) && !NO_KEY_SELECTED.equals(entry))
        {
            CertUtils.selectUserKey(entry);
            logger.debug("Set the new PKI Key in user prefs: {}", entry);
        }
    }

    /**
     * Get the human readable string form of the user PKI key
     *
     * @return String key description
     */
    String getSelectedKeyEntry()
    {
        return (String) keyComboBox.getSelectedItem();
    }

    private boolean containsKey(String key)
    {
        int count = keyComboBox.getItemCount();
        for (int i = 0; i < count; i++)
        {
            if (key.equals(keyComboBox.getItemAt(i)))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Populate the combo box with human readable string PKI key descriptions.
     *
     * @throws Exception exception populating key list
     */
    private void populateKeys() throws Exception
    {
        KeyStore keyStore = CertUtils.getWindowsKeystore(); // this will only work on windows.
        if (keyStore != null)
        {
            Enumeration<String> aliases = keyStore.aliases();
            if (aliases != null)
            {
                while (aliases.hasMoreElements())
                {
                    String alias = aliases.nextElement();
                    Certificate cert = keyStore.getCertificate(alias);
                    if (cert instanceof X509Certificate)
                    {
                        X509Certificate x509Certificate = (X509Certificate) cert;
                        String entry = CertUtils.createEntryStringFromCert(x509Certificate);
                        keyComboBox.addItem(entry);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Entry: {}", entry);
                            logger.debug("X509 issuer = {}", x509Certificate.getIssuerDN());
                            logger.debug("X509 subject = {}", x509Certificate.getSubjectDN());
                        }
                    }
                }
            }
        }
    }

    /**
     * Combo box selection has changed... update the text view that displays the information about the selected
     * PKI key.
     */
    private void keyComboBoxActionPerformed()
    {
        String entry = (String) keyComboBox.getSelectedItem();
        if (NO_KEY_SELECTED.equals(entry))
        {
            keyInfoTextPane.setText("No key selected");
        } else
        {
            try
            {
                X509Certificate cert = CertUtils.getKeyFromEntryString(entry);
                if (cert != null)
                {
                    keyInfoTextPane.setText(cert.toString());
                    invalidate();
                } else
                {
                    logger.error("Error matching user entry string to actual PKI key");
                }
            } catch (Exception e)
            {
                logger.error("Error getting certificate for alias {}", entry, e);
                keyInfoTextPane.setText("Error getting information for key alias " + entry);
            }
        }
        if (parent != null) parent.setPanelDirty();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var label1 = new JLabel();
        keyComboBox = new JComboBox<>();
        var scrollPane1 = new JScrollPane();
        keyInfoTextPane = new JTextArea();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelPkiPreferences");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== panel1 ========
        {
            panel1.setBorder(new TitledBorder("PKI Key"));
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $rgap, default:grow",
                    "default, $lgap, fill:default:grow"));

            //---- label1 ----
            label1.setText("Select a Key:");
            label1.setName("label1");
            panel1.add(label1, cc.xy(1, 1));

            //---- keyComboBox ----
            keyComboBox.setName("keyComboBox");
            keyComboBox.addActionListener(e -> keyComboBoxActionPerformed());
            panel1.add(keyComboBox, cc.xy(3, 1));

            //======== scrollPane1 ========
            {
                scrollPane1.setName("scrollPane1");

                //---- keyInfoTextPane ----
                keyInfoTextPane.setName("keyInfoTextPane");
                scrollPane1.setViewportView(keyInfoTextPane);
            }
            panel1.add(scrollPane1, cc.xywh(1, 3, 3, 1));
        }
        add(panel1, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<String> keyComboBox;
    private JTextArea keyInfoTextPane;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
