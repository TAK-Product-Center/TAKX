package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.terrain.BasicElevationModel;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.TileKey;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @since 1.35
 * @deprecated to be removed
 */
@Experimental
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class TerrainUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Get the best elevation model resolution with which a sector intersects.  E.g. the returned resolution represents
     * the resolution of the highest level in an elevation model which INTERSECTS a sector.
     * <p/>
     * Note: the returned value represents the best possible resolution overall in the given sector.  There is no
     * guarantee that the elevation model supports this resolution across the entire sector.
     *
     * @param elevationModel elevation model of interest.
     * @param sector         sector of interest.
     * @return the best elevation model resolution with which a sector intersects.
     */
    public static double getBestIntersectingResolution(BasicElevationModel elevationModel, Sector sector)
    {
        if (sector == null)
        {
            return elevationModel.getBestResolution(null);
        }

        LevelSet levelSet = elevationModel.getLevels();

        if (!levelSet.getSector().intersects(sector))
        {
            return Double.MAX_VALUE;
        }

        LevelSet.SectorResolution bestSectorResolution = null;
        Level level = levelSet.getLevel(levelSet.getNumLevels() - 1);

        if (levelSet.getSectorLevelLimits() != null)
        {
            for (LevelSet.SectorResolution sr : levelSet.getSectorLevelLimits())
            {
                if (sr.getSector().intersects(sector) && sr.getLevelNumber() <= level.getLevelNumber())
                {
                    level = levelSet.getLevel(sr.getLevelNumber());
                    bestSectorResolution = sr;
                    break;
                }
            }
        }

        if (logger.isDebugEnabled() &&
                bestSectorResolution != null &&
                !bestSectorResolution.getSector().contains(sector))
        {
            logger.debug("Intersecting resolution limit does not fully contain sector: {}", sector);
        }
        return level != null ? level.getTexelSize() : Double.MAX_VALUE;
    }

    /**
     * Get the best elevation model resolution with which a sector is contained.  E.g. the returned resolution
     * represents the resolution of the highest level which CONTAINS an entire sector.
     * <p/>
     * Note: the returned resolution value represents the best possible resolution across the entire sector.  This
     * means that the elevation model supports data at this resolution across any location inside the sector.  This
     * value is not necessarily the best possible resolution overall in the sector.
     *
     * @param elevationModel elevation model of interest.
     * @param sector         sector of interest.
     * @return the best elevation model resolution with which a sector is contained.
     */
    public static double getBestContainingResolution(BasicElevationModel elevationModel, Sector sector)
    {
        if (sector == null)
        {
            return elevationModel.getBestResolution(null);
        }

        LevelSet levelSet = elevationModel.getLevels();

        if (!levelSet.getSector().intersects(sector))
        {
            return Double.MAX_VALUE;
        }

        Level level = levelSet.getLevel(levelSet.getNumLevels() - 1);

        if (levelSet.getSectorLevelLimits() != null)
        {
            for (LevelSet.SectorResolution sr : levelSet.getSectorLevelLimits())
            {
                if (sr.getSector().contains(sector) && sr.getLevelNumber() <= level.getLevelNumber())
                {
                    level = levelSet.getLevel(sr.getLevelNumber());
                    break;
                }
            }
        }

        return level != null ? level.getTexelSize() : Double.MAX_VALUE;
    }

    /**
     * Get the best elevation model resolution with which a sector intersects.  E.g. the returned resolution represents
     * the resolution of the highest level in an elevation model which INTERSECTS a sector.
     * <p/>
     * Note: the returned value represents the best possible resolution overall in the given sector.  There is no
     * guarantee that the elevation model supports this resolution across the entire sector.
     *
     * @param elevationModels elevation models of interest.
     * @param sector          sector of interest.
     * @return the best elevation model resolution with which a sector intersects.
     */
    public static double getBestIntersectingResolution(List<? extends BasicElevationModel> elevationModels,
                                                       Sector sector)
    {
        double res = 0;

        for (BasicElevationModel em : elevationModels)
        {
            if (!em.isEnabled())
            {
                continue;
            }

            if (sector != null && em.intersects(sector) < 0) // sector does not intersect elevation model
            {
                continue;
            }

            double r = getBestIntersectingResolution(em, sector);
            if (r < res || res == 0)
            {
                res = r;
            }
        }

        return res != 0 ? res : Double.MAX_VALUE;
    }

    /**
     * Get the best elevation model resolution with which a sector is contained.  E.g. the returned resolution
     * represents the resolution of the highest level which CONTAINS a sector.
     * <p/>
     * Note: the returned resolution value represents the best possible resolution across the entire sector.  This
     * means that the elevation model supports data at this resolution across any location inside the sector.  This
     * value is not necessarily the best possible resolution overall in the sector.
     *
     * @param elevationModels elevation models of interest.
     * @param sector          sector of interest.
     * @return the best elevation model resolution with which a sector is contained.
     */
    public static double getBestContainingResolution(List<? extends BasicElevationModel> elevationModels,
                                                     Sector sector)
    {
        double res = 0;

        for (BasicElevationModel em : elevationModels)
        {
            if (!em.isEnabled())
            {
                continue;
            }

            if (sector != null && em.intersects(sector) < 0) // sector does not intersect elevation model
            {
                continue;
            }

            double r = getBestContainingResolution(em, sector);
            if (r < res || res == 0)
            {
                res = r;
            }
        }

        return res != 0 ? res : Double.MAX_VALUE;
    }

    /**
     * @since 5.2
     */
    public static <T extends ElevationModel> List<T> splitModels(Class<T> clazz, ElevationModel em)
    {
        List<T> models = new ArrayList<>();
        if (em instanceof CompoundElevationModel)
        {
            addAllElevationModels(clazz, (CompoundElevationModel) em, models);
        } else if (clazz.isAssignableFrom(em.getClass()))
        {
            models.add(clazz.cast(em));
        } else if (logger.isDebugEnabled())
        {
            logger.debug("Unable to use elevation model of type: {}", em.getClass().getName());
        }
        return models;
    }

    public static List<BasicElevationModel> splitModels(ElevationModel em)
    {
        return splitModels(BasicElevationModel.class, em);
    }

    /**
     * Descending order sort of elevation models by resolution.  Larger resolution values correspond
     * to a lower actual resolution.  Puts the lowest resolution models at the beginning of the list,
     * and the highest resolution models at the end.
     *
     * @param models a list of elevation models.
     * @param sector sector of the elevation model to query for resolution.
     */
    public static void sortModelsByResolution(List<? extends BasicElevationModel> models, final Sector sector)
    {
        Comparator<BasicElevationModel> comparator = new Comparator<BasicElevationModel>()
        {
            @Override
            public int compare(BasicElevationModel em1, BasicElevationModel em2)
            {
                double em1Resolution = getBestIntersectingResolution(em1, sector);
                double em2Resolution = getBestIntersectingResolution(em2, sector);
                if (em1Resolution < em2Resolution)
                {
                    return 1;
                }
                if (em1Resolution > em2Resolution)
                {
                    return -1;
                }

                // next, prioritize by network connectivity (local caches vs. networked layers)
                boolean em1Networked = em1.isNetworkRetrievalEnabled();
                boolean em2Networked = em2.isNetworkRetrievalEnabled();
                if (em1Networked != em2Networked)
                {
                    if (!em2Networked)
                    {
                        return -1;
                    } else
                    {
                        return 1;
                    }
                }

                // attempt to make the sort deterministic: if both models have the same resolution, then order them by
                // their String names.  Of course, if two equal models have the same name, then you got yourself a
                // problem.
                return em2.getName().compareTo(em1.getName());
            }
        };
        Collections.sort(models, comparator);
    }

    private static <T extends ElevationModel> void addAllElevationModels(Class<T> clazz, CompoundElevationModel em, List<T> models)
    {
        List<ElevationModel> newModels = em.getElevationModels();
        List<CompoundElevationModel> compoundModels = new ArrayList<CompoundElevationModel>();
        for (ElevationModel model : newModels)
        {
            if (model instanceof CompoundElevationModel)
            {
                compoundModels.add((CompoundElevationModel) model);
            } else if (clazz.isAssignableFrom(model.getClass()))
            {
                models.add(clazz.cast(model));
            } else if (logger.isDebugEnabled())
            {
                logger.debug("Unable to use elevation model of type: {}", model.getClass().getName());
            }
        }

        for (CompoundElevationModel compoundModel : compoundModels)
        {
            addAllElevationModels(clazz, compoundModel, models);
        }
    }

    /**
     * Get the highest resolution model from a list of sorted models (lowest to highest resolution), which is both
     * enabled and contains the entire given sector.
     *
     * @param sector       the sector in question.
     * @param sortedModels a list of models sorted from lowest resolution to highest within the given sector.
     * @return the best model containing the given sector.
     */
    public static BasicElevationModel getBestContainingModel(Sector sector, List<? extends BasicElevationModel> sortedModels)
    {
        for (int i = sortedModels.size() - 1; i >= 0; i--)
        {
            BasicElevationModel model = sortedModels.get(i);
            if (model.intersects(sector) == 0 && model.isEnabled())
            {
                return model;
            }
        }
        return null;
    }

    public static class ElevationKey
    {
        public final LocalDataBasicElevationModel model;
        public final TileKey key;
        public final double resolution;

        public ElevationKey(LocalDataBasicElevationModel model, TileKey key, double resolution)
        {
            this.model = model;
            this.key = key;
            this.resolution = resolution;
        }

        public boolean isEqualTo(ElevationKey o)
        {
            return resolution == o.resolution && model.hashCode() == o.model.hashCode() && key.equals(o.key);
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Elevation Key model: ").append(model.getName())
                    .append(", key: ").append(key)
                    .append(", resolution: ").append(resolution);
            return sb.toString();
        }
    }
}
