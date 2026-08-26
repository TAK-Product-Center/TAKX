package gov.takx.commons.swing;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 * <p>
 * Copied from <a href="https://tips4java.wordpress.com/2008/11/06/wrap-layout/">Java Tips Weblog: Wrap Layout</a>.
 *
 * @author <a href="https://tips4java.wordpress.com/">Rob Camick</a>
 * @since 3.0
 */
public class WrapLayout extends FlowLayout
{
    private final int defaultWidth;

    /**
     * Constructs a new {@code WrapLayout} with a left alignment and a default 5-unit horizontal and vertical gap.
     *
     * @param defaultWidth The default width to use when one hasn't been set yet
     */
    public WrapLayout(int defaultWidth)
    {
        super(FlowLayout.LEFT, 5, 5);
        this.defaultWidth = defaultWidth;
    }

    /**
     * Returns the preferred dimensions for this layout given the <i>visible</i> components in the specified target
     * container.
     *
     * @param target the component which needs to be laid out
     * @return the preferred dimensions to lay out the subcomponents of the specified container
     */
    @Override
    public Dimension preferredLayoutSize(Container target)
    {
        return layoutSize(target, true);
    }

    /**
     * Returns the minimum dimensions needed to layout the <i>visible</i> components contained in the specified target
     * container.
     *
     * @param target the component which needs to be laid out
     * @return the minimum dimensions to lay out the subcomponents of the specified container
     */
    @Override
    public Dimension minimumLayoutSize(Container target)
    {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    /**
     * Returns the minimum or preferred dimension needed to layout the target container.
     *
     * @param target    target to get layout size for
     * @param preferred should preferred size be calculated
     * @return the dimension to layout the target container
     */
    private Dimension layoutSize(Container target, boolean preferred)
    {
        synchronized (target.getTreeLock())
        {
            // Each row must fit with the width allocated to the container. When the container width = 0, the preferred
            // width of the container has not yet been calculated so let's use the default provided on creation.
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null)
            {
                container = container.getParent();
            }

            int targetWidth = container.getSize().width;
            if (targetWidth == 0) targetWidth = defaultWidth;

            int hGap = getHgap();
            int vGap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hGap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            // Fit components into the allowed width
            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int numMembers = target.getComponentCount();

            for (int i = 0; i < numMembers; i++)
            {
                Component m = target.getComponent(i);

                if (m.isVisible())
                {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    // Can't add the component to current row. Start a new row.
                    if (rowWidth + d.width > maxWidth)
                    {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    // Add a horizontal gap for all components after the first
                    if (rowWidth != 0)
                    {
                        rowWidth += hGap;
                    }

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vGap * 2;

            // When using a scroll pane or the DecoratedLookAndFeel we need to make sure the preferred size is less than
            // the size of the target container so shrinking the container size works correctly. Removing the horizontal
            // gap is an easy way to do this.
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

            if (scrollPane != null && target.isValid())
            {
                dim.width -= (hGap + 1);
            }

            return dim;
        }
    }

    /**
     * A new row has been completed. Use the dimensions of this row to update the preferred size for the container.
     *
     * @param dim       update the width and height when appropriate
     * @param rowWidth  the width of the row to add
     * @param rowHeight the height of the row to add
     */
    private void addRow(Dimension dim, int rowWidth, int rowHeight)
    {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) dim.height += getVgap();

        dim.height += rowHeight;
    }
}
