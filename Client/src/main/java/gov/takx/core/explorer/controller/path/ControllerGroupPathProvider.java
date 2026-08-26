package gov.takx.core.explorer.controller.path;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.core.explorer.controller.ExplorerHashtagController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Implementation of {@link IExplorablePathProvider} that nests all {@link IUserExplorableGroup}s under designated paths.
 *
 * @since 5.3
 */
public class ControllerGroupPathProvider implements IExplorablePathProvider
{
    @Override
    @Nullable
    public List<String> createDefaultPath(@Nonnull IExplorable explorable)
    {
        return explorable instanceof IUserExplorableGroup
                ? explorable.getUniqueKey().startsWith(ExplorerHashtagController.HASHTAG_GROUP_KEY_PREFIX)
                        ? ExplorerPathConstants.HASHTAGS_PATH
                        : ExplorerPathConstants.GROUPS_PATH
                : null;
    }
}