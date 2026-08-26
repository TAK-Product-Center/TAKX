package gov.takx.core.view.gui.maptools.tacticalgraphics;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ShapeType;
import gov.takx.api.milsym.Echelon;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.ISymbolSelection;
import gov.takx.api.milsym.Presence;
import gov.takx.core.view.gui.milsym.MilSymRootPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Displays the {@link MilSymRootPanel} with the option to cancel the selection.
 *
 * @since 5.1
 */
public class SymbolChooserPanel extends JPanel
{
    private final IMilStd2525Service milStd2525Service;
    private MilSymRootPanel milSymPanel;

    /**
     * @param milStd2525Service MIL-STD-2525 service to pull symbols from.
     */
    public SymbolChooserPanel(@Nonnull IMilStd2525Service milStd2525Service)
    {
        this.milStd2525Service = milStd2525Service;

        initComponents();
    }

    /**
     * @param shapeName Name of the shape to display in the title. Can be {@code null} for a generic title.
     * @param shapeType The shape type; the chooser UI will be limited to symbols that support this shape type. Can be {@code null} for no limiting.
     * @param initialSymbol The initial symbol to show as being selected. Can be {@code null} to start at the root of the selection tree.
     * @param initialAffiliation The initial affiliation to show for symbols. Can be {@code null} to default to {@link Affiliation#Unknown}.
     * @param cotSupport {@code true} for showing only symbols with CoT support. {@code false} to not consider CoT support.
     * @param symbolConsumer The consumer that will handle the selected symbol.
     * @param cancelListener The listener that will respond if the user cancels the selection process.
     */
    public void init(@Nullable String shapeName,
                     @Nullable ShapeType shapeType,
                     @Nullable String initialSymbol,
                     @Nullable Affiliation initialAffiliation,
                     boolean cotSupport,
                     @Nonnull Consumer<ISymbolSelection> symbolConsumer,
                     @Nonnull ActionListener cancelListener)
    {
        titleLabel.setText("Choose A New Symbol" + (shapeName == null ? "" : " For " + shapeName));
        cancelButton.addActionListener(cancelListener);

        milSymPanel = new MilSymRootPanel(milStd2525Service);
        if (shapeType != null) milSymPanel.setShapeType(shapeType, false);
        milSymPanel.setInitialSymbol(initialSymbol);
        milSymPanel.setAffiliation(Objects.requireNonNullElse(initialAffiliation, Affiliation.Unknown));
        milSymPanel.setCustomListener(symbolConsumer);
        milSymPanel.setCotSupportFiltering(cotSupport);

        add(milSymPanel, new CellConstraints(1, 3));
        revalidate();
    }

    /**
     * @param affiliation Affiliation to set.
     * @since 5.3
     */
    public void setAffiliation(@Nonnull Affiliation affiliation)
    {
        milSymPanel.setAffiliation(affiliation);
    }

    /**
     * @param echelon Echelon to set.
     * @since 5.3
     */
    public void setEchelon(@Nonnull Echelon echelon)
    {
        milSymPanel.setEchelon(echelon);
    }

    /**
     * @param presence Presence to set.
     * @since 5.3
     */
    public void setPresence(@Nonnull Presence presence)
    {
        milSymPanel.setPresence(presence);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        titleLabel = new JLabel();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "24dlu, $lgap, fill:default:grow, $lgap, 24dlu"));

        //---- titleLabel ----
        titleLabel.setText("Choose A New Symbol");
        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD));
        titleLabel.setName("titleLabel");
        add(titleLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.setName("cancelButton");
        add(cancelButton, cc.xy(1, 5, CellConstraints.CENTER, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel titleLabel;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
