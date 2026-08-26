package gov.tak.core.comms.cot;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.CotEventExporter;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.dataservices.persist.AShapeMapObject;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.dataservices.persist.ShapeCircleMapObject;
import gov.takx.dataservices.persist.ShapeRectangleMapObject;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Validate {@link CotExporterManager} functionality.
 *
 * @since 3.0
 */
public class CotExporterManagerTest extends AContainerTest
{
    private final ShapeCircleMapObject circleMapObject = new ShapeCircleMapObject();

    private final CotExporterManager exporterManager;

    public CotExporterManagerTest()
    {
        exporterManager = getBean(CotExporterManager.class);
        CdiTestUtils.setField(circleMapObject, "attributeSet", new AttributeSet());
    }

    @BeforeEach
    public void startClean()
    {
        stopService(CotExporterManager.class);
    }

    @AfterEach
    public void stopClean()
    {
        stopService(CotExporterManager.class);
    }

    @Test
    public void test_findExporter_for_specific_type()
    {
        final TestExporter exporter = new TestExporter(ShapeRectangleMapObject.class);
        exporterManager.registerExporter(exporter);

        final CotEventExporter foundExporter = exporterManager.findExporter(ShapeRectangleMapObject.class);

        assertThat(foundExporter).isSameAs(exporter);
    }

    @Test
    public void test_findExporter_returns_null_for_unregistered_type()
    {
        // Test when none registered
        assertThat(exporterManager.findExporter(MapObject.class)).isNull();

        // Test with non-matching type
        exporterManager.registerExporter(new TestExporter(ShapeRectangleMapObject.class));

        assertThat(exporterManager.findExporter(MapObject.class)).isNull();
    }

    @Test
    public void test_findExporter_returns_registered_super_type()
    {
        final TestExporter exporter = new TestExporter(AShapeMapObject.class);
        exporterManager.registerExporter(exporter);

        assertThat(exporterManager.findExporter(ShapeRectangleMapObject.class)).isSameAs(exporter);
        assertThat(exporterManager.findExporter(ShapeCircleMapObject.class)).isSameAs(exporter);
    }

    @Test
    public void test_findExporter_returns_most_specific_type_registered()
    {
        final TestExporter exporter1 = new TestExporter(MapObject.class);
        final TestExporter exporter2 = new TestExporter(AShapeMapObject.class);
        final TestExporter exporter3 = new TestExporter(ShapeCircleMapObject.class);

        exporterManager.registerExporter(exporter1);
        exporterManager.registerExporter(exporter2);
        exporterManager.registerExporter(exporter3);

        assertThat(exporterManager.findExporter(MarkerMapObject.class)).isSameAs(exporter1);
        assertThat(exporterManager.findExporter(ShapeRectangleMapObject.class)).isSameAs(exporter2);
        assertThat(exporterManager.findExporter(ShapeCircleMapObject.class)).isSameAs(exporter3);
    }

    @SuppressLogging({CotExporterManager.class})
    @Test
    public void test_export_returns_null_when_no_exporter_found()
    {
        final TestExporter exporter = new TestExporter(ShapeRectangleMapObject.class);
        exporterManager.registerExporter(exporter);

        final CotEvent event = exporterManager.export(circleMapObject);

        assertThat(event).isNull();
        assertThat(exporter.called).isFalse();
    }

    @Test
    public void test_exporter_called_when_type_registered()
    {
        final TestExporter exporter = new TestExporter(ShapeCircleMapObject.class);
        exporterManager.registerExporter(exporter);

        final CotEvent event = exporterManager.export(circleMapObject);

        assertThat(event).isNotNull();
        assertThat(exporter.called).isTrue();
    }

    @Test
    public void test_exporter_called_when_super_type_registered()
    {
        final TestExporter exporter = new TestExporter(AShapeMapObject.class);
        exporterManager.registerExporter(exporter);

        final CotEvent event = exporterManager.export(circleMapObject);

        assertThat(event).isNotNull();
        assertThat(exporter.called).isTrue();
    }

    @SuppressLogging(CotExporterManager.class)
    @Test
    public void test_unregistered_exporter_is_not_called()
    {
        final TestExporter exporter = new TestExporter(ShapeCircleMapObject.class);

        exporterManager.registerExporter(exporter);
        exporterManager.unregisterExporter(exporter);

        final CotEvent event = exporterManager.export(circleMapObject);

        assertThat(event).isNull();
        assertThat(exporter.called).isFalse();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test_register_null_exporter_throws()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> exporterManager.registerExporter(null));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test_unregister_null_exporter_throws()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> exporterManager.unregisterExporter(null));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test_export_null_map_object_throws()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> exporterManager.export(null));
    }

    /**
     * Test exporter to help validate execution.
     */
    private static class TestExporter implements CotEventExporter
    {
        private final Class<? extends MapObject> type;

        boolean called = false;

        protected TestExporter(Class<? extends MapObject> type)
        {
            this.type = type;
        }

        @Override
        public Class<? extends MapObject> getType()
        {
            return type;
        }

        @Nullable
        @Override
        public CotEvent export(@Nonnull IMapObject mapObject)
        {
            called = true;
            return new CotEvent();
        }
    }
}
