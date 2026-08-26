package gov.takx.platform.ui.location; // TODO KMB: Move to new location package after review

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.pointdropper.IPointDropperService;
import gov.takx.api.pointdropper.PointDropperConfiguration;
import gov.takx.api.ui.location.ILocationInputListener;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URI;

/**
 * Prompts the user for a location and marker type to place the marker at the given location.
 *
 * @since 5.1 (Moved here at this version, original version is unknown)
 */
public class GoToLocationDialog extends AStandardDialog implements ILocationInputListener
{
    private final Helper helper = new Helper();

    public GoToLocationDialog()
    {
        initComponents();

        locationInputPanel.addListener(this);
        locationInputPanel.setShowMarkerIcons(true);
        locationInputPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                pack();
            }
        });
    }

    @Override
    public void onLocationSelected(@Nonnull ILocation location, @Nonnull CoordinateSystem coordinateSystem,
                                   @Nullable URI markerIconUri, @Nullable String cotType)
    {
        helper.onLocationSelected(location, coordinateSystem, markerIconUri, cotType);
        dispose();
    }

    @Override
    public void onCancel()
    {
        helper.onCancel();
        dispose();
    }

    /**
     * The internal handling of the go-to dialog is exposed in this helper class to facilitate test coverage.
     * Standard dialogs cannot be tested due to the reliance on a non-headless display.
     *
     * @since 5.3
     */
    @PseudoBean
    @VisibleForTesting
    static final class Helper implements ILocationInputListener
    {
        @Inject
        private IGlobeManager globeManager;

        @Inject
        private IPointDropperService pointDropperService;

        @Override
        public void onLocationSelected(@Nonnull ILocation location, @Nonnull CoordinateSystem coordinateSystem,
                                       @Nullable URI markerIconUri, @Nullable String cotType)
        {
            if (cotType != null) createLocationMarker(location, coordinateSystem, markerIconUri, cotType);

            //noinspection DataFlowIssue
            globeManager.getLastFocusedGlobe().goTo(location);
        }

        @Override
        public void onCancel()
        {
            // no-op
        }

        /**
         * Creates a map object with the selected marker icon at the specified location.
         *
         * @param location The selected location
         * @param coordinateSystem The coordinate system the user used for the location
         * @param markerIconUri The URI of the selected marker icon
         * @param cotType The CoT type to use for the created map object
         */
        private void createLocationMarker(@Nonnull ILocation location, @Nonnull CoordinateSystem coordinateSystem,
                                          @Nullable URI markerIconUri, @Nonnull String cotType)
        {
            final var configuration = PointDropperConfiguration.builder(cotType)
                    .initializer(mapObject -> {
                        // Give this new location marker a name from the location.
                        mapObject.setName(CoordinateSystem.getLocationString(location, coordinateSystem));

                        // This will disable marker icon adaptation down the line.
                        if (markerIconUri != null) mapObject.setIconImage(markerIconUri);

                        mapObject.setAllowDrag(false);
                        mapObject.setAllowDeviceToBeContainer(false);
                    }).build();

            pointDropperService.dropPoint(configuration, location);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        locationInputPanel = new LegacyLocationInputPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Go To Location");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setName("dialogGoToLocation");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "default:grow"));

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- locationInputPanel ----
            locationInputPanel.setName("locationInputPanel");
            dialogPane.add(locationInputPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, cc.xy(1, 1));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private LegacyLocationInputPanel locationInputPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            //noinspection ResultOfObjectAllocationIgnored
            new JFrame("main");
            JDialog d = new GoToLocationDialog();
            d.setVisible(true);
            System.exit(0);
        });
    }
}
