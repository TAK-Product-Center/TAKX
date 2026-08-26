package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.WWIcon;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.globe.spiderfy.IIconStack;
import gov.takx.api.globe.spiderfy.IIconStackController;
import gov.takx.api.globe.spiderfy.IconStackExpansionShape;
import gov.takx.test.AContainerTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.utils.CdiTestUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the behavior within the IIconStackController API.
 *
 * @since 2.26
 */
@SuppressWarnings("unused")
public class IconStackControllerTest extends AContainerTest
{
    private IIconStackController iconStackController;

    @BeforeEach
    public void setUp(@Mocked WorldWindow worldWindow)
    {
        iconStackController = getBean(IIconStackController.class);

        startService(IconStackController.class);

        // Need to inject this since it is initialized inside IconStackController in an event handler for an event
        // that is not fired during testing.
        CdiTestUtils.setField(iconStackController, "primaryWorldWindow", worldWindow);
    }

    @AfterEach
    public void afterMethod()
    {
        stopService(IconStackController.class);
    }

    @Test
    public void testAddIconStackStandard()
    {
        IconStackLayer iconStackLayer = CdiTestUtils.getField(iconStackController, IconStackLayer.class);
        IIconStack iconStack = createSampleIconStack();

        List<WWIcon> icons = Lists.newArrayList(iconStackLayer.getIcons());
        assertThat(icons).withFailMessage("All of the nested icons should be added to the icon layer").hasSize(6);
        assertThat(iconStack.isVisible()).withFailMessage("Icon Stack visibility was not set after being added").isTrue();
        iconStack.getChildIconStacks().forEach(nestedStack -> testIconStackVisibility(nestedStack, false));
    }

    @Test
    @DependsOn("testAddIconStackStandard")
    public void testRemoveIconStack()
    {
        IconStackLayer iconStackLayer = CdiTestUtils.getField(iconStackController, IconStackLayer.class);
        IIconStack iconStack = createSampleIconStack();
        IIconStack secondaryStack = iconStackController
                .createIconStack("", new Position(Angle.fromDegrees(0), Angle.fromDegrees(0), 0));

        iconStackController.addIconStackToGlobe(secondaryStack);
        iconStackController.removeIconStackFromGlobe(iconStack);

        List<WWIcon> icons = Lists.newArrayList(iconStackLayer.getIcons());
        assertThat(icons).withFailMessage("Only the secondary icon should be left in the icon layer").hasSize(1);
        assertThat(icons.get(0)).isEqualTo(secondaryStack);
    }

    @Test
    public void testExpandIconStack(@Mocked WorldWindow worldWindow)
    {
        IIconStack iconStack = createSampleIconStack();

        iconStack.setExpansionShape(IconStackExpansionShape.NONE);
        iconStackController.expandIconStack(iconStack, worldWindow, false);

        assertThat(iconStack.isVisible()).isTrue();
        iconStack.getChildIconStacks().forEach(childIcon -> {
            assertThat(childIcon.isVisible()).isTrue();
            childIcon.getChildIconStacks().forEach(grandChildIconStack -> testIconStackVisibility(grandChildIconStack, false));
        });
    }

    @Test
    public void testCollapseIconStack()
    {
        IIconStack iconStack = createSampleIconStack();

        iconStackController.collapseIconStack(iconStack);

        assertThat(iconStack.isVisible()).isTrue();
        iconStack.getChildIconStacks().forEach(nestedStack -> testIconStackVisibility(nestedStack, false));
    }

    private IIconStack createSampleIconStack()
    {
        IIconStack iconGrandparent = iconStackController.createIconStack("Icon Path",
                new Position(Angle.fromDegrees(32.34192071), Angle.fromDegrees(-3), 0));
        iconStackController.addIconStackToGlobe(iconGrandparent);

        IIconStack iconParent = iconStackController.createIconStack("Icon Path",
                new Position(Angle.fromDegrees(31.34192071), Angle.fromDegrees(-2), 0));
        iconGrandparent.addChildIconStack(iconParent);

        IIconStack grandparentChildIconStack = iconStackController.createIconStack("Icon Path",
                new Position(Angle.fromDegrees(37.34192071), Angle.fromDegrees(-1), 0));
        iconGrandparent.addChildIconStack(grandparentChildIconStack);
        iconGrandparent.setExpansionShape(IconStackExpansionShape.NONE);

        for (int index = 0; index < 3; index++)
        {
            IIconStack parentChildIconStack = iconStackController.createIconStack("Icon Path",
                    new Position(Angle.fromDegrees(32.34192071), Angle.fromDegrees(index), 0));
            iconParent.addChildIconStack(parentChildIconStack);
        }

        iconParent.setExpansionShape(IconStackExpansionShape.CIRCLE);

        return iconGrandparent;
    }

    /**
     * Verifies that the visibility of an icon stack and all nested icons are set correctly.
     *
     * @param iconStack The root icon to change the visibility of.
     * @param visible   True if all icons stemming from {@code iconStack} should be visible; otherwise false.
     */
    private void testIconStackVisibility(IIconStack iconStack, boolean visible)
    {
        if (iconStack != null)
        {
            assertThat(iconStack.isVisible()).withFailMessage("The icon stack's visibility does not match the " + "expected visibility:").isEqualTo(visible);

            iconStack.getChildIconStacks().forEach(nestedStack -> testIconStackVisibility(nestedStack, visible));
        }
    }
}
