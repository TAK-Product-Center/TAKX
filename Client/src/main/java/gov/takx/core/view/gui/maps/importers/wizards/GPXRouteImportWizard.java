package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.core.routes.RouteImportService;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Import a GPX file containing a Route.
 *
 * @since 5.8
 */
@PseudoBean
public class GPXRouteImportWizard extends AFileImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String TYPE = "gpx_route_map_objects";

    @Inject
    private RouteImportService routeImportService;

    @Inject
    private INotificationService notificationService;

    @Override
    public String getDisplayName()
    {
        return "GPX Route";
    }

    @Override
    public String getHelpMessage()
    {
        return "Import Routes from a file into a new or existing map object.";
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "gpxRoute_import.png");
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        return fileExtension.equalsIgnoreCase("gpx");
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> preconfigProps)
    {
        try
        {
            routeImportService.gpxToRoute(file);
        } catch (Exception e)
        {
            notificationService.show(Notification.builder("GPX Route Import failed", Notification.Type.Error).build());
            logger.error("GPX Route Import failed", e);
        }
    }

    @Nullable
    @Override
    public Map<String, Object> showImporterWizardDialog() throws ImportCancelledException
    {
        return null;
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
