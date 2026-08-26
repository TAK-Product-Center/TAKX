package gov.tak.platform.cot.importer;

import com.google.common.io.Resources;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.core.shapes.ShapeService;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base for shape importer tests.
 *
 * @since 3.0
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class AShapeImporterTest extends AIntegrationTest
{
    protected final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
    protected final CotImporterManager importerManager = getBean(CotImporterManager.class);
    protected final ShapeServiceFactory shapeServiceFactory = getBean(ShapeServiceFactory.class);

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    /**
     * The KML style details are obsolete, but still supported. If the event has the newer strokeColor, strokeWeight,
     * and fillColor, they should override the KML Style.
     * <p>
     * Modify the XML for a given CoT message to include the overrides, and produce a CoT event from that.
     * Import that event and validate that the override values are set on the resulting shape.
     *
     * @param resourcePath Path to CoT message to modify
     */
    protected void checkEventWithStyleOverride(String resourcePath, String shapeUid) throws IOException
    {
        final URL url = Objects.requireNonNull(getClass().getResource(resourcePath));
        final String eventXml = Resources.toString(url, StandardCharsets.UTF_8);

        final String override = """
                        <strokeColor value="255"/>
                        <strokeWeight value="5.0"/>
                        <fillColor value="-128"/>
                    </detail>\
                """;

        final String newEventXml = eventXml.replaceFirst("</detail>", override);
        final CotEvent event = CotEvent.parse(newEventXml);

        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final IMutableShape shape = coreShapeService.getShape(shapeUid);

        assertThat(shape).isNotNull();
        assertThat(shape.getLineColor()).isEqualTo(new Color(255, false)); // No alpha for line color
        assertThat(shape.getLineWidth()).isEqualTo(5.0);
        assertThat(shape.getFillColor()).isEqualTo(new Color(-128, true));
    }

    /**
     * Create a MO to be used as a given type of anchor.
     */
    protected IMapObject createAnchorMapObject(@Nonnull String type, @Nonnull ILocation location)
    {
        final MapObjectSpec spec = MapObjectSpec.builder("test", "test_" + type, "1")
                .location(location)
                .uid(type + "Anchor")
                .build();
        return mapObjectManager.getOrCreateMapObject(spec);
    }

    @Nonnull
    protected CotEvent getCotEvent(@Nonnull String resourcePath) throws IOException
    {
        final URL url = Objects.requireNonNull(getClass().getResource(resourcePath));
        return CotEvent.parse(Resources.toString(url, StandardCharsets.UTF_8));
    }
}
