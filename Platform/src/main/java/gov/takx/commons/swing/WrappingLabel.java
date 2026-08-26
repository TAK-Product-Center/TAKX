/*
 * Created by JFormDesigner on Thu Jun 16 15:09:38 PDT 2022
 */

package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.util.UnitConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import java.awt.Dimension;

/**
 * A Swing label that is wrapped in a panel that will set the preferred width and height to appropriate values as the
 * text is wrapped.
 *
 * @since 3.3
 */
public class WrappingLabel extends JPanel
{
    private static final String HTML_START = "<html>";
    private static final String HTML_END = "</html>";
    private static final int PREF_WIDTH_DLU = 200;
    private static final UnitConverter UNIT_CONVERTER = Sizes.getUnitConverter();

    // Dummy label for use in calculating the preferred size
    private static final JLabel LABEL_FOR_SIZING = new JLabel();

    public WrappingLabel()
    {
        initComponents();
    }

    /**
     * Set the text for the label.
     * @param text The text to set (use an empty string to clear)
     */
    public void setText(@Nonnull String text)
    {
        final StringBuilder stringBuilder = new StringBuilder(text);

        // Make sure the text is surrounded by HTML tags
        if (!text.startsWith(HTML_START)) stringBuilder.insert(0, HTML_START);
        if (!text.endsWith(HTML_END)) stringBuilder.append(HTML_END);

        final String htmlText = stringBuilder.toString();
        label.setText(htmlText);

        // Story time: the whole purpose of the rest of this method is to fix an issue that was happening with some
        // dialogs where the dialog would show the complete description in the header, but some of the components in the
        // center of the dialog would be cut off. At first, it seemed like the correct solution would just be to call
        // pack() on the dialog. However, that didn't solve the problem because the layout manager wouldn't adjust the
        // height for the label once it had chosen a dialog width that would cause the text to wrap. So, the dialog
        // would be too small overall, and any components near the bottom would be cut off.

        // Here are some things that didn't work so no one is tempted to try them in the future:
        // 1. Some online sources suggest wrapping the text in <div style="width:200px;"></div> so that the HTML view
        //    would wrap the text as part of the preferred size calculation. This works, but is ultimately ugly because
        //    it forces the text to wrap at that width and if the layout manager ultimately chooses a larger width (to
        //    accommodate other components), the text will wrap too early and never adjust to use the extra space.
        // 2. Another idea is switching the use of a JLabel to a JTextArea (and just styling it like a label). However,
        //    JTextArea doesn't support HTML text, which we need for some of our descriptions, so that was out.
        // 3. JTextPane _does_ support HTML (as long as you set the contentType to "text/html"), but using it doesn't
        //    solve the issue due to how layout sizes are determined once and not updated during layout if a dimension
        //    ends up being smaller than desired.
        // 4. I tried setting the preferred size calculated below directly on the label. This has the same issue as #1.

        // Dialog units are more versatile for managing UI sizes, which is why I am using DLU to specify the starting
        // preferred width and converting to pixels instead of just hard-coding a pixel value.
        final int prefWidthPx = UNIT_CONVERTER.dialogUnitXAsPixel(PREF_WIDTH_DLU, getParent());
        Dimension prefSize = getPreferredSize(htmlText, prefWidthPx);

        // Update the layout for the row containing the label so the height will be large enough for the wrapped text.
        // The one downside of doing it this way: if the user resizes the dialog manually, the row will not shrink below
        // this size even if the text no longer takes up the whole space. I can live with this since the dialog at least
        // starts with everything at a good size, which it wasn't before.
        final FormLayout layout = (FormLayout) getLayout();
        final RowSpec rowSpec = RowSpec.decode("[" + prefSize.height + "px,pref]");
        layout.setRowSpec(1, rowSpec);
    }

    /**
     * Given a preferred width, determines the appropriate dimension for a label containing the provided HTML text so
     * that when the text wraps, the text will not be cut off.
     *
     * @param htmlText    The HTML text to be displayed in the label
     * @param prefWidthPx The preferred with for the label, in pixels
     * @return Preferred size for the label containing the wrapped HTML text.
     * @see <a href="https://stackoverflow.com/a/1048348">Copied from this Stack Overflow answer</a>
     */
    private static Dimension getPreferredSize(String htmlText, int prefWidthPx)
    {
        LABEL_FOR_SIZING.setText(htmlText);

        View view = (View) LABEL_FOR_SIZING.getClientProperty(BasicHTML.propertyKey);
        view.setSize(prefWidthPx, 0);

        float w = view.getPreferredSpan(View.X_AXIS);
        float h = view.getPreferredSpan(View.Y_AXIS);

        return new Dimension((int) Math.ceil(w), (int) Math.ceil(h));
    }

    /**
     * Set the icon for the label.
     *
     * @param icon The icon to set (may be {@code null} to clear)
     */
    public void setIcon(@Nullable Icon icon)
    {
        label.setIcon(icon);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        label = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "[pref,200dlu]:grow",
                "default"));

        //---- label ----
        label.setName("label");
        add(label, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel label;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
