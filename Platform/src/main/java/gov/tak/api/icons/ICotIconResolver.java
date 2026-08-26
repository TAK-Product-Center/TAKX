package gov.tak.api.icons;

import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.awt.image.BufferedImage;

/**
 * Contract for service capable of resolving CoT icons from CoT types.
 *
 * @since 5.0
 */
@InjectableService
@NoPluginImplementations
public interface ICotIconResolver
{
    /**
     * Resolves a CoT type to an image.
     *
     * @param sizePixels Desired size of the resulting icon.
     * @param cotType The CoT type to use for looking up known icons.
     *
     * @return Image for the CoT type, or a generic unknown icon.
     */
    @Nonnull
    BufferedImage getIcon(int sizePixels, @Nonnull String cotType);

    /**
     * Resolves a CoT event to an image based on its type.
     *
     * @param sizePixels Desired size of the resulting icon.
     * @param event The CoT event with a type to use for looking up known icons.
     *
     * @return Image for the CoT type, or a generic unknown icon.
     */
    @Nonnull
    BufferedImage getIcon(int sizePixels, @Nonnull CotEvent event);
}
