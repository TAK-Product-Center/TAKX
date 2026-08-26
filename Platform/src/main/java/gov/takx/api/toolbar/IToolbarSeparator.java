package gov.takx.api.toolbar;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.IPlugin;

/**
 * Interface representing a vertical separator that can be added to an {@link IToolbarTab} for display on the 
 * application toolbar. To create a toolbar separator, use {@link IToolbarManager#createSeparator(IPlugin, String)}.
 *
 * @since 3.4
 */
@NoPluginImplementations
public interface IToolbarSeparator extends IToolbarComponent
{
}
