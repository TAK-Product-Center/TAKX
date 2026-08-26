package gov.takx.core.view.gui.explorer;

import gov.takx.api.explorer.Explorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorerDelegate;
import gov.takx.commons.swing.entity.SortFilterEntityTreeTableModel;
import gov.takx.core.explorer.Explorer;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.view.gui.explorer.view.ExplorerPanel;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for explorer manager functionality.
 *
 * @since 2.7
 */
@TestMethodOrder(DependsOnOrdering.class)
public class ExplorerManagerTest extends AIntegrationTest
{
    private IExplorerManager explorerManager;
    private IExplorer defaultExplorer;

    @BeforeAll
    public void beforeAll()
    {
        explorerManager = getBean(IExplorerManager.class);
        defaultExplorer = explorerManager.getDefaultExplorerDelegate();
    }

    @AfterEach
    public void clearExplorers()
    {
        // Remove any leftover explorables
        explorerManager.getExplorerDelegates().forEach(e -> {
            Collection<IExplorable> explorables = e.getExplorables();
            for (IExplorable explorable : explorables)
            {
                e.removeExplorable(explorable, false);
            }
        });

        // Delete any leftover explorers
        explorerManager.getExplorerDelegates().stream()
                .map(IExplorerDelegate::getUUID)
                .filter(id -> !id.equals(defaultExplorer.getUUID()))
                .forEach(id -> explorerManager.deleteExplorer(id));
    }

    @Test
    public void testCreateExplorer()
    {
        // Get initial number of explorers for comparison
        int size = explorerManager.getExplorerDelegates().size();

        IExplorerDelegate testExplorer = explorerManager.createExplorer("Test");
        assertThat(testExplorer).isNotNull();

        IExplorerDelegate returnedExplorer = explorerManager.getExplorerDelegate(testExplorer.getUUID());
        assertThat(testExplorer).isEqualTo(returnedExplorer);

        List<IExplorerDelegate> explorerDelegates = explorerManager.getExplorerDelegates();
        assertThat(explorerDelegates).hasSize(size + 1);
        assertThat(explorerDelegates).contains(testExplorer);
    }

    /**
     * Test for RAPTORX-1624 (creating embeddable explorers)
     * <p>
     * This ensures that a hidden explorer can be created, that it receives a UUID, and that it is included in
     * the explorer manager's list of delegates.
     *
     * @since 2.39
     */
    @Test
    void testCreateEmbeddableExplorer()
    {
        int initialNumberOfExplorers = explorerManager.getExplorerDelegates().size();

        IExplorerDelegate testEmbeddableExplorer = explorerManager.createEmbeddableExplorer("Test");
        assertThat(testEmbeddableExplorer).isNotNull();
        IExplorerDelegate retrievedExplorerByUUID = explorerManager.getExplorerDelegate(testEmbeddableExplorer.getUUID());
        assertThat(retrievedExplorerByUUID).isEqualTo(testEmbeddableExplorer);

        List<IExplorerDelegate> explorerDelegates = explorerManager.getExplorerDelegates();
        assertThat(explorerDelegates).hasSize(initialNumberOfExplorers + 1);
        assertThat(explorerDelegates).contains(testEmbeddableExplorer);
    }

    /**
     * Changes to the display name should not affect the explorable path name.  The path should be equal to the
     * original display name.
     *
     * @since 5.3
     */
    @Test
    public void addExplorable_changeDisplayName_pathUnchanged()
    {
        IExplorerDelegate testExplorer = explorerManager.createExplorer("Test");

        TestExplorable explorable = new TestExplorable("Test 1", "Parent 1");
        testExplorer.addExplorable(explorable);
        explorable.setDisplayName("New DisplayName");
        assertThat(explorable.getPathName()).isEqualTo("Test 1");
        assertThat(explorable.getPath()).contains("Test 1");
        assertThat(explorable.getDisplayName()).isEqualTo("New DisplayName");

        // Verify that we can still find it under the original path
        assertThat(testExplorer.getExplorableByPath("Parent 1/Test 1")).isEqualTo(explorable);
    }

