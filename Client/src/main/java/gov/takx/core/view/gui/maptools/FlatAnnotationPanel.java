package gov.takx.core.view.gui.maptools;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwindx.examples.util.ShapeUtils;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.platform.globe.GlobeTextUtils;
import gov.takx.platform.ui.FontUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import net.java.dev.colorchooser.ColorChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_BG_COLOR;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_BG_FILL_ENABLED;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_BORDER_COLOR;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_BORDER_THICKNESS;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_ENABLE_BORDER;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_FONT;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_OPACITY;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_ORIENTATION;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_SIZE;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_TEXT;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.FLAT_ANNOTATION_TEXT_COLOR;
import static gov.takx.api.mapobject.IFlatAnnotationMapObject.PROPERTIES_TO_WATCH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
public class FlatAnnotationPanel extends APropertiesEditorPanel<IFlatAnnotationMapObject>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final URL ICON_URL = UIResources.getUrl(PLATFORM_ICONS, "text_box.png");

    protected static final List<String> FONT_LIST =
            Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

    // font size determines the object's resolution - don't change it in the text area
    private static final float TEXT_AREA_FONT_SIZE = FontUtils.DEFAULT_FONT_SIZE;
    private static final ImageIcon LEFT_ALIGN_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "align_left.png");
    private static final ImageIcon CENTER_ALIGN_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "align_center.png");
    private static final ImageIcon RIGHT_ALIGN_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "align_right.png");

    // The font is a reflection of the annotation's resolution - we shouldn't display the changes in the text area.
    private Font actualFont = GlobeTextUtils.DEFAULT_FLAT_ANNOTATION_FONT;

    /**
     * Constructor used when this panel is in the right-side properties panel for initial creation of an annotation.
     */
    public FlatAnnotationPanel()
    {
        this(null, true, null);
    }

    /**
     * Constructor used when this panel is in the right-side properties panel to edit an existing annotation.
     */
    public FlatAnnotationPanel(@Nonnull IFlatAnnotationMapObject annotation)
    {
        this(annotation, true, null);
    }

    /**
     * Constructor used when this panel is being used in the Preferences dialog.
     *
     * @param annotation Annotation to edit
     * @param panelStateListener Listener for "panel dirty" events
     */
    public FlatAnnotationPanel(@Nonnull IFlatAnnotationMapObject annotation, @Nonnull IPanelStateListener panelStateListener)
    {
        this(annotation, false, panelStateListener);
    }

    private FlatAnnotationPanel(@Nullable IFlatAnnotationMapObject annotation, boolean directActionMode,
                                @Nullable IPanelStateListener panelStateListener)
    {
        super("Flat Annotation", annotation, directActionMode, panelStateListener);

        setupControls();

        if (annotation != null) setMapObject(annotation);
    }

    @PostConstruct
    private void postConstruct()
    {
        registerForEvents();
    }

    @Override
    public void releaseResources()
    {
        if (target != null) target.removePropertyChangeListener(PROPERTIES_TO_WATCH, this);
        super.releaseResources();
    }

    @Override
    public URL getIcon()
    {
        return ICON_URL;
    }

    /**
     * Set the UI control values from the given Map Object.
     */
    private void setMapObject(IFlatAnnotationMapObject mapObject)
    {
        updatingPanel = true;

        target = mapObject;

        final Font font = mapObject.getFont();
        actualFont = font;

        textArea.setText(mapObject.getText());
        textArea.setFont(font.deriveFont(TEXT_AREA_FONT_SIZE));
        textArea.setForeground(mapObject.getTextColor());
        if (mapObject.isBackgroundFillEnabled())
        {
            textArea.setBackground(mapObject.getBackgroundColor());
        }

        setDocumentAlignment(mapObject.getTextAlignment());
        setDocumentAlignmentButton(mapObject.getTextAlignment());

        fontComboBox.setSelectedItem(font.getFamily());
        boldCheckBox.setSelected(font.isBold());
        italicCheckBox.setSelected(font.isItalic());
        textColorChooser.setColor(mapObject.getTextColor());
        backgroundColorChooser.setColor(mapObject.getBackgroundColor());
        enableBackgroundFillCheckBox.setSelected(mapObject.isBackgroundFillEnabled());
        borderColorChooser.setColor(mapObject.getBorderColor());
        orientationSpinner.setValue(mapObject.getOrientation());
        enableBorderCheckBox.setSelected(mapObject.isBorderEnabled());
        opacitySpinner.setValue((int) (mapObject.getOpacity() * 100d));
        sizeSpinner.setValue(mapObject.getSize());

        updatingPanel = false;

        mapObject.addPropertyChangeListener(PROPERTIES_TO_WATCH, this);
    }

    @Override
    protected void initializeNewInstance()
    {
        // Scale the text size to the current viewport
        target.setSize(ShapeUtils.getViewportScaleFactor(getWwd()) / 2.5);
    }

    @Override
    protected void updateTargetProperties()
    {
        target.setBackgroundColor(backgroundColorChooser.getColor());
        target.setBackgroundFillEnabled(enableBackgroundFillCheckBox.isSelected());
        target.setBorderColor(borderColorChooser.getColor());
        target.setBorderThickness((Float) borderThicknessSpinner.getValue());
        target.setBorderEnabled(enableBorderCheckBox.isSelected());
        target.setFont(actualFont);
        target.setOpacity((Integer) opacitySpinner.getValue() / 100.0);
        target.setOrientation((Double) orientationSpinner.getValue());
        target.setSize((Double) sizeSpinner.getValue());
        target.setText(textArea.getText());
        target.setTextAlignment(getAlignmentKey());
        target.setTextColor(textColorChooser.getColor());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();
        final Object value = evt.getNewValue();

        logger.debug("property change: ignore {} : {}={}", ignoreEvent, propertyName, value);

        if (ignoreEvent) return;

        updatingPanel = true;

        // update UI to match the updated property
        switch (propertyName)
        {
            case FLAT_ANNOTATION_TEXT -> textArea.setText((String) value);
            case FLAT_ANNOTATION_FONT -> updateFont((Font) value);
            case FLAT_ANNOTATION_TEXT_COLOR -> textColorChooser.setColor((Color) value);
            case FLAT_ANNOTATION_BG_COLOR -> backgroundColorChooser.setColor((Color) value);
            case FLAT_ANNOTATION_BORDER_COLOR -> borderColorChooser.setColor((Color) value);
            case FLAT_ANNOTATION_ENABLE_BORDER -> enableBorderCheckBox.setSelected((boolean) value);
            case FLAT_ANNOTATION_OPACITY -> opacitySpinner.setValue((int) ((double) value * 100d));
            case FLAT_ANNOTATION_SIZE -> sizeSpinner.setValue(value);
            case FLAT_ANNOTATION_BORDER_THICKNESS -> borderThicknessSpinner.setValue(value);
            case FLAT_ANNOTATION_BG_FILL_ENABLED -> enableBackgroundFillCheckBox.setSelected((boolean) value);
            case FLAT_ANNOTATION_ORIENTATION -> orientationSpinner.setValue(value);
            default -> logger.info("Received unexpected property change event: {}", evt);
        }

        updatingPanel = false;
    }

    private void updateFont(Font font)
    {
        actualFont = font;
        textArea.setFont(actualFont.deriveFont(TEXT_AREA_FONT_SIZE));
        fontComboBox.setSelectedItem(font.getFamily());
        boldCheckBox.setSelected(font.isBold());
        italicCheckBox.setSelected(font.isItalic());
    }

    private boolean isBackgroundFillEnabled()
    {
        return enableBackgroundFillCheckBox.isSelected();
    }

    private void updateTextAreaBackgroundColor()
    {
        if (isBackgroundFillEnabled())
        {
            textArea.setBackground(getColorWithOpacity());
        } else
        {
            textArea.setBackground(null);
        }
    }

    /**
     * Changes the annotation's Font Style
     *
     * @param style   The int representing the style parameter
     * @param enabled whether it is "on" or "off"
     */
    private void setFontStyle(int style, boolean enabled)
    {
        Font oldFont = actualFont;
        int newFontStyle = enabled ? oldFont.getStyle() | style : oldFont.getStyle() ^ style;
        Font newFont = oldFont.deriveFont(newFontStyle);

        updateProperty(() -> target.setFont(newFont));

        actualFont = newFont;
        textArea.setFont(actualFont.deriveFont(TEXT_AREA_FONT_SIZE));
    }

    /**
     * Changes the text color of the annotation.
     */
    private void setTextColor(Color color)
    {
        updateProperty(() -> target.setTextColor(color));

        // Work around for bug where color won't set for empty String
        if (textArea.getText().isEmpty())
        {
            textArea.setText(" ");
            textArea.setForeground(color);
            textArea.setText("");
        } else
        {
            textArea.setForeground(color);
        }
    }

    /**
     * Changes the background color of the annotation.
     */
    private void setBackgroundColor(Color color)
    {
        updateProperty(() -> target.setBackgroundColor(color));
        updateTextAreaBackgroundColor();
    }

    private Color getColorWithOpacity()
    {
        Color c = backgroundColorChooser.getColor();
        double opacity = ((Integer) opacitySpinner.getValue()) / 100.0;
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (opacity * 255));
    }

    /**
     * Changes the border color of the annotation.
     */
    private void setBorderColor(Color color)
    {
        updateProperty(() -> target.setBorderColor(color));
    }

    /**
     * Changes the annotation's opacity.
     */
    private void opacitySpinnerStateChanged()
    {
        updateProperty(() -> target.setOpacity((Integer) opacitySpinner.getValue() / 100.0));
        updateTextAreaBackgroundColor();
    }

    private void enableBackgroundFillCheckBoxActionPerformed()
    {
        updateProperty(() -> target.setBackgroundFillEnabled(isBackgroundFillEnabled()));
        updateTextAreaBackgroundColor();
    }

    private void boldCheckBoxActionPerformed()
    {
        setFontStyle(Font.BOLD, boldCheckBox.isSelected());
    }

    private void italicCheckBoxActionPerformed()
    {
        setFontStyle(Font.ITALIC, italicCheckBox.isSelected());
    }

    /**
     * Sets the annotation's font.
     */
    private void fontComboBoxActionPerformed()
    {
        final String newFontFamily = String.valueOf(fontComboBox.getSelectedItem());
        final Font newFont = new Font(newFontFamily, actualFont.getStyle(), actualFont.getSize());
        actualFont = newFont;
        textArea.setFont(newFont);

        updateProperty(() -> target.setFont(newFont));
    }

    /**
     * Sets the annotation's text.
     */
    private void textAreaCaretUpdate()
    {
        updateProperty(() -> target.setText(textArea.getText()));
    }

    /**
     * Set the annotation size in meters.
     */
    private void sizeSpinnerStateChanged()
    {
        updateProperty(() -> target.setSize((Double) sizeSpinner.getValue()));
    }

    /**
     * Set the annotation orientation angle in degrees
     */
    private void orientationSpinnerStateChanged()
    {
        updateProperty(() -> target.setOrientation((Double) orientationSpinner.getValue()));
    }

    /**
     * Enable or Disable the annotation border.
     */
    private void enableBorderCheckBoxActionPerformed()
    {
        updateProperty(() -> target.setBorderEnabled(enableBorderCheckBox.isSelected()));
    }

    /**
     * Set the annotation border size in pixels.
     */
    private void borderThicknessSpinnerStateChanged()
    {
        updateProperty(() -> target.setBorderThickness((Float) borderThicknessSpinner.getValue()));
    }

    private void leftAlignmentActionPerformed()
    {
        updateAlignment();
    }

    private void centerAlignmentActionPerformed()
    {
        updateAlignment();
    }

    private void rightAlignmentActionPerformed()
    {
        updateAlignment();
    }

    private void setDocumentAlignment(String alignment)
    {
        final int alignmentStyle = switch (alignment)
        {
            case AVKey.LEFT -> StyleConstants.ALIGN_LEFT;
            case AVKey.CENTER -> StyleConstants.ALIGN_CENTER;
            case AVKey.RIGHT -> StyleConstants.ALIGN_RIGHT;
            default -> throw new IllegalStateException("Unknown alignment: " + alignment);
        };

        SimpleAttributeSet alignmentSet = new SimpleAttributeSet();
        StyleConstants.setAlignment(alignmentSet, alignmentStyle);

        StyledDocument doc = textArea.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), alignmentSet, false);
    }

    private void setDocumentAlignmentButton(String alignment)
    {
        switch (alignment)
        {
            case AVKey.LEFT -> leftAlignment.setSelected(true);
            case AVKey.CENTER -> centerAlignment.setSelected(true);
            case AVKey.RIGHT -> rightAlignment.setSelected(true);
            default -> throw new IllegalStateException("Unknown alignment: " + alignment);
        }
    }

    private void updateAlignment()
    {
        final String alignment = getAlignmentKey();

        updateProperty(() -> target.setTextAlignment(alignment));
        setDocumentAlignment(alignment);
    }

    @Nonnull
    private String getAlignmentKey()
    {
        final String alignment;
        if (leftAlignment.isSelected())
        {
            alignment = AVKey.LEFT;
        } else if (centerAlignment.isSelected())
        {
            alignment = AVKey.CENTER;
        } else if (rightAlignment.isSelected())
        {
            alignment = AVKey.RIGHT;
        } else
            throw new IllegalStateException("Alignment button not selected");
        return alignment;
    }

    protected void setupControls()
    {
        initComponents();
        leftAlignment.setIcon(LEFT_ALIGN_ICON);
        centerAlignment.setIcon(CENTER_ALIGN_ICON);
        rightAlignment.setIcon(RIGHT_ALIGN_ICON);

        updatingPanel = true;

        for (String font : FONT_LIST) fontComboBox.addItem(font);

        textArea.setDocument(new LimitedDocument(2048));
        fontComboBox.setSelectedItem(GlobeTextUtils.DEFAULT_FLAT_ANNOTATION_FONT.getFamily());

        textColorChooser.setColor(textArea.getForeground());
        backgroundColorChooser.setColor(textArea.getBackground());

        setupColorChooser(textColorChooser, this::setTextColor);
        setupColorChooser(backgroundColorChooser, this::setBackgroundColor);
        setupColorChooser(borderColorChooser, this::setBorderColor);

        setDocumentAlignment(AVKey.CENTER);

        updatingPanel = false;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var toolbar = new JPanel();
        leftAlignment = new JToggleButton();
        centerAlignment = new JToggleButton();
        rightAlignment = new JToggleButton();
        textLabel = new JLabel();
        var scrollPane1 = new JScrollPane();
        textArea = new JTextPane();
        var label8 = new JLabel();
        sizeSpinner = new JSpinner();
        var label1 = new JLabel();
        fontComboBox = new JComboBox<>();
        var label2 = new JLabel();
        panel1 = new JPanel();
        boldCheckBox = new JCheckBox();
        italicCheckBox = new JCheckBox();
        var label4 = new JLabel();
        textColorChooser = new ColorChooser();
        var label7 = new JLabel();
        backgroundColorChooser = new ColorChooser();
        var label12 = new JLabel();
        opacitySpinner = new JSpinner();
        var label11 = new JLabel();
        enableBackgroundFillCheckBox = new JCheckBox();
        var label6 = new JLabel();
        borderColorChooser = new ColorChooser();
        var label10 = new JLabel();
        borderThicknessSpinner = new JSpinner();
        var label9 = new JLabel();
        enableBorderCheckBox = new JCheckBox();
        var orientationLabel = new JLabel();
        orientationSpinner = new JSpinner();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "right:default, $lcgap, default:grow",
                "default, $lgap, 70dlu, 11*($lgap, default)"));

        //======== toolbar ========
        {
            toolbar.setName("toolbar");
            toolbar.setLayout(new FormLayout(
                    "2*(default, $lcgap), default",
                    "fill:default"));

            //---- leftAlignment ----
            leftAlignment.setToolTipText("Left alignment");
            leftAlignment.setName("leftAlignment");
            leftAlignment.addActionListener(e -> leftAlignmentActionPerformed());
            toolbar.add(leftAlignment, cc.xy(1, 1));

            //---- centerAlignment ----
            centerAlignment.setSelected(true);
            centerAlignment.setToolTipText("Center alignment");
            centerAlignment.setName("centerAlignment");
            centerAlignment.addActionListener(e -> centerAlignmentActionPerformed());
            toolbar.add(centerAlignment, cc.xy(3, 1));

            //---- rightAlignment ----
            rightAlignment.setToolTipText("Right alignment");
            rightAlignment.setName("rightAlignment");
            rightAlignment.addActionListener(e -> rightAlignmentActionPerformed());
            toolbar.add(rightAlignment, cc.xy(5, 1));
        }
        add(toolbar, cc.xy(3, 1));

        //---- textLabel ----
        textLabel.setText("Text:");
        textLabel.setName("textLabel");
        add(textLabel, cc.xy(1, 3));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- textArea ----
            textArea.setName("textArea");
            textArea.addCaretListener(e -> textAreaCaretUpdate());
            scrollPane1.setViewportView(textArea);
        }
        add(scrollPane1, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.FILL));

        //---- label8 ----
        label8.setText("Line Height (m):");
        label8.setName("label8");
        add(label8, cc.xy(1, 5));

        //---- sizeSpinner ----
        sizeSpinner.setModel(new SpinnerNumberModel(1000.0, 0.0, null, 100.0));
        sizeSpinner.setName("sizeSpinner");
        sizeSpinner.addChangeListener(e -> sizeSpinnerStateChanged());
        add(sizeSpinner, cc.xy(3, 5));

        //---- label1 ----
        label1.setText("Font:");
        label1.setName("label1");
        add(label1, cc.xy(1, 7));

        //---- fontComboBox ----
        fontComboBox.setName("fontComboBox");
        fontComboBox.addActionListener(e -> fontComboBoxActionPerformed());
        add(fontComboBox, cc.xy(3, 7));

        //---- label2 ----
        label2.setText("Font Style:");
        label2.setName("label2");
        add(label2, cc.xy(1, 9));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default",
                    "default"));

            //---- boldCheckBox ----
            boldCheckBox.setText("Bold");
            boldCheckBox.setName("boldCheckBox");
            boldCheckBox.addActionListener(e -> boldCheckBoxActionPerformed());
            panel1.add(boldCheckBox, cc.xy(1, 1));

            //---- italicCheckBox ----
            italicCheckBox.setText("Italic");
            italicCheckBox.setName("italicCheckBox");
            italicCheckBox.addActionListener(e -> italicCheckBoxActionPerformed());
            panel1.add(italicCheckBox, cc.xy(3, 1));
        }
        add(panel1, cc.xy(3, 9));

        //---- label4 ----
        label4.setText("Text Color:");
        label4.setName("label4");
        add(label4, cc.xy(1, 11));

        //---- textColorChooser ----
        textColorChooser.setColor(Color.black);
        textColorChooser.setName("textColorChooser");
        add(textColorChooser, cc.xy(3, 11));

        //---- label7 ----
        label7.setText("Background Color:");
        label7.setName("label7");
        add(label7, cc.xy(1, 13));

        //---- backgroundColorChooser ----
        backgroundColorChooser.setColor(Color.white);
        backgroundColorChooser.setName("backgroundColorChooser");
        add(backgroundColorChooser, cc.xy(3, 13));

        //---- label12 ----
        label12.setText("Fill Opacity:");
        label12.setName("label12");
        add(label12, cc.xy(1, 15));

        //---- opacitySpinner ----
        opacitySpinner.setModel(new SpinnerNumberModel(80, 0, 100, 1));
        opacitySpinner.setName("opacitySpinner");
        opacitySpinner.addChangeListener(e -> opacitySpinnerStateChanged());
        add(opacitySpinner, cc.xy(3, 15));

        //---- label11 ----
        label11.setText("Fill Background:");
        label11.setName("label11");
        add(label11, cc.xy(1, 17));

        //---- enableBackgroundFillCheckBox ----
        enableBackgroundFillCheckBox.setText("On");
        enableBackgroundFillCheckBox.setName("enableBackgroundFillCheckBox");
        enableBackgroundFillCheckBox.addActionListener(e -> enableBackgroundFillCheckBoxActionPerformed());
        add(enableBackgroundFillCheckBox, cc.xy(3, 17));

        //---- label6 ----
        label6.setText("Border Color:");
        label6.setName("label6");
        add(label6, cc.xy(1, 19));

        //---- borderColorChooser ----
        borderColorChooser.setColor(Color.white);
        borderColorChooser.setName("backgroundColorChooser");
        add(borderColorChooser, cc.xy(3, 19));

        //---- label10 ----
        label10.setText("Border Thickness");
        label10.setName("label10");
        add(label10, cc.xy(1, 21));

        //---- borderThicknessSpinner ----
        borderThicknessSpinner.setModel(new SpinnerNumberModel(3.0F, 0.0F, null, 1.0F));
        borderThicknessSpinner.setName("borderThicknessSpinner");
        borderThicknessSpinner.addChangeListener(e -> borderThicknessSpinnerStateChanged());
        add(borderThicknessSpinner, cc.xy(3, 21));

        //---- label9 ----
        label9.setText("Enable Border:");
        label9.setName("label9");
        add(label9, cc.xy(1, 23));

        //---- enableBorderCheckBox ----
        enableBorderCheckBox.setText("On");
        enableBorderCheckBox.setName("enableBorderCheckBox");
        enableBorderCheckBox.addActionListener(e -> enableBorderCheckBoxActionPerformed());
        add(enableBorderCheckBox, cc.xy(3, 23));

        //---- orientationLabel ----
        orientationLabel.setText("Orientation (Deg):");
        orientationLabel.setName("orientationLabel");
        add(orientationLabel, cc.xy(1, 25));

        //---- orientationSpinner ----
        orientationSpinner.setModel(new SpinnerNumberModel(0.0, -360.0, 360.0, 1.0));
        orientationSpinner.setName("orientationSpinner");
        orientationSpinner.addChangeListener(e -> orientationSpinnerStateChanged());
        add(orientationSpinner, cc.xy(3, 25));

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(leftAlignment);
        buttonGroup1.add(centerAlignment);
        buttonGroup1.add(rightAlignment);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JToggleButton leftAlignment;
    private JToggleButton centerAlignment;
    private JToggleButton rightAlignment;
    private JLabel textLabel;
    private JTextPane textArea;
    private JSpinner sizeSpinner;
    private JComboBox<String> fontComboBox;
    private JPanel panel1;
    private JCheckBox boldCheckBox;
    private JCheckBox italicCheckBox;
    private ColorChooser textColorChooser;
    private ColorChooser backgroundColorChooser;
    private JSpinner opacitySpinner;
    private JCheckBox enableBackgroundFillCheckBox;
    private ColorChooser borderColorChooser;
    private JSpinner borderThicknessSpinner;
    private JCheckBox enableBorderCheckBox;
    private JSpinner orientationSpinner;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
