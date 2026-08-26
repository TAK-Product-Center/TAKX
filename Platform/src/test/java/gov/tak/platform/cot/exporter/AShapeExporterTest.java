package gov.tak.platform.cot.exporter;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.AfterEach;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_CALLSIGN;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_VALUE;
import static gov.tak.api.cot.detail.DetailConstants.CONTACT;
import static gov.tak.api.cot.detail.DetailConstants.FILL_COLOR;
import static gov.tak.api.cot.detail.DetailConstants.STROKE_COLOR;
import static gov.tak.api.cot.detail.DetailConstants.STROKE_WEIGHT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base for shape exporter tests.
 *
 * @since 3.0
 */
public class AShapeExporterTest extends AIntegrationTest
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
     * Determine if the shape details match the expected values
     */
    protected void checkCotDetails(CotEvent event, IShapeMapObject mapObject)
    {
        assertThat(new GeoPoint(event.getPoint())).isEqualTo(mapObject.getLocation());

        final CotDetail detail = event.getDetail();
        assertThat(detail).isNotNull();

        // Validate callsign is the shape title
        final CotDetail contact = detail.getFirstChildByName(CONTACT);
        assertThat(contact).isNotNull();
        assertThat(contact.getAttribute(ATTR_CALLSIGN)).isEqualTo(mapObject.getTitle());

        // Verify stroke/fill style
        assertThat(detail.getFirstChildByName(FILL_COLOR).getAttribute(ATTR_VALUE))
                .isEqualTo(Integer.toString(mapObject.getFillColor().getRGB()));

        assertThat(detail.getFirstChildByName(STROKE_COLOR).getAttribute(ATTR_VALUE))
                .isEqualTo(Integer.toString(mapObject.getLineColor().getRGB()));

        assertThat(detail.getFirstChildByName(STROKE_WEIGHT).getAttribute(ATTR_VALUE))
                .isEqualTo(String.valueOf(mapObject.getLineWidth()));
    }
}