    /**
     * Test that adding nodes out of order alphabetically, can then be sorted in forward and reverse order
     * using a predicate comparator.
     *
     * @since 5.7.1
     */
    @Test
    public void testAddExplorables_addPredicateComparators_childrenSorted()
    {
        IExplorerDelegate testExplorer = explorerManager.createExplorer("ComparatorTest");
        TestExplorable explorable = new TestExplorable("Parent");
        testExplorer.addExplorable(explorable);
        TestExplorable explorableB = new TestExplorable("Child B", "Parent");
        testExplorer.addExplorable(explorableB);
        TestExplorable explorableC = new TestExplorable("Child C", "Parent");
        testExplorer.addExplorable(explorableC);
        TestExplorable explorableA = new TestExplorable("Child A", "Parent");
        testExplorer.addExplorable(explorableA);

        ((Explorer) testExplorer).initUI();
        SwingTestUtils.waitForEdtEvents();

        ExplorerPanel explorerPanel = ((Explorer) testExplorer).getExplorerPanel();
        SortFilterEntityTreeTableModel<IExplorable> model = explorerPanel.getFilteredExplorerModel();

        testExplorer.registerExplorableComparator(n -> n instanceof TestExplorable,
                Comparator.comparing(IExplorable::getDisplayName, Comparator.reverseOrder()));

        int indexA = model.getIndexOfChild(explorable, explorableA);
        int indexB = model.getIndexOfChild(explorable, explorableB);
        int indexC = model.getIndexOfChild(explorable, explorableC);

        assertThat(indexA).isEqualTo(2);
        assertThat(indexB).isEqualTo(1);
        assertThat(indexC).isEqualTo(0);

        testExplorer.registerExplorableComparator(n -> n instanceof TestExplorable,
                Comparator.comparing(IExplorable::getDisplayName, String::compareToIgnoreCase));

        indexA = model.getIndexOfChild(explorable, explorableA);
        indexB = model.getIndexOfChild(explorable, explorableB);
        indexC = model.getIndexOfChild(explorable, explorableC);

        assertThat(indexA).isEqualTo(0);
        assertThat(indexB).isEqualTo(1);
        assertThat(indexC).isEqualTo(2);
    }

    @Test
    @DependsOn("testCreateExplorer")
    @Disabled
    public void testAddExplorable_noCopyProperty()
    {
        // Get initial number of explorables in default explorer for comparison
        int defaultExplorablesCount = defaultExplorer.getExplorables().size();

        IExplorerDelegate testExplorer = explorerManager.createExplorer("Test");

        TestExplorable explorable = new TestExplorable("Test 1", "Parent 1");
        testExplorer.addExplorable(explorable);

        Collection<IExplorable> testExplorables = testExplorer.getExplorables();
        assertThat(testExplorables).hasSize(3); // Explorable + parent
        assertThat(testExplorables).contains(explorable);

        // Make sure nothing was added to the default explorer
        assertThat(defaultExplorer.getExplorables().size()).isEqualTo(defaultExplorablesCount);
    }

