package gov.takx.core.explorer.controller.path;

import gov.takx.api.explorer.IAppExplorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorablePathProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Implementation of {@link IExplorablePathProvider} that provides paths for instances of {@link IAppExplorable}.
 *
 * @since 5.3
 */
public class AppExplorablePathProvider implements IExplorablePathProvider
{
    @Nullable
    @Override
    public List<String> createDefaultPath(@Nonnull IExplorable explorable)
    {
        return explorable instanceof IAppExplorable ? ExplorerPathConstants.APPS_PATH : null;
    }
}