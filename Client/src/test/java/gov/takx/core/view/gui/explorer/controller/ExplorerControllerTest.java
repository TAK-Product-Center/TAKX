package gov.takx.core.view.gui.explorer.controller;

import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.explorer.IExplorable;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.controller.ExplorerController;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.ExplorerTestConstants;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.47
 */
public class ExplorerControllerTest extends AIntegrationTest
{
    @Test
    public void explorerAddRemoveTest(@Mocked IExplorer explorer)
    {
        ExplorerTestConstants data = new ExplorerTestConstants();
        IExplorerController controller = new ExplorerController(explorer, data.explorerModel);

        // Test adding explorable objects.  Any parent groups that don't exist will be created and tested for existence.
        testAdd(controller, data.EXPLORABLE_1);
        testAdd(controller, data.EXPLORABLE_2);
        testAdd(controller, data.EXPLORABLE_3);

        // Test Removal of explorable objects
        testRemove(controller, data.EXPLORABLE_1);
        testRemove(controller, data.EXPLORABLE_2);
        testRemove(controller, data.EXPLORABLE_3);

        disposeController(controller);
    }

    @Test
    public void explorerDisposeObjectsTest(@Mocked IExplorer explorer)
    {
        ExplorerTestConstants data = new ExplorerTestConstants();
        IExplorerController controller = new ExplorerController(explorer, data.explorerModel);

        // Add new explorer objects into new groups
        List<IExplorable> add = List.of(data.EXPLORABLE_GROUP_1, data.EXPLORABLE_1, data.EXPLORABLE_2, data.EXPLORABLE_3);
        add.forEach(e -> testAdd(controller, e));

        // Setup a listener for disposal events coming from explorer groups.
        DisposeListener groupDisposalListener = new DisposeListener(add);

        assertThat(controller.removeExplorable(data.EXPLORABLE_GROUP_1)).isTrue();
        assertThat(controller.removeExplorable(data.EXPLORABLE_1)).isTrue();
        assertThat(controller.removeExplorable(data.EXPLORABLE_2)).isTrue();
        assertThat(controller.removeExplorable(data.EXPLORABLE_3)).isTrue();

        // The number of disposed explorer groups should equal the total number of explorer groups.
        assertThat(groupDisposalListener.getDisposedCount()).isEqualTo(groupDisposalListener.getExpectedDisposedCount());

        disposeController(controller);
    }

    private void disposeController(IExplorerController explorer)
    {
        // Flush the explorer
        explorer.onClearState(null);
        assertThat(explorer.getExplorables()).isEmpty();
    }

    private void testAdd(IExplorerController explorer, IExplorable explorable)
    {
        // Make sure explorer object has not yet been added
        assertThat(explorer.getExplorable(explorable.getUniqueKey())).isNull();

        // Add explorer object (and its parent path)
        explorer.addExplorable(explorable);

        // Check that the new explorer object was added.
        assertThat(explorable).isEqualTo(explorer.getExplorable(explorable.getUniqueKey()));
    }

    private void testRemove(IExplorerController explorer, IExplorable e)
    {
        // Make sure that the explorer starts out with the explorable
        assertThat(explorer.getExplorable(e.getUniqueKey())).isNotNull();

        explorer.removeExplorable(e);

        // Make sure that the explorer no longer has the explorable
        assertThat(explorer.getExplorable(e.getUniqueKey())).isNull();
    }

    /**
     * Attaches as property change listener to explorables.  Tracks the number of disposed events.
     */
    protected static class DisposeListener implements PropertyChangeListener
    {
        private int disposedCount;
        private int expectedDisposedCount;

        DisposeListener(Collection<? extends IExplorable> explorables)
        {
            disposedCount = 0;
            expectedDisposedCount = 0;
            for (IExplorable explorable : explorables)
            {
                explorable.addPropertyChangeListener(this);
                expectedDisposedCount++;
            }
        }

        int getDisposedCount()
        {
            return disposedCount;
        }

        int getExpectedDisposedCount()
        {
            return expectedDisposedCount;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (evt.getPropertyName().equals(IExplorable.DISPOSED))
            {
                disposedCount++;
            }
        }
    }
}
