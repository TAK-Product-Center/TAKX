package gov.tak.core.icons;

import gov.tak.api.icons.IconAdapter;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.milsym.CotSidcAdapter;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.IMilSymIconBuilder;
import gov.takx.api.milsym.MilSymIconScaling;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Icon adapter to handle converting CoT types and MIL-STD-2525 symbology enabled map objects into MIL-STD-2525 icons.
 * @since 5.5
 */
@ApplicationScoped
public class Mil2525IconAdapter implements IconAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IMilStd2525ServiceProvider milStd2525ServiceProvider;

    @Inject
    public Mil2525IconAdapter(@Nonnull IMilStd2525ServiceProvider milStd2525ServiceProvider)
    {
        this.milStd2525ServiceProvider = milStd2525ServiceProvider;
    }

    @Override
    public boolean adapt(@Nonnull Marker marker)
    {
        // Apply the rendered symbol as the external icon if it is a valid mil-sym object.
        final String symbolCode = getSymbolCode(marker);
        return symbolCode != null && updateMapObjectIcon(marker, symbolCode);
    }

    @Override
    public void dispose()
    {
        // no-op
    }

    /**
     * Register for property changes that might require the need to update the MO's icon.
     */
    @SuppressWarnings("unused")
    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        final var mapObject = event.getMapObject();
        mapObject.getAttributeSet().addPropertyChangeListener(evt -> onPropertyChange(mapObject, evt));
    }

    /**
     * Handler to update the map object's icon when any mil-sym related attribute is modified.
     *
     * @since 5.5
     */
    private void onPropertyChange(IMapObject mapObject, PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();
        Object propertyValue = evt.getNewValue();
        if (StandardAttributeNames.MIL_SYM.equals(propertyName))
        {
            // The symbol code has changed or been removed.
            if (propertyValue != null)
            {
                String newSymbolCode = propertyValue.toString();
                updateMapObjectIcon(mapObject, newSymbolCode);
            } else
            {
                resetIcon(mapObject);
            }
        } else if (StandardAttributeNames.COT_TYPE.equals(propertyName))
        {
            // The CoT type has changed, so we need to update the map object to reflect the new type.
            resetIcon(mapObject);
        } else if (propertyName.startsWith(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX))
        {
            // A 2525 modifier has changed, so we should update the map object's display.
            // If there is no symbol code then we are likely in the process of clearing off modifiers from a symbol,
            // and thus we should reset the icon.
            String symbolCode = getSymbolCode(mapObject);
            if (!StringUtils.isEmpty(symbolCode))
            {
                updateMapObjectIcon(mapObject, symbolCode);
            } else
            {
                resetIcon(mapObject);
            }
        }
    }

    /**
     * Sets the {@link IMapObject#getIconImage()} to the generated icon of a given symbol code.
     *
     * @param mapObject Map item to update icon of.
     * @param symbolCode Symbol code to use for icon rendering.
     * @return {@code true} if an icon was actually applied, {@code false} if not
     */
    private boolean updateMapObjectIcon(@Nonnull IMapObject mapObject, @Nonnull String symbolCode)
    {
        // Skip if the provided code does not map to a recognized symbol.
        if (!isValidSymbol(symbolCode))
        {
            logger.debug("Map object '{}' has an invalid mil-sym code attached to it: {}", mapObject.getName(), symbolCode);
            return false;
        }

        // Calling 'renderSinglePointIcon' falls back to a generic preview if the symbol has
        // no support for the 'Point' shape. So even multipoint graphics can be rendered with this.
        BufferedImage image = render(mapObject, symbolCode);

        // Skip if the image could not be properly rendered.
        // This call goes *after* the render call above to prevent double rendering the symbol.
        if (isUnrenderableSymbol(symbolCode)) return false;

        // Set a flag indicating we are currently adapting this map object's 2525 symbol.
        // This will signal to consumers that the next immediate update they see should be from the following image/icon update.
        // That update will be our rendered 2525 graphic.
        AttributeSet attrs = mapObject.getAttributeSet();
        attrs.setAttribute(StandardAttributeNames.MIL_SYM_ADAPTING, true);

        // Update the map object's image to our 2525 rendered symbol.
        // This will temporarily set 'ADAPT_MARKER_ICON' to 'false'.
        // Normally when you see that, it implies a user defined icon is set.
        // However, we do not want 2525 symbols to be treated this way, and for our marker feature provider we use the 'MIL_SYM_ADAPTING'
        // as a backup flag to see if the set image is our intended 2525 symbol or some user-defined graphic.
        mapObject.setExternalIconImage(image);

        // Cleanup now that the icon is set.
        // We no longer are adapting the 2525 symbol so 'MIL_SYM_ADAPTING' is removed.
        // We also want to allow users to be able to override the image with their own, so 'ADAPT_MARKER_ICON' is removed as well.
        attrs.removeAttribute(StandardAttributeNames.MIL_SYM_ADAPTING);
        attrs.removeAttribute(StandardAttributeNames.ADAPT_MARKER_ICON);
        return true;
    }

    /**
     * Restores the given map object to its baseline image.
     * <p/>
     * This either will be derived from the CoT type, or the plugin definition provided icon if the CoT type
     * is not mappable to a standard 2525 symbol code.
     *
     * @param mapObject Map object to reset.
     */
    private void resetIcon(@Nonnull IMapObject mapObject)
    {
        String cotType = mapObject.getCotType();
        if (!cotType.startsWith("a-"))
        {
            // Map object CoT type isn't in the atom group, so we cannot derive a default from it.
            mapObject.resetIconImage();
            return;
        }

        // Directly map the CoT to a 2525C symbol code. We already verified the CoT type is in the atom
        // group so the conversion should never fail.
        String symbolCode = Objects.requireNonNull(CotSidcAdapter.mil2525CFromCotType(cotType), "");
        updateMapObjectIcon(mapObject, symbolCode);
    }

    private boolean isUnrenderableSymbol(@Nonnull String symbolCode)
    {
        return milStd2525ServiceProvider.get2525ServiceForSymbol(symbolCode).isUnrenderableSymbol(symbolCode);
    }

    private boolean isValidSymbol(@Nonnull String symbolCode)
    {
        return milStd2525ServiceProvider.get2525ServiceForSymbol(symbolCode).isValidSymbol(symbolCode);
    }

    @Nullable
    private String getSymbolCode(@Nonnull IMapObject mapObject)
    {
        final var symbolCode = milStd2525ServiceProvider.get2525ServiceForMapObject(mapObject).getSymbolCode(mapObject);
        return symbolCode == null ? CotSidcAdapter.findRenderable2525CSymbol(mapObject.getCotType()) : symbolCode;
    }

    @Nonnull
    private BufferedImage render(@Nonnull IMapObject mapObject, @Nonnull String symbolCode)
    {
        var attributeSet = mapObject.getAttributeSet();
        return milStd2525ServiceProvider.get2525ServiceForSymbol(symbolCode)
                .newSymbolIconBuilder()
                .setCode(symbolCode)
                .addAttributes(attributeSet)
                .setResolution(IMilSymIconBuilder.DEFAULT_SINGLE_POINT_SIZE)
                .setResolutionScaling(MilSymIconScaling.NONE)
                .setCenterPointConsumer(center -> {
                    attributeSet.setAttribute(StandardAttributeNames.MIL_SYM_CENTER_X, center.x);
                    attributeSet.setAttribute(StandardAttributeNames.MIL_SYM_CENTER_Y, center.y);
                })
                .render();
    }
}
