package gov.takx.core.view.gui.maps;

import gov.takx.api.annotation.InvokeOnEDT;

import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;

/**
 */
public class AddKMLOverlayDialog extends AddOverlayDialog implements AddOverlayDialog.IOverlayDialogListener
{
    private static AddKMLOverlayDialog instance = null;

    public AddKMLOverlayDialog()
    {
        super("Overlay KML/KMZ");
        setDescription("Select a KML or KMZ file to be overlaid on the globe. A KML/KMZ overlay can be turned " +
                "on and off as a single layer in the Layers panel.");
        setFileFilter(new FileNameExtensionFilter("KML/KMZ Files (*.kml, *.kmz)", "kml", "kmz"));

        setIcon("kml_overlay.png");
        setOverlayDialogListener(this);
    }

    @Override
    public void addOverlayFile(File overlayFile)
    {
        detect3DOverlayFileAndImport(new AddKMLOverlayController(overlayFile));
    }

    @SuppressWarnings("NonThreadSafeLazyInitialization") // Single-threaded since only on EDT
    @InvokeOnEDT
    public static void showInstance()
    {
        if (instance == null) instance = new AddKMLOverlayDialog();
        instance.showOnTop();
    }
}
