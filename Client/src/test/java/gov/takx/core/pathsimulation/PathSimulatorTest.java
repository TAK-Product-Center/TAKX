package gov.takx.core.pathsimulation;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.pathsimulation.IPathSimulationEventListener;
import gov.takx.api.pathsimulation.IPathSimulationService;
import gov.takx.api.pathsimulation.IPathSimulator;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;

/**
 */
@TestMethodOrder(DependsOnOrdering.class)
public class PathSimulatorTest extends AIntegrationTest
{
    private static final String UNIT_ID = "Unit 1";
    private static final double MAX_DISTANCE_BETWEEN_POINTS = 500;

    private IPathSimulator bh1PathSimulator;
    private List<ILocation> path;
    private IMapObject mapObject;

    private IMapObjectManager mapObjectManager;
    private IPathSimulationService pathSimulationService;
    private List<ILocation> locations;
    private IPrePersistRaptorDataMessage providedRDM;

    public PathSimulatorTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    @Test
    public void bringPluginOnline()
    {
        mapObjectManager = getBean(IMapObjectManager.class);

        pathSimulationService = getBean(IPathSimulationService.class);
        locations = createLocations();
        createMOAndSimulator();
    }

    /**
     * Convenience method
     */
    private void createMOAndSimulator()
    {
        final MapObjectSpec spec = MapObjectSpec.builder(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID)
                .location(new RaptorLocation(0, 0)).build();
        mapObject = mapObjectManager.getOrCreateMapObject(spec);
        bh1PathSimulator = pathSimulationService.createSimulator(locations, mapObject, MAX_DISTANCE_BETWEEN_POINTS);
        path = bh1PathSimulator.getSimulationPoints();
    }

    private List<ILocation> createLocations()
    {
        List<ILocation> path = new ArrayList<>();
        // LA Area
        path.add(new RaptorLocation(33.832779, -118.324342));
        path.add(new RaptorLocation(33.823653, -118.204865));
        path.add(new RaptorLocation(33.806538, -118.122468));
        path.add(new RaptorLocation(33.69121, -117.915101));
        path.add(new RaptorLocation(33.568861, -117.687135));
        return path;
    }

    /**
     * Makes sure that playback indices are correct
     */
    @Test
    @DependsOn("bringPluginOnline")
    public void indexTest()
    {
        createMOAndSimulator();

        // Test proper index values
        bh1PathSimulator.stop();
        assertThat(bh1PathSimulator.getCurrentIndex()).isEqualTo(-1);

        // Jump to the last location
        bh1PathSimulator.jumpToSimulationPoint(path.size() - 1);
        assertThat(bh1PathSimulator.getCurrentIndex()).isEqualTo(path.size() - 1);

        // Jump to the first location
        bh1PathSimulator.jumpToSimulationPoint(0);
        assertThat(bh1PathSimulator.getCurrentIndex()).isEqualTo(0);

        // Jump to the middle somewhere
        int simulationPointIndex = path.size() / 2;
        bh1PathSimulator.jumpToSimulationPoint(simulationPointIndex);
        assertThat(bh1PathSimulator.getCurrentIndex()).isEqualTo(simulationPointIndex);
    }

    /**
     * Tests the simulator play state
     */
    @Test
    @DependsOn("bringPluginOnline")
    public void stateTest()
    {
        createMOAndSimulator();
        // Make sure its not running on startup
        assertThat(bh1PathSimulator.isSimulationRunning()).isFalse();
        bh1PathSimulator.play();
        assertThat(bh1PathSimulator.isSimulationRunning()).isTrue();
        bh1PathSimulator.stop();
        assertThat(bh1PathSimulator.isSimulationRunning()).isFalse();
        bh1PathSimulator.play();
        assertThat(bh1PathSimulator.isSimulationRunning()).isTrue();
        bh1PathSimulator.pause();
        assertThat(bh1PathSimulator.isSimulationRunning()).isFalse();
        bh1PathSimulator.play();
        bh1PathSimulator.stepForward();
        assertThat(bh1PathSimulator.isSimulationRunning()).isFalse();
        bh1PathSimulator.play();
        bh1PathSimulator.stepBack();
        assertThat(bh1PathSimulator.isSimulationRunning()).isFalse();
    }

