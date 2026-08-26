package gov.takx.core.view.gui.milsym;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.symbology.ISymbolTable;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.SymbolWrapper;
import jakarta.annotation.Nonnull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;

/**
 * A panel that shows {@link gov.tak.api.symbology.ISymbolTable.Symbol} entries in a flat list
 * according to a specified filter.
 *
 * @since 5.1
 */
public class MilSymSearchResultsPanel extends AMilSymFilterablePanel
{
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref"); // Fill, but don't grow vertically
    private static final String ENCODED_COLUMN_SPEC = "default:grow"; // Grow horizontally

    /**
     * @param milStd2525Service MIL-STD-2525 service to pull symbols from.
     * @param onSelectEntry Action to run when clicked on.
     */
    public MilSymSearchResultsPanel(@Nonnull IMilStd2525Service milStd2525Service,
                                    @Nonnull Consumer<ISymbolTable.Symbol> onSelectEntry)
    {
        super(milStd2525Service, onSelectEntry);

        initComponents();
        scrollPanel.getVerticalScrollBar().setUnitIncrement(20);
    }

    @Override
    @InvokeOnEDT
    protected void updateSearch(@Nonnull List<SymbolWrapper> symbolWrappers)
    {
        JPanel content = new JPanel();
        contentWrapper.removeAll();
        contentWrapper.add(BorderLayout.CENTER, content);

        CellConstraints constraints = new CellConstraints(1, 1);
        FormLayout formLayout;

        // Display note if there are no matching symbols.
        if (symbolWrappers.isEmpty())
        {
            formLayout = new FormLayout(ENCODED_COLUMN_SPEC);
            formLayout.appendRow(ROW_SPEC);
            constraints.xy(1, formLayout.getRowCount());

            JLabel label = new JLabel("No matching symbols");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(new EmptyBorder(10, 10, 10, 10));

            content.add(label, constraints);
            return;
        }

        // Start with no rows, rows are added dynamically
        formLayout = new FormLayout(ENCODED_COLUMN_SPEC);
        content.setLayout(formLayout);
        for (SymbolWrapper wrapper : symbolWrappers)
        {
            formLayout.appendRow(ROW_SPEC);
            constraints.xy(1, formLayout.getRowCount());
            MilSymEntryPanel entry = new MilSymEntryPanel(milStd2525Service, wrapper, adapter, onSelectEntry);
            content.add(entry, constraints);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        scrollPanel = new JScrollPane();
        contentWrapper = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== scrollPanel ========
        {
            scrollPanel.setName("scrollPanel");

            //======== contentWrapper ========
            {
                contentWrapper.setName("contentWrapper");
                contentWrapper.setLayout(new BorderLayout());
            }
            scrollPanel.setViewportView(contentWrapper);
        }
        add(scrollPanel, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JScrollPane scrollPanel;
    private JPanel contentWrapper;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
