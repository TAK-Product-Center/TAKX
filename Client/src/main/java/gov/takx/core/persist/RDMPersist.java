package gov.takx.core.persist;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IRDMPersist;
import gov.takx.api.query.param.HQLParameter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 */
@ApplicationScoped
public class RDMPersist implements IRDMPersist
{
    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Override
    public List<IRaptorDataMessage> searchLatLong(@Nullable IMapObject mo, @Nonnull Sector bounds)
    {
        return searchLatLong(mo, bounds, null);
    }

    @Override
    public List<IRaptorDataMessage> searchLatLong(@Nullable IMapObject mo, @Nonnull Sector bounds, Integer maxRows)
    {
        String hql = "from RaptorDataMessage rdm"
                + " where rdm.hasLocation=true"
                + "   and rdm.isManualLocation=false"
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax";
        if (mo != null)
        {
            hql += "  and rdm.mapObject = :mo";
        }

        return persistServiceProvider.getPersistService().queryFromRdm(hql, maxRows,
                new HQLParameter("latMin", bounds.getMinLatitude().degrees),
                new HQLParameter("lonMin", bounds.getMinLongitude().degrees),
                new HQLParameter("latMax", bounds.getMaxLatitude().degrees),
                new HQLParameter("lonMax", bounds.getMaxLongitude().degrees),
                new HQLParameter("mo", mo));
    }

    @Override
    public IRaptorDataMessage findMostRecentWithLocation(@Nonnull IMapObject mapObject)
    {
        final IPersistService ps = persistServiceProvider.getPersistService();

        String hql = "from RaptorDataMessage rdm " +
                "where rdm.mapObject = :mo" +
                " and rdm.relevantTime = (" +
                "  select max(relevantTime) from RaptorDataMessage rdm2" +
                "  where rdm2.mapObject_id = :mo" + // Must use raw column name in sub-select
                "  and rdm2.hasLocation=true)";

        List<IRaptorDataMessage> list = ps.queryFromRdm(hql, new HQLParameter("mo", mapObject));

        IRaptorDataMessage ret = null;
        if (!list.isEmpty())
        {
            ret = list.get(0);
        }
        return ret;
    }

    @Override
    public boolean mapObjectHasNewerMessages(@Nonnull IMapObject mapObject, long time)
    {
        final String sql = "select max(rdm.receiveTime) from RaptorDataMessage rdm where rdm.mapObject = :mo";

        Long result = persistServiceProvider.getPersistService().queryUniqueFromRdm(sql,
                new HQLParameter("mo", mapObject));

        return result != null && result > time;
    }

    @Override
    public List<IRaptorDataMessage> searchMapObject(@Nonnull IMapObject mo)
    {
        return searchMapObject(mo, null);
    }

    @Override
    public List<IRaptorDataMessage> searchMapObject(@Nonnull IMapObject mo, Integer maxRows)
    {
        String hql = "from RaptorDataMessage rdm where rdm.mapObject = :mo";

        return persistServiceProvider.getPersistService().queryFromRdm(hql, maxRows, new HQLParameter("mo", mo));
    }

    @Override
    public List<IRaptorDataMessage> searchReceiveDate(@Nullable IMapObject mo, long minDT, long maxDT)
    {
        return searchReceiveDate(mo, minDT, maxDT, null, SortOrder.NONE);
    }

    @Override
    public List<IRaptorDataMessage> searchReceiveDate(@Nullable IMapObject mo, long minDT, long maxDT,
                                                      @Nullable Integer maxRows, @Nullable SortOrder sortOrder)
    {
        if (sortOrder == null) sortOrder = SortOrder.NONE;

        String hql = "from RaptorDataMessage rdm"
                + " where rdm.receiveTime >= :minDT"
                + "   and rdm.receiveTime <= :maxDT";
        if (mo != null)
        {
            hql += "  and rdm.mapObject = :mo";
        }
        if (sortOrder != SortOrder.NONE)
        {
            hql += " order by rdm.receiveTime " + (sortOrder == SortOrder.ASCENDING ? "asc" : "desc");
        }

        return persistServiceProvider.getPersistService().queryFromRdm(hql, maxRows,
                new HQLParameter("minDT", min(minDT, maxDT)),
                new HQLParameter("maxDT", max(minDT, maxDT)),
                new HQLParameter("mo", mo));
    }

    @Override
    public List<IRaptorDataMessage> searchDeviceDate(@Nullable IMapObject mo, long minDT, long maxDT)
    {
        return searchDeviceDate(mo, minDT, maxDT, SortOrder.NONE);
    }

