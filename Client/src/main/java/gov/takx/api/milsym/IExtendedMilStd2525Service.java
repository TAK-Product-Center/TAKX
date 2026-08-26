package gov.takx.api.milsym;

import gov.tak.api.symbology.ISymbologyProvider;
import jakarta.annotation.Nonnull;

/**
 * Extended {@link IMilStd2525Service} for internal client use only.
 *
 * @since 5.1
 */
public interface IExtendedMilStd2525Service extends IMilStd2525Service
{
    /**
     * @return Backing symbology provider.
     */
    @Nonnull
    ISymbologyProvider getSymbologyProvider();

    /**
     * @return Symbol cache used for storing generated icons.
     */
    @Nonnull
    IMilSymIconCache getSymbolCache();
}
