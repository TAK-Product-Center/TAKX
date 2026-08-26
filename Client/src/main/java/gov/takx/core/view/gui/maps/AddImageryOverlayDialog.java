package gov.takx.core.view.gui.maps;

import gov.takx.api.media.MediaType;
import gov.takx.core.globe.layers.LayerImportParameters;
import gov.takx.core.view.gui.maps.importers.OverlayImageImporter;
import gov.takx.core.view.gui.maps.importers.SurfaceImageryFileImporter;
import gov.takx.platform.preferences.PreferenceEntry;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import java.io.File;

/**
 */
public class AddImageryOverlayDialog extends AddOverlayDialog implements AddOverlayDialog.IOverlayDialogListener
{
    private static AddImageryOverlayDialog instance = null;

    private final JCheckBox copyToVfsCheck = new JCheckBox("Copy image to project", true);

    public AddImageryOverlayDialog()
    {
        super("Overlay Image");

        final String programName = PreferenceEntry.NAME.getStringValue();

        setDescription("The selected image will be overlaid on the surface of the globe and can be moved, stretched," +
                " and adjusted. " + programName + " will search for geographic information in the image" +
                " file. If this information exists, it will be used to initially place the image, but after " +
                "placement the image can still be adjusted. Image overlays can be found in the Layers panel under " +
                "Overlays in the drop down menu.");

        // CORE-3462
        setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File f)
            {
                for (String suffix : MediaType.Image.getExtensions())
                {
                    if ((f.getName().toLowerCase().endsWith(suffix) && SurfaceImageryFileImporter.isValidImageGeotiffFile(f))
                            || f.isDirectory())
                    {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription()
            {
                return "Images";
            }
        });

        setIcon("imagery_overlay.png");

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.X_AXIS));
        checkboxPanel.add(Box.createHorizontalGlue());
        checkboxPanel.add(copyToVfsCheck);
        copyToVfsCheck.setToolTipText("When enabled, any imported imagery will first get copied to the project for centralized tracking.\n"
                + "When disabled, the file will be read from in its provided location.");

        enableExtrasPanel(true);
        setExtrasPanel(checkboxPanel);

        setOverlayDialogListener(this);
    }

    @Override
    public void addOverlayFile(File overlayFile)
    {
        new OverlayImageImporter(overlayFile, new LayerImportParameters(copyToVfsCheck.isSelected())).beginImport();
    }

    public synchronized static void showInstance()
    {
        if (instance == null) instance = new AddImageryOverlayDialog();
        instance.showOnTop();
    }
}
