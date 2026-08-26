package gov.tak.core.icons;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.icons.ICotIconResolver;
import gov.tak.api.icons.PredefinedCotIconAdapter;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.milsym.*;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.URI;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Service responsible for resolving CoT icons from CoT types.
 *
 * @since 5.0
 */
@ApplicationScoped
public class CotIconResolver implements ICotIconResolver
{
    private final IMapObjectFinder mapObjectFinder;
    private final PredefinedCotIconAdapter predefinedCotIconAdapter;
    private final IMilStd2525Service milStd2525Service;

    @Inject
    public CotIconResolver(IMapObjectFinder mapObjectFinder, PredefinedCotIconAdapter predefinedCotIconAdapter,
                           IMilStd2525ServiceProvider milStd2525ServiceProvider)
    {
        this.mapObjectFinder = mapObjectFinder;
        this.predefinedCotIconAdapter = predefinedCotIconAdapter;
        milStd2525Service = milStd2525ServiceProvider.get2525Service(MilStd2525Variant.CHARLIE);
    }

    @Nonnull
    @Override
    public BufferedImage getIcon(int sizePixels, @Nonnull String cotType)
    {
        // Try for a pre-defined icon mapping
        final URI imageUri = predefinedCotIconAdapter.getUriForType(cotType);
        if (imageUri != null) return UIResources.getSizedImage(imageUri, new Dimension(sizePixels, sizePixels));

        // Try for a MIL-2525 mapping
        final var sidc = CotSidcAdapter.findRenderable2525CSymbol(cotType);
        final var image = sidc != null ? milStd2525Service.renderSinglePointIcon(sidc, null, IMilSymIconBuilder.DEFAULT_SINGLE_POINT_SIZE) : null;
        if (image != null) return ImageUtils.getScaledInstance(image, sizePixels);

        // Default to a general "unknown" icon
        return UIResources.getSizedImage(getUnknownCotIconUri(cotType), new Dimension(sizePixels, sizePixels));
    }

    @Nonnull
    @Override
    public BufferedImage getIcon(int size, @Nonnull CotEvent event)
    {
        // Check the map object's icon image.
        final IMapObject mapObject = mapObjectFinder.findMapObjectByUid(event.getUID());
        if (mapObject != null) return ImageUtils.getScaledInstance(mapObject.getIconImage(), size, size);

        // Check for supported types in the standard cot icon adapters.
        return getIcon(size, event.getType());
    }

    /**
     * Return a default "unknown" icon, based on the CoT type affiliation.
     *
     * @param cotType CoT type to interpret.
     * @return URI to unknown icon.
     * @since 5.7 - Moved here from  {@link MarkerIconAdapter}
     */
    @Nonnull
    public static URI getUnknownCotIconUri(@Nonnull String cotType)
    {
        String iconName;

        if (CotTypes.isFriendly(cotType))
        {
            iconName = "unknown-type-f.png";
        } else if (CotTypes.isHostile(cotType))
        {
            iconName = "unknown-type-h.png";
        } else if (CotTypes.isNeutral(cotType))
        {
            iconName = "unknown-type-n.png";
        } else if (CotTypes.isUnknown(cotType))
        {
            iconName = "unknown-type-u.png";
        } else
        {
            iconName = "unknown-type.png";
        }

        return UIResources.getUri(TAK_ASSETS_ICONS, iconName);
    }
}
