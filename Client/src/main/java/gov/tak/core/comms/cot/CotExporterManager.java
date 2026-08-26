package gov.tak.core.comms.cot;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.CotEventExporter;
import gov.tak.api.cot.exporter.ICotExporterManager;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.cot.exporter.ArrowExporter;
import gov.tak.platform.cot.exporter.BullseyeExporter;
import gov.tak.platform.cot.exporter.CircleExporter;
import gov.tak.platform.cot.exporter.EllipseExporter;
import gov.tak.platform.cot.exporter.MarkerExporter;
import gov.tak.platform.cot.exporter.MissionExporter;
import gov.tak.platform.cot.exporter.MultiPolylineExporter;
import gov.tak.platform.cot.exporter.PolygonExporter;
import gov.tak.platform.cot.exporter.PolylineExporter;
import gov.tak.platform.cot.exporter.RectangleExporter;
import gov.tak.platform.cot.exporter.RouteExporter;
import gov.tak.platform.cot.exporter.SquareExporter;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.dataservices.persist.MapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service to manage CoT event exporters.  Exporters convert MapObjects into {@link CotEvent} messages.
 *
 * @since 3.0
 */
@ApplicationScoped
public class CotExporterManager implements ICotExporterManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<Class<? extends MapObject>, CotEventExporter> exportersByType = new ConcurrentHashMap<>();

    /**
     * Start our service.
     */
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices event)
    {
        event.getProgressMonitor().setText("Starting CoT Services");
        registerDefaultExporters();
    }

    /**
     * Stop this service, clear all registered importers.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        exportersByType.clear();
    }

    @Override
    public void registerExporter(@Nonnull CotEventExporter exporter)
    {
        exportersByType.put(exporter.getType(), exporter);
    }

    @Override
    public void unregisterExporter(@Nonnull CotEventExporter exporter)
    {
        exportersByType.remove(exporter.getType());
    }

    @Override
    public CotEvent export(@Nonnull IMapObject mapObject)
    {
        if (mapObject.getMetaBoolean(StandardAttributeNames.NEVER_COT, false))
        {
            logger.debug("Skipping MO marked as 'nevercot': {}", mapObject.getKey());
            return null;
        }

        final CotEventExporter exporter = findExporter(mapObject.getClass());

        if (exporter != null)
        {
            try
            {
                return exporter.export(mapObject);
            } catch (Exception e)
            {
                logger.warn("Failed to export MapObject as CoT: {}", e.getMessage());
            }
        } else
        {
            logger.warn("No exporter registered for type: {}", mapObject.getClass());
        }
        return null;
    }

    /**
     * Find a registered exporter for the given type.  If no exporter is registered for the exact type,
     * try the superclass of the given type.  Continue until an exporter is found, or until we run out of superclasses.
     *
     * @param mapObjectType Type of Map Object
     * @return Registered exporter, or {@code null} if none found
     */
    @VisibleForTesting
    @Nullable
    CotEventExporter findExporter(@Nonnull Class<?> mapObjectType)
    {
        Class<?> type = mapObjectType;
        CotEventExporter exporter = exportersByType.get(type);

        while (exporter == null && type != Object.class)
        {
            type = type.getSuperclass();
            exporter = exportersByType.get(type);
        }

        return exporter;
    }

    /**
     * Register our default (core) exporters.
     */
    private void registerDefaultExporters()
    {
        registerExporter(new ArrowExporter());
        registerExporter(new BullseyeExporter());
        registerExporter(new CircleExporter());
        registerExporter(new EllipseExporter());
        registerExporter(new MarkerExporter());
        registerExporter(new MultiPolylineExporter());
        registerExporter(new PolygonExporter());
        registerExporter(new PolylineExporter());
        registerExporter(new RectangleExporter());
        registerExporter(new SquareExporter());
        registerExporter(new RouteExporter());
        registerExporter(new MissionExporter());
    }
}
