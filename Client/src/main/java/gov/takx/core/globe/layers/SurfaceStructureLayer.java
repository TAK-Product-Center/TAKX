package gov.takx.core.globe.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.structures.ApplicationCachedSurfaceStructureLayer;
import gov.takx.core.structures.CachedSurfaceStructureLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 */
public class SurfaceStructureLayer extends WWGlobeLayer
{
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public SurfaceStructureLayer(IRaptorLayer iRaptorLayer, Layer layer)
    {
        super(iRaptorLayer, layer);

        setSharable(true); // We are sharable
    }

    public static Layer makeWWLayer(String restorableState)
    {
        double[] dblArr = new double[4];
        boolean usePreviewLayer = false;
        String structurePreviewLayerRestorableState = null;
        if (restorableState != null)
        {
            //the first 4 elements (if they exist) are lat/lon values for the bounding sector
            String[] strArr = restorableState.split(",");
            for (int i = 0; i < 4 && i < strArr.length; i++) dblArr[i] = Double.parseDouble(strArr[i]);

            //if the 5th element exists, it is a boolean specifying whether a preview layer should be used
            if (strArr.length >= 5)
            {
                usePreviewLayer = Boolean.parseBoolean(strArr[4]);
            }

            //if the 6th element exists, it is restorable state for the preview layer.
            //if it does not exist, then a preview layer must be created from scratch.
            if (strArr.length >= 6)
            {
                structurePreviewLayerRestorableState = strArr[5];
            }
        }

        Sector boundingSector = Sector.fromDegrees(dblArr);
        Layer wwLayer;
        if (usePreviewLayer)
        {
            //if preview layer restorable state exists, restore the preview layer from this data, otherwise create
            //a new preview layer.
            if (structurePreviewLayerRestorableState != null)
            {
                wwLayer = new ApplicationCachedSurfaceStructureLayer(structurePreviewLayerRestorableState);
            } else
            {
                wwLayer = new ApplicationCachedSurfaceStructureLayer();
            }
        } else
        {
            //if not using a preview layer, create an ordinary cached surface structure layer
            wwLayer = new CachedSurfaceStructureLayer(boundingSector);
        }

        if (!wwLayer.hasKey(AVKey.CONSTRUCTION_PARAMETERS))
        {
            AVList avlist = new AVListImpl();
            AVList constparms = new AVListImpl();
            constparms.setValue(AVKey.SECTOR, boundingSector);
            avlist.setValue(AVKey.CONSTRUCTION_PARAMETERS, constparms);
            wwLayer.setValues(avlist);
        }
        return wwLayer;
    }

    @Override
    public String getRestorableState()
    {
        String rs = getLayer().getRestorableState();
        if (rs == null && layer.hasKey(AVKey.CONSTRUCTION_PARAMETERS))
        {
            AVList constparms = (AVList) layer.getValue(AVKey.CONSTRUCTION_PARAMETERS);
            Sector boundingSector = (Sector) constparms.getValue(AVKey.SECTOR);
            double[] boundingSectorArray = boundingSector.asDegreesArray();
            Double[] boundingSectorDblArray = new Double[boundingSectorArray.length];
            for (int i = 0; i < boundingSectorArray.length; i++) boundingSectorDblArray[i] = boundingSectorArray[i];
            rs = StringUtils.join(boundingSectorDblArray);
        }
        return rs;
    }
}
