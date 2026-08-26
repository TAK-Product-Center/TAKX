package gov.tak.core.comms.cot;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.CotEventImporter;
import gov.tak.api.cot.importer.CotEventTypeImporter;
import gov.tak.api.cot.importer.ICotImporterManager;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.platform.cot.detail.CotDetailManager;
import gov.tak.platform.cot.importer.ArrowImporter;
import gov.tak.platform.cot.importer.BullseyeImporter;
import gov.tak.platform.cot.importer.CircleImporter;
import gov.tak.platform.cot.importer.CotDeleteImporter;
import gov.tak.platform.cot.importer.EllipseImporter;
import gov.tak.platform.cot.importer.EmergencyCancelImporter;
import gov.tak.platform.cot.importer.MarkerImporter;
import gov.tak.platform.cot.importer.MissionImporter;
import gov.tak.platform.cot.importer.MultiPolylineImporter;
import gov.tak.platform.cot.importer.PolyShapeImporter;
import gov.tak.platform.cot.importer.RectangleImporter;
import gov.tak.platform.cot.importer.RouteImporter;
import gov.tak.platform.cot.importer.SpiDeleteImporter;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service to manage CoT message importers.  Importers convert CoT event message data into Map Objects or other
 * local operations (like deleting Map Objects).
 *
 * @since 3.0
 */
