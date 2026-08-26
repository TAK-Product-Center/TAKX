package gov.takx.core.milsym;

import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.APP6dSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525cSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525dSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525eSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525ech1SymbologyProvider;
import gov.takx.api.milsym.IExtendedMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.MilStd2525Variant;
import gov.takx.api.preferences.IPreferenceMonitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
    private final IExtendedMilStd2525Service app6dService = new MilStd2525Service(this, new APP6dSymbologyProvider(), MilStd2525Variant.APP6D);
    private final IExtendedMilStd2525Service charlieService = new MilStd2525Service(this, new MilStd2525cSymbologyProvider(), MilStd2525Variant.CHARLIE);
    private final IExtendedMilStd2525Service deltaService = new MilStd2525Service(this, new MilStd2525dSymbologyProvider(), MilStd2525Variant.DELTA);
    private final IExtendedMilStd2525Service echoService = new MilStd2525Service(this, new MilStd2525eSymbologyProvider(), MilStd2525Variant.ECHO);
    private final IExtendedMilStd2525Service echoch1Service = new MilStd2525Service(this, new MilStd2525ech1SymbologyProvider(), MilStd2525Variant.ECHOCH1);

    private final IPreferenceMonitor<String> currentServiceMonitor = MIL_STD_2525_PREF.newStringMonitor(this::updatePreferredService);
    private IExtendedMilStd2525Service currentService;

    private Map<MilStd2525Variant, IExtendedMilStd2525Service> services;

    @PostConstruct
    private void postConstruct()
    {
        services = Map.ofEntries(
                Map.entry(MilStd2525Variant.CHARLIE, charlieService),
                Map.entry(MilStd2525Variant.DELTA, deltaService),
                Map.entry(MilStd2525Variant.ECHO, echoService),
                Map.entry(MilStd2525Variant.ECHOCH1, echoch1Service),
                Map.entry(MilStd2525Variant.APP6D, app6dService));

        currentService = getImpl(currentServiceMonitor.getCurrentValue());
    }

    @Nonnull
    @Override
    public IExtendedMilStd2525Service get2525ServiceForSymbol(@Nonnull String symbolCode)
    {
        // Default to Charlie if the symbol is empty.
        if (symbolCode.isBlank()) return charlieService;

        do // Defer to SymbologyProvider to determine best service
        {
            final var provider = SymbologyProvider.getProviderFromSymbol(symbolCode);
            if (provider == null) break;

            final var service = services.values().stream().filter(x -> provider == x.getSymbologyProvider()).findFirst();
            if (service.isEmpty()) break;

            return service.get();
        } while (false);

        return charlieService;
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
    public @NotNull IMilStd2525Service get2525Service(@NotNull MilStd2525Variant variant)
    {
        return services.getOrDefault(variant, charlieService);
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
        return services.getOrDefault(MilStd2525Variant.fromName(stdImpl), charlieService);
    }
}
