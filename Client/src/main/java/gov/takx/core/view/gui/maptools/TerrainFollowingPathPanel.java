package gov.takx.core.view.gui.maptools;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.terrain.HighResolutionTerrain;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.platform.IProgressCallback;
import gov.takx.api.shapes.IPolyline;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.globe.utils.GlobeRuler;
import gov.takx.core.globe.utils.TerrainRuler;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import java.awt.CardLayout;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Reusable panel that shows terrain following length on a polyline.  When the path is initially set, a suggested
 * sampling length is generated from the LOS distance.  The user can change this value, and the number of samples
 * to use in the TF distance calculation will be determined from that.  This seems a bit backwards (why not just
 * let them specify the number of samples to make), but it is what the sponsor wants.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement@since 1.16
 * @since 1.16
 */
@PseudoBean
public class TerrainFollowingPathPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int SAMPLE_COUNT_FOR_ESTIMATE = 200;
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    @Inject
    private IGlobeManager globeManager;

    private final TerrainRuler measurer = new TerrainRuler();
    private IPolyline path;
    private double losLength;
    private RangeUnits rangeUnits = null;

    private HighResolutionTerrain terrain;
    private MeasurementWorker worker;

    /**
     * Constructor.
     */
    public TerrainFollowingPathPanel()
    {
        measurer.setPathType(WorldWind.LINEAR);

        initComponents();

        String tooltip = "<html>Enter the length in meters of the sampling steps."
                + "<br>The initial value was determined using " + SAMPLE_COUNT_FOR_ESTIMATE + " steps<html>";
        txtSampleLength.setToolTipText(tooltip);

        terrainCalculationsPanel.setVisible(false); // Not until the checkbox is tweaked
    }

    /**
     * Set the shape that this panel is operating upon.  Pass {@code null} to clear the shape and disable the
     * calculate button.
     *
     * @param path The path to operate upon, may be {@code null}.
     */
    public void setPath(IPolyline path)
    {
        this.path = path;

        calculateSampleLength();

        btnCalculate.setEnabled(path != null);
    }

    /**
     * @param rangeUnits The units to use
     * @since 5.5
     */
    public void setUnits(RangeUnits rangeUnits)
    {
        this.rangeUnits = rangeUnits;

        // If our value is visible, then recalculate it
        if (chkShowTF.isSelected()) btnCalculateActionPerformed();
    }

    /**
     * Calculate the length of a sample segment based on the LOS length of the path.
     */
    private void calculateSampleLength()
    {
        GlobeRuler globeRuler = new GlobeRuler();
        globeRuler.setFollowTerrain(false);
        globeRuler.setPathType(WorldWind.LINEAR);

        // Initialize the length measurer with the points long the path
        globeRuler.setPositions(getPositionsFromPath(), 0);

        final WorldWindow wwd = globeManager.getPrimaryGlobeDelegate().getWorldWindow();

        losLength = globeRuler.getLength(wwd.getModel().getGlobe());

        double sampleLength = losLength / SAMPLE_COUNT_FOR_ESTIMATE;

        // Format with reasonable precision.  If LOS length is > 1 Km, then use whole meters, else use 2 decimal points
        final var lengthText = String.format((losLength > 1000) ? "%.0f" : "%.2f", sampleLength);
        txtSampleLength.setText(lengthText);
        lblSampleDistance.setText("Sampling distance (" + RangeUnits.METERS.getAbbreviation() + ")");
    }

    private ArrayList<LatLon> getPositionsFromPath()
    {
        return path.getVertices().stream()
                .map(location -> LatLon.fromDegrees(location.getLatitude(), location.getLongitude()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Calculate the terrain following length of the current path
     */
    private void btnCalculateActionPerformed()
    {
        if (worker != null && !worker.isDone())
        {
            worker.cancel(true);
            worker = null;
            return;
        }

        if (terrain == null)
        {
            final WorldWindow wwd = globeManager.getPrimaryGlobeDelegate().getWorldWindow();
            terrain = new HighResolutionTerrain(wwd.getModel().getGlobe(), null);
            terrain.setTimeout(TIMEOUT);
        }

        // Do this every time to ensure most up-to-date data (and skip using a listener)
        measurer.setPositions(getPositionsFromPath(), 0);

        // Calculate the number of samples from the sample length
        double stepLength;

        try
        {
            stepLength = Double.parseDouble(txtSampleLength.getText());
        } catch (NumberFormatException e)
        {
            JOptionPane.showMessageDialog(this,
                    "Sample length must be a valid number, you entered: " + txtSampleLength.getText(),
                    "Invalid Sample Length", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Step length must be > 0
        if (stepLength <= 0)
        {
            JOptionPane.showMessageDialog(this,
                    "Sample length must be greater than zero.", "Invalid Sample Length", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int steps = (int) (losLength / stepLength);
        if (steps == 0) steps = 1;

        btnCalculate.setText("Cancel");

        CardLayout layout = (CardLayout) lengthPanel.getLayout();
        layout.show(lengthPanel, "progressBar");

        worker = new MeasurementWorker(terrain, steps);
        measurer.setProgessCallback(worker);
        worker.execute();
    }

    /**
     * Toggle the visibility of the TF panel based on the checkbox state.
     */
    private void chkShowTFActionPerformed()
    {
        final boolean selected = chkShowTF.isSelected();
        terrainCalculationsPanel.setVisible(selected);

        if (selected)
        {
            btnCalculateActionPerformed(); // Also do the calculation
        }
    }

    /**
     * When the user presses enter in the sample length field, then go ahead and do the calculation.
     */
    private void txtSampleLengthActionPerformed()
    {
        btnCalculateActionPerformed();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        chkShowTF = new JCheckBox();
        var panel1 = new JPanel();
        terrainCalculationsPanel = new JPanel();
        var panel3 = new JPanel();
        lblSampleDistance = new JLabel();
        txtSampleLength = new JTextField();
        var pnlTFLength = new JPanel();
        var lengthBorder = new JPanel();
        var label22 = new JLabel();
        lengthPanel = new JPanel();
        lblTFLength = new JLabel();
        progressBar = new JProgressBar();
        btnCalculate = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, default"));

        //---- chkShowTF ----
        chkShowTF.setText("Show Terrain Following (TF) length");
        chkShowTF.setName("chkShowTF");
        chkShowTF.addActionListener(e -> chkShowTFActionPerformed());
        add(chkShowTF, cc.xy(1, 1));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "2*($ugap), default:grow",
                    "default"));

            //======== terrainCalculationsPanel ========
            {
                terrainCalculationsPanel.setBorder(new TitledBorder("Terrain Calculations"));
                terrainCalculationsPanel.setName("terrainCalculationsPanel");
                terrainCalculationsPanel.setLayout(new FormLayout(
                        "default:grow",
                        "2*(default)"));

                //======== panel3 ========
                {
                    panel3.setName("panel3");
                    panel3.setLayout(new FormLayout(
                            "default, $lcgap, default:grow",
                            "default"));

                    //---- lblSampleDistance ----
                    lblSampleDistance.setText("Sampling Distance (m)");
                    lblSampleDistance.setName("lblSampleDistance");
                    panel3.add(lblSampleDistance, cc.xy(1, 1));

                    //---- txtSampleLength ----
                    txtSampleLength.setColumns(8);
                    txtSampleLength.setName("txtSampleLength");
                    txtSampleLength.addActionListener(e -> txtSampleLengthActionPerformed());
                    panel3.add(txtSampleLength, cc.xy(3, 1));
                }
                terrainCalculationsPanel.add(panel3, cc.xy(1, 1));

                //======== pnlTFLength ========
                {
                    pnlTFLength.setName("pnlTFLength");
                    pnlTFLength.setLayout(new FormLayout(
                            "default, 2*($lcgap, default:grow), $lcgap, default",
                            "2*(default)"));

                    //======== lengthBorder ========
                    {
                        lengthBorder.setName("lengthBorder");
                        lengthBorder.setLayout(new FormLayout(
                                "default",
                                "default"));

                        //---- label22 ----
                        label22.setText("TF Length");
                        label22.setName("label22");
                        lengthBorder.add(label22, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.CENTER));
                    }
                    pnlTFLength.add(lengthBorder, cc.xy(1, 1));

                    //======== lengthPanel ========
                    {
                        lengthPanel.setName("lengthPanel");
                        lengthPanel.setLayout(new CardLayout());

                        //---- lblTFLength ----
                        lblTFLength.setName("lblTFLength");
                        lengthPanel.add(lblTFLength, "label");

                        //---- progressBar ----
                        progressBar.setName("progressBar");
                        lengthPanel.add(progressBar, "progressBar");
                    }
                    pnlTFLength.add(lengthPanel, cc.xywh(3, 1, 5, 1));

                    //---- btnCalculate ----
                    btnCalculate.setText("Calculate");
                    btnCalculate.setName("btnCalculate");
                    btnCalculate.addActionListener(e -> btnCalculateActionPerformed());
                    pnlTFLength.add(btnCalculate, cc.xy(7, 2));
                }
                terrainCalculationsPanel.add(pnlTFLength, cc.xy(1, 2));
            }
            panel1.add(terrainCalculationsPanel, cc.xy(3, 1));
        }
        add(panel1, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    class MeasurementWorker extends SwingWorker<Double, Object> implements IProgressCallback
    {
        private final Terrain terrain;
        private final int steps;

        public MeasurementWorker(Terrain terrain, int steps)
        {
            this.terrain = terrain;
            this.steps = steps;
        }

        @Override
        protected Double doInBackground() throws Exception
        {
            measurer.setFollowTerrain(true);
            measurer.setLengthTerrainSamplingSteps(steps);

            return measurer.getLength(terrain);
        }

        @Override
        protected void done()
        {
            try
            {
                final Double length = get();
                final var lengthText = rangeUnits.format(length, 1);
                lblTFLength.setText(lengthText);
            } catch (CancellationException e)
            {
                logger.info("Calculation cancelled");
                lblTFLength.setText("Cancelled");
            } catch (InterruptedException e)
            {
                logger.warn("Interrupted computing length of path.");
                lblTFLength.setText("Cancelled");
            } catch (ExecutionException e)
            {
                if (e.getCause() instanceof WWRuntimeException)
                {
                    AlwaysOnTopJOptionPane.showErrorDialog("Time out occurred attempting to retrieve elevation data.", "Error");
                } else
                {
                    logger.warn("Exception computing length of path", e);
                    AlwaysOnTopJOptionPane.showErrorDialog("Error occurred calculating path length.", "Error");
                }
            } finally
            {
                progressBar.setValue(0);

                CardLayout layout = (CardLayout) lengthPanel.getLayout();
                layout.show(lengthPanel, "label");

                btnCalculate.setText("Calculate");
            }
        }

        @Override
        public void onStageStarted(String stage)
        {
        }

        @Override
        public void onProgress(final double progress)
        {
            SwingUtilities.invokeLater(() -> progressBar.setValue((int) progress));
        }

        @Override
        public void onError(Throwable t)
        {
        }

        @Override
        public boolean isCancel()
        {
            return false;
        }

        private String trimDoubleString(String doubleString)
        {
            double d = Double.parseDouble(doubleString);
            return String.format("%.3f", d);
        }
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JCheckBox chkShowTF;
    private JPanel terrainCalculationsPanel;
    private JLabel lblSampleDistance;
    private JTextField txtSampleLength;
    private JPanel lengthPanel;
    private JLabel lblTFLength;
    private JProgressBar progressBar;
    private JButton btnCalculate;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
