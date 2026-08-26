package gov.takx.core.view.gui.projectadmin;

import gov.takx.test.AContainerTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests on {@link KnownProjectDirectoriesManager}.
 *
 * @since 2.20
 */
@TestMethodOrder(DependsOnOrdering.class)
public class KnownProjectDirectoriesManagerTest extends AContainerTest
{
    private static final Path SOME_PATH = Paths.get("/some/path").toAbsolutePath();
    private static final Path ANOTHER_PATH = Paths.get("another/Path").toAbsolutePath();
    private static final Path LONG_PATH = Paths.get("./IAmAVeryLongPathName/AndIamCertainlyLongerThan/80Characters").toAbsolutePath();

    private KnownProjectDirectoriesManager manager;

    @BeforeAll
    public void clearPrefs()
    {
        manager = getBean(KnownProjectDirectoriesManager.class); // Find the manager
        manager.setKnownDirectories(Collections.emptySet()); // Start empty
    }

    /**
     * Ensure that since the collection was cleared in @BeforeAll, that we actually start with an empty set.
     */
    @Test
    public void testStartsEmpty()
    {
        assertThat(manager.getKnownProjectDirectories()).isEmpty();
    }

    /**
     * Ensure that when we set the collection of paths, that we get it back from the manager when requested.
     */
    @Test
    @DependsOn("testStartsEmpty")
    public void testGetReturnsWhatWasSet()
    {
        Set<Path> paths = new HashSet<>(Collections.singletonList(SOME_PATH));
        manager.setKnownDirectories(paths);
        assertThat(manager.getKnownProjectDirectories()).isEqualTo(paths);

        // And ensure we can modify it more than once
        paths = new HashSet<>(Arrays.asList(SOME_PATH, ANOTHER_PATH));
        manager.setKnownDirectories(paths);
        assertThat(manager.getKnownProjectDirectories()).isEqualTo(paths);
    }

    /**
     * Ensure that we can store a path longer than 80 characters (regression test to verify CORE-5150).
     */
    @Test
    @DependsOn("testStartsEmpty")
    public void testLongPath()
    {
        assertThat(LONG_PATH.toAbsolutePath().toString().length()).isGreaterThan(80); // Ensure test data is long enough

        final Set<Path> paths = new HashSet<>(Arrays.asList(SOME_PATH, LONG_PATH));

        manager.setKnownDirectories(paths); // Prior to fixing CORE-5150, this would have thrown an exception
        assertThat(manager.getKnownProjectDirectories()).isEqualTo(paths);
    }
}
