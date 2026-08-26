package gov.takx.api.plugin.datamonitor;

import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.ADataMonitor;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URL;

/**
 * An abstract extension of {@link ADataMonitor} for use in standard data monitors.
 * It prevents changes to the display name and icon.
 *
 * @since 1.11
 */
@Internal
abstract class AStockDataMonitor extends ADataMonitor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructor supporting specification of the display name and icon URL.
     * @since 3.3
     */
    protected AStockDataMonitor(@Nonnull String family, @Nonnull String type, @Nonnull String displayName, @Nonnull URL customIconUrl)
    {
        super(family, type, displayName, customIconUrl);
        setAffectsAudioAndIcons(true);
    }

    @Override
    public void setDisplayName(@Nonnull String name)
    {
        // Cannot be renamed.
        logger.warn("Stock data monitors cannot be renamed.");
    }

    @Override
    public void setCustomIcon(@Nonnull URL url)
    {
        // Cannot override stock icon.
        logger.warn("The custom icon used for stock data monitors cannot be changed.");
    }
}
