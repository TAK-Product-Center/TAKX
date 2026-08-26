package gov.takx.core.persist.breadcrumb;

import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.query.param.HQLParameter;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Persistence helpers for {@link IBreadcrumb breadcrumbs}.
 *
 * @since 5.3
 */
@ApplicationScoped
public class BreadcrumbPersist
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BREADCRUMB_TABLE = Breadcrumb.class.getSimpleName();
    private static final String BREADCRUMBS_BY_TRACK_UID_QUERY = "FROM " + BREADCRUMB_TABLE + " WHERE trackUid = :trackUid ORDER BY time";
    private static final String DELETE_TRACK = "DELETE FROM " + BREADCRUMB_TABLE + " WHERE trackUid = :trackUid";

    private final IPersistServiceProvider persistServiceProvider;
    private final ICoreExecutorService executorService;

    @Inject
    BreadcrumbPersist(IPersistServiceProvider persistServiceProvider, ICoreExecutorService executorService)
    {
        this.persistServiceProvider = persistServiceProvider;
        this.executorService = executorService;
    }

    /**
     * @param breadcrumb The breadcrumb to persist
     */
    public void persist(@Nonnull Breadcrumb breadcrumb)
    {
        getPersistService().persistEntity(breadcrumb);
    }

    /**
     * Deletes all {@link IBreadcrumb breadcrumbs} associated with a {@link gov.takx.api.breadcrumb.ITrack}.
     *
     * @param trackUid The track's UID
     */
    public void deleteTrack(@Nonnull String trackUid)
    {
        getPersistService().executeNativeUpdateSQL(DELETE_TRACK, new HQLParameter("trackUid", trackUid));
    }

    /**
     * @param trackUid The {@link gov.takx.api.breadcrumb.ITrack track's} UID
     * @return all breadcrumbs that are part of the specified {@link gov.takx.api.breadcrumb.ITrack}
     */
    @Nonnull
    public List<IBreadcrumb> getBreadcrumbs(@Nonnull String trackUid)
    {
        try
        {
            // Allows us to call cleanupAndClose without affecting existing sessions
            return CompletableFuture.supplyAsync(
                    () -> {
                        Thread.currentThread().setName("Track breadcrumbs query");

                        final var persistService = getPersistService();
                        final var breadcrumbs = persistService.<IBreadcrumb>query(BREADCRUMBS_BY_TRACK_UID_QUERY,
                                new HQLParameter("trackUid", trackUid));

                        // Keep the breadcrumbs out of the session to allow for native delete in deleteTrack(); queried
                        // breadcrumbs should be immutable
                        persistService.cleanupAndClose();

                        return breadcrumbs;
                    },
                    executorService).get();
        } catch (InterruptedException | ExecutionException e)
        {
            logger.error("Error retrieving breadcrumbs for track {}", trackUid, e);
            return Collections.emptyList();
        }
    }

    private IPersistService getPersistService()
    {
        return persistServiceProvider.getPersistService();
    }
}
