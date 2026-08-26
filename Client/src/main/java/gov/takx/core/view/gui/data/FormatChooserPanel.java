package gov.takx.core.view.gui.data;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.io.Format;
import gov.takx.api.plugin.io.IImportExportWizard;
import jakarta.annotation.Nonnull;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wizard panel to allow the user to choose a format for import or export.
 *
 */

public class FormatChooserPanel extends JPanel
{
    public static final String SELECTED_FORMAT_PROPERTY = "selectedFormat";

    public FormatChooserPanel(List<? extends IImportExportWizard> items)
    {
        List<FormatWrapper> formats = new ArrayList<>();

        for (IImportExportWizard wizard : items)
        {
            for (Format format : wizard.getFormats())
            {
                if (format != null)
                {
                    formats.add(new FormatWrapper(format, wizard));
                }
            }
        }

        Collections.sort(formats);

        initComponents(formats.toArray(new FormatWrapper[0]));
    }

    private void connectionListValueChanged()
    {
        if (formatList.getSelectedIndex() >= 0)
        {
            Object newValue = formatList.getSelectedValue();
            firePropertyChange(SELECTED_FORMAT_PROPERTY, null, newValue);
        }
    }

    private void initComponents(FormatWrapper[] listItems)
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var label1 = new JLabel();
        var scrollPane1 = new JScrollPane();
        formatList = new JList<>(listItems);
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, default"));

        //---- label1 ----
        label1.setText("Please select an Import/Export format:");
        label1.setName("label1");
        add(label1, cc.xy(1, 1));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- formatList ----
            formatList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            formatList.setVisibleRowCount(12);
            formatList.setName("formatList");
            formatList.addListSelectionListener(e -> connectionListValueChanged());
            scrollPane1.setViewportView(formatList);
        }
        add(scrollPane1, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JList<FormatWrapper> formatList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    static class FormatWrapper implements Comparable<FormatWrapper>
    {
        public Format format;
        public IImportExportWizard wizard;

        public FormatWrapper(Format format, IImportExportWizard wizard)
        {
            this.format = format;
            this.wizard = wizard;
        }

        public String toString()
        {
            return format.toString();
        }

        @Override
        public int compareTo(@Nonnull FormatWrapper o)
        {
            return format.getTitle().compareTo(o.format.getTitle());
        }
    }
}
