package gov.takx.core.view.gui.maps;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.kml.KmlMapObjectImporter;
import jakarta.inject.Inject;

import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;

/**
 */
@PseudoBean
public class AddKMLMapObjectsDialog extends AddOverlayDialog implements AddOverlayDialog.IOverlayDialogListener
{
    private static AddKMLMapObjectsDialog instance = null;

    @Inject
    private KmlMapObjectImporter kmlMapObjectImporter;

    public AddKMLMapObjectsDialog()
    {
        super("Import KML/KMZ Map Objects");
        setDescription("Select a KML or KMZ file containing map object data to create map objects on the globe. Map " +
                "objects will show up in the Overlays panel.");
        setFileFilter(new FileNameExtensionFilter("KML/KMZ Files (*.kml, *.kmz)", "kml", "kmz"));

        setIcon("kml_overlay.png");
        setOverlayDialogListener(this);
        enableExtrasPanel(false);
    }

    @Override
    public void addOverlayFile(File overlayFile)
    {
        kmlMapObjectImporter.importKML(overlayFile);
    }

    @SuppressWarnings("NonThreadSafeLazyInitialization") // Single-threaded since only on EDT
    @InvokeOnEDT
    public static void showInstance()
    {
        if (instance == null) instance = new AddKMLMapObjectsDialog();
        instance.showOnTop();
    }
}
