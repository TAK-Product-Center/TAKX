package gov.takx.core.view.gui.preferences.toolbar;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarConfiguration;
import gov.takx.core.toolbar.ToolbarTab;
import jakarta.annotation.Nonnull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Renderer for a tree cell containing a portion of a {@link ToolbarConfiguration}.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationTreeCellRenderer extends DefaultTreeCellRenderer
{
    private static final CellConstraints CC = new CellConstraints();
    private static final int CHECKBOX_COLUMN = 1;
    private static final int LABEL_COLUMN = 2;
    private static final int SEPARATOR_THICKNESS = 2;

    private final JCheckBox checkBox = new JCheckBox();
    private final JLabel nameLabel = new JLabel();
    private final JPanel panel = new JPanel();
    private final JLabel placeholderLabel = new JLabel(" ");

    /**
     * Label to emulate a separator so it will be big enough for the user to see and select. We can't change the height
     * of a single separator (since this is controlled by the UI class), so the best way to render a bigger separator
     * is to render an empty label with a line border.
     */
    private final JLabel separatorLabel = new JLabel();

    protected ToolbarConfigurationTreeCellRenderer()
    {
        panel.setLayout(new FormLayout("default, default:grow", "default"));
        panel.setOpaque(false);
        checkBox.setOpaque(false);

        placeholderLabel.setPreferredSize(new Dimension(1, 1));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
        // Call super to let it figure out the correct background/foreground colors
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        boolean showCheckbox = false;
        boolean showNameLabel = false;
        boolean showSeparatorLabel = false;

        if (value instanceof ToolbarTabTreeNode tabNode)
        {
            ToolbarTab tab = tabNode.tab;
            final String identificationString = tab.getIdentificationString();
            nameLabel.setText(tab.isValid()
                    ? identificationString
                    : "<html><body><span style='text-decoration: line-through;'>" + identificationString + "</span></body></html>");
            nameLabel.setIcon(null);
            showNameLabel = true;

            checkBox.setSelected(tab.isVisible());
            showCheckbox = true;
        } else if (value instanceof ToolbarComponentTreeNode componentNode)
        {
            AToolbarComponent component = componentNode.component;
            if (component instanceof ToolbarButton button)
            {
                final String identificationString = button.getIdentificationString();
                nameLabel.setText(button.isValid()
                        ? identificationString
                        : "<html><body><span style='text-decoration: line-through;'>" + identificationString + "</span></body></html>");
                nameLabel.setIcon(button.getIcon());
                showNameLabel = true;
            } else
            {
                showSeparatorLabel = true;
            }
        } else
        {
            return placeholderLabel;
        }

        final Color foreground = getForeground();
        final Color background = getBackground();
        panel.setForeground(foreground);
        panel.setBackground(background);
        nameLabel.setForeground(foreground);
        nameLabel.setBackground(background);
        separatorLabel.setBackground(background);

        panel.removeAll();

        if (showCheckbox) panel.add(checkBox, CC.xy(CHECKBOX_COLUMN, 1));
        if (showNameLabel) panel.add(nameLabel, CC.xy(LABEL_COLUMN, 1));
        if (showSeparatorLabel)
        {
            separatorLabel.setBorder(createSeparatorBorder(foreground));
            panel.add(separatorLabel, CC.xy(LABEL_COLUMN, 1));
        }

        return panel;
    }

    /**
     * Creates a border to visually represent a separator toolbar component in the configuration dialog.
     *
     * @param color The color to use for the border
     * @return A compound border to represent a separator toolbar component in the tree
     * @since 3.4
     */
    @Nonnull
    private Border createSeparatorBorder(@Nonnull Color color)
    {
        return new CompoundBorder(
                Borders.DLU2_BORDER,
                new LineBorder(color, SEPARATOR_THICKNESS, false));
    }
}
