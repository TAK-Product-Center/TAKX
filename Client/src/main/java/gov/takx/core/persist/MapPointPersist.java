package gov.takx.core.persist;

import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapPointPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.core.mapdata.MapPoint;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Persistence helpers for MapPoint objects.  MapPoints are Data Transfer Objects (DTO) and therefore unique from our
 * other persistent entities.  They are created on demand, and are not persistent as far as hibernate is concerned.
 * We construct new ones on each query.
 */
@ApplicationScoped
public class MapPointPersist implements IMapPointPersist
{
    private static final String SELECT_NEW_MAPPOINT = "select new " + MapPoint.class.getName()
            + "(rdm.id, rdm.mapObject_id, rdm.latitude, rdm.longitude, rdm.elevation, rdm.heading, rdm.hasElevation, rdm.hasHeading, rdm.hasTime, rdm.time, rdm.receiveTime)";

    private static final String BASE_SELECT =
            SELECT_NEW_MAPPOINT
                    + " from RaptorDataMessage rdm"
                    + " where rdm.hasLocation=true"
                    + "   and rdm.isManualLocation=false"
                    + "   and rdm.isRenderable=true";

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @UseClientClassLoader
    @Override
    public List<MapPoint> searchMapObject(@Nonnull IMapObject mo)
    {
        String hql = BASE_SELECT + " and rdm.mapObject_id = :mo";

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("mo", mo));
    }

    @UseClientClassLoader
    @Override
    public List<MapPoint> getByIds(@Nonnull Collection<Integer> rdmIds)
    {
        if (rdmIds.isEmpty()) throw new IllegalArgumentException("rdmIds must not be empty");

        String hql = BASE_SELECT + " and rdm.id in (:rdmIds)";

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("rdmIds", rdmIds));
    }

    @UseClientClassLoader
    @Override
    public List<MapPoint> searchLatLong(@Nonnull IMapObject mo, @Nonnull ILocation minLoc, @Nonnull ILocation maxLoc)
    {
        String hql = BASE_SELECT
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax"
                + "   and rdm.mapObject = :mo";

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("latMin", min(minLoc.getLatitude(), maxLoc.getLatitude())),
                new HQLParameter("lonMin", min(minLoc.getLongitude(), maxLoc.getLongitude())),
                new HQLParameter("latMax", max(minLoc.getLatitude(), maxLoc.getLatitude())),
                new HQLParameter("lonMax", max(minLoc.getLongitude(), maxLoc.getLongitude())),
                new HQLParameter("mo", mo));
    }

    @UseClientClassLoader
    @Override
    public List<MapPoint> searchTimeThreshold(@Nonnull IMapObject mo, long threshold)
    {
        String hql = BASE_SELECT
                + "   and ((rdm.hasTime=true and rdm.time >= :threshold) or (rdm.hasTime=false and rdm.receiveTime >= :threshold))"
                + "   and rdm.mapObject = :mo";

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("threshold", threshold),
                new HQLParameter("mo", mo));
    }

    @UseClientClassLoader
    @Override
    public List<MapPoint> searchLatLongTimeThreshold(@Nonnull IMapObject mo, @Nonnull ILocation minLoc, @Nonnull ILocation maxLoc, long threshold)
    {
        String hql = BASE_SELECT
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax"
                + "   and ((rdm.hasTime=true and rdm.time >= :threshold) or (rdm.hasTime=false and rdm.receiveTime >= :threshold))"
                + "   and rdm.mapObject = :mo";

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("latMin", min(minLoc.getLatitude(), maxLoc.getLatitude())),
                new HQLParameter("lonMin", min(minLoc.getLongitude(), maxLoc.getLongitude())),
                new HQLParameter("latMax", max(minLoc.getLatitude(), maxLoc.getLatitude())),
                new HQLParameter("lonMax", max(minLoc.getLongitude(), maxLoc.getLongitude())),
                new HQLParameter("threshold", threshold),
                new HQLParameter("mo", mo));
    }

    @UseClientClassLoader
    @Override
    public List<MapPoint> searchLatLongDate(@Nonnull IMapObject mo, @Nonnull ILocation minLoc, @Nonnull ILocation maxLoc, long minDT, long maxDT)
    {
        String hql = BASE_SELECT
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax"
                + "   and rdm.receiveTime >= :minDT"
                + "   and rdm.receiveTime <= :maxDT"
                + "  and rdm.mapObject = :mo";

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("latMin", min(minLoc.getLatitude(), maxLoc.getLatitude())),
                new HQLParameter("lonMin", min(minLoc.getLongitude(), maxLoc.getLongitude())),
                new HQLParameter("latMax", max(minLoc.getLatitude(), maxLoc.getLatitude())),
                new HQLParameter("lonMax", max(minLoc.getLongitude(), maxLoc.getLongitude())),
                new HQLParameter("minDT", min(minDT, maxDT)),
                new HQLParameter("maxDT", max(minDT, maxDT)),
                new HQLParameter("mo", mo));
    }

    @UseClientClassLoader
    @Override
    public List<MapPoint> searchReceiveDate(@Nonnull IMapObject mo, long minDT, long maxDT)
    {
        String hql = BASE_SELECT
                + "   and rdm.receiveTime >= :minDT"
                + "   and rdm.receiveTime <= :maxDT"
                + "   and rdm.mapObject = :mo";

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("minDT", min(minDT, maxDT)),
                new HQLParameter("maxDT", max(minDT, maxDT)),
                new HQLParameter("mo", mo));
    }
}
