package gov.takx.core.persist;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.dataservices.persist.MultiPolylineMapObject;
import gov.takx.dataservices.persist.RouteMapObject;
import gov.takx.dataservices.persist.ShapePathMapObject;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.datasim.BloodHoundSim;
import gov.takx.test.datasim.BlueJaySim;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static gov.tak.api.util.StandardAttributeNames.COT_TYPE;
import static gov.tak.api.util.StandardAttributeNames.UID;
import static gov.takx.test.datasim.BloodHoundSim.BLOODHOUND_FAMILY;
import static gov.takx.test.datasim.BloodHoundSim.BLOODHOUND_TYPE;
import static gov.takx.test.datasim.BlueJaySim.BLUEJAY_FAMILY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for MapObjectPersist
 *
 */
public class MapObjectPersistTest extends AIntegrationTest
{
    private IMapObjectManager mapObjectManager;
    private IMapObjectPersist moPersist;
    private IMapEntity defaultMapEntity;
    private IMapEntity polylineMapEntity;
    private IMapEntity routeMapEntity;

    @AfterEach
    public void deleteMapObjects()
    {
        deleteAllMapObjects();
    }

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        moPersist = getBean(IMapObjectPersist.class);

        defaultMapEntity = mapObjectManager.getOrCreateMapEntity(KeyUtils.DEFAULT_FAMILY, "test");
        polylineMapEntity = mapObjectManager.getOrCreateMapEntity(KeyUtils.TOOL_FAMILY, MapToolType.DRAWING_PATH.getShapeTypeName());
        routeMapEntity = mapObjectManager.getOrCreateMapEntity(KeyUtils.TOOL_FAMILY, IRouteMapObject.ROUTE_TYPE);
    }

    @Test
    public void test_query_all()
    {
        assertThat(moPersist.findAllMapObjects()).isEmpty(); // Should be empty to start

        for (int i = 0; i < 5; i++)
        {
            mapObjectManager.getOrCreateMapObject(MapObjectSpec.builder(defaultMapEntity, "" + i).build());
        }

        assertThat(moPersist.findAllMapObjects())
                .hasSize(5)
                .hasOnlyElementsOfType(IIconMapObject.class);
    }

    @Test
    public void test_containment()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(defaultMapEntity, "1234").build();
        final IIconMapObject child1 = (IIconMapObject) mapObjectManager.getOrCreateMapObject(spec);

        final MapObjectSpec spec2 = MapObjectSpec.builder(defaultMapEntity, "4321").build();
        final IIconMapObject child2 = (IIconMapObject) mapObjectManager.getOrCreateMapObject(spec2);

        final MapObjectSpec spec3 = MapObjectSpec.builder(defaultMapEntity, "parent").build();
        final IIconMapObject parent = (IIconMapObject) mapObjectManager.getOrCreateMapObject(spec3);
        final String parentMoKey = parent.getKey();

        parent.addMapObjectToContainer(child1);
        parent.addMapObjectToContainer(child2);

        final IPersistService ps = getBean(IPersistServiceProvider.class).getPersistService();
        ps.persistEntity(parent);
        ps.persistEntity(child1);
        ps.persistEntity(child2);

        final IIconMapObject foundParent = (IIconMapObject) moPersist.findMapObject(parentMoKey);
        assertThat(foundParent).isNotNull();

        final List<IIconMapObject> containedMapObjects = foundParent.getContainedMapObjects();
        assertThat(containedMapObjects).hasSize(2);

        assertThat(containedMapObjects).are(new Condition<>(mo -> mo.getParentContainer() == foundParent, "has correct parent"));
    }

    @Test
    public void test_route_path_and_waypoints()
    {
        GeoPointMetaData location1 = new GeoPointMetaData(new GeoPoint(1, 1));
        location1.getMetaData().setAttribute(UID, UUID.randomUUID().toString());
        GeoPointMetaData location2 = new GeoPointMetaData(new GeoPoint(2, 2));
        location2.getMetaData().setAttribute(UID, UUID.randomUUID().toString());
        GeoPointMetaData location3 = new GeoPointMetaData(new GeoPoint(3, 3));
        location3.getMetaData().setAttribute(UID, UUID.randomUUID().toString());

        List<GeoPointMetaData> controlPoints = List.of(location1, location2, location3);

        // Create a route MO with 3 control points and a waypoint at each end of the path
        MapObjectSpec routeSpec = MapObjectSpec.builder(routeMapEntity, "456").build();
        RouteMapObject routeMO = (RouteMapObject) mapObjectManager.getOrCreateMapObject(routeSpec);

        routeMO.setPoints(controlPoints);

        IRouteMapObject foundRouteMO = (IRouteMapObject) moPersist.findMapObject(routeMO.getKey());
        assertThat(foundRouteMO).isNotNull();
        assertThat(foundRouteMO.getControlPoints()).usingRecursiveFieldByFieldElementComparator().containsExactlyElementsOf(controlPoints);

        assertThat(foundRouteMO.getWaypoints()).containsOnly(location1, location3);
        assertThat(routeMO.getPointByIndex(0)).isEqualTo(location1);
        assertThat(routeMO.getPointByIndex(2)).isEqualTo(location3);
    }

    @Test
    public void test_multi_polyline()
    {
        MapObjectSpec path1Spec = MapObjectSpec.builder(polylineMapEntity, "1").build();
        var polyline1 = (ShapePathMapObject<?>) mapObjectManager.getOrCreateMapObject(path1Spec);

        MapObjectSpec path2Spec = MapObjectSpec.builder(polylineMapEntity, "2").build();
        var polyline2 = (ShapePathMapObject<?>) mapObjectManager.getOrCreateMapObject(path2Spec);

        // Create a multi-polyline with both polylines
        MapObjectSpec multiPolylineSpec = MapObjectSpec.builder(KeyUtils.COMPOUND_FAMILY, IMultiPolylineMapObject.MULTI_POLYLINE_TYPE, "main").build();
        final var multiPolylineMO = (MultiPolylineMapObject) mapObjectManager.getOrCreateMapObject(multiPolylineSpec);

        multiPolylineMO.addPolyline(polyline1);
        multiPolylineMO.addPolyline(polyline2);

        final MultiPolylineMapObject foundMultiPolylineMO = (MultiPolylineMapObject) moPersist.findMapObject(multiPolylineMO.getKey());
        assertThat(foundMultiPolylineMO).isNotNull();
        assertThat(foundMultiPolylineMO.getPolylines()).containsOnly(polyline1, polyline2);
    }

    @Test
    public void test_mo_query()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(BLOODHOUND_FAMILY, BLOODHOUND_TYPE, "1").build();
        mapObjectManager.getOrCreateMapObject(spec);

        final String key = KeyUtils.getUniqueKey(BLOODHOUND_FAMILY, BLOODHOUND_TYPE, "1");
        assertThat(moPersist.findMapObject(key)).isNotNull();
    }

    @Test
    public void test_find_structure()
    {
        final BloodHoundSim bhSim = new BloodHoundSim(false);
        final IRaptorDataStructure structure = bhSim.makeDataStructure();

        final String hash = structure.getStructure().getHashValue();
        assertThat(moPersist.findStructureByHash(hash)).isNotNull();
    }

    @Test
    public void test_query_all_structures()
    {
        final BloodHoundSim bhSim = new BloodHoundSim(false);
        final IRaptorDataStructure bhStruct = bhSim.makeDataStructure();
        final BlueJaySim bjSim = new BlueJaySim(false);
        final IRaptorDataStructure bjStruct = bjSim.makeDataStructure();

        final List<IStructure> structures = moPersist.findAllStructures();

        // BH_STATUS, BJ_STATUS, ManualLocation
        assertThat(structures).hasSize(3).hasOnlyElementsOfType(IStructure.class)
                .contains(bhStruct.getStructure(), bjStruct.getStructure());
    }

    @Test
    public void test_find_map_entity()
    {
        assertThat(moPersist.findMapEntity(defaultMapEntity.getFamily(), defaultMapEntity.getType())).isNotNull();
        assertThat(moPersist.findMapEntity(routeMapEntity.getFamily(), routeMapEntity.getType())).isNotNull();
    }

    @Test
    public void test_find_all_map_entities()
    {
        assertThat(moPersist.findAllMapEntities())
                .hasSizeGreaterThanOrEqualTo(3) // polyline, route and default
                .hasOnlyElementsOfType(IMapEntity.class);
    }

    @Test
    public void getOrCreateMapEntity_whenNotExist_createsMapEntity()
    {
        AtomicBoolean onCreateCalled = new AtomicBoolean(false);
        AtomicBoolean onExistCalled = new AtomicBoolean(false);

        // Use a unique "type" to ensure test order doesn't matter
        final IMapEntity mapEntity = moPersist.getOrCreateMapEntity("TEST", "type" + System.nanoTime(),
                me -> onCreateCalled.set(true), me -> onExistCalled.set(true));

        assertThat(onCreateCalled.get()).isTrue();
        assertThat(onExistCalled.get()).isFalse();

        // Newly created MapEntity should have exactly 1 structure, the manual location message
        assertThat(mapEntity.getDataStructures()).hasSize(1)
                .map(IStructure::getMessageType).containsExactly(DataStructureBuilder.LOCATION_MESSAGE_TYPE);
    }

    @Test
    public void getOrCreateMapEntity_whenExist_returnsExistingMapEntity()
    {
        // Use a unique "type" to ensure test order doesn't matter
        final String type = "type" + System.nanoTime();
        final IMapEntity mapEntity = moPersist.getOrCreateMapEntity("TEST", type);

        AtomicBoolean onCreateCalled = new AtomicBoolean(false);
        AtomicBoolean onExistCalled = new AtomicBoolean(false);

        final IMapEntity foundMapEntity = moPersist.getOrCreateMapEntity("TEST", type,
                me -> onCreateCalled.set(true), me -> onExistCalled.set(true));

        assertThat(onCreateCalled.get()).isFalse();
        assertThat(onExistCalled.get()).isTrue();

        assertThat(mapEntity).isSameAs(foundMapEntity);
    }

    @Test
    public void test_query_family()
    {
        for (int i = 0; i < 5; i++)
        {
            mapObjectManager.getOrCreateMapObject(MapObjectSpec.builder(BLOODHOUND_FAMILY, BLOODHOUND_TYPE, "" + i).build());
        }

        for (int i = 0; i < 3; i++)
        {
            mapObjectManager.getOrCreateMapObject(MapObjectSpec.builder(BLUEJAY_FAMILY, BLUEJAY_FAMILY, "" + i).build());
        }

        assertThat(moPersist.findMapObjectsByFamily(BLOODHOUND_FAMILY))
                .hasSize(5)
                .hasOnlyElementsOfType(IMapObject.class);

        assertThat(moPersist.findMapObjectsByFamily(BLUEJAY_FAMILY))
                .hasSize(3)
                .hasOnlyElementsOfType(IMapObject.class);

        assertThat(moPersist.findMapObjectsByFamily(KeyUtils.DEFAULT_FAMILY)).isEmpty();
    }

    @Test
    public void test_create_and_delete_mo()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(defaultMapEntity, "0").build();
        final IMapObject mo = mapObjectManager.getOrCreateMapObject(spec);

        final IMapObject foundMapObject = moPersist.findMapObject(mo.getKey());

        assertThat(foundMapObject).isNotNull();
        assertThat(foundMapObject.getKey()).isEqualTo(mo.getKey());

        mapObjectManager.deleteMapObject(mo);
        assertThat(moPersist.findMapObject(mo.getKey())).isNull(); // Should be gone now
    }

    @Test
    public void test_attribute_set_persist()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(defaultMapEntity, "0").build();
        final IMapObject mo = mapObjectManager.getOrCreateMapObject(spec);

        final AttributeSet as = mo.getAttributeSet();

        final int anInt = 42;
        final String aString = "I'm a string";
        final byte[] aBlob = {1, 2, 3, 4};

        as.setAttribute("int", anInt);
        as.setAttribute("string", aString);
        as.setAttribute("blob", aBlob);

        final IPersistService ps = getBean(IPersistServiceProvider.class).getPersistService();

        ps.persistEntity(mo);

        // Go direct to the DB to avoid the in-memory cache
        final IMapObject foundMapObject = ps.queryUnique("from MapObject where id = " + mo.getId());

        assertThat(foundMapObject).isNotNull();
        assertThat(foundMapObject.getKey()).isEqualTo(mo.getKey());

        final AttributeSet foundAttributeSet = foundMapObject.getAttributeSet();

        assertThat(foundAttributeSet.getIntAttribute("int")).isEqualTo(anInt);
        assertThat(foundAttributeSet.getStringAttribute("string")).isEqualTo(aString);
        assertThat(foundAttributeSet.getBinaryAttribute("blob")).isEqualTo(aBlob);
    }

    @Test
    public void test_uid()
    {
        // Test default is a valid UUID
        MapObjectSpec spec = MapObjectSpec.builder(defaultMapEntity, "0").build();
        IMapObject mo = mapObjectManager.getOrCreateMapObject(spec);

        assertThat(UUID.fromString(mo.getUid())).isNotNull();
        assertThat(mo.getAttributeSet().getStringAttribute(UID)).isEqualTo(mo.getUid());

        // Test specified UID
        final String uid = "I am groot";
        spec = MapObjectSpec.builder(defaultMapEntity, "1").uid(uid).build();
        mo = mapObjectManager.getOrCreateMapObject(spec);

        assertThat(mo.getUid()).isEqualTo(uid);
        assertThat(mo.getAttributeSet().getStringAttribute(UID)).isEqualTo(mo.getUid());

        // Ensure we can find it by UID
        assertThat(moPersist.findMapObjectByUid(uid)).isNotNull();

        mapObjectManager.deleteMapObject(mo);
        assertThat(moPersist.findMapObjectByUid(uid)).isNull(); // Should be gone now
    }

    @Test
    public void test_cot_type()
    {
        // Test default is empty type
        MapObjectSpec spec = MapObjectSpec.builder(defaultMapEntity, "0").build();
        IMapObject mo = mapObjectManager.getOrCreateMapObject(spec);
        assertThat(mo.getCotType()).isEqualTo(CotTypes.EMPTY);
        assertThat(mo.getAttributeSet().getStringAttribute(COT_TYPE)).isEqualTo(mo.getCotType());

        // Test specified type
        final String type = "a-f-g";
        spec = MapObjectSpec.builder(defaultMapEntity, "1").cotType(type).build();
        mo = mapObjectManager.getOrCreateMapObject(spec);
        assertThat(mo.getCotType()).isEqualTo(type);
        assertThat(mo.getAttributeSet().getStringAttribute(COT_TYPE)).isEqualTo(mo.getCotType());

        // Validate property changes
        final AtomicBoolean fired = new AtomicBoolean(false);
        mo.addPropertyChangeListener(IMapObject.COT_TYPE, pce -> fired.set(true));

        mo.setCotType(type); // No change, no PCE
        assertThat(fired.get()).isFalse();

        mo.setCotType("a-h-g"); // Change
        assertThat(fired.get()).isTrue();
    }

    /**
     * @since 3.2
     */
    @Test
    public void test_cot_event() throws IOException
    {
        // Test default is null
        MapObjectSpec spec = MapObjectSpec.builder(defaultMapEntity, "0").build();
        IMapObject mo = mapObjectManager.getOrCreateMapObject(spec);
        assertThat(mo.getLatestCotEvent()).isNull();

        // Test specified event
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/marker.cot"));
        spec = MapObjectSpec.builder(defaultMapEntity, "1").cotEvent(event).build();
        mo = mapObjectManager.getOrCreateMapObject(spec);
        assertThat(mo.getLatestCotEvent()).isEqualTo(event);

        // Ensure we can find the CoT event
        IMapObject foundMO = moPersist.findMapObject(mo.getKey());
        assertThat(foundMO).isNotNull();
        assertThat(foundMO.getLatestCotEvent()).isEqualTo(event);
    }

    /**
     * Regression test for TAKX-1489, shape property changes were not being persisted.
     */
    @Test
    public void shapeProperties_ArePersisted_WhenForceUpdateCalled()
    {
        final IPersistService ps = getBean(IPersistServiceProvider.class).getPersistService();

        final MapObjectSpec spec = MapObjectSpec.builder(polylineMapEntity, "1")
                .initializer(mo -> ((ShapePathMapObject<?>) mo).setLineColor(Color.RED))
                .build();
        final var polyline = (ShapePathMapObject<?>) mapObjectManager.getOrCreateMapObject(spec);

        polyline.setLineColor(Color.BLUE); // This will be automatically persisted

        // Need to pull it directly from persistence (not from the MapObjectPersist cache)
        final ShapePathMapObject<?> polylineFromPersistence = ps.queryUnique("from ShapePathMapObject where id=" + polyline.getId());

        assertThat(polylineFromPersistence.getLineColor()).isEqualTo(Color.BLUE);
    }
}
