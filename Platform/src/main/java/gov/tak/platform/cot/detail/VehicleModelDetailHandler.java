package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.maps.MapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import jakarta.inject.Inject;

import static gov.tak.api.icons.IIconSetManager.VEHICLE_MODELS_UID;

/**
 * Handle the {@code <model/>} detail, which defines a specific 3D vehicle model to be displayed for the marker.
 *
 * @implNote Most of the work is delegated to the kernel provided detail handler.
 * @since 5.8
 */
@PseudoBean
public class VehicleModelDetailHandler extends CotDetailHandler
{
    private static final ModelInterop interop = new ModelInterop();
    private static final gov.tak.api.cot.detail.VehicleModelDetailHandler kernelHandler =
            new gov.tak.api.cot.detail.VehicleModelDetailHandler(interop);

    @Inject
    private IIconSetManager iconSetManager;

    VehicleModelDetailHandler()
    {
        super(kernelHandler.getDetailNames());
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        return kernelHandler.toCotDetail(item, item.getAttributeSet(), event, detail);
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        // We can be invoked up to three times, since the kernel handler processes three details: model, strokeColor, and fillColor
        // We only want to set the icon image when we get the "model" detail.

        final var result = ImportResult.convertKernelResult(kernelHandler.toItemMetadata(item, item.getAttributeSet(), event, detail));
        if (result == ImportResult.SUCCESS && "model".equals(detail.getElementName()) && item instanceof IMapObject mapObject)
        {
            final String name = interop.getVehicleName(item);
            final var icon = iconSetManager.getIcon(VEHICLE_MODELS_UID, name);
            if (icon != null) mapObject.setIconImage(icon.toUri());
        }
        return result;
    }

    /**
     * Interop for how we store the vehicle model information on our Map Items.
     */
    private static class ModelInterop implements gov.tak.api.cot.detail.VehicleModelDetailHandler.Interop<MapItem>
    {
        @Override
        public boolean isVehicleModel(MapItem mapItem)
        {
            return mapItem.hasMetaValue(StandardAttributeNames.MODEL_NAME);
        }

        @Override
        public void setShowOutline(MapItem mapItem, boolean enabled)
        {
            mapItem.setMetaBoolean(StandardAttributeNames.MODEL_OUTLINE_ENABLED, enabled);
        }

        @Override
        public boolean getShowOutline(MapItem mapItem)
        {
            return mapItem.getMetaBoolean(StandardAttributeNames.MODEL_OUTLINE_ENABLED, false);
        }

        @Override
        public boolean setVehicleInfo(MapItem mapItem, String category, String name)
        {
            mapItem.setMetaString(StandardAttributeNames.MODEL_GROUP, category);
            mapItem.setMetaString(StandardAttributeNames.MODEL_NAME, name);
            return true;
        }

        @Override
        public String getVehicleName(MapItem mapItem)
        {
            return mapItem.getMetaString(StandardAttributeNames.MODEL_NAME, "unknown");
        }

        @Override
        public String getVehicleCategory(MapItem mapItem)
        {
            return mapItem.getMetaString(StandardAttributeNames.MODEL_GROUP, "unknown");
        }

        @Override
        public void setColor(MapItem mapItem, int color)
        {
            mapItem.setMetaInteger(StandardAttributeNames.MODEL_COLOR, color);
        }

        @Override
        public int getColor(MapItem mapItem)
        {
            return mapItem.getMetaInteger(StandardAttributeNames.MODEL_COLOR, 0xFFFFFFFF);
        }
    }
}
