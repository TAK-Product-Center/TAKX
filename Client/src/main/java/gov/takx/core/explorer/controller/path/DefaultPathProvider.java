package gov.takx.core.explorer.controller.path;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorablePathProvider;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Implementation of {@link IExplorablePathProvider} that simply returns the parent path within the {@link IExplorable}.
 *
 * @since 5.3
 */
public class DefaultPathProvider implements IExplorablePathProvider
{
    @Override
    @Nonnull
    public List<String> createDefaultPath(@Nonnull IExplorable explorable)
    {
        return explorable.getParentPath();
    }
}