package gov.takx.core.media.decode;

import java.awt.Dimension;
import java.util.Optional;

/**
 * Configuration options for {@link StreamHandler}s
 *
 * @since 2.8
 */
public class HandlerConfiguration
{
    Optional<Dimension> maxImageOutputSize = Optional.empty();

    public Optional<Dimension> getMaxImageOutputSize()
    {
        return maxImageOutputSize;
    }

    public void setMaxImageOutputSize(int width, int height)
    {
        maxImageOutputSize = Optional.of(new Dimension(width, height));
    }
}
