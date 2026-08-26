package gov.takx.core.view.gui.maptools.tacticalgraphics;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.event.ActionListener;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Displays the symbol name on a button. The button activates the symbol chooser.
 *
 * @since 5.1
 */
public class SymbolPanel extends JPanel
{
    private static final ImageIcon DELETE_IMAGE = UIResources.getSizedIcon(PLATFORM_ICONS, "delete.png", UIResources.ACTION_BUTTON_IMAGE_SIZE, true);

    /**
     * @param symbolName The symbol name
     * @param clearSymbolListener The listener that will fire when the symbol clear button is clicked
     * @param symbolSelectionListener The listener that will fire when the symbol selection button is clicked
     */
    public SymbolPanel(@Nonnull String symbolName, @Nonnull ActionListener clearSymbolListener, @Nonnull ActionListener symbolSelectionListener)
    {
        initComponents();

        clearSymbolButton.setIcon(DELETE_IMAGE);
        clearSymbolButton.addActionListener(e -> ActionPopupFactory.yesNo("Are you sure you want to clear 2525 attributes?",
                clearSymbolButton, () -> clearSymbolListener.actionPerformed(e)).show());
        chooseSymbolButton.setText(symbolName);
        chooseSymbolButton.addActionListener(symbolSelectionListener);
    }

    /**
     * @param enabled true if this component and its children should be enabled, false otherwise
     * @since 5.2
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        chooseSymbolButton.setEnabled(enabled);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        symbolNameLabel = new JLabel();
        clearSymbolButton = new JButton();
        chooseSymbolButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $lcgap, default, $lcgap, 100dlu",
                "fill:default"));

        //---- symbolNameLabel ----
        symbolNameLabel.setText("MilSym Details");
        symbolNameLabel.setName("symbolNameLabel");
        add(symbolNameLabel, cc.xy(1, 1));

        //---- clearSymbolButton ----
        clearSymbolButton.setName("clearSymbolButton");
        add(clearSymbolButton, cc.xy(3, 1));

        //---- chooseSymbolButton ----
        chooseSymbolButton.setText("text");
        chooseSymbolButton.setName("chooseSymbolButton");
        add(chooseSymbolButton, cc.xy(5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel symbolNameLabel;
    private JButton clearSymbolButton;
    private JButton chooseSymbolButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
