package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.BorderHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import java.awt.Component;

/**
 * A JXTable highlighter that draws a border around a row rather than individual cells. Since all borders are inherently cell
 * borders, this highlighter works by omitting left and right borders based on the column index.
 *
 * @since 5.6
 */
@IncubatingApi(since = "5.6")
public class RowBorderHighlighter extends BorderHighlighter
{
    protected EmptyBorder left;
    protected EmptyBorder middle;
    protected EmptyBorder right;

    /**
     * Construct an instance using the given empty border as a template.
     *
     * @param predicate the predicate for the highlighter
     * @param border the border to use as a template
     */
    public RowBorderHighlighter(@Nonnull HighlightPredicate predicate, @Nonnull EmptyBorder border)
    {
        super(predicate, border);
        var insets = border.getBorderInsets();
        left = new EmptyBorder(insets.top, insets.left, insets.bottom, 0);
        middle = new EmptyBorder(insets.top, 0, insets.bottom, 0);
        right = new EmptyBorder(insets.top, 0, insets.bottom, insets.right);
    }

    /**
     * Construct an instance using the given matte border as a template.
     *
     * @param predicate the predicate for the highlighter
     * @param border the border to use as a template
     */
    public RowBorderHighlighter(@Nonnull HighlightPredicate predicate, @Nonnull MatteBorder border)
    {
        super(predicate, border);
        var insets = border.getBorderInsets();
        left = new MatteBorder(insets.top, insets.left, insets.bottom, 0, border.getMatteColor());
        middle = new MatteBorder(insets.top, 0, insets.bottom, 0, border.getMatteColor());
        right = new MatteBorder(insets.top, 0, insets.bottom, insets.right, border.getMatteColor());
    }

    @Override
    protected Component doHighlight(Component renderer, ComponentAdapter adapter)
    {
        var table = (JXTable) adapter.getComponent();
        var border = adapter.column == 0 ? left : adapter.column == table.getColumnModel().getColumnCount() - 1 ? right : middle;
        ((JComponent) renderer).setBorder(border);
        return renderer;
    }
}
