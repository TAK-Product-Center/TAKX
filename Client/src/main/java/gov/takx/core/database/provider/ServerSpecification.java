package gov.takx.core.database.provider;

import gov.takx.platform.preferences.PreferencesNode;

import java.io.File;

/**
 * Holder containing details of a database server.
 *
 * @param rootDir       Root directory of project
 * @param configuration Database configuration preferences
 * @since 3.0
 */
public record ServerSpecification(File rootDir, PreferencesNode configuration)
{
}
