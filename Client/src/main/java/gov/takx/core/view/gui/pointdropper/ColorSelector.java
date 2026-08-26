package gov.takx.core.view.gui.pointdropper;

import gov.tak.api.icons.TakColor;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

/**
 * A color swatch selection panel with the standard TAK colors to select from.
 * @since 5.3
 */
public class ColorSelector extends JPanel
{
    private static final int COLOR_SWATCH_SIZE = 35;
    private static final Dimension COLOR_BUTTON_SIZE = new Dimension(50, 50);

    /**
     * Creates a color swatch selection panel. At least one of (rows, cols) must be non-zero. If both are zero,
     * this well create a single row, multi-column panel
     * @param rows The desired number of rows, setting to 0 enables any number of rows
     * @param cols The desired number of cols, setting to 0 enables any number of cols
     * @param colorList A list of colors to display in the panel for selection
     * @param colorConsumer A consumer for the selected color
     * @since 5.4 (signature change)
     */
    public ColorSelector(int rows, int cols, List<TakColor> colorList, Consumer<TakColor> colorConsumer)
    {
        if (rows == 0 && cols == 0) rows = 1;
        setLayout(new GridLayout(rows, cols));
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        colorList.forEach(color -> {
            JButton colorButton = createColorButton(color);
            colorButton.addActionListener(e -> colorConsumer.accept(color));
            add(colorButton);
        });
    }

    /**
     * Create a colored swatch {@link JButton} based on the {@link TakColor} passed in
     * @param color The color of the button
     * @return a button with a colored square in the center
     */
    private JButton createColorButton(TakColor color)
    {
        BufferedImage colorSquare = new BufferedImage(COLOR_SWATCH_SIZE, COLOR_SWATCH_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = colorSquare.createGraphics();
        g.setPaint(new Color(color.colorValue));
        g.fillRect(0, 0, colorSquare.getWidth(), colorSquare.getHeight());
        JButton colorButton = new JButton();
        colorButton.setName(color.colorName);
        colorButton.setPreferredSize(COLOR_BUTTON_SIZE);
        colorButton.setIcon(new ImageIcon(colorSquare));
        colorButton.setBackground(null);
        colorButton.setBorderPainted(false);
        g.dispose();
        return colorButton;
    }
}