    @Test
    @DependsOn("testCreateExplorer")
    @Disabled
    public void testAddExplorable_copyProperty()
    {
        // Get initial number of explorables in default explorer for comparison
        int defaultExplorablesCount = defaultExplorer.getExplorables().size();

        IExplorerDelegate testExplorer = explorerManager.createExplorer("Test");

        // Create explorable with a copy property so that it will be copied to the default explorer
        String displayName = "Test 1";
        String parentName = "Parent 1";
        TestExplorable explorable = new TestExplorable(displayName, parentName);
        explorable.addProperty(IExplorable.COPY_TO_DEFAULT_EXPLORER, true);

        testExplorer.addExplorable(explorable);

        Collection<IExplorable> testExplorables = testExplorer.getExplorables();
        assertThat(testExplorables).hasSize(3); // Explorable + parent
        assertThat(testExplorables).contains(explorable);

        // Make sure explorable was copied to the default explorer as a proxy
        Collection<IExplorable> defaultExplorables = defaultExplorer.getExplorables();
        assertThat(defaultExplorables).hasSize(defaultExplorablesCount + 2); // Explorable + parent

        Optional<IExplorable> foundExplorable = defaultExplorables.stream()
                .filter(e -> e.getDisplayName().equals(displayName))
                .findFirst();
        assertThat(foundExplorable).isPresent();

        foundExplorable = defaultExplorables.stream()
                .filter(e -> e.getDisplayName().equals(parentName))
                .findFirst();
        assertThat(foundExplorable).isPresent();
    }

    @Test
    @DependsOn({"testCreateExplorer", "testAddExplorable_copyProperty"})
    @Disabled
    public void testAddExplorable_copyPropertyAndPrependParent()
    {
        // Get initial number of explorables in default explorer for comparison
        int defaultExplorablesCount = defaultExplorer.getExplorables().size();

        IExplorerDelegate testExplorer = explorerManager.createExplorer("Test");
        Collection<IExplorable> base = testExplorer.getExplorables();

        // Create explorable with a copy property so that it will be copied to the default explorer
        String displayName = "Test 1";
        String parentName = "Parent 1";
        String prependedParent = "Parent 0";
        TestExplorable explorable = new TestExplorable(displayName, parentName);
        explorable.addProperty(IExplorable.COPY_TO_DEFAULT_EXPLORER, true);
        explorable.addProperty(IExplorable.PARENT_TO_PREPEND_TO_PATH_ON_COPY, prependedParent);

        testExplorer.addExplorable(explorable);

        Collection<IExplorable> testExplorables = testExplorer.getExplorables();
        assertThat(testExplorables).hasSize(2 + base.size()); // Explorable + parent
        assertThat(testExplorables).contains(explorable);

        // Make sure explorable was copied to the default explorer as a proxy
        Collection<IExplorable> defaultExplorables = defaultExplorer.getExplorables();
        assertThat(defaultExplorables).hasSize(defaultExplorablesCount + 3); // Explorable + 2 parents

        Optional<IExplorable> foundExplorable = defaultExplorables.stream()
                .filter(e -> e.getDisplayName().equals(displayName))
                .findFirst();
        assertThat(foundExplorable).isPresent();

        IExplorable copiedExplorable = foundExplorable.get();
        assertThat(copiedExplorable.getParentPath()).containsExactly(prependedParent, parentName);

        // Check for original parent name
        foundExplorable = defaultExplorables.stream()
                .filter(e -> e.getDisplayName().equals(parentName))
                .findFirst();
        assertThat(foundExplorable).isPresent();

        // Check for prepended parent name
        foundExplorable = defaultExplorables.stream()
                .filter(e -> e.getDisplayName().equals(prependedParent))
                .findFirst();
        assertThat(foundExplorable).isPresent();
    }

    private static class TestExplorable extends Explorable
    {
        private final Map<String, Object> propertyMap = new HashMap<>();

        public TestExplorable(String displayName, String... parentPath)
        {
            super(displayName, Arrays.asList(parentPath));
        }

        @Override
        public final Collection<String> getKeys()
        {
            return Collections.unmodifiableCollection(propertyMap.keySet());
        }

        @Override
        public final boolean hasKey(String key)
        {
            return propertyMap.containsKey(key);
        }

        @Override
        public final Object getValue(String key)
        {
            return propertyMap.get(key);
        }

        /**
         * Adds the provided property key, value pair to the explorable
         *
         * @param propertyKey   The property key
         * @param propertyValue The property value
         */
        void addProperty(String propertyKey, Object propertyValue)
        {
            propertyMap.put(propertyKey, propertyValue);
        }
    }
}
