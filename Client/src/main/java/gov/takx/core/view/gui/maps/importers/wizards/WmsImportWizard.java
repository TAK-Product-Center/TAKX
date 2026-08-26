package gov.takx.core.view.gui.maps.importers.wizards;

import com.atakmap.map.layer.Layer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.globe.TakLayerFactory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Wizard for importing WMS imagery data.
 * @since 5.8
 */
@PseudoBean
public class WmsImportWizard extends AMapImportWizard
{
    public static final String TYPE = "maps_imagery_wms";

    private static final Pattern NAME_ATTR_PATTERN = Pattern.compile(".*<name>(.+)<\\/name>.*");
    private static final Predicate<File> WMS_FILTER = file -> {
        if (!file.exists()) return false;
        if (!"xml".equalsIgnoreCase(FileUtils.getExtension(file))) return false;
        return Arrays.stream(FileUtils.readLines(file)).anyMatch(s -> s.contains("<customWmsMapSource>"));
    };

    @Inject
    private TakLayerFactory layerFactory;
    @Inject
    private IRaptorLayerManager layerManager;

    public WmsImportWizard()
    {
        super(WMS_FILTER);
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports WMS map sources that will be displayed as new layer(s) on the globe.";
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> preconfigProps)
    {
        File target = file;
        if (target == null && !files.isEmpty()) target = files.get(0);
        if (target == null) return;

        // Extract layer name from the config file, if specified.
        // Otherwise, fall back to the file name (sans extension)
        String layerName = Arrays.stream(FileUtils.readLines(target))
                .map(line -> {
                    Matcher matcher = NAME_ATTR_PATTERN.matcher(line);
                    if (matcher.find())
                        return matcher.group(1);
                    return "";
                })
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse(FileUtils.getNameWithoutExtension(target));

        Layer layer = layerFactory.createRasterLayer(target, layerName);
        layerManager.addLayer(layer, LayerGroup.IMAGERY);
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "WMS Imagery Import";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "imagery_overlay.png");
    }
}
