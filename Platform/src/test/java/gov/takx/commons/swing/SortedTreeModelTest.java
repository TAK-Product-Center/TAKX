package gov.takx.commons.swing;

import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;

import java.util.Comparator;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.3
 */
public class SortedTreeModelTest
{
    @Test
    public void testConstruct()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel baseModel = new DefaultTreeModel(root);
        new SortFilterTreeModel(baseModel);
    }

    @Test
    public void testInsert()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel baseModel = new DefaultTreeModel(root);
        SortFilterTreeModel sortedModel = new SortFilterTreeModel(baseModel, Comparator.comparing(n -> ((DefaultMutableTreeNode) n).getUserObject().toString()));

        DefaultMutableTreeNode aaa = new DefaultMutableTreeNode("AAA");
        DefaultMutableTreeNode abc = new DefaultMutableTreeNode("ABC");
        DefaultMutableTreeNode def = new DefaultMutableTreeNode("DEF");
        DefaultMutableTreeNode zzz = new DefaultMutableTreeNode("ZZZ");

        insertNodes(baseModel, root, abc);
        assertChildren(sortedModel, root, abc);

        insertNodes(baseModel, root, aaa);
        assertChildren(sortedModel, root, aaa, abc);

        insertNodes(baseModel, root, def);
        assertChildren(sortedModel, root, aaa, abc, def);

        insertNodes(baseModel, root, zzz);
        assertChildren(sortedModel, root, aaa, abc, def, zzz);
    }

    /**
     * @since 5.5.1
     */
    @Test
    public void testChangeSort()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel baseModel = new DefaultTreeModel(root);
        SortFilterTreeModel sortedModel = new SortFilterTreeModel(baseModel, Comparator.comparing(n -> ((DefaultMutableTreeNode) n).getUserObject().toString()));

        DefaultMutableTreeNode aaa = new DefaultMutableTreeNode("AAA");
        DefaultMutableTreeNode def = new DefaultMutableTreeNode("DEF");
        DefaultMutableTreeNode zzz = new DefaultMutableTreeNode("ZZZ");

        baseModel.insertNodeInto(zzz, root, 0);
        assertChildren(sortedModel, root, zzz);

        baseModel.insertNodeInto(aaa, root, 0);
        assertChildren(sortedModel, root, aaa, zzz);

        baseModel.insertNodeInto(def, root, 0);
        assertChildren(sortedModel, root, aaa, def, zzz);

        aaa.setUserObject("FFF");
        baseModel.nodeChanged(aaa);

        assertChildren(sortedModel, root, def, aaa, zzz);
    }

    @Test
    public void testPrepopulate()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel baseModel = new DefaultTreeModel(root);

        DefaultMutableTreeNode zzz = new DefaultMutableTreeNode("ZZZ");
        DefaultMutableTreeNode abc = new DefaultMutableTreeNode("ABC");
        DefaultMutableTreeNode aaa = new DefaultMutableTreeNode("AAA");
        DefaultMutableTreeNode def = new DefaultMutableTreeNode("DEF");
        insertNodes(baseModel, root, zzz, abc, aaa, def);
        assertChildren(baseModel, root, def, aaa, abc, zzz);

        SortFilterTreeModel sortedModel = new SortFilterTreeModel(baseModel, Comparator.comparing(n -> ((DefaultMutableTreeNode) n).getUserObject().toString()));
        assertChildren(sortedModel, root, aaa, abc, def, zzz);
    }

    @Test
    public void testRemove()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel baseModel = new DefaultTreeModel(root);
        SortFilterTreeModel sortedModel = new SortFilterTreeModel(baseModel, Comparator.comparing(n -> ((DefaultMutableTreeNode) n).getUserObject().toString()));

        DefaultMutableTreeNode aaa = new DefaultMutableTreeNode("AAA");
        DefaultMutableTreeNode zzz = new DefaultMutableTreeNode("ZZZ");
        insertNodes(baseModel, root, aaa, zzz);
        assertChildren(sortedModel, root, aaa, zzz);

        baseModel.removeNodeFromParent(aaa);
        assertChildren(sortedModel, root, zzz);
    }

    @Test
    public void testFilter()
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultTreeModel baseModel = new DefaultTreeModel(root);
        SortFilterTreeModel sortedModel = new SortFilterTreeModel(baseModel, Comparator.comparing(n -> ((DefaultMutableTreeNode) n).getUserObject().toString()));

        DefaultMutableTreeNode aaa = new DefaultMutableTreeNode("AAA");
        DefaultMutableTreeNode def = new DefaultMutableTreeNode("DEF");
        DefaultMutableTreeNode zzz = new DefaultMutableTreeNode("ZZZ");
        insertNodes(baseModel, root, aaa, zzz);
        insertNodes(baseModel, aaa, def);

        Predicate<Object> aFilter = o -> ((DefaultMutableTreeNode) o).getUserObject().toString().contains("A");
        Predicate<Object> zFilter = o -> ((DefaultMutableTreeNode) o).getUserObject().toString().contains("Z");
        Predicate<Object> cFilter = o -> ((DefaultMutableTreeNode) o).getUserObject().toString().contains("C");

        // Adding filters

        assertThat(sortedModel.getChildCount(aaa)).isEqualTo(1);
        sortedModel.addFilter(aaa, cFilter);
        assertThat(sortedModel.getChildCount(aaa)).isEqualTo(0);

        assertThat(sortedModel.getChildCount(root)).isEqualTo(2);
        sortedModel.addFilter(aFilter);
        assertThat(sortedModel.getChildCount(root)).isEqualTo(1);
        sortedModel.addFilter(zFilter);
        assertThat(sortedModel.getChildCount(root)).isEqualTo(0);

        // Removing filters

        assertThat(sortedModel.removeFilter(aFilter)).isTrue();
        assertThat(sortedModel.getChildCount(root)).isEqualTo(1);
        sortedModel.removeFilter(zFilter);
        assertThat(sortedModel.getChildCount(root)).isEqualTo(2);
        sortedModel.removeFilter(cFilter);
        assertThat(sortedModel.getChildCount(aaa)).isEqualTo(1);
    }

    protected void insertNodes(DefaultTreeModel model, MutableTreeNode parent, MutableTreeNode... children)
    {
        for (MutableTreeNode child : children)
        {
            model.insertNodeInto(child, parent, 0);
        }
    }

    protected void assertChild(TreeModel model, Object parent, Object child, int index)
    {
        assertThat(model.getIndexOfChild(parent, child)).isEqualTo(index);
    }

    protected void assertChildren(TreeModel model, Object parent, Object... children)
    {
        assertThat(model.getChildCount(parent)).isEqualTo(children.length);
        for (int i = 0; i < children.length; i++)
        {
            assertChild(model, parent, children[i], i);
        }
    }
}