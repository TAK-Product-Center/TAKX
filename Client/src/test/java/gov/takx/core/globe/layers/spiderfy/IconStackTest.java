package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.globe.spiderfy.IIconStack;
import gov.takx.api.globe.spiderfy.IIconStackController;
import gov.takx.api.globe.spiderfy.IconStackExpansionShape;
import gov.takx.api.globe.spiderfy.IconStackPropertyConstants;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verify the functionality of IconStacks.
 *
 * @since 2.26
 */
@SuppressWarnings("unused")
public class IconStackTest extends AContainerTest //extends IconStackPositionCalculator
{
    private IIconStackController iconStackController;

    @BeforeEach
    public void setUp(@Mocked WorldWindow worldWindow)
    {
        iconStackController = getBean(IIconStackController.class);

        // Need to inject this since it is initialized inside IconStackController in an event handler for an event
        // that is not fired during testing.
        CdiTestUtils.setField(iconStackController, "primaryWorldWindow", worldWindow);

        startService(IconStackController.class);
    }

    @AfterEach
    public void stopService()
    {
        stopService(IconStackController.class);
    }

    @Test
    public void setParentTest()
    {
        IIconStack iconStack = createSampleIconStack();
        IIconStack parentIconStack =
                iconStackController.createIconStack("", new Position(Angle.NEG90, Angle.NEG90, 0));
        IIconStack secondParentIconStack =
                iconStackController.createIconStack("", new Position(Angle.NEG90, Angle.NEG90, 0));

        assertThat(parentIconStack.getChildIconStacks().anyMatch(compareTo -> compareTo.equals(iconStack))).isFalse();
        iconStack.setParentIconStack(parentIconStack);
        assertThat(parentIconStack.getChildIconStacks().anyMatch(compareTo -> compareTo.equals(iconStack))).isTrue();
        assertThat(iconStack.getParentIconStack()).isEqualTo(parentIconStack);

        iconStack.setParentIconStack(secondParentIconStack);
        assertThat(parentIconStack.getChildIconStacks().anyMatch(compareTo -> compareTo.equals(iconStack))).isFalse();
        assertThat(secondParentIconStack.getChildIconStacks().anyMatch(compareTo -> compareTo.equals(iconStack))).isTrue();
        assertThat(iconStack.getParentIconStack()).isEqualTo(secondParentIconStack);
    }

    @Test
    public void addRemoveChildIconStackTest()
    {
        IIconStack iconStack = createSampleIconStack();
        IIconStack parentIconStack =
                iconStackController.createIconStack("", new Position(Angle.NEG90, Angle.NEG90, 0));
        IIconStack secondParentIconStack =
                iconStackController.createIconStack("", new Position(Angle.NEG90, Angle.NEG90, 0));

        assertThat(parentIconStack.getChildIconStacks().anyMatch(compareTo -> compareTo.equals(iconStack))).isFalse();
        parentIconStack.addChildIconStack(iconStack);
        assertThat(parentIconStack.getChildIconStacks().anyMatch(compareTo -> compareTo.equals(iconStack))).isTrue();
        assertThat(iconStack.getParentIconStack()).isEqualTo(parentIconStack);

        secondParentIconStack.addChildIconStack(iconStack);
        assertThat(parentIconStack.getChildIconStacks().anyMatch(compareTo -> compareTo.equals(iconStack))).isFalse();
        assertThat(secondParentIconStack.getChildIconStacks().anyMatch(compareTo -> compareTo.equals(iconStack))).isTrue();
        assertThat(iconStack.getParentIconStack()).isEqualTo(secondParentIconStack);
    }

    @Test
    public void setAllPositions()
    {
        IIconStack iconStack = createSampleIconStack();

        AtomicBoolean propertyChangeEventFired = new AtomicBoolean(false);

        iconStack.addPropertyChangeListener(event -> {
            if (IconStackPropertyConstants.ALL_POSITIONS.equals(event.getPropertyName()))
            {
                propertyChangeEventFired.set(true);
            }
        });

        iconStack.setAllPositions(new Position(Angle.NEG90, Angle.NEG180, 0));
        assertThat(propertyChangeEventFired.get()).withFailMessage("Failed to fire all positions changed property event").isTrue();
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
}
