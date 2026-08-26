package gov.tak.api.icons;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.Marker;
import gov.tak.api.symbology.ISymbolTable.Symbol;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.IMilSymIconBuilder;
import gov.takx.api.milsym.SymbolWrapper;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Locale;

import static gov.takx.api.milsym.CotSidcAdapter.mil2525CFromCotType;
import static gov.takx.api.milsym.MilStd2525Variant.CHARLIE;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Icon adapter to handle converting CoT types to MIL-STD-2525-C icons.
 *
 * @since 3.2
 * @deprecated Superseded by Mil2525IconAdapter
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
@ApplicationScoped
public class CotMil2525cIconAdapter implements IconAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IMilStd2525Service milStd2525Service;

    @Inject
    CotMil2525cIconAdapter(IMilStd2525ServiceProvider milStd2525ServiceProvider)
    {
        this.milStd2525Service = milStd2525ServiceProvider.get2525Service(CHARLIE);
    }

    @Override
    public boolean adapt(@Nonnull Marker marker)
    {
        final String cotType = marker.getCotType().toUpperCase(Locale.getDefault()); // Force upper case
        if (CotTypes.EMPTY.equalsIgnoreCase(cotType)) return false;

        // If the marker has a milsym code already set, then use it
        String symbolCode = marker.getMetaString(StandardAttributeNames.MIL_SYM, "");
        if (symbolCode.isEmpty())
        {
            symbolCode = mil2525CFromCotType(cotType);
        }

        // Otherwise convert the CoT type to MIL2525C SIDC.
        if (symbolCode != null)
        {
            final var symbolSource = find2525Symbol(cotType, symbolCode);
            if (symbolSource != null)
            {
                logger.debug("Applying 2525 icon: {}", symbolSource);

                final BufferedImage image = getSymbolImage(symbolSource, marker.getAttributeSet());
                if (!milStd2525Service.isUnrenderableSymbol(symbolCode))
                {
                    marker.setExternalIconImage(image);
                    marker.getAttributeSet().removeAttribute(StandardAttributeNames.ADAPT_MARKER_ICON);
                    return true;
                } else
                {
                    logger.warn("Failed to get icon for: {} on marker {}", symbolSource, marker);
                }
            }
        }

        logger.debug("Failed to get SIDC for '{}' on marker {}", cotType, marker);

        return false;
    }

    /**
     * @return the icon image representing the given CoT type, {@code null} if one can not be found
     */
    @Nullable
    public BufferedImage get2525Icon(@Nonnull String cotType)
    {
        final String sidc = mil2525CFromCotType(cotType);
        if (sidc == null) return null;
        final SymbolSource symbol = find2525Symbol(cotType, sidc);
        return symbol != null ? getSymbolImage(symbol, null) : null;
    }

    /**
     * Find a valid 2525 symbol starting with the given SIDC.
     * <p/>
     * This loop will iteratively try to find a valid SIDC given the CoT type, successively chopping off the
     * last component until a valid SIDC is generated, or we run out of components.
     * <p/>
     * E.g., A CoT type of 'a-.-G-I-i-e-icc' will be shortened to 'a-.-G-I' before yielding a valid SIDC
     *
     * @param cotType Starting CoT type code
     * @param sidc Starting SIDC code
     * @return SymbolSource, {@code null} if no matching symbol could be found
     */
    @Nullable
    private SymbolSource find2525Symbol(@Nonnull String cotType, @Nonnull String sidc)
    {
        while (!StringUtils.isEmpty(sidc))
        {
            final SymbolWrapper symbol = milStd2525Service.getSymbol(sidc);
            if (symbol != null)
            {
                logger.debug("For '{}' found '{}' '{}'", sidc, symbol.getCode(), symbol.getName());
                return new SymbolSource(symbol, sidc);
            }

            // Either the SIDC is invalid or we didn't get a usable icon, so chop a component and try again.
            final int index = cotType.lastIndexOf('-');
            if (index != -1)
            {
                cotType = cotType.substring(0, index);
                sidc = mil2525CFromCotType(cotType);
            } else
            {
                break; // Nothing left, so bail
            }
        }
        return null;
    }

    /**
     * @param symbolSource Symbol to render.
     * @param attributes  Additional attributes for symbol modifiers, if any are present.
     * @return The image for the given symbol
     * @since 5.1
     */
    @Nonnull
    private BufferedImage getSymbolImage(@Nonnull SymbolSource symbolSource, @Nullable AttributeSet attributes)
    {
        return milStd2525Service.renderSinglePointIcon(symbolSource.sourceSidc(), attributes, IMilSymIconBuilder.DEFAULT_SINGLE_POINT_SIZE);
    }

    /**
     * Return a default "unknown" icon, based on the CoT type affiliation.
     *
     * @param cotType CoT type to interpret
     * @return true
     */
    @Nonnull
    public URI getUnknownIconUri(@Nonnull String cotType)
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

    @Override
    public void dispose()
    {
    }

    /**
     * The {@link Symbol} contains a code that has certain components turned into asterisks, e.g. {@code 'S*G*EWMAS-*****'}.
     * The first two asterisks would be where the unit affiliation and the unit status codes go.  Rendering from this
     * "generic" code yields an icon using the styling for an "Unknown" unit.  In order to properly render, we need
     * to use the source SIDC that was used to look up the symbol.
     *
     * @param symbol Symbol definition
     * @param sourceSidc Original SIDC from which the {@code symbol} was retrieved from the symbology provider
     * @since 5.1
     */
    private record SymbolSource(@Nonnull Symbol symbol, @Nonnull String sourceSidc)
    {
        @Override
        public String toString()
        {
            return "SymbolSource{" +
                    "sourceSidc='" + sourceSidc + '\'' +
                    ", symbol.code='" + symbol.getCode() + '\'' +
                    ", symbol.name='" + symbol.getName() + '\'' +
                    '}';
        }
    }
}
