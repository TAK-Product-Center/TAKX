package gov.takx.core.view.gui.maps;

import gov.takx.api.annotation.InvokeOnEDT;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;

/**
 */
public class AddShapeOverlayDialog extends AddOverlayDialog implements AddOverlayDialog.IOverlayDialogListener
{
    private static AddShapeOverlayDialog instance = null;

    public AddShapeOverlayDialog()
    {
        super("Overlay SHP");
        setIcon("shp_overlay.png");
        setDescription("Select a SHP file to be overlaid on the globe. A SHP overlay can be turned on and off as a single layer in the Layers panel.");
        setFileFilter(new FileNameExtensionFilter("SHP Files (*.shp)", "shp"));
        setOverlayDialogListener(this);
    }

    @SuppressWarnings("NonThreadSafeLazyInitialization") // Single-threaded since only on EDT
    @InvokeOnEDT
    public static void showInstance()
    {
        if (instance == null) instance = new AddShapeOverlayDialog();
        instance.showOnTop();
    }

    @Override
    public void addOverlayFile(final File overlayFile)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> addOverlayFile(overlayFile));
            return;
        }

        detect3DOverlayFileAndImport(new AddShapeOverlayController(overlayFile));
    }
}
