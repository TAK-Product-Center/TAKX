package gov.takx.core.milsym;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.platform.symbology.milstd2525.MilStd2525cSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525dSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525eSymbologyProvider;
import gov.takx.api.milsym.IExtendedMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.preferences.IPreferenceMonitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

import static gov.takx.platform.preferences.PreferenceEntry.MIL_STD_2525_PREF;

/**
 * Service implementation of {@link IMilStd2525ServiceProvider}.
 * <p/>
 * Provides a singleton instance of {@link IMilStd2525Service} / {@link gov.takx.api.milsym.IExtendedMilStd2525Service}.
 *
 * @since 5.1
 */
@ApplicationScoped
public class MilStd2525ServiceProvider implements IMilStd2525ServiceProvider
{
    private final IExtendedMilStd2525Service charlieService = new MilStd2525Service(this, new MilStd2525cSymbologyProvider());
    private final IExtendedMilStd2525Service deltaService = new MilStd2525Service(this, new MilStd2525dSymbologyProvider());
    private final IExtendedMilStd2525Service echoService = new MilStd2525Service(this, new MilStd2525eSymbologyProvider());
    private final IPreferenceMonitor<String> currentServiceMonitor = MIL_STD_2525_PREF.newStringMonitor(this::updatePreferredService);
    private IExtendedMilStd2525Service currentService = getImpl(currentServiceMonitor.getCurrentValue());

    @Override
    @Nonnull
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public IExtendedMilStd2525Service getMilStd2525Service()
    {
        return get2525cService();
    }

    @Nonnull
    @Override
    public IExtendedMilStd2525Service get2525ServiceForSymbol(@Nonnull String symbolCode)
    {
        // Default to Charlie if the symbol is empty.
        if (symbolCode.isBlank()) return charlieService;

        // Numeric symbols are D/E, where E has a specific version code in the 2nd character.
        char identifier = symbolCode.charAt(1);
        return Character.isDigit(identifier) ? (identifier == '3' ? echoService : deltaService) : charlieService;
    }

    @Override
    @Nonnull
    public IExtendedMilStd2525Service get2525cService()
    {
        return charlieService;
    }

    @Override
    @Nonnull
    public IExtendedMilStd2525Service get2525dService()
    {
        return deltaService;
    }

    @Override
    @Nonnull
    public IExtendedMilStd2525Service get2525eService()
    {
        return echoService;
    }

    @Override
    @Nonnull
    public IExtendedMilStd2525Service getPreferred2525Service()
    {
        return currentService;
    }

    private void updatePreferredService(@Nullable String stdImpl)
    {
        currentService = getImpl(stdImpl);
    }

    @Nonnull
    private IExtendedMilStd2525Service getImpl(@Nullable String stdImpl)
    {
        return switch (Character.toLowerCase(Objects.requireNonNullElse(stdImpl, "c").charAt(0)))
        {
            case 'd' -> deltaService;
            case 'e' -> echoService;
            default -> charlieService;
        };
    }
}
