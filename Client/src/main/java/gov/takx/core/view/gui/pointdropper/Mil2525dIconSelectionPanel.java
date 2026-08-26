package gov.takx.core.view.gui.pointdropper;

import gov.takx.api.milsym.MilStd2525Variant;
import gov.takx.core.view.gui.pointdropper.PointDropperPanel.PointDropperSelection;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Consumer;

/**
 * Icon selection panel for MIL-STD-2525D icons.
 *
 * @since 5.5
 */
public class Mil2525dIconSelectionPanel extends AMil2525IconSelectionPanel
{
    private MilStdUnit defaultUnit;

    /**
     * New selection panel.
     */
    Mil2525dIconSelectionPanel(@Nonnull Consumer<PointDropperSelection> selectionConsumer)
    {
        super(MilStd2525Variant.DELTA, selectionConsumer);
    }

    @Nonnull
    @Override
    protected MilStdUnit provideDefaultUnit()
    {
        if (defaultUnit == null) defaultUnit = new MilStdUnit("G", "Ground");
        return defaultUnit;
    }

    @Nonnull
    @Override
    protected List<MilStdUnit> provideUnits()
    {
        return List.of(
                provideDefaultUnit(),
                new MilStdUnit("A", "Aircraft"),
                new MilStdUnit("P", "Spacecraft"),
                new MilStdUnit("S", "Ship"),
                new MilStdUnit("U", "Subterranean"),
                new MilStdUnit("G-I", "Building"),
                new MilStdUnit("G-U-C-D", "Air Defense"),
                new MilStdUnit("G-E-W-A", "Anti-Aircraft Artillery"),
                new MilStdUnit("G-U-C-F", "Artillery"),
                new MilStdUnit("G-U-C-D-M", "Surface to Air Missile"),
                new MilStdUnit("G-E-X-M", "Landmine"),
                new MilStdUnit("G-E-V-A-T", "Tank"),
                new MilStdUnit("G-U-C-I", "Troop"),
                new MilStdUnit("G-E-V", "Vehicle"));
    }
}