    /**
     * Makes sure that bad indices throw exceptions
     */
    @Test
    @DependsOn("bringPluginOnline")
    public void badIndices()
    {
        createMOAndSimulator();
        bh1PathSimulator.stepForward();
        bh1PathSimulator.stepForward();

        int index = bh1PathSimulator.getCurrentIndex();

        // Jump outside of our bounds
        assertThatIndexOutOfBoundsException()
                .isThrownBy(() -> bh1PathSimulator.jumpToSimulationPoint(bh1PathSimulator.getSimulationPoints().size()));
        // Should be the same index as the last test
        assertThat(bh1PathSimulator.getCurrentIndex()).isEqualTo(index);
    }

    /**
     * Makes sure that locations provided by the simulation listener are used
     */
    @Test
    @DependsOn("bringPluginOnline")
    public void providedLocationTest()
    {
        createMOAndSimulator();
        bh1PathSimulator.stop();
        bh1PathSimulator.stepForward();
        assertThat(mapObject.hasHeading()).isFalse();
        MyIPathSimulationEventListener simulationEventListener = new MyIPathSimulationEventListener();
        bh1PathSimulator.addEventListener(simulationEventListener);
        providedRDM = mapObject.createLocationDataMessage();
        providedRDM.setHeading(3);
        bh1PathSimulator.stepForward();
        // Make sure that the plugin provided RDM with a heading is used
        assertThat(mapObject.hasHeading()).isTrue();
        bh1PathSimulator.removeEventListener(simulationEventListener);
    }

    /**
     * The next three tests make sure that valid params are passed to the simulation service
     */
    @Test
    @DependsOn("bringPluginOnline")
    public void nullMOTest()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> pathSimulationService.createSimulator(createLocations(), null, MAX_DISTANCE_BETWEEN_POINTS));
    }

    @Test
    @DependsOn("bringPluginOnline")
    public void nullPathTest()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> pathSimulationService.createSimulator(null, mapObject, MAX_DISTANCE_BETWEEN_POINTS));
    }

    @Test
    @DependsOn("bringPluginOnline")
    public void emptyPathTest()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> pathSimulationService.createSimulator(new ArrayList<>(), mapObject, MAX_DISTANCE_BETWEEN_POINTS));
    }

// TODO: need to figure out how catch an exception triggered on the play timer thread
//  @Test
//  @DependsOn("bringPluginOnline")
//  public void deletedMapObjectTestPlay()
//  {
//      createMOAndSimulator();
//      bh1PathSimulator.stop();
//      mapObjectManager.deleteMapObject(mapObject);
//      assertThatIllegalStateException().isThrownBy(() -> bh1PathSimulator.play());
//  }

    @Test
    @DependsOn("bringPluginOnline")
    public void deletedMapObjectTestStepForward()
    {
        createMOAndSimulator();
        bh1PathSimulator.stop();
        mapObjectManager.deleteMapObject(mapObject);
        assertThatIllegalStateException()
                .isThrownBy(() -> bh1PathSimulator.stepForward());
    }

    @Test
    @DependsOn("bringPluginOnline")
    public void deletedMapObjectTestStepBack()
    {
        createMOAndSimulator();
        bh1PathSimulator.stop();
        bh1PathSimulator.stepForward();
        bh1PathSimulator.stepForward();
        mapObjectManager.deleteMapObject(mapObject);
        assertThatIllegalStateException()
                .isThrownBy(() -> bh1PathSimulator.stepBack());
    }

    private class MyIPathSimulationEventListener implements IPathSimulationEventListener
    {
        @Override
        public IPrePersistRaptorDataMessage prePointTraversal(ILocation pointToTraverse, int simulationPointIndex)
        {
            assertThat(bh1PathSimulator.getCurrentIndex()).isEqualTo(simulationPointIndex);
            assertThat(pointToTraverse.getLatitude()).isEqualTo(path.get(simulationPointIndex).getLatitude());
            assertThat(pointToTraverse.getLongitude()).isEqualTo(path.get(simulationPointIndex).getLongitude());
            return providedRDM;
        }

        @Override
        public void pointTraversed(ILocation traversedPoint, int simulationPointIndex)
        {
            assertThat(bh1PathSimulator.getCurrentIndex()).isEqualTo(simulationPointIndex);
            assertThat(traversedPoint.getLatitude()).isEqualTo(path.get(simulationPointIndex).getLatitude());
            assertThat(traversedPoint.getLongitude()).isEqualTo(path.get(simulationPointIndex).getLongitude());
        }
    }
}
