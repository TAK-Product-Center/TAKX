package gov.takx.core.view.gui.maptools;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeEyeElevationListener;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import net.java.dev.colorchooser.ColorChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_BACKGROUND_COLOR;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_BORDER_COLOR;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_BORDER_LEAD_ENABLE;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_BORDER_RADIUS;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_BORDER_SHAPE;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_BORDER_THICKNESS;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_ENABLE_FIXED_HEIGHT;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_FIXED_HEIGHT;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_FONT;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_MAX_ACTIVE_ALTITUDE;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_MIN_ACTIVE_ALTITUDE;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_OPACITY;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_TEXT;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_TEXT_ALIGN;
import static gov.takx.api.mapobject.IAnnotationMapObject.ANNOTATION_TEXT_COLOR;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@PseudoBean
public class AnnotationPanel extends APropertiesEditorPanel<IAnnotationMapObject> implements IGlobeEyeElevationListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final URL ICON_URL = UIResources.getUrl(PLATFORM_ICONS, "text_callout.png");

    protected static final List<String> FONT_LIST =
            Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

    private final List<String> propertiesToWatch = Arrays.asList(
            ANNOTATION_BACKGROUND_COLOR, ANNOTATION_BORDER_COLOR, ANNOTATION_BORDER_LEAD_ENABLE,
            ANNOTATION_BORDER_RADIUS, ANNOTATION_BORDER_SHAPE, ANNOTATION_BORDER_THICKNESS,
            ANNOTATION_ENABLE_FIXED_HEIGHT, ANNOTATION_FIXED_HEIGHT, ANNOTATION_FONT,
            ANNOTATION_MAX_ACTIVE_ALTITUDE, ANNOTATION_MIN_ACTIVE_ALTITUDE, ANNOTATION_OPACITY,
            ANNOTATION_TEXT, ANNOTATION_TEXT_ALIGN, ANNOTATION_TEXT_COLOR);

    /**
     * Constructor used when this panel is in the right-side properties panel for initial creation of an annotation.
     */
    public AnnotationPanel()
    {
        this(null, true, null);
    }

    /**
     * Constructor used when this panel is in the right-side properties panel to edit an existing annotation.
     */
    public AnnotationPanel(@Nonnull IAnnotationMapObject annotation)
    {
        this(annotation, true, null);
    }

    /**
     * Constructor used when this panel is being used in the Preferences dialog.
     *
     * @param annotation Annotation to edit
     * @param panelStateListener Listener for "panel dirty" events
     */
    public AnnotationPanel(@Nonnull IAnnotationMapObject annotation, @Nonnull IPanelStateListener panelStateListener)
    {
        this(annotation, false, panelStateListener);
    }

    private AnnotationPanel(@Nullable IAnnotationMapObject annotation, boolean directActionMode,
                            @Nullable IPanelStateListener panelStateListener)
    {
        super("Annotation", annotation, directActionMode, panelStateListener);

        setupControls();

        if (annotation != null) setMapObject(annotation);
    }

    @PostConstruct
    private void postConstruct()
    {
        adjustToDefaultFixedHeight();
        adjustFixedHeightStepSize(getWwd().getView().getCurrentEyePosition().elevation);
        globeManager.getLastFocusedGlobe().addGlobeEyeElevationListener(this);

        registerForEvents();
    }

    @Override
    public void releaseResources()
    {
        if (target != null) target.removePropertyChangeListener(propertiesToWatch, this);
        globeManager.getPrimaryGlobeDelegate().removeGlobeEyeElevationListener(this);
        super.releaseResources();
    }

    /**
     * Set the UI control values from the given Map Object.
     */
    private void setMapObject(IAnnotationMapObject mapObject)
    {
        updatingPanel = true;

        target = mapObject;

        final Font font = mapObject.getFont();

        textArea.setText(mapObject.getText());
        textArea.setFont(font);
        textArea.setBackground(mapObject.getBackgroundColor());
        textArea.setForeground(mapObject.getTextColor());
        fontTypeComboBox.setSelectedItem(font.getFamily());
        fontSizeComboBox.setSelectedItem(((Integer) (font.getSize())).toString());
        boldCheckBox.setSelected(font.isBold());
        italicCheckBox.setSelected(font.isItalic());
        textColorChooser.setColor(mapObject.getTextColor());
        backgroundColorChooser.setColor(mapObject.getBackgroundColor());
        opacitySpinner.setValue((int) (mapObject.getOpacity() * 100d));
        minVisibleAltitudeTextField.setValue(mapObject.getMinActiveAltitude() / 1000d);
        maxVisibleAltitudeTextField.setValue(mapObject.getMaxActiveAltitude() / 1000d);

        leaderCheckBox.setSelected(mapObject.isLeadEnabled());
        borderColorChooser.setColor(mapObject.getBorderColor());
        borderShapeComboBox.setSelectedIndex(mapObject.getBorderShape());
        borderThicknessSpinner.setValue(mapObject.getBorderThickness());

        fixedHeightCheckbox.setSelected(mapObject.isHeightFixed());
        fixedHeightSpinner.setValue(mapObject.getFixedHeight());

        updatingPanel = false;

        mapObject.addPropertyChangeListener(propertiesToWatch, this);
    }

    @Override
    protected void updateTargetProperties()
    {
        target.setText(textArea.getText());
        target.setFont(textArea.getFont());
        target.setTextColor(textColorChooser.getColor());
        target.setBackgroundColor(backgroundColorChooser.getColor());
        target.setOpacity((Integer) opacitySpinner.getValue() / 100.0);
        target.setMinActiveAltitude(((Number) minVisibleAltitudeTextField.getValue()).doubleValue() * 1000d);
        target.setMaxActiveAltitude(((Number) maxVisibleAltitudeTextField.getValue()).doubleValue() * 1000d);
        target.enableFixedHeight(fixedHeightCheckbox.isSelected());
        target.setFixedHeight((Double) fixedHeightSpinner.getValue());
        target.enableLeader(leaderCheckBox.isSelected());
        target.setBorderShape(borderShapeComboBox.getSelectedIndex());
        target.setBorderColor(borderColorChooser.getColor());
        target.setBorderThickness((Integer) borderThicknessSpinner.getValue());
    }

    /**
     * Handle a property change on the shape object we are tracking.
     *
     * @param evt Event to process
     */
    @Override
    @InvokeAndWaitOnEDT
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();
        final Object value = evt.getNewValue();

        logger.debug("property change: ignore {} : {}={}", ignoreEvent, propertyName, value);

        if (ignoreEvent) return;

        updatingPanel = true; // Prevent changeEvents from triggering actions

        // Update our controls to match the updated property
        switch (propertyName)
        {
            case ANNOTATION_TEXT -> textArea.setText((String) value);
            case ANNOTATION_TEXT_COLOR -> textColorChooser.setColor((Color) value);
            case ANNOTATION_FONT -> updateFontControls((Font) value);
            case ANNOTATION_BACKGROUND_COLOR -> backgroundColorChooser.setColor((Color) value);
            case ANNOTATION_OPACITY -> opacitySpinner.setValue((int) ((double) value * 100d));
            case ANNOTATION_MIN_ACTIVE_ALTITUDE -> minVisibleAltitudeTextField.setValue((double) value / 1000d);
            case ANNOTATION_MAX_ACTIVE_ALTITUDE -> maxVisibleAltitudeTextField.setValue((double) value / 1000d);
            case ANNOTATION_BORDER_COLOR -> borderColorChooser.setColor((Color) value);
            case ANNOTATION_BORDER_LEAD_ENABLE -> leaderCheckBox.setSelected((boolean) value);
            case ANNOTATION_BORDER_SHAPE -> borderShapeComboBox.setSelectedIndex((int) value);
            case ANNOTATION_BORDER_THICKNESS -> borderThicknessSpinner.setValue(value);
            case ANNOTATION_ENABLE_FIXED_HEIGHT -> fixedHeightCheckbox.setSelected((boolean) value);
            case ANNOTATION_FIXED_HEIGHT -> fixedHeightSpinner.setValue(value);
            case ANNOTATION_TEXT_ALIGN, ANNOTATION_BORDER_RADIUS -> logger.debug("Ignore {}", propertyName);
            default -> logger.warn("Received unexpected property change event: {}", evt);
        }

        updatingPanel = false;
    }

    @Override
    public void elevationChanged(double newElevation)
    {
        // Always change the step size accordingly
        adjustFixedHeightStepSize(newElevation);

        // If we are creating a new annotation OR the fixed height checkbox is not checked
        // on an old annotation, change the default fixed height spinner based on eye elevation
        if (initialCreationOnly || !fixedHeightCheckbox.isSelected())
        {
            adjustToDefaultFixedHeight();
        }
    }

    @Override
    public URL getIcon()
    {
        return ICON_URL;
    }

    private void adjustToDefaultFixedHeight()
    {
        double alt = getWwd().getView().getCurrentEyePosition().elevation;
        fixedHeightSpinner.setValue(alt / 50_000d);
    }

    /**
     * Change the step size according to our current elevation.
     */
    private void adjustFixedHeightStepSize(double elevation)
    {
        final int exp = (int) Math.log10(elevation);
        final double stepSize = Math.pow(10, exp - 5);
        ((SpinnerNumberModel) fixedHeightSpinner.getModel()).setStepSize(stepSize);
    }

    private void updateFontControls(Font font)
    {
        textArea.setFont(font);
        fontTypeComboBox.setSelectedItem(font.getFamily());
        fontSizeComboBox.setSelectedItem(((Integer) (font.getSize())).toString());
        boldCheckBox.setSelected(font.isBold());
        italicCheckBox.setSelected(font.isItalic());
    }

    private void fontTypeComboBoxActionPerformed()
    {
        if (fontTypeComboBox.getSelectedItem() != null)
        {
            final Font oldFont = textArea.getFont();
            final String fontName = fontTypeComboBox.getSelectedItem().toString();
            updateFont(new Font(fontName, oldFont.getStyle(), oldFont.getSize()));
        }
    }

    private void fontSizeComboBoxActionPerformed()
    {
        if (fontSizeComboBox.getSelectedItem() != null)
        {
            final float fontSize = Float.parseFloat(fontSizeComboBox.getSelectedItem().toString());
            updateFont(textArea.getFont().deriveFont(fontSize));
        }
    }

    private void updateFont(Font newFont)
    {
        updateProperty(() -> target.setFont(newFont));
        textArea.setFont(newFont);
    }

    private void textAreaCaretUpdate()
    {
        updateProperty(() -> target.setText(textArea.getText()));
    }

    private void italicCheckBoxActionPerformed()
    {
        setFontStyle(Font.ITALIC, italicCheckBox.isSelected());
    }

    private void boldCheckBoxActionPerformed()
    {
        setFontStyle(Font.BOLD, boldCheckBox.isSelected());
    }

    private void setFontStyle(int style, boolean enabled)
    {
        Font oldFont = textArea.getFont();
        int newFontStyle = enabled ? oldFont.getStyle() | style : oldFont.getStyle() ^ style;
        updateFont(oldFont.deriveFont(newFontStyle));
    }

    private void setTextColor(Color color)
    {
        updateProperty(() -> target.setTextColor(color));
        textArea.setForeground(color);
    }

    private void setBackgroundColor(Color color)
    {
        updateProperty(() -> target.setBackgroundColor(color));
        textArea.setBackground(color);
    }

    private void opacitySpinnerStateChanged()
    {
        updateProperty(() -> target.setOpacity((Integer) opacitySpinner.getValue() / 100.0));
    }

    private void minVisibleAltitudeTextFieldActionPerformed()
    {
        setMinActiveAltitude();
    }

    private void setMinActiveAltitude()
    {
        updateProperty(() -> target.setMinActiveAltitude(((Number) minVisibleAltitudeTextField.getValue()).doubleValue() * 1000d));
    }

    private void setMaxActiveAltitude()
    {
        updateProperty(() -> target.setMaxActiveAltitude(((Number) maxVisibleAltitudeTextField.getValue()).doubleValue() * 1000d));
    }

    private void maxVisibleAltitudeTextFieldActionPerformed()
    {
        setMaxActiveAltitude();
    }

    private void minVisibleAltitudeTextFieldCaretUpdate()
    {
        setMinActiveAltitude();
    }

    private void maxVisibleAltitudeTextFieldCaretUpdate()
    {
        setMaxActiveAltitude();
    }

    private void setBorderColor(Color color)
    {
        updateProperty(() -> target.setBorderColor(borderColorChooser.getColor()));
    }

    private void borderShapeComboBoxActionPerformed()
    {
        updateProperty(() -> target.setBorderShape(borderShapeComboBox.getSelectedIndex()));
    }

    private void leaderCheckBoxActionPerformed()
    {
        updateProperty(() -> target.enableLeader(leaderCheckBox.isSelected()));
    }

    private void borderThicknessSpinnerStateChanged()
    {
        updateProperty(() -> target.setBorderThickness((Integer) borderThicknessSpinner.getValue()));
    }

    private void fixedHeightCheckboxActionPerformed()
    {
        updateProperty(() -> target.enableFixedHeight(fixedHeightCheckbox.isSelected()));
    }

    private void fixedHeightSpinnerStateChanged()
    {
        updateProperty(() -> target.setFixedHeight((Double) fixedHeightSpinner.getValue()));
    }

    protected void setupControls()
    {
        initComponents();

        updatingPanel = true;

        initFontList();
        textArea.setDocument(new LimitedDocument(2048));
        fontSizeComboBox.setSelectedItem(((Integer) (textArea.getFont().getSize())).toString());

        textColorChooser.setColor(textArea.getForeground());
        setupColorChooser(textColorChooser, this::setTextColor);

        backgroundColorChooser.setColor(textArea.getBackground());
        setupColorChooser(backgroundColorChooser, this::setBackgroundColor);

        setupColorChooser(borderColorChooser, this::setBorderColor);
        borderThicknessSpinner.setValue(IAnnotationMapObject.DEFAULT_BORDER_THICKNESS);

        minVisibleAltitudeTextField.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter()));
        maxVisibleAltitudeTextField.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter()));
        minVisibleAltitudeTextField.setValue(IAnnotationMapObject.DEFAULT_MIN_ACTIVE_ALTITUDE / 1000d);
        maxVisibleAltitudeTextField.setValue(IAnnotationMapObject.DEFAULT_MAX_ACTIVE_ALTITUDE / 1000d);

        fixedHeightCheckbox.setSelected(IAnnotationMapObject.DEFAULT_FIXED_HEIGHT_ENABLED);
        leaderCheckBox.setSelected(true);

        updatingPanel = false;
    }

    private void initFontList()
    {
        for (String font : FONT_LIST)
        {
            fontTypeComboBox.addItem(font);
        }

        // Set the default font. On Windows, we like Arial. If Arial is not available (e.g., on Linux), choose
        // something reasonable.
        final String defaultFont = new JLabel().getFont().getFamily();
        if (FONT_LIST.contains("Arial"))
        {
            fontTypeComboBox.setSelectedItem("Arial");
        } else if (FONT_LIST.contains(defaultFont))
        {
            fontTypeComboBox.setSelectedItem(defaultFont);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var textLabel = new JLabel();
        var textScrollPane = new JScrollPane();
        textArea = new JTextPane();
        var fontLabel = new JLabel();
        fontTypeComboBox = new JComboBox<>();
        var fontStyleLabel = new JLabel();
        var panel5 = new JPanel();
        boldCheckBox = new JCheckBox();
        italicCheckBox = new JCheckBox();
        var fontSizeLabel = new JLabel();
        fontSizeComboBox = new JComboBox<>();
        var textColorLabel = new JLabel();
        textColorChooser = new ColorChooser();
        var backgroundColorLabel = new JLabel();
        backgroundColorChooser = new ColorChooser();
        var opacityLabel = new JLabel();
        opacitySpinner = new JSpinner();
        var visibleAltitudeLabel = new JLabel();
        var panel6 = new JPanel();
        var minVisibleAltitudeLabel = new JLabel();
        var label1 = new JLabel();
        minVisibleAltitudeTextField = new JFormattedTextField();
        maxVisibleAltitudeTextField = new JFormattedTextField();
        var label3 = new JLabel();
        var panel2 = new JPanel();
        fixedHeightCheckbox = new JCheckBox();
        fixedHeightSpinner = new JSpinner();
        var label2 = new JLabel();
        leaderCheckBox = new JCheckBox();
        var label4 = new JLabel();
        borderShapeComboBox = new JComboBox<>();
        var borderColorLabel = new JLabel();
        borderColorChooser = new ColorChooser();
        var label5 = new JLabel();
        borderThicknessSpinner = new JSpinner();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "right:default, $lcgap, default:grow",
                "70dlu, 12*($lgap, default)"));

        //---- textLabel ----
        textLabel.setText("Text:");
        textLabel.setName("textLabel");
        add(textLabel, cc.xy(1, 1));

        //======== textScrollPane ========
        {
            textScrollPane.setName("textScrollPane");

            //---- textArea ----
            textArea.setSelectedTextColor(new Color(0x66ffff));
            textArea.setBackground(Color.black);
            textArea.setForeground(Color.white);
            textArea.setName("textArea");
            textArea.addCaretListener(e -> textAreaCaretUpdate());
            textScrollPane.setViewportView(textArea);
        }
        add(textScrollPane, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

        //---- fontLabel ----
        fontLabel.setText("Font:");
        fontLabel.setName("fontLabel");
        add(fontLabel, cc.xy(1, 3));

        //---- fontTypeComboBox ----
        fontTypeComboBox.setName("fontTypeComboBox");
        fontTypeComboBox.addActionListener(e -> fontTypeComboBoxActionPerformed());
        add(fontTypeComboBox, cc.xy(3, 3));

        //---- fontStyleLabel ----
        fontStyleLabel.setText("Font Style:");
        fontStyleLabel.setName("fontStyleLabel");
        add(fontStyleLabel, cc.xy(1, 5));

        //======== panel5 ========
        {
            panel5.setName("panel5");
            panel5.setLayout(new FormLayout(
                    "default, $rgap, default",
                    "default"));

            //---- boldCheckBox ----
            boldCheckBox.setText("Bold");
            boldCheckBox.setName("boldCheckBox");
            boldCheckBox.addActionListener(e -> boldCheckBoxActionPerformed());
            panel5.add(boldCheckBox, cc.xy(1, 1));

            //---- italicCheckBox ----
            italicCheckBox.setText("Italic");
            italicCheckBox.setName("italicCheckBox");
            italicCheckBox.addActionListener(e -> italicCheckBoxActionPerformed());
            panel5.add(italicCheckBox, cc.xy(3, 1));
        }
        add(panel5, cc.xy(3, 5));

        //---- fontSizeLabel ----
        fontSizeLabel.setText("Font Size:");
        fontSizeLabel.setName("fontSizeLabel");
        add(fontSizeLabel, cc.xy(1, 7));

        //---- fontSizeComboBox ----
        fontSizeComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "8",
                "10",
                "11",
                "12",
                "14",
                "16",
                "18",
                "20",
                "24",
                "30",
                "36",
                "40",
                "48",
                "60",
                "72"
        }));
        fontSizeComboBox.setSelectedIndex(0);
        fontSizeComboBox.setName("fontSizeComboBox");
        fontSizeComboBox.addActionListener(e -> fontSizeComboBoxActionPerformed());
        add(fontSizeComboBox, cc.xy(3, 7));

        //---- textColorLabel ----
        textColorLabel.setText("Text Color:");
        textColorLabel.setName("textColorLabel");
        add(textColorLabel, cc.xy(1, 9));

        //---- textColorChooser ----
        textColorChooser.setColor(Color.black);
        textColorChooser.setName("textColorChooser");
        add(textColorChooser, cc.xy(3, 9));

        //---- backgroundColorLabel ----
        backgroundColorLabel.setText("Background Color:");
        backgroundColorLabel.setName("backgroundColorLabel");
        add(backgroundColorLabel, cc.xy(1, 11));

        //---- backgroundColorChooser ----
        backgroundColorChooser.setColor(Color.white);
        backgroundColorChooser.setName("backgroundColorChooser");
        add(backgroundColorChooser, cc.xy(3, 11));

        //---- opacityLabel ----
        opacityLabel.setText("Opacity:");
        opacityLabel.setName("opacityLabel");
        add(opacityLabel, cc.xy(1, 13));

        //---- opacitySpinner ----
        opacitySpinner.setModel(new SpinnerNumberModel(80, 0, 100, 1));
        opacitySpinner.setName("opacitySpinner");
        opacitySpinner.addChangeListener(e -> opacitySpinnerStateChanged());
        add(opacitySpinner, cc.xy(3, 13));

        //---- visibleAltitudeLabel ----
        visibleAltitudeLabel.setText("Visible alt. (km):");
        visibleAltitudeLabel.setName("visibleAltitudeLabel");
        add(visibleAltitudeLabel, cc.xy(1, 15));

        //======== panel6 ========
        {
            panel6.setName("panel6");
            panel6.setLayout(new FormLayout(
                    "default:grow, $rgap, default:grow",
                    "2*(default)"));

            //---- minVisibleAltitudeLabel ----
            minVisibleAltitudeLabel.setText("  min ");
            minVisibleAltitudeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            minVisibleAltitudeLabel.setName("minVisibleAltitudeLabel");
            panel6.add(minVisibleAltitudeLabel, cc.xy(1, 1));

            //---- label1 ----
            label1.setText("  max");
            label1.setName("label1");
            panel6.add(label1, cc.xy(3, 1));

            //---- minVisibleAltitudeTextField ----
            minVisibleAltitudeTextField.setName("minVisibleAltitudeTextField");
            minVisibleAltitudeTextField.addActionListener(e -> minVisibleAltitudeTextFieldActionPerformed());
            minVisibleAltitudeTextField.addCaretListener(e -> minVisibleAltitudeTextFieldCaretUpdate());
            panel6.add(minVisibleAltitudeTextField, cc.xy(1, 2));

            //---- maxVisibleAltitudeTextField ----
            maxVisibleAltitudeTextField.setName("maxVisibleAltitudeTextField");
            maxVisibleAltitudeTextField.addActionListener(e -> maxVisibleAltitudeTextFieldActionPerformed());
            maxVisibleAltitudeTextField.addCaretListener(e -> maxVisibleAltitudeTextFieldCaretUpdate());
            panel6.add(maxVisibleAltitudeTextField, cc.xy(3, 2));
        }
        add(panel6, cc.xy(3, 15));

        //---- label3 ----
        label3.setText("Fixed Height (km):");
        label3.setName("label3");
        add(label3, cc.xy(1, 17));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- fixedHeightCheckbox ----
            fixedHeightCheckbox.setText("On");
            fixedHeightCheckbox.setToolTipText("Turns off annotation scaling and sets the annotation to be visible only at a fixed height.");
            fixedHeightCheckbox.setName("fixedHeightCheckbox");
            fixedHeightCheckbox.addActionListener(e -> fixedHeightCheckboxActionPerformed());
            panel2.add(fixedHeightCheckbox, cc.xy(1, 1));

            //---- fixedHeightSpinner ----
            fixedHeightSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, null, 10.0));
            fixedHeightSpinner.setName("fixedHeightSpinner");
            fixedHeightSpinner.addChangeListener(e -> fixedHeightSpinnerStateChanged());
            panel2.add(fixedHeightSpinner, cc.xy(3, 1));
        }
        add(panel2, cc.xy(3, 17));

        //---- label2 ----
        label2.setText("Leader:");
        label2.setName("label2");
        add(label2, cc.xy(1, 19));

        //---- leaderCheckBox ----
        leaderCheckBox.setText("On");
        leaderCheckBox.setName("leaderCheckBox");
        leaderCheckBox.addActionListener(e -> leaderCheckBoxActionPerformed());
        add(leaderCheckBox, cc.xy(3, 19));

        //---- label4 ----
        label4.setText("Border Shape:");
        label4.setName("label4");
        add(label4, cc.xy(1, 21));

        //---- borderShapeComboBox ----
        borderShapeComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "Rectangle",
                "Ellipse",
                "None"
        }));
        borderShapeComboBox.setName("borderShapeComboBox");
        borderShapeComboBox.addActionListener(e -> borderShapeComboBoxActionPerformed());
        add(borderShapeComboBox, cc.xy(3, 21));

        //---- borderColorLabel ----
        borderColorLabel.setText("Border Color:");
        borderColorLabel.setName("borderColorLabel");
        add(borderColorLabel, cc.xy(1, 23));

        //---- borderColorChooser ----
        borderColorChooser.setColor(Color.white);
        borderColorChooser.setName("borderColorChooser");
        add(borderColorChooser, cc.xy(3, 23));

        //---- label5 ----
        label5.setText("Border Thickness:");
        label5.setName("label5");
        add(label5, cc.xy(1, 25));

        //---- borderThicknessSpinner ----
        borderThicknessSpinner.setModel(new SpinnerNumberModel(0, 0, null, 5));
        borderThicknessSpinner.setName("borderThicknessSpinner");
        borderThicknessSpinner.addChangeListener(e -> borderThicknessSpinnerStateChanged());
        add(borderThicknessSpinner, cc.xy(3, 25));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextPane textArea;
    private JComboBox<String> fontTypeComboBox;
    private JCheckBox boldCheckBox;
    private JCheckBox italicCheckBox;
    private JComboBox<String> fontSizeComboBox;
    private ColorChooser textColorChooser;
    private ColorChooser backgroundColorChooser;
    private JSpinner opacitySpinner;
    private JFormattedTextField minVisibleAltitudeTextField;
    private JFormattedTextField maxVisibleAltitudeTextField;
    private JCheckBox fixedHeightCheckbox;
    private JSpinner fixedHeightSpinner;
    private JCheckBox leaderCheckBox;
    private JComboBox<String> borderShapeComboBox;
    private ColorChooser borderColorChooser;
    private JSpinner borderThicknessSpinner;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
