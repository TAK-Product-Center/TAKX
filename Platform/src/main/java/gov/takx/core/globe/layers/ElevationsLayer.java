package gov.takx.core.globe.layers;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.terrain.BasicElevationModel;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Internal;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRestorable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 */
@DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
@Deprecated(since = "5.6", forRemoval = true)
@Internal
public class ElevationsLayer implements IRestorable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String RESTORED = "Restored";

    private final ElevationModel em;

    public ElevationsLayer(IRaptorLayer raptorLayer, ElevationModel em)
    {
        this.em = em;

        // WW ElevationModel doesn't persist the enabled flag in its restorable state.  Synch up the enabled flag with
        // the raptor layer's state.
        em.setEnabled(raptorLayer.isVisible());

        // Some elevation models have a detail hint set before ElevationsLayer is created, so initialize the owning
        // RaptorLayer with this value.
        raptorLayer.setSplitScale(em.getDetailHint(Sector.FULL_SPHERE));

        // Add listener for RaptorLayer property changes.
        raptorLayer.addPropertyChangeListener(new RaptorLayerPropertyChangeHandler());

        if (logger.isDebugEnabled())
        {
            for (BasicElevationModel basicElevationModel : getBasicElevationModels(em))
            {
                logger.debug("Created ElevationsLayer for elevation model: {}, with detailHint: {}", basicElevationModel.getName(), em.getDetailHint(Sector.FULL_SPHERE));
            }
        }
    }

    public ElevationModel getEm()
    {
        return em;
    }

    @Override
    public String getRestorableState()
    {
        /*
         * CORE-3239:
         *
         * Special handling of CompoundElevationModel (which does not provide/handle restorable state).  Return a
         * non-empty String as a flag to determine whether the model has been restored.
         */
        if (em instanceof CompoundElevationModel)
        {
            return RESTORED;
        }
        return em.getRestorableState();
    }

    @Override
    public void restoreState(String stateInXml)
    {
        if (!RESTORED.equals(stateInXml))
        {
            em.restoreState(stateInXml);
        }
    }

    /**
     * @deprecated as of 5.6
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    public boolean isInstalled()
    {
        return true;
    }

    /**
     * Split scale is the detail hint in BasicElevationModel (same functionality in GlobeLayer's Layer)
     *
     * @since 1.39
     */
    private void setSplitScale(double splitScale)
    {
        for (BasicElevationModel basicElevationModel : getBasicElevationModels(em))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("setting detailHint: {} for: {}", splitScale, basicElevationModel.getName());
            }
            basicElevationModel.setDetailHint(splitScale);
        }
    }

    /**
     * Class to handle changes on the {@link IRaptorLayer} that this elevations layer wraps by propagating the changes
     * into the elevations layer.
     */
    private class RaptorLayerPropertyChangeHandler implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            logger.debug("ElevationsLayer property change: {}", evt);

            final String propName = evt.getPropertyName();
            final IRaptorLayer raptorLayer = (IRaptorLayer) evt.getSource();

            if (propName.equals(IRaptorLayer.SPLIT_SCALE))
            {
                setSplitScale(raptorLayer.getSplitScale());
            }
            /*
             * Update visibility of Elevation Model on VISIBLE event.
             *
             * @since 1.40
             */
            else if (propName.equals(IRaptorLayer.VISIBLE))
            {
                em.setEnabled(raptorLayer.isVisible());
            }
        }
    }

    /**
     * Get a list of all of the BasicElevationModels from an ElevationModel.  Most ElevationModels are implemented by
     * BasicElevationModel (or one of its descendents), but they can also be ComboElevationModels.  If the
     * ElevationModel is a ComboElevationModel, then all of the BasicElevationModels inside of it will be extracted.
     *
     * @since 1.40
     */
    public static List<BasicElevationModel> getBasicElevationModels(ElevationModel em)
    {
        return getBasicElevationModels(em, new ArrayList<>());
    }

    private static List<BasicElevationModel> getBasicElevationModels(ElevationModel elevationModel,
                                                                     List<BasicElevationModel> basicElevationModelList)
    {
        if (elevationModel instanceof BasicElevationModel)
        {
            basicElevationModelList.add((BasicElevationModel) elevationModel);
        } else if (elevationModel instanceof CompoundElevationModel)
        {
            for (ElevationModel em2 : ((CompoundElevationModel) elevationModel).getElevationModels())
            {
                getBasicElevationModels(em2, basicElevationModelList);
            }
        }
        return basicElevationModelList;
    }
}