@Singleton
public class CotImporterManager implements ICotImporterManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CotDetailManager cotDetailManager;

    // Importers which handle specific CoT types, these are always given precedence over non-type-specific importers
    private final Map<String, List<CotEventTypeImporter>> typeSpecificImporters = new ConcurrentHashMap<>();

    // Importers which do not handle specific CoT types, called if no type-specific importer is matched
    private final List<CotEventImporter> nonSpecificImporters = Collections.synchronizedList(new ArrayList<>());

    // Default importer to use if no other registered importer claims SUCCESS/FAILURE (creates a simple Marker)
    private final CotEventImporter defaultImporter = new MarkerImporter(true, "");

    // Set of CoT types we ignore
    private static final Set<String> FILTERED_TYPES = new HashSet<>();

    // Set of CoT Type prefixes to ignore
    private static final Set<String> FILTERED_TYPE_PREFIXES = new HashSet<>();

    static
    {
        FILTERED_TYPES.add("t-x-c-t"); // Ping
        FILTERED_TYPES.add("t-x-c-t-r"); // Pong
        FILTERED_TYPES.add("b-f-t-a"); // Mission Package ACK
        FILTERED_TYPES.add("a-u-X"); // Unknown "other" incident
        FILTERED_TYPES.add("effects_event");
        FILTERED_TYPES.add("t-x-a-m-Geofence");
        FILTERED_TYPES.add(CotTypes.TASK_DELETE);
        FILTERED_TYPES.add(CotTypes.FILE_TRANSFER_REQUEST);

        FILTERED_TYPE_PREFIXES.add("a-f-G-E-S-rad"); // Friendly RAD sensors
        FILTERED_TYPE_PREFIXES.add("y"); // Task responses
        FILTERED_TYPE_PREFIXES.add("c"); // Capabilities
        FILTERED_TYPE_PREFIXES.add(CotTypes.PREFIX_FREE_TEXT); // Chat messages
    }

    @Inject
    CotImporterManager(CotDetailManager cotDetailManager)
    {
        this.cotDetailManager = cotDetailManager;
    }

    /**
     * Start our service.
     */
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices event)
    {
        registerDefaultImporters();

        cotDetailManager.startService();
    }

    /**
     * Stop this service, clear all registered importers.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        typeSpecificImporters.clear();
        nonSpecificImporters.clear();

        cotDetailManager.stopService();
    }

    @Override
    public void registerImporter(CotEventImporter importer)
    {
        if (importer instanceof CotEventTypeImporter typeImporter)
        {
            // If the importer handles specific types, and not type prefixes, then add it to typeSpecificImporters
            // for each declared type.
            if (!typeImporter.isPrefixOnly())
            {
                typeImporter.getSupportedCotTypes()
                        .forEach(type -> typeSpecificImporters.computeIfAbsent(type,
                                k -> Collections.synchronizedList(new ArrayList<>())).add(typeImporter));

                return;
            }
        }

        nonSpecificImporters.add(importer);
    }

    @Override
    public void unregisterImporter(CotEventImporter importer)
    {
        if (importer instanceof CotEventTypeImporter typeImporter)
        {
            if (!typeImporter.isPrefixOnly())
            {
                // Remove the importer from each type-specific list
                typeImporter.getSupportedCotTypes().forEach(type -> {
                    if (typeSpecificImporters.containsKey(type)) typeSpecificImporters.get(type).remove(typeImporter);
                });
            }
        }

        nonSpecificImporters.remove(importer);
    }

    /**
     * Dispatch the given CoT event to appropriate registered importers, stopping after the first one returns
     * {@link ImportResult#SUCCESS}, or {@link ImportResult#FAILURE} (which indicates the event was not imported, and
     * should NOT be handed to any subsequent importers).
     * <p>
     * Importers registered on specific CoT types are given precedence over those that don't.
     *
     * @param event CoT Event to process
     * @return import result
     */
    @VisibleForTesting
    public ImportResult importEvent(CotEvent event)
    {
        final String type = event.getType();

        // Find matching importers
        final List<CotEventTypeImporter> typeImporters = typeSpecificImporters.get(type);

        final List<? extends CotEventImporter> importers = typeImporters != null && !typeImporters.isEmpty()
                ? typeImporters
                : nonSpecificImporters;

        // Process with matching importers
        for (CotEventImporter importer : importers)
        {
            final ImportResult result = runImporter(importer, event);

            // If the importer returned IGNORE, then move on to the next one
            if (result != ImportResult.IGNORE)
            {
                if (result == ImportResult.FAILURE) logger.error("Importer {} failed on type: {}", importer, type);
                return result;
            }
        }

        // If no registered importer handled it, fallback to the default marker importer unless it is
        // one we specifically want to filter out.
        if (logger.isDebugEnabled()) logger.debug("Using default importer for event: {}/{}", type, event.getUID());

        if (isFiltered(type, event.getDetail()))
        {
            if (logger.isDebugEnabled()) logger.debug("Skip event: {}/{}", type, event.getUID());
            return ImportResult.FAILURE;
        } else
        {
            return runImporter(defaultImporter, event);
        }
    }

    /**
     * Run an importer and trap any exceptions.
     *
     * @param importer Importer to run
     * @param event    CoT event for the importer
     * @return importer result
     */
    @VisibleForTesting
    ImportResult runImporter(CotEventImporter importer, CotEvent event)
    {
        try
        {
            return importer.importData(event);
        } catch (Exception e)
        {
            logger.error("Failed to import event: {}/{}", event.getType(), event.getUID(), e);
            return ImportResult.FAILURE;
        }
    }

    /**
     * Determine if the event should be filtered (not processed) based on its type and detail.
     *
     * @param type   CoT type
     * @param detail Detail from the event
     * @return {@code true} if the event should be filtered out
     */
    private boolean isFiltered(String type, CotDetail detail)
    {
        if (FILTERED_TYPES.contains(type)) return true;

        // Check prefixes
        if (FILTERED_TYPE_PREFIXES.stream().anyMatch(type::startsWith)) return true;

        // Check detail for rad sensor
        if (detail != null && detail.getFirstChildByName("rad-event") != null) return true;

        // Check for unsupported tasking
        return type.startsWith("t")
                && !type.equals(CotTypes.TASK_DESTROY) && !type.equals(CotTypes.TASK_DELETE);
    }

    /**
     * Register our default (core) importers.
     */
    private void registerDefaultImporters()
    {
        registerImporter(new ArrowImporter());
        registerImporter(new BullseyeImporter());
        registerImporter(new CircleImporter());
        registerImporter(new CotDeleteImporter());
        registerImporter(new EllipseImporter());
        registerImporter(new EmergencyCancelImporter());
        registerImporter(new MissionImporter());
        registerImporter(new MultiPolylineImporter());
        registerImporter(new PolyShapeImporter());
        registerImporter(new RectangleImporter());
        registerImporter(new RouteImporter());
        registerImporter(new SpiDeleteImporter());
    }
}
