package gov.takx.core.explorer.controller.path;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IUserExplorableGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Implementation of {@link IExplorablePathProvider} that nests all {@link IUserExplorableGroup}s under the root groups path.
 *
 * @since 5.3
 */
public class ControllerGroupPathProvider implements IExplorablePathProvider
{
    @Override
    @Nullable
    public List<String> createDefaultPath(@Nonnull IExplorable explorable)
    {
        return explorable instanceof IUserExplorableGroup ? ExplorerPathConstants.GROUPS_PATH : null;
    }
}