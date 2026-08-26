package gov.takx.api.routes;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IRoute;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.InputStream;

/**
 * A utility class to import a file into a {@link IRoute}.
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
@InjectableService
@NoPluginImplementations
public interface IRouteImportService
{
    /**
     * Import a GPX file to a new {@link IRoute} map object
     * @param gpx the GPX file to parse and convert to a Route
     */
    void gpxToRoute(@Nonnull File gpx);

    /**
     * Import a GPX file as an InputStream to an {@link IRoute} new map object
     * @param gpx the GPX file InputStream to parse and convert to a Route
     */
    void gpxToRoute(@Nonnull InputStream gpx);
}
