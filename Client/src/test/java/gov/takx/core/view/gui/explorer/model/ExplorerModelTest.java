package gov.takx.core.view.gui.explorer.model;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.core.explorer.model.IExplorerModel;
import gov.takx.test.AContainerTest;
import gov.takx.test.mock.explorer.TestExplorerPropertyChangeListener;
import gov.takx.test.utils.ExplorerTestConstants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ExplorerModelTest extends AContainerTest
{
    @Test
    public void explorerModelAddRemoveTest()
    {
        ExplorerTestConstants data = new ExplorerTestConstants();

        testAdd(data.explorerModel, data.EXPLORABLE_1);
        testAdd(data.explorerModel, data.EXPLORABLE_2);

        testRemove(data.explorerModel, data.EXPLORABLE_2);

        testAdd(data.explorerModel, data.EXPLORABLE_3);
        testAdd(data.explorerModel, data.EXPLORABLE_4);

        testRemove(data.explorerModel, data.EXPLORABLE_1);
        testRemove(data.explorerModel, data.EXPLORABLE_3);
        testRemove(data.explorerModel, data.EXPLORABLE_4);
    }

    @Test
    public void explorerModelLinkUnlinkTest()
    {
        ExplorerTestConstants data = new ExplorerTestConstants();

        testLinkUnlink(data.explorerModel, data.EXPLORABLE_GROUP_1, data.CHILD_1);
        testLinkUnlink(data.explorerModel, data.EXPLORABLE_GROUP_1, data.CHILD_1, data.CHILD_2);
    }

    @Test
    public void explorerModelDisposeTest()
    {
        ExplorerTestConstants data = new ExplorerTestConstants();

        // Add some explorables, linking some children to a parent.
        testAdd(data.explorerModel, data.EXPLORABLE_GROUP_1);
        testAdd(data.explorerModel, data.CHILD_1);
        testAdd(data.explorerModel, data.CHILD_2);

        data.EXPLORABLE_GROUP_1.addChild(data.CHILD_1);
        data.EXPLORABLE_GROUP_1.addChild(data.CHILD_2);

        // Check that the explorer has the correct child count for the parent.
        assertThat(data.EXPLORABLE_GROUP_1.getChildrenCount()).isEqualTo(2);

        // Add some more assorted explorables.
        testAdd(data.explorerModel, data.EXPLORABLE_1);
        testAdd(data.explorerModel, data.EXPLORABLE_2);
        testAdd(data.explorerModel, data.EXPLORABLE_3);
        testAdd(data.explorerModel, data.EXPLORABLE_4);

        // DISPOSE!
        data.explorerModel.dispose();

        // Check that the model is now empty.
        assertThat(data.explorerModel.getExplorables().size()).isLessThanOrEqualTo(0);
    }

    private void testLinkUnlink(IExplorerModel explorerModel, IUserExplorableGroup parent, IExplorable... children)
    {
        // Add the parent to the model
        testAdd(explorerModel, parent);

        // Add the children and link them to the parent.
        for (IExplorable child : children)
        {
            testAdd(explorerModel, parent, child);
        }

        testRemove(explorerModel, parent);
        for (IExplorable child : children)
        {
            testRemove(explorerModel, child);
        }
    }

    private void testAdd(IExplorerModel explorerModel, IExplorable explorableToAdd)
    {
        testAdd(explorerModel, null, explorableToAdd);
    }

    private void testAdd(IExplorerModel explorerModel, IUserExplorableGroup parent, IExplorable explorableToAdd)
    {
        // Model should not yet have the explorable.
        assertThat(explorerModel.hasExplorable(explorableToAdd.getUniqueKey())).isFalse();

        // Get the current size for later comparison.
        int oldSize = explorerModel.getExplorables().size();

        // Create the tester object - this will track information on the add event.
        TestExplorerPropertyChangeListener addedTester = new TestExplorerPropertyChangeListener(
                IExplorerModel.EXPLORABLE_ADDED,
                null,
                explorableToAdd,
                explorerModel);

        // Add the explorable
        if (parent == null)
        {
            explorerModel.addExplorable(explorableToAdd);
        } else
            parent.addChild(explorableToAdd);

        // Make sure the number of explorables in the model has grown by one.
        assertThat(explorerModel.getExplorables()).hasSize(oldSize + 1);

        // Check the tester object's status.
        assertThat(addedTester.hasOccurred()).isTrue();
        assertThat(addedTester.isExpectedNewValue()).isTrue();

        // The explorable should now be in the model.
        assertThat(explorerModel.hasExplorable(explorableToAdd.getUniqueKey())).isTrue();
        assertThat(explorerModel.getExplorable(explorableToAdd.getUniqueKey())).isEqualTo(explorableToAdd);

        if (parent != null)
        {
            List<IExplorable> childSet = parent.getChildren();
            assertThat(childSet).contains(explorableToAdd);
        }
    }

    private void testRemove(IExplorerModel explorerModel, IExplorable explorableToRemove)
    {
        // Model should currently have the explorable.
        assertThat(explorerModel.hasExplorable(explorableToRemove.getUniqueKey())).isTrue();

        // Get the current size for later comparison.
        int oldSize = explorerModel.getExplorables().size();

        // Create the tester object - this will track information on the remove event.
        TestExplorerPropertyChangeListener removedTester = new TestExplorerPropertyChangeListener(
                IExplorerModel.EXPLORABLE_REMOVED,
                explorableToRemove,
                null,
                explorerModel);

        // Remove the explorable
        explorerModel.removeExplorable(explorableToRemove);

        // Check the tester object's status.
        assertThat(explorerModel.getExplorables()).hasSize(oldSize - 1);
        assertThat(removedTester.hasOccurred()).isTrue();
        assertThat(removedTester.isExpectedOldValue()).isTrue();

        // The explorable should no longer be in the model.
        assertThat(explorerModel.hasExplorable(explorableToRemove.getUniqueKey())).isFalse();
        assertThat(explorerModel.getExplorable(explorableToRemove.getUniqueKey())).isNull();
    }

}
