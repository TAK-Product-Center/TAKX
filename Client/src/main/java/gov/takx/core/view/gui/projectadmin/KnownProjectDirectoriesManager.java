package gov.takx.core.view.gui.projectadmin;

import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.api.project.IProjectManager;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manager to track and update the known project directories that have been used.  These are stored in the
 * Program Properties. The collection of directories being managed here is not the set of individual project
 * directories, it is the the unique set of the parent directories of all projects that have been opened.
 * <p>
 * Since these directory paths are stored in a {@code Preferences} node, we can not use the actual path as the
 * value key since it may exceed the 80 character key length limit.  So, instead, we maintain the known paths
 * as a set (to avoid duplicate entries), and use an integer as the key for the value on the node.
 *
 * @see PreferenceEntry#KNOWN_PROJECT_DIRECTORIES_NODE
 * @since 2.20
 */
@ApplicationScoped
public class KnownProjectDirectoriesManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IPreferenceManager preferenceManager;

    @Inject
    private IProjectManager projectManager;

    private final PreferencesNode knownDirectoriesNode = PreferenceEntry.KNOWN_PROJECT_DIRECTORIES_NODE.node();

    private Set<Path> knownDirectories;

    @PostConstruct
    private void postConstruct()
    {
        knownDirectories = knownDirectoriesNode.values()
                .stream()
                .map(Paths::get)
                .collect(Collectors.toSet());
    }

    /**
     * Get all the directories within which projects have been opened (the unique collection of the parent
     * directories of all projects that have been opened).
     *
     * @return Set of known project parent directory paths
     */
    public Set<Path> getKnownProjectDirectories()
    {
        return Collections.unmodifiableSet(knownDirectories);
    }

    /**
     * Set the known project parent directories to the given set of Paths.
     *
     * @param directories New collection of project parent directories
     */
    public void setKnownDirectories(Set<Path> directories)
    {
        knownDirectories = new HashSet<>(directories);
        updateNodeWithDirectories();
    }

    /**
     * Observe project opened events and record the (potentially) new project parent directory path into
     * the preferences.
     */
    private void handleNewProject(@SuppressWarnings("unused") @Observes ProjectOpened event)
    {
        final String projectDirectory = projectManager.getCurrentOpenProject().getProjectDirectory();
        final Path parentPath = Paths.get(projectDirectory).getParent();

        knownDirectories.add(parentPath);
        updateNodeWithDirectories();
    }

    /**
     * Update our "known directories" node with the current set of directories.
     */
    private void updateNodeWithDirectories()
    {
        knownDirectoriesNode.putAsValues(knownDirectories, p -> p.toAbsolutePath().toString());
        preferenceManager.savePreferences();
    }
}
