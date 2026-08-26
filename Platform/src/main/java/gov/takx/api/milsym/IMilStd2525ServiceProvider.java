package gov.takx.api.milsym;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;

/**
 * Contract for a service providing a MIL-STD-2525 service to injection targets.
 * <p/>
 * <b>Note:</b> This does not "produce" a {@link IMilStd2525Service} in the CDI sense.
 * Instead, this acts as a manager object that offers access to a relevant implementation
 * based on the available symbology providers.
 *
 * @since 5.1
 */
@InjectableService
@NoPluginImplementations
public interface IMilStd2525ServiceProvider
{

    /**
     * @return The 2525C Service
     * @since 5.5
     */
    @Deprecated(since = "5.8", forRemoval = true)
    @DeprecatedApi(since = "5.8", forRemoval = true, removeAt = "5.10")
    @Nonnull
    IMilStd2525Service get2525cService();

    /**
     * @return The 2525D Service.
     * @since 5.5
     */
    @Deprecated(since = "5.8", forRemoval = true)
    @DeprecatedApi(since = "5.8", forRemoval = true, removeAt = "5.10")
    @Nonnull
    IMilStd2525Service get2525dService();

    /**
     * @return The 2525E Service.
     * @since 5.5
     */
    @Deprecated(since = "5.8", forRemoval = true)
    @DeprecatedApi(since = "5.8", forRemoval = true, removeAt = "5.10")
    @Nonnull
    IMilStd2525Service get2525eService();

    /**
     * @param mapObject The {@link IMapObject} to extract an SIDC from to analyze and determine what service it should use.
     * @return The relevant 2525 service based on what kind of SIDC it is.
     * @since 5.5
     */
    @Nonnull
    default IMilStd2525Service get2525ServiceForMapObject(@Nonnull IMapObject mapObject)
    {
        String symbolCode = get2525Service(MilStd2525Variant.CHARLIE).getSymbolCode(mapObject);
        return symbolCode == null ? getPreferred2525Service() : get2525ServiceForSymbol(symbolCode);
    }

    /**
     * @param symbolCode The SIDC code to analyze from a 2525 variant. For instance an equipment sensor could be:
     *                   <ul>
     *                   <li>C: {@code SUGPES----*****}</li>
     *                   <li>D: {@code 10011500002201000000}</li>
     *                   <li>E: {@code 130115000022010000000000000000}</li>
     *                   </ul>
     * @return The relevant 2525 service that handles symbol codes of the same variant.
     * @since 5.5
     */
    @Nonnull
    IMilStd2525Service get2525ServiceForSymbol(@Nonnull String symbolCode);

    /**
     * @param variant Variant of 2525 to select.
     * @return The 2525 service for the respective variant.
     * @since 5.5
     */
    @Nonnull
    default IMilStd2525Service get2525Service(@Nonnull MilStd2525Variant variant)
    {

        if (variant == MilStd2525Variant.CHARLIE)
            return get2525ServiceForSymbol("");
        else
            return get2525ServiceForSymbol(variant.version() + "000000000000000000");
    }

    /**
     * @return The 2525 Service currently selected in the {@link PreferenceEntry#MIL_STD_2525_PREF User Preferences}.
     * @since 5.5
     */
    @Nonnull
    IMilStd2525Service getPreferred2525Service();
}
