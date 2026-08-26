package gov.takx.core.view.gui.maptools;

import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IMutableRangeFan;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.shapes.ShapeType;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link RangeFanEditorPanel}.
 *
 * @since 5.1
 */
public class RangeFanEditorPanelTest extends AIntegrationTest
{
    private static final String UNIT_ID = "1";
    private static final String KEY = KeyUtils.getUniqueKey(KeyUtils.TOOL_FAMILY, ShapeType.RANGE_FAN.getShapeType(), UNIT_ID);

    @Mocked
    private IMutableRangeFan rangeFan;

    @BeforeAll
    public void beforeAll()
    {
        final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
        final MapObjectSpec spec =
                MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, ShapeType.RANGE_FAN.getShapeType(), UNIT_ID)
                        .build();

        // Create a MO so we don't NPEs when the panel requests the GMO from the primary globe
        mapObjectManager.getOrCreateMapObject(spec);
    }

    @BeforeEach
    public void beforeEach()
    {
        when(rangeFan.getKey()).thenReturn(KEY);
    }

    /**
     * When a user sets custom range ring ranges, separate events are fired for the changes to the range distances and
     * the changes to the range labels. This tests makes sure the panel handles the split update correctly.
     */
    @Test
    public void propertyChange_whenDistanceAndLabelSizesDifferent_doesNotThrowException()
    {
        final RangeFanEditorPanel rangeFanEditorPanel = new RangeFanEditorPanel(rangeFan);

        // Make sure the two arrays have unequal lengths
        when(rangeFan.getRangeDistances()).thenReturn(List.of(1d, 2d)); // range list length = 2
        when(rangeFan.getRangeLabels()).thenReturn(List.of("one")); // label list length = 1

        final PropertyChangeEvent pceRanges = new PropertyChangeEvent(this, ShapeProperties.RANGE_DISTANCES,
                "unused", "unused");

        // The property change handling happens on the EDT. SwingTestUtils.invokeAndWaitOnEDT() will ensure the call
        // completes before further calls are made and that exceptions are not consumed by the EDT's exception handler
        assertThatCode(() -> SwingTestUtils.invokeAndWaitOnEDT(() -> rangeFanEditorPanel.propertyChange(pceRanges)))
                .withFailMessage("Range distances PCE not handled correctly")
                .doesNotThrowAnyException();

        when(rangeFan.getRangeLabels()).thenReturn(List.of("one", "two")); // label list length = 2

        final PropertyChangeEvent pceLabels = new PropertyChangeEvent(this, ShapeProperties.RANGE_LABELS,
                "unused", "unused");

        assertThatCode(() -> SwingTestUtils.invokeAndWaitOnEDT(() -> rangeFanEditorPanel.propertyChange(pceLabels)))
                .withFailMessage("Range labels PCE not handled correctly")
                .doesNotThrowAnyException();
    }
}
