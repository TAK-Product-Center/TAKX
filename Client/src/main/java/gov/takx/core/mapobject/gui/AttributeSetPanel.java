package gov.takx.core.mapobject.gui;

import gov.tak.api.util.AttributeSet;
import gov.takx.api.plugin.IMutableRaptorPanel;
import gov.takx.platform.ui.UIResources;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.net.URL;
import java.util.Arrays;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Panel to display the key/value pairs in an {@link AttributeSet}.
 * Individual cells are selectable, and can be copied using {@code Ctrl+C}.
 *
 * @since 5.5.1
 */
public class AttributeSetPanel extends JPanel implements IMutableRaptorPanel
{
    private static final URL SKULL_URL = UIResources.getUrl(TAK_ASSETS_PNG, "Skull-Logo_48x48.png");

    private final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Key", "Type", "Value"}, 0);

    /**
     * @param attributeSet Attribute set to display
     */
    public AttributeSetPanel(AttributeSet attributeSet)
    {
        super(new BorderLayout());

        final var attributesTable = new JTable();
        attributesTable.setName("attributesTable");
        attributesTable.setCellSelectionEnabled(true);

        add(new JScrollPane(attributesTable), BorderLayout.CENTER);

        addAttributeRows(attributeSet, 0);
        attributesTable.setModel(tableModel);

        final var typeColumn = attributesTable.getColumnModel().getColumn(1);
        typeColumn.setMaxWidth(120);
        typeColumn.setPreferredWidth(80);
    }

    /**
     * Add a table row for each key/value pair in the given {@code attributeSet}.  If an attribute value is of type
     * {@link gov.tak.api.util.AttributeSetBase.AttributeType#ATTRIBUTE_SET ATTRIBUTE_SET}, then this method is
     * invoked recursively to expand those attribute key/value pairs.
     *
     * @param attributeSet Attribute set to process
     * @param indent indentation level for nested attribute sets
     */
    private void addAttributeRows(AttributeSet attributeSet, int indent)
    {
        final var keys = attributeSet.getAttributeNames().stream().sorted().toList();
        for (String key : keys)
        {
            final String keyPrefix = indent > 0 ? "    ".repeat(indent) + "./" : "";
            final String keyText = keyPrefix + key;
            final var attributeType = attributeSet.getAttributeType(key);
            final var type = attributeType.toString();

            switch (attributeType)
            {
                case ATTRIBUTE_SET ->
                {
                    tableModel.addRow(new String[]{keyText, type, ""});
                    addAttributeRows(attributeSet.getAttributeSetAttribute(key), indent + 1);
                }
                case BLOB -> tableModel.addRow(new String[]{keyText, type, Arrays.toString(attributeSet.getBinaryAttribute(key))});
                case BLOB_ARRAY -> tableModel.addRow(new String[]{keyText, type, Arrays.toString(attributeSet.getBinaryArrayAttribute(key))});
                case BOOLEAN -> tableModel.addRow(new String[]{keyText, type, String.valueOf(attributeSet.getBooleanAttribute(key))});
                case DOUBLE -> tableModel.addRow(new String[]{keyText, type, String.valueOf(attributeSet.getDoubleAttribute(key))});
                case DOUBLE_ARRAY -> tableModel.addRow(new String[]{keyText, type, Arrays.toString(attributeSet.getDoubleArrayAttribute(key))});
                case INTEGER -> tableModel.addRow(new String[]{keyText, type, String.valueOf(attributeSet.getIntAttribute(key))});
                case INTEGER_ARRAY -> tableModel.addRow(new String[]{keyText, type, Arrays.toString(attributeSet.getIntArrayAttribute(key))});
                case LONG -> tableModel.addRow(new String[]{keyText, type, String.valueOf(attributeSet.getLongAttribute(key))});
                case LONG_ARRAY -> tableModel.addRow(new String[]{keyText, type, Arrays.toString(attributeSet.getLongArrayAttribute(key))});
                case STRING -> tableModel.addRow(new String[]{keyText, type, attributeSet.getStringAttribute(key)});
                case STRING_ARRAY -> tableModel.addRow(new String[]{keyText, type, Arrays.toString(attributeSet.getStringArrayAttribute(key))});
                default -> tableModel.addRow(new String[]{keyText, type, "<unknown value type>"});
            }
        }
    }

    @Override
    public void applyAction()
    {
        // Read-only display
    }

    @Override
    public JPanel getPanel()
    {
        return this;
    }

    @Override
    public String getTitle()
    {
        return "Attribute Set";
    }

    @Override
    public URL getIcon()
    {
        return SKULL_URL;
    }

    @Override
    public String validateContents()
    {
        return null;
    }
}
