package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import gov.tak.api.maps.Marker;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.dataservices.persist.MarkerMapObject;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * A panel that displays controls to customize markers' style.
 *
 * @since 3.0.1
 */
public class MarkerMapObjectPrimaryPanel extends AMapObjectPanel
{
    private final MarkerMapObject markerMapObject;

    public MarkerMapObjectPrimaryPanel(IMapObjectController mapObjectController)
    {
        super(mapObjectController);

        initComponents();

        // TODO: Add a generic parameter to AMapObjectPanel so we do not need to blindly cast in subclasses
        markerMapObject = ((MarkerMapObject) mapObjectController.getMapObject());
        int currentStyle = markerMapObject.getStyle();

        showHeadingIndicatorCheckBox.setSelected((currentStyle & Marker.STYLE_ROTATE_HEADING_MASK) != 0);
        smoothRotationsCheckBox.setSelected((currentStyle & Marker.STYLE_SMOOTH_ROTATION_MASK) != 0);
        smoothMovementsCheckBox.setSelected((currentStyle & Marker.STYLE_SMOOTH_MOVEMENT_MASK) != 0);
        shortenTitlesCheckBox.setSelected((currentStyle & Marker.STYLE_MARQUEE_TITLE_MASK) != 0);
        alertCheckBox.setSelected((currentStyle & Marker.STYLE_ALERT_MASK) != 0);
    }

    private void styleCheckBoxActionPerformed()
    {
        int style = 0;

        if (showHeadingIndicatorCheckBox.isSelected())
        {
            style = style | Marker.STYLE_ROTATE_HEADING_MASK;
        }
        if (smoothRotationsCheckBox.isSelected())
        {
            style = style | Marker.STYLE_SMOOTH_ROTATION_MASK;
        }
        if (smoothMovementsCheckBox.isSelected())
        {
            style = style | Marker.STYLE_SMOOTH_MOVEMENT_MASK;
        }
        if (shortenTitlesCheckBox.isSelected())
        {
            style = style | Marker.STYLE_MARQUEE_TITLE_MASK;
        }
        if (alertCheckBox.isSelected())
        {
            style = style | Marker.STYLE_ALERT_MASK;
        }

        markerMapObject.setStyle(style);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var styleOptionsLabel = new JLabel();
        showHeadingIndicatorCheckBox = new JCheckBox();
        shortenTitlesCheckBox = new JCheckBox();
        smoothRotationsCheckBox = new JCheckBox();
        smoothMovementsCheckBox = new JCheckBox();
        alertCheckBox = new JCheckBox();

        //======== this ========
        setName("this");
        setLayout(new GridBagLayout());
        ((GridBagLayout) getLayout()).columnWidths = new int[]{0, 0, 0, 0};
        ((GridBagLayout) getLayout()).rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
        ((GridBagLayout) getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};
        ((GridBagLayout) getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

        //---- styleOptionsLabel ----
        styleOptionsLabel.setText("Style Options:");
        styleOptionsLabel.setName("styleOptionsLabel");
        add(styleOptionsLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(5, 5, 5, 5), 0, 0));

        //---- showHeadingIndicatorCheckBox ----
        showHeadingIndicatorCheckBox.setText("Show heading indicator");
        showHeadingIndicatorCheckBox.setToolTipText("Shows heading indicator aligned to track heading");
        showHeadingIndicatorCheckBox.setName("showHeadingIndicatorCheckBox");
        showHeadingIndicatorCheckBox.addActionListener(e -> styleCheckBoxActionPerformed());
        add(showHeadingIndicatorCheckBox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(5, 5, 5, 5), 0, 0));

        //---- shortenTitlesCheckBox ----
        shortenTitlesCheckBox.setText("Shorten titles");
        shortenTitlesCheckBox.setToolTipText("Shorten long titles with marquee");
        shortenTitlesCheckBox.setName("shortenTitlesCheckBox");
        shortenTitlesCheckBox.addActionListener(e -> styleCheckBoxActionPerformed());
        add(shortenTitlesCheckBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 5), 0, 0));

        //---- smoothRotationsCheckBox ----
        smoothRotationsCheckBox.setText("Smooth rotations");
        smoothRotationsCheckBox.setToolTipText("The marker will smoothly interpolate between rotation changes");
        smoothRotationsCheckBox.setName("smoothRotationsCheckBox");
        smoothRotationsCheckBox.addActionListener(e -> styleCheckBoxActionPerformed());
        add(smoothRotationsCheckBox, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 0), 0, 0));

        //---- smoothMovementsCheckBox ----
        smoothMovementsCheckBox.setText("Smooth movements");
        smoothMovementsCheckBox.setToolTipText("The marker will smoothly interpolate between point changes");
        smoothMovementsCheckBox.setName("smoothMovementsCheckBox");
        smoothMovementsCheckBox.addActionListener(e -> styleCheckBoxActionPerformed());
        add(smoothMovementsCheckBox, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 10, 5), 0, 0));

        //---- alertCheckBox ----
        alertCheckBox.setText("Alert");
        alertCheckBox.setToolTipText("Adds an additional layer to the marker");
        alertCheckBox.setName("alertCheckBox");
        alertCheckBox.addActionListener(e -> styleCheckBoxActionPerformed());
        add(alertCheckBox, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 5), 0, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JCheckBox showHeadingIndicatorCheckBox;
    private JCheckBox shortenTitlesCheckBox;
    private JCheckBox smoothRotationsCheckBox;
    private JCheckBox smoothMovementsCheckBox;
    private JCheckBox alertCheckBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
