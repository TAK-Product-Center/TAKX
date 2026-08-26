package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.atakmap.coremap.maps.coords.NorthReference;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.globe.IGlobeClickService;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.commons.swing.ColorChooserUtils;
import gov.takx.commons.swing.SplitButton;
import gov.takx.core.view.gui.media.UrlDialog;
import gov.takx.dataservices.shapes.FieldOfView;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.java.dev.colorchooser.ColorChooser;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

/**
 * Displays controls to modify a {@link FieldOfView}.
 *
 * @since 5.8
 */
@PseudoBean
public class FieldOfViewPanel extends JPanel implements PropertyChangeListener
{
    public static final String TITLE = "Field of View";
    public static final URL ICON_URL = UIResources.getUrl(ResourceDirectory.TAK_ASSETS_SVG, "Video-Player.svg");

    private static final List<String> PROPERTIES = List.of(FieldOfView.AZIMUTH, FieldOfView.FOV_DEGREES, FieldOfView.MEDIA, FieldOfView.VISIBILITY,
            FieldOfView.SHOW_FOV_LABELS, ShapeProperties.RANGE, ShapeProperties.RANGE_DISTANCES);
    private static final Icon MEDIA_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "media.svg");
    private static final String MEDIA_LIST_HTML = "<html><h3>%s</h3><p>%s</p></html>";

    @Inject
    private IGlobeClickService globeClickService;

    @Inject
    private IMediaManager mediaManager;

    @Inject
    private IUISpaceManager uiSpaceManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    private boolean updatingFov;
    private FieldOfView fieldOfView;

    private FieldOfViewPanel()
    {
        initComponents();
        selectMediaButton.setIcon(MEDIA_ICON);
        northReferenceCombo.setModel(new DefaultComboBoxModel<>(new NorthReference[]{NorthReference.TRUE, NorthReference.MAGNETIC}));
        northReferenceCombo.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText("\u00b0" + ((NorthReference) value).getAbbrev());
                return this;
            }
        });
        northReferenceCombo.addActionListener(e -> {
            final var northReference = (NorthReference) northReferenceCombo.getSelectedItem();
            updateFov(() -> {
                // noinspection DataFlowIssue - northReference cannot be null
                fieldOfView.setNorthReference(northReference);
                directionSpinner.setValue((int) Math.round(fieldOfView.getAzimuth()));
            });
        });

        configureSpinnerSlider(rangeSpinner, rangeSlider, range -> fieldOfView.setRange(range));
        configureSpinnerSlider(directionSpinner, directionSlider, azimuth -> fieldOfView.setAzimuth(azimuth));
        configureSpinnerSlider(fieldOfViewSpinner, fieldOfViewSlider, fovDegress -> fieldOfView.setFovDegrees(fovDegress));
        configureSpinnerSlider(rangeLineSpacingSpinner, rangeLineSpacingSlider, rangeLineSpacing -> fieldOfView.setRangeLineSpacing(rangeLineSpacing));

        configureColorChooser(fillColorChooser, color -> updateFov(() -> {
            var newColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), fillColorOpacitySlider.getValue());
            fieldOfView.setFillColor(newColor);
        }));
        configureColorChooser(outlineColorChooser, color -> updateFov(() -> fieldOfView.setStrokeColor(color)));
        configureColorChooser(rangeLinesOutlineColorChooser, color -> updateFov(() -> fieldOfView.setRangeLinesStrokeColor(color)));

        fillColorOpacitySlider.addChangeListener(e -> updateFov(() -> fieldOfView.setFillAlpha(fillColorOpacitySlider.getValue())));
        outlineWeightSlider.addChangeListener(e -> updateFov(() -> fieldOfView.setStrokeWeight(outlineWeightSlider.getValue() / 100d)));
        rangeLinesWeightSlider.addChangeListener(e -> updateFov(() -> fieldOfView.setRangeLineStrokeWeight(rangeLinesWeightSlider.getValue() / 100d)));

    }

    @PostConstruct
    private void postConstruct()
    {
        uiEventBus.register(this);
        selectMediaButton.setPopupMenuProvider(this::createPopupMenu);
    }

    /**
     * Allows FOV panel instance to only be instantiated once the class is accessed.
     */
    private static final class FieldOfViewSingleton
    {
        public static final FieldOfViewPanel instance = new FieldOfViewPanel();
    }

    /**
     * Provide a singleton instance of the FieldOfView panel.
     * @return FieldOfViewPanel
     */
    public static FieldOfViewPanel getInstance()
    {
        return FieldOfViewSingleton.instance;
    }

    private JPopupMenu createPopupMenu()
    {
        var menu = new JPopupMenu();
        mediaManager.getAllMedia().forEach(media -> {
            var mi = new JMenuItem(MEDIA_LIST_HTML.formatted(media.getName(), media.getSourceUri().toString()));
            mi.addActionListener(e -> {
                fieldOfView.setMedia(media);
                urlButton.setText(media.getName());
            });
            menu.add(mi);
        });
        return menu;
    }

    /**
     * Try to set the FOV panel to the new map object selection if we have one.
     *
     * @param event MapObjectSelectedEvent
     */
    @Subscribe
    @SuppressWarnings("unused")
    public void selectionChanged(MapObjectSelectedEvent event)
    {
        IMapObjectController controller = event.getMapObjectController();
        if (controller == null) return;
        var mapObject = controller.getMapObject();
        var fov = mapObject.getFieldOfView();
        if (fov != null)
        {
            setFieldOfView(fov);
        } else
        {
            uiSpaceManager.removeComponent(this);
        }
    }

    /**
     * Create an {@link ActionListener} that will open the FieldOfViewPanel set to the given {@link FieldOfView}.
     * <p/>
     * Cannot be {@code static} as it requires injected classes to function. Must use {@link #getInstance()}.
     * @param fov the FOV to show in the panel
     * @return an ActionListener that will show a panel with the details from the FOV.
     */
    public ActionListener createPanelActionListener(FieldOfView fov)
    {
        return l -> {
            setFieldOfView(fov);
            var cd = new ComponentDescriptorBuilder(this, TITLE)
                    .setComponentPosition(ComponentPosition.RIGHT)
                    .setIcon(ICON_URL).setPinned(true).build();
            uiSpaceManager.addComponent(cd, this);
            uiSpaceManager.showComponent(this);
        };
    }

    /**
     * @param fieldOfView The field of view to display in this panel
     */
    public void setFieldOfView(@Nonnull FieldOfView fieldOfView)
    {
        if (this.fieldOfView != null) this.fieldOfView.removePropertyChangeListener(PROPERTIES, this);

        this.fieldOfView = null; // Prevent updates to the old FOV

        showFovCheckBox.setSelected(fieldOfView.isVisible());
        rangeSpinner.setValue((int) fieldOfView.getRange());
        directionSpinner.setValue((int) fieldOfView.getAzimuth());
        fieldOfViewSpinner.setValue((int) fieldOfView.getFovDegrees());
        showRangeLinesAndLabelsCheckBox.setSelected(fieldOfView.isShowLabels());
        rangeLineSpacingSpinner.setValue((int) fieldOfView.getRangeLineSpacing());

        final var fillColor = fieldOfView.getFillColor();
        fillColorChooser.setColor(new Color(fillColor.getRGB())); // fill color has alpha, which is incorrect in the panel display
        outlineColorChooser.setColor(fieldOfView.getStrokeColor());
        rangeLinesOutlineColorChooser.setColor(fieldOfView.getRangeLinesStrokeColor());
        northReferenceCombo.setSelectedItem(fieldOfView.getNorthReference());

        fillColorOpacitySlider.setValue(fillColor.getAlpha());
        outlineWeightSlider.setValue((int) (fieldOfView.getStrokeWeight() * 100));
        rangeLinesWeightSlider.setValue((int) (fieldOfView.getRangeLineStrokeWeight() * 100));

        if (fieldOfView.getMedia() != null) urlButton.setText(fieldOfView.getMedia().getName());

        this.fieldOfView = fieldOfView;
        fieldOfView.addPropertyChangeListener(PROPERTIES, this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (updatingFov) return; // Ignore our own changes

        switch (evt.getPropertyName())
        {
            case FieldOfView.AZIMUTH -> directionSpinner.setValue(((Number) evt.getNewValue()).intValue());
            case FieldOfView.FOV_DEGREES -> fieldOfViewSpinner.setValue(((Number) evt.getNewValue()).intValue());
            case ShapeProperties.RANGE -> rangeSpinner.setValue(((Number) evt.getNewValue()).intValue());
            case ShapeProperties.RANGE_DISTANCES -> rangeLineSpacingSpinner.setValue(((Number) evt.getNewValue()).intValue());
            case FieldOfView.VISIBILITY -> showFovCheckBox.setSelected((Boolean) evt.getNewValue());
            case FieldOfView.SHOW_FOV_LABELS -> showRangeLinesAndLabelsCheckBox.setSelected((Boolean) evt.getNewValue());
            case FieldOfView.MEDIA -> urlButton.setText(((IMedia) evt.getNewValue()).getName());
        }
    }

    private void showAnglesCheckBoxStateChanged()
    {
        updateFov(() -> fieldOfView.setShowLabels(showRangeLinesAndLabelsCheckBox.isSelected()));
    }

    private void showFovCheckBoxStateChanged()
    {
        updateFov(() -> fieldOfView.setVisible(showFovCheckBox.isSelected()));
    }

    private void placeEndpoint()
    {
        globeClickService.deactivate();
        globeClickService.activate("Click map to place endpoint. Press ESC to cancel.", location -> {
            try
            {
                fieldOfView.setExtentPoint(location);
            } catch (IllegalArgumentException ignored)
            {
                // no-op here, exception was already handled by notification to user.
            }

            globeClickService.deactivate();
        });
    }

    private void videoButtonClicked()
    {
        var fovMedia = fieldOfView.getMedia();
        boolean updatingMedia = fovMedia != null;
        var urlDialog = new UrlDialog(mediaManager, media -> {
            if (updatingMedia) mediaManager.removeMedia(fovMedia);
            mediaManager.addMedia(media);
            fieldOfView.setMedia(media);
            urlButton.setText(media.getName());
        }, updatingMedia);
        if (updatingMedia)
        {
            urlDialog.setName(fovMedia.getName());
            urlDialog.setUrl(fovMedia.getSourceUri().toString());
        }

        urlDialog.setVisible(true);
    }

    private void configureColorChooser(@Nonnull ColorChooser colorChooser, @Nonnull Consumer<Color> handler)
    {
        colorChooser.setPalettes(ColorChooserUtils.getDefaultPalettes());
        colorChooser.addPropertyChangeListener("color", pce -> handler.accept((Color) pce.getNewValue()));
    }

    private void configureSpinnerSlider(JSpinner spinner, JSlider slider, Consumer<Integer> fovUpdater)
    {
        spinner.addChangeListener(e -> {
            final var value = (Integer) spinner.getValue();
            if (value == null) return;
            slider.setValue(value);

            // Some FOV updates require follow-up UI updates; don't loop them back to the FOV
            if (!updatingFov) updateFov(() -> fovUpdater.accept(value));
        });

        slider.addChangeListener(e -> {
            final var sliderVal = slider.getValue();
            spinner.setValue(sliderVal);
        });
    }

    /**
     * Call this method when updating {@link #fieldOfView} to ignore PCEs and inform UI elements they should not update
     * {@link #fieldOfView} (creating a loop).
     * 
     * @param runnable The operation to run
     */
    private void updateFov(@Nonnull Runnable runnable)
    {
        if (fieldOfView == null) return;

        updatingFov = true;
        runnable.run();
        updatingFov = false;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        showFovCheckBox = new JCheckBox();
        var label1 = new JLabel();
        var videoUrlPanel = new JPanel();
        urlButton = new JButton();
        selectMediaButton = new SplitButton();
        var label2 = new JLabel();
        rangeSpinner = new JSpinner();
        var label3 = new JLabel();
        var placeEndpointButton = new JButton();
        rangeSlider = new JSlider();
        var label10 = new JLabel();
        fillColorChooser = new ColorChooser();
        fillColorOpacitySlider = new JSlider();
        var label11 = new JLabel();
        outlineColorChooser = new ColorChooser();
        outlineWeightSlider = new JSlider();
        var label4 = new JLabel();
        directionSpinner = new JSpinner();
        northReferenceCombo = new JComboBox<>();
        directionSlider = new JSlider();
        var label7 = new JLabel();
        fieldOfViewSpinner = new JSpinner();
        var label9 = new JLabel();
        fieldOfViewSlider = new JSlider();
        showRangeLinesAndLabelsCheckBox = new JCheckBox();
        var label5 = new JLabel();
        rangeLineSpacingSpinner = new JSpinner();
        var label6 = new JLabel();
        rangeLineSpacingSlider = new JSlider();
        var label12 = new JLabel();
        rangeLinesOutlineColorChooser = new ColorChooser();
        rangeLinesWeightSlider = new JSlider();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setName("this");
        setLayout(new FormLayout(
                "30dlu, $lcgap, 38dlu, 2*($lcgap, default), $lcgap, [100dlu,default]:grow, 3*($lcgap, default)",
                "default, $pgap, default, $ugap, 3*(default, $lgap), 2*(default, $ugap), default, $pgap, 2*(default, $lgap), default"));

        //---- showFovCheckBox ----
        showFovCheckBox.setText("Show FOV Overlay");
        showFovCheckBox.setSelected(true);
        showFovCheckBox.setName("showFovCheckBox");
        showFovCheckBox.addChangeListener(e -> showFovCheckBoxStateChanged());
        add(showFovCheckBox, cc.xywh(1, 1, 7, 1));

        //---- label1 ----
        label1.setText("Video URL");
        label1.setName("label1");
        add(label1, cc.xywh(1, 3, 2, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

        //======== videoUrlPanel ========
        {
            videoUrlPanel.setName("videoUrlPanel");
            videoUrlPanel.setLayout(new FormLayout(
                    "213dlu:grow, $lcgap, 32dlu",
                    "fill:default"));

            //---- urlButton ----
            urlButton.setText("None");
            urlButton.setName("urlButton");
            urlButton.addActionListener(e -> videoButtonClicked());
            videoUrlPanel.add(urlButton, cc.xy(1, 1));

            //---- selectMediaButton ----
            selectMediaButton.setToolTipText("Select the Media for this Sensor");
            selectMediaButton.setName("selectMediaButton");
            videoUrlPanel.add(selectMediaButton, cc.xy(3, 1));
        }
        add(videoUrlPanel, cc.xywh(3, 3, 13, 1));

        //---- label2 ----
        label2.setText("Range");
        label2.setName("label2");
        add(label2, cc.xywh(1, 5, 2, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

        //---- rangeSpinner ----
        rangeSpinner.setModel(new SpinnerNumberModel(20000, 0, 60000, 1));
        rangeSpinner.setName("rangeSpinner");
        add(rangeSpinner, cc.xywh(3, 5, 3, 1));

        //---- label3 ----
        label3.setText("m");
        label3.setName("label3");
        add(label3, cc.xy(7, 5));

        //---- placeEndpointButton ----
        placeEndpointButton.setText("Place Endpoint");
        placeEndpointButton.setName("placeEndpointButton");
        placeEndpointButton.addActionListener(e -> placeEndpoint());
        add(placeEndpointButton, cc.xy(9, 5));

        //---- rangeSlider ----
        rangeSlider.setMaximum(60000);
        rangeSlider.setValue(20000);
        rangeSlider.setMajorTickSpacing(60000);
        rangeSlider.setPaintLabels(true);
        rangeSlider.setName("rangeSlider");
        add(rangeSlider, cc.xywh(1, 7, 15, 1));

        //---- label10 ----
        label10.setText("Fill");
        label10.setName("label10");
        add(label10, cc.xy(3, 9));

        //---- fillColorChooser ----
        fillColorChooser.setBackground(Color.white);
        fillColorChooser.setToolTipText("Fill color");
        fillColorChooser.setName("fillColorChooser");
        add(fillColorChooser, cc.xy(5, 9, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- fillColorOpacitySlider ----
        fillColorOpacitySlider.setValue(30);
        fillColorOpacitySlider.setToolTipText("Fill opacity");
        fillColorOpacitySlider.setMaximum(255);
        fillColorOpacitySlider.setName("fillColorOpacitySlider");
        add(fillColorOpacitySlider, cc.xywh(7, 9, 9, 1));

        //---- label11 ----
        label11.setText("Outline");
        label11.setName("label11");
        add(label11, cc.xy(3, 11));

        //---- outlineColorChooser ----
        outlineColorChooser.setBackground(new Color(0x99ffff));
        outlineColorChooser.setToolTipText("Outline color");
        outlineColorChooser.setName("outlineColorChooser");
        add(outlineColorChooser, cc.xy(5, 11, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- outlineWeightSlider ----
        outlineWeightSlider.setValue(30);
        outlineWeightSlider.setToolTipText("Outline opacity");
        outlineWeightSlider.setName("outlineWeightSlider");
        add(outlineWeightSlider, cc.xywh(7, 11, 9, 1));

        //---- label4 ----
        label4.setText("Direction");
        label4.setName("label4");
        add(label4, cc.xywh(1, 13, 2, 1));

        //---- directionSpinner ----
        directionSpinner.setModel(new SpinnerNumberModel(0, 0, 359, 1));
        directionSpinner.setName("directionSpinner");
        add(directionSpinner, cc.xywh(3, 13, 3, 1));

        //---- northReferenceCombo ----
        northReferenceCombo.setName("northReferenceCombo");
        add(northReferenceCombo, cc.xy(7, 13));

        //---- directionSlider ----
        directionSlider.setMaximum(359);
        directionSlider.setMajorTickSpacing(359);
        directionSlider.setPaintLabels(true);
        directionSlider.setValue(0);
        directionSlider.setName("directionSlider");
        add(directionSlider, cc.xywh(9, 13, 7, 1));

        //---- label7 ----
        label7.setText("<html>Field of View</html>");
        label7.setName("label7");
        add(label7, cc.xywh(1, 15, 2, 1));

        //---- fieldOfViewSpinner ----
        fieldOfViewSpinner.setModel(new SpinnerNumberModel(45, 0, 359, 1));
        fieldOfViewSpinner.setName("fieldOfViewSpinner");
        add(fieldOfViewSpinner, cc.xywh(3, 15, 3, 1));

        //---- label9 ----
        label9.setText("\u00b0");
        label9.setName("label9");
        add(label9, cc.xy(7, 15));

        //---- fieldOfViewSlider ----
        fieldOfViewSlider.setMaximum(359);
        fieldOfViewSlider.setValue(45);
        fieldOfViewSlider.setMajorTickSpacing(359);
        fieldOfViewSlider.setPaintLabels(true);
        fieldOfViewSlider.setName("fieldOfViewSlider");
        add(fieldOfViewSlider, cc.xywh(9, 15, 7, 1));

        //---- showRangeLinesAndLabelsCheckBox ----
        showRangeLinesAndLabelsCheckBox.setText("Show Range Lines, Angles");
        showRangeLinesAndLabelsCheckBox.setName("showRangeLinesAndLabelsCheckBox");
        showRangeLinesAndLabelsCheckBox.addChangeListener(e -> showAnglesCheckBoxStateChanged());
        add(showRangeLinesAndLabelsCheckBox, cc.xywh(1, 17, 9, 1));

        //---- label5 ----
        label5.setText("<html>Range Line Dist</html>");
        label5.setName("label5");
        add(label5, cc.xy(1, 19));

        //---- rangeLineSpacingSpinner ----
        rangeLineSpacingSpinner.setModel(new SpinnerNumberModel(0, 0, 60000, 1));
        rangeLineSpacingSpinner.setToolTipText("Range line spacing");
        rangeLineSpacingSpinner.setName("rangeLineSpacingSpinner");
        add(rangeLineSpacingSpinner, cc.xywh(3, 19, 3, 1));

        //---- label6 ----
        label6.setText("m");
        label6.setName("label6");
        add(label6, cc.xy(7, 19));

        //---- rangeLineSpacingSlider ----
        rangeLineSpacingSlider.setMaximum(60000);
        rangeLineSpacingSlider.setMajorTickSpacing(60000);
        rangeLineSpacingSlider.setPaintLabels(true);
        rangeLineSpacingSlider.setToolTipText("Range line distance");
        rangeLineSpacingSlider.setName("rangeLineSpacingSlider");
        add(rangeLineSpacingSlider, cc.xywh(9, 19, 7, 1));

        //---- label12 ----
        label12.setText("Color / Weight");
        label12.setName("label12");
        add(label12, cc.xywh(1, 21, 3, 1));

        //---- rangeLinesOutlineColorChooser ----
        rangeLinesOutlineColorChooser.setBackground(new Color(0x3399ff));
        rangeLinesOutlineColorChooser.setToolTipText("Range line color");
        rangeLinesOutlineColorChooser.setName("rangeLinesOutlineColorChooser");
        add(rangeLinesOutlineColorChooser, cc.xy(5, 21, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- rangeLinesWeightSlider ----
        rangeLinesWeightSlider.setValue(30);
        rangeLinesWeightSlider.setToolTipText("Range line weight");
        rangeLinesWeightSlider.setName("rangeLinesWeightSlider");
        add(rangeLinesWeightSlider, cc.xywh(9, 21, 7, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JCheckBox showFovCheckBox;
    private JButton urlButton;
    private SplitButton selectMediaButton;
    private JSpinner rangeSpinner;
    private JSlider rangeSlider;
    private ColorChooser fillColorChooser;
    private JSlider fillColorOpacitySlider;
    private ColorChooser outlineColorChooser;
    private JSlider outlineWeightSlider;
    private JSpinner directionSpinner;
    private JComboBox<NorthReference> northReferenceCombo;
    private JSlider directionSlider;
    private JSpinner fieldOfViewSpinner;
    private JSlider fieldOfViewSlider;
    private JCheckBox showRangeLinesAndLabelsCheckBox;
    private JSpinner rangeLineSpacingSpinner;
    private JSlider rangeLineSpacingSlider;
    private ColorChooser rangeLinesOutlineColorChooser;
    private JSlider rangeLinesWeightSlider;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
