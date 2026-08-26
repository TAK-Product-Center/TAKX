package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.core.globe.imagerywizard.ModelFileSetFilter;
import gov.takx.core.view.gui.maps.LoadModelFilesDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Wizard for importing 3D model data.
 *
 * @since 5.3
 */
public class ModelMapImportWizard extends AMapImportWizard
{
    public static final String TYPE = "maps_model";

    private static final ModelFileSetFilter MODEL_SET_FILTER = new ModelFileSetFilter();

    public ModelMapImportWizard()
    {
        super(MODEL_SET_FILTER::accept);
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports 3D model data that will be displayed as new layer(s) on the globe.";
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        new LoadModelFilesDialog(files.toArray(new File[0])).setVisible(true);
        return null;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "3D Model Import";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "icon_3d_map.png");
    }
}