    @Override
    public List<IRaptorDataMessage> searchDeviceDate(@Nullable IMapObject mo, long minDT, long maxDT,
                                                     @Nullable SortOrder sortOrder)
    {
        if (sortOrder == null) sortOrder = SortOrder.NONE;

        String hql = "select rdm"
                + " from RaptorDataMessage rdm"
                + " where ((hastime=true and rdm.time >= :minDT) or (hasTime=false and rdm.receiveTime >= :minDT))"
                + "   and ((hastime=true and rdm.time <= :maxDT) or (hasTime=false and rdm.receiveTime <= :maxDT))";
        if (mo != null)
        {
            hql += "  and rdm.mapObject = :mo";
        }
        if (sortOrder != SortOrder.NONE)
        {
            hql += " order by case when hastime=true then rdm.time else rdm.receiveTime end " + (sortOrder == SortOrder.ASCENDING ? "asc" : "desc");
        }

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("minDT", min(minDT, maxDT)),
                new HQLParameter("maxDT", max(minDT, maxDT)),
                new HQLParameter("mo", mo));
    }

    @Override
    public List<IRaptorDataMessage> searchLatLongDate(IMapObject mo, @Nonnull Sector bounds, long minDT, long maxDT)
    {
        return searchLatLongDate(mo, bounds, minDT, maxDT, null);
    }

    @Override
    public List<IRaptorDataMessage> searchLatLongDate(IMapObject mo, @Nonnull Sector bounds, long minDT, long maxDT,
                                                      @Nullable Integer maxRows)
    {
        String hql = "from RaptorDataMessage rdm"
                + " where rdm.hasLocation=true"
                + "   and rdm.isManualLocation=false"
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax"
                + "   and (rdm.receiveTime >= :minDT)"
                + "   and (rdm.receiveTime <= :maxDT)";
        if (mo != null)
        {
            hql += "  and rdm.mapObject = :mo";
        }

        return persistServiceProvider.getPersistService().queryFromRdm(hql, maxRows,
                new HQLParameter("latMin", bounds.getMinLatitude().degrees),
                new HQLParameter("lonMin", bounds.getMinLongitude().degrees),
                new HQLParameter("latMax", bounds.getMaxLatitude().degrees),
                new HQLParameter("lonMax", bounds.getMaxLongitude().degrees),
                new HQLParameter("minDT", min(minDT, maxDT)),
                new HQLParameter("maxDT", max(minDT, maxDT)),
                new HQLParameter("mo", mo));
    }

    @Override
    public List<IRaptorDataMessage> searchLatLongTimeThreshold(@Nonnull IMapObject mo, Sector bounds, long threshold,
                                                               @Nullable Integer maxRows, @Nullable SortOrder sortOrder)
    {
        String hql = "from RaptorDataMessage rdm"
                + " where rdm.hasLocation=true"
                + "   and rdm.isManualLocation=false"
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax"
                + "   and ((rdm.hasTime=true and rdm.time >= :threshold) or (rdm.hasTime=false and rdm.receiveTime >= :threshold))"
                + "   and rdm.mapObject = :mo";

        if (sortOrder != SortOrder.NONE)
        {
            hql += " order by case when hastime=true then rdm.time else rdm.receiveTime end " + (sortOrder == SortOrder.ASCENDING ? "asc" : "desc");
        }

        return persistServiceProvider.getPersistService().queryFromRdm(hql, maxRows,
                new HQLParameter("latMin", bounds.getMinLatitude().degrees),
                new HQLParameter("lonMin", bounds.getMinLongitude().degrees),
                new HQLParameter("latMax", bounds.getMaxLatitude().degrees),
                new HQLParameter("lonMax", bounds.getMaxLongitude().degrees),
                new HQLParameter("threshold", threshold),
                new HQLParameter("mo", mo));
    }

    @Override
    public IRaptorDataMessage searchUUID(@Nonnull UUID uuid)
    {
        String hql = "from RaptorDataMessage rdm where uuid = :uuid";
        return persistServiceProvider.getPersistService().queryUniqueFromRdm(hql, new HQLParameter("uuid", uuid));
    }

    @Override
    public boolean hasUUID(@Nonnull UUID uuid)
    {
        String hql = "select count(rdm) from RaptorDataMessage rdm where uuid = :uuid";
        long count = persistServiceProvider.getPersistService().queryUniqueFromRdm(hql, new HQLParameter("uuid", uuid));

        return count > 0;
    }

    @Override
    public IRaptorDataMessage searchById(int id)
    {
        String hql = "from RaptorDataMessage rdm where id = :id";
        return persistServiceProvider.getPersistService().queryUniqueFromRdm(hql, new HQLParameter("id", id));
    }

    @Override
    public long countAll(@Nonnull IMapObject mo)
    {
        String hql = "select count(rdm) from RaptorDataMessage rdm";
        if (mo != null)
        {
            hql += "    and rdm.mapObject = :mo";
        }

        return persistServiceProvider.getPersistService().queryUniqueFromRdm(hql, new HQLParameter("mo", mo));
    }

    @Override
    public long countLatLong(@Nullable IMapObject mo, @Nonnull Sector bounds)
    {
        String hql = "select count(rdm) from RaptorDataMessage rdm"
                + " where rdm.hasLocation=true"
                + "   and rdm.isManualLocation=false"
                + "   and rdm.isRenderable=true"
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax";
        if (mo != null)
        {
            hql += "  and rdm.mapObject = :mo";
        }

        return persistServiceProvider.getPersistService().queryUniqueFromRdm(hql,
                new HQLParameter("latMin", bounds.getMinLatitude().degrees),
                new HQLParameter("lonMin", bounds.getMinLongitude().degrees),
                new HQLParameter("latMax", bounds.getMaxLatitude().degrees),
                new HQLParameter("lonMax", bounds.getMaxLongitude().degrees),
                new HQLParameter("mo", mo));
    }

    @Override
    public long countLatLongDate(@Nullable IMapObject mo, @Nonnull Sector bounds, long minDT, long maxDT)
    {
        String hql = "select count(rdm) from RaptorDataMessage rdm"
                + " where rdm.hasLocation=true"
                + "   and rdm.isManualLocation=false"
                + "   and rdm.isRenderable=true"
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax"
                + "   and rdm.receiveTime >= :minDT"
                + "   and rdm.receiveTime <= :maxDT";
        if (mo != null)
        {
            hql += "  and rdm.mapObject = :mo";
        }

        return persistServiceProvider.getPersistService().queryUniqueFromRdm(hql,
                new HQLParameter("latMin", bounds.getMinLatitude().degrees),
                new HQLParameter("lonMin", bounds.getMinLongitude().degrees),
                new HQLParameter("latMax", bounds.getMaxLatitude().degrees),
                new HQLParameter("lonMax", bounds.getMaxLongitude().degrees),
                new HQLParameter("minDT", min(minDT, maxDT)),
                new HQLParameter("maxDT", max(minDT, maxDT)),
                new HQLParameter("mo", mo));
    }

    @Override
    public long countLatLongTimeThreshold(@Nonnull IMapObject mo, @Nonnull Sector bounds, long threshold)
    {
        String hql = "select count(rdm) from RaptorDataMessage rdm"
                + " where rdm.hasLocation=true"
                + "   and rdm.isManualLocation=false"
                + "   and rdm.isRenderable=true"
                + "   and rdm.latitude >= :latMin"
                + "   and rdm.longitude >= :lonMin"
                + "   and rdm.latitude <= :latMax"
                + "   and rdm.longitude <= :lonMax"
                + "   and ((rdm.hasTime=true and rdm.time >= :threshold) or (rdm.hasTime=false and rdm.receiveTime >= :threshold))"
                + "   and rdm.mapObject = :mo";

        return persistServiceProvider.getPersistService().queryUniqueFromRdm(hql,
                new HQLParameter("latMin", bounds.getMinLatitude().degrees),
                new HQLParameter("lonMin", bounds.getMinLongitude().degrees),
                new HQLParameter("latMax", bounds.getMaxLatitude().degrees),
                new HQLParameter("lonMax", bounds.getMaxLongitude().degrees),
                new HQLParameter("threshold", threshold),
                new HQLParameter("mo", mo));
    }

    @Override
    public long countDate(@Nullable IMapObject mo, long minDT, long maxDT)
    {
        String hql = "select count(rdm) from RaptorDataMessage rdm"
                + " where (rdm.receiveTime >= :minDT)"
                + "   and (rdm.receiveTime <= :maxDT)";
        if (mo != null)
        {
            hql += "  and rdm.mapObject = :mo";
        }

        return persistServiceProvider.getPersistService().queryUniqueFromRdm(hql,
                new HQLParameter("minDT", min(minDT, maxDT)),
                new HQLParameter("maxDT", max(minDT, maxDT)),
                new HQLParameter("mo", mo));
    }

    @Override
    public long[] queryReceiveDateRange()
    {
        String hql = "select min(receiveTime), max(receiveTime) from RaptorDataMessage";

        Object[] query = persistServiceProvider.getPersistService().queryUniqueFromRdm(hql);

        return prepareMinMaxValues(query);
    }

    @Override
    public long[] queryDeviceDateRange()
    {
        String hql = "select min(time), max(time) from RaptorDataMessage where hasTime = true";

        Object[] query = persistServiceProvider.getPersistService().queryUniqueFromRdm(hql);

        return prepareMinMaxValues(query);
    }

    @Override
    public void persistRDM(@Nonnull IPrePersistRaptorDataMessage rdm)
    {
        persistServiceProvider.getPersistService().persistRDM(rdm);
    }

    /**
     * Process the results of a query that returns two long values (min and max) and provide reasonable
     * defaults if the result is {@code null}.
     *
     * @param query Query results - should be an array of 2 longs
     * @return newly allocated {@code long[]}
     */
    private static long[] prepareMinMaxValues(@Nonnull Object[] query)
    {
        long[] l = {0L, Long.MAX_VALUE};

        if (query[0] != null)
        {
            l[0] = (Long) query[0];
        }
        if (query[1] != null)
        {
            l[1] = (Long) query[1];
        }

        return l;
    }
}
