package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;

/**
 * A base table cell renderer based on (and with most code copied from) {@link javax.swing.table.DefaultTableCellRenderer}
 * except that this class uses a {@link JPanel} rather than a {@link javax.swing.JLabel}. Designed to be used when the cell
 * needs to contain a more complex component hierarchy.
 *
 * @since 5.3
 * @see javax.swing.table.DefaultTableCellRenderer
 */
@IncubatingApi(since = "5.3")
public class DefaultJPanelTableCellRenderer extends JPanel implements TableCellRenderer, Serializable
{
    private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
    private static final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

    protected static Border noFocusBorder = DEFAULT_NO_FOCUS_BORDER;

    private Color unselectedForeground;
    private Color unselectedBackground;
    private int toolTipRow = -1;

    /**
     * Construct a new instance.
     */
    public DefaultJPanelTableCellRenderer()
    {
        setOpaque(true);
        setBorder(getNoFocusBorder());
        setName("Table.cellRenderer");
    }

    @SuppressWarnings("removal")
    private Border getNoFocusBorder()
    {
        Border border = UIManager.getBorder("Table.cellNoFocusBorder");
        if (System.getSecurityManager() != null)
        {
            if (border != null) return border;
            return SAFE_NO_FOCUS_BORDER;
        } else if (border != null)
        {
            if (noFocusBorder == null || noFocusBorder == DEFAULT_NO_FOCUS_BORDER)
            {
                return border;
            }
        }
        return noFocusBorder;
    }

    @Override
    public void setForeground(@Nullable Color c)
    {
        super.setForeground(c);
        unselectedForeground = c;
    }

    @Override
    public void setBackground(@Nullable Color c)
    {
        super.setBackground(c);
        unselectedBackground = c;
    }

    @Override
    public void updateUI()
    {
        super.updateUI();
        setForeground(null);
        setBackground(null);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column)
    {
        if (table == null)
        {
            return this;
        }

        Color fg = null;
        Color bg = null;

        JTable.DropLocation dropLocation = table.getDropLocation();
        if (dropLocation != null
                && !dropLocation.isInsertRow()
                && !dropLocation.isInsertColumn()
                && dropLocation.getRow() == row
                && dropLocation.getColumn() == column)
        {

            fg = UIManager.getColor("Table.dropCellForeground");
            bg = UIManager.getColor("Table.dropCellBackground");

            isSelected = true;
        }

        if (isSelected)
        {
            super.setForeground(fg == null ? table.getSelectionForeground() : fg);
            super.setBackground(bg == null ? table.getSelectionBackground() : bg);
        } else
        {
            Color background = unselectedBackground != null
                    ? unselectedBackground
                    : table.getBackground();
            if (background == null || background instanceof javax.swing.plaf.UIResource)
            {
                Color alternateColor = UIManager.getColor("Table.alternateRowColor");
                if (alternateColor != null && row % 2 != 0)
                {
                    background = alternateColor;
                }
            }
            super.setForeground(unselectedForeground != null
                    ? unselectedForeground
                    : table.getForeground());
            super.setBackground(background);
        }

        setFont(table.getFont());

        if (hasFocus)
        {
            Border border = null;
            if (isSelected)
            {
                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
            }
            if (border == null)
            {
                border = UIManager.getBorder("Table.focusCellHighlightBorder");
            }
            setBorder(border);

            if (!isSelected && table.isCellEditable(row, column))
            {
                Color col;
                col = UIManager.getColor("Table.focusCellForeground");
                if (col != null)
                {
                    super.setForeground(col);
                }
                col = UIManager.getColor("Table.focusCellBackground");
                if (col != null)
                {
                    super.setBackground(col);
                }
            }
        } else
        {
            setBorder(getNoFocusBorder());
        }
        return this;
    }

    /**
     * Sets the tooltip based on the children of the cell renderer. This must be called by subclasses after adding their
     * components in overridden instances of the {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}
     * method.<br/>
     * <br/>
     * DCW NOTE: If there is a better way to do this, then the magic eludes me. I recommend leaving the line by line comments
     * unless a clearer method becomes available. To explain - because cells are "stamped out"
     * by renderers, this means their child components are not technically part of the component hierarchy.
     * It also means that tooltips set on those child components are not detected or rendered. Only the tooltip
     * set on the renderer is respected, and thus it must be manually updated as the mouse moves to reflect any child tooltips.
     * Because the renderer is called repeatedly when the mouse is moving over
     * the cell, we can use that call to manually detect the child component, and transfer its tooltip to the renderer. The
     * process is not straightforward though, since the child components are not technically part of any true component
     * hierarchy. So first we must have the mouse position, which is not normally part of the
     * call to the renderer, and second, we must manually translate that position into the coordinate space of the renderer
     * to find the child component.
     *
     * @param table the table instance
     * @param row the row index
     * @param column the column index
     */
    protected void setToolTips(@Nonnull JTable table, int row, int column)
    {
        // Get the mouse position and convert it into the coordinate space of the table
        var pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo != null)
        {
            Point mouseScreenPoint = pointerInfo.getLocation();
            Dimension d = getPreferredSize();
            setBounds(0, 0, d.width, d.height);
            SwingUtilities.convertPointFromScreen(mouseScreenPoint, table);
            // Convert the table coordinate to the cell coordinate
            Rectangle cellRect = table.getCellRect(row, column, false);
            mouseScreenPoint.translate(-cellRect.x, -cellRect.y);
            // Find the deepest component of the cell
            Component deepest = SwingUtilities.getDeepestComponentAt(this, mouseScreenPoint.x, mouseScreenPoint.y);
            if (deepest instanceof JComponent component && deepest.isVisible())
            {
                // If the row changes then we need to trigger the tooltip manager to re-locate the tooltip
                if (toolTipRow != row) setToolTipText("");
                setToolTipText(component.getToolTipText());
            } else
            {
                setToolTipText("");
            }
            toolTipRow = row;
        }
    }

    @Override
    public boolean isOpaque()
    {
        Color back = getBackground();
        Component p = getParent();
        if (p != null)
        {
            p = p.getParent();
        }

        // p should now be the JTable.
        boolean colorMatch = (back != null) && (p != null) &&
                back.equals(p.getBackground()) &&
                p.isOpaque();
        return !colorMatch && super.isOpaque();
    }

    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue)
    {
    }
}