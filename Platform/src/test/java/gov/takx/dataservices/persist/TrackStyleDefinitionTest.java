package gov.takx.dataservices.persist;

import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.ISmartTrackPersist;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ISize;
import gov.takx.api.tracks.ITrackStyle;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentListener;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentManager;
import gov.takx.platform.tracks.BucketMode;
import gov.takx.platform.tracks.Groupability;
import gov.takx.platform.tracks.TrackShape;
import gov.takx.platform.tracks.TrackStyleDefinitionBuilder;
import gov.takx.platform.tracks.VerticalAlignment;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validate that RDM-level TSD operations are working properly.
 *
 */
public class TrackStyleDefinitionTest extends AIntegrationTest
{
    private static final String UNIT_ID_1 = "1";
    private static final String UNIT_ID_2 = "2";
    private static final String UNIT_ID_3 = "3";
    private static final String TEST_FAMILY = "gov.takx.test";
    private static final String TSD = "tsd";

    private IExtendedSmartTrackManager stm;
    private ISmartTrackPersist stp;
    private IPrePersistRaptorDataMessage rdm;
    private final Map<Integer, List<Integer>> mo2rdmId = new HashMap<>();
    private final Map<Integer, IRaptorDataMessage> rdmsById = new HashMap<>();
    private ISmartTrackAssignmentManager smartTrackAssignmentManager;

    /**
     * Construct with a registered plugin so we can use it to send messages.
     */
    public TrackStyleDefinitionTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);

        mo2rdmId.put(1, new ArrayList<>());
        mo2rdmId.put(2, new ArrayList<>());
        mo2rdmId.put(3, new ArrayList<>());
    }

    @Override
    protected void postProjectOpen()
    {
        TestDevicePlugin plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);

        smartTrackAssignmentManager = getBean(ISmartTrackAssignmentManager.class);

        stm = getBean(IExtendedSmartTrackManager.class);
        stp = getBean(ISmartTrackPersist.class);

        final IDeviceGateway gateway = getBean(IDeviceGateway.class);

        IGatewayController gc = gateway.getController(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_1);

        rdm = plugin.generateMessage(UNIT_ID_1);
        gc.processParserMessage(rdm);

        mo2rdmId.get(rdm.getMapObject().getId()).add(rdm.getId()); // Add it to the map/list
        rdmsById.put(rdm.getId(), rdm);

        // Now create messages for 2 additional devices. 5 messages each

        IGatewayController gc2 = gateway.getController(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_2);
        IGatewayController gc3 = gateway.getController(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_3);

        for (int i = 0; i < 5; i++)
        {
            IPrePersistRaptorDataMessage rdm2 = plugin.generateMessage(UNIT_ID_2);
            gc2.processParserMessage(rdm2);
            mo2rdmId.get(rdm2.getMapObject().getId()).add(rdm2.getId()); // Add it to the map/list
            rdmsById.put(rdm2.getId(), rdm2);

            IPrePersistRaptorDataMessage rdm3 = plugin.generateMessage(UNIT_ID_3);
            gc3.processParserMessage(rdm3);
            mo2rdmId.get(rdm3.getMapObject().getId()).add(rdm3.getId()); // Add it to the map/list
            rdmsById.put(rdm3.getId(), rdm3);
        }
    }

    @Test
    public void testTSDAssignments()
    {
        ITrackStyleDefinition tsd = rdm.getTrackStyleDefinition();
        assertThat(tsd).withFailMessage("The TrackStyle should be null at the start").isNull();

        tsd = new TrackStyleDefinition(TEST_FAMILY, TSD, "0", false);
        stm.registerTrackStyle(tsd);

        Integer tsdId = tsd.getId();
        rdm.setTrackStyleDefinition(tsd);

        // Ensure that the proper TSD id was installed
        assertThat(rdm.getTrackStyleDefinitionId()).withFailMessage("RDM's TSD Id must equal TSDs Id").isEqualTo(tsdId);
        assertThat(rdm.getOverrideTrackStyleDefinitionId()).withFailMessage("Shouldn't have an override TSD").isNull();

        // Ensure we can get the TSD back
        tsd = rdm.getTrackStyleDefinition();
        assertThat(tsd).withFailMessage("The TrackStyle could not be retrieved").isNotNull();

        // Now set an override TSD and ensure it is recorded properly
        ITrackStyleDefinition overrideTSD = new TrackStyleDefinition(TEST_FAMILY, TSD, "1", false);
        stm.registerTrackStyle(overrideTSD);

        rdm.setOverrideTrackStyleDefinition(overrideTSD);
        assertThat(rdm.getOverrideTrackStyleDefinition()).withFailMessage("The Override TSD could not be retrieved").isNotNull();

        // Assure that the override is now the active TSD
        ITrackStyleDefinition activeTSD = stp.getActiveTSDforRDM(rdm.getId());
        assertThat(activeTSD).withFailMessage("Active is override").isEqualTo(overrideTSD);

        // Delete the override TSD and ensure the permanent one is still in place
        rdm.resetOverrideTrackStyleDefinition();
        assertThat(rdm.getOverrideTrackStyleDefinition()).withFailMessage("The override TSD should be null again").isNull();
        assertThat(rdm.getTrackStyleDefinition()).withFailMessage("The original TSD should still be there").isNotNull();

        // Validate that the original TSD is now active again
        activeTSD = stp.getActiveTSDforRDM(rdm.getId());
        assertThat(activeTSD).withFailMessage("active is original").isEqualTo(tsd);

        // Delete the permanent TSD and ensure that is went away
        rdm.resetTrackStyleDefinition();
        assertThat(rdm.getTrackStyleDefinition()).withFailMessage("The permanent TSD should be null again").isNull();

        activeTSD = stp.getActiveTSDforRDM(rdm.getId());
        assertThat(activeTSD).withFailMessage("should be no active TSD now").isNull();
    }

    /**
     * Test that the default values for TSDs have not changed from what they should be according to the API.
     *
     * @since 2.30
     */
    @Test
    public void testTSDDefaultValues()
    {

        ITrackStyleDefinition tsd = new TrackStyleDefinition(TEST_FAMILY, TSD, "default", false);
        validateCommonDefaultValues(tsd);

        tsd = new TrackStyleDefinitionBuilder(TEST_FAMILY, TSD, "default", false).build();
        validateCommonDefaultValues(tsd);

        tsd = new TrackStyleDefinitionBuilder(TEST_FAMILY, TSD, "default", true).build();
        validateCommonDefaultValues(tsd);
    }

    @Test
    public void testBulkAssignment()
    {
        final STAListener listener = new STAListener();
        smartTrackAssignmentManager.addAssignmentListener(listener);

        final TrackStyleDefinition tsd = new TrackStyleDefinition(TEST_FAMILY, TSD, "bulk1", false);
        stm.registerTrackStyle(tsd);
        final Integer tsdId = tsd.getId();

        final TrackStyleDefinition tsd2 = new TrackStyleDefinition(TEST_FAMILY, TSD, "bulk2", false);
        stm.registerTrackStyle(tsd2);
        final Integer tsdId2 = tsd2.getId();

        final List<Integer> mo1RdmIds = mo2rdmId.get(1);
        final List<Integer> mo2RdmIds = mo2rdmId.get(2);
        final List<Integer> mo3RdmIds = mo2rdmId.get(3);

        // Try 1 from each
        final List<Integer> rdmIds = new ArrayList<>();
        rdmIds.add(mo1RdmIds.get(0));
        rdmIds.add(mo2RdmIds.get(0));
        rdmIds.add(mo3RdmIds.get(0));

        smartTrackAssignmentManager.assignTSDtoRDMs(tsdId, rdmIds);
        assertThat(listener.assignments).hasSize(3);
        validateAssignments(listener, tsdId, rdmIds, null, null);

        // Now do them all
        final List<Integer> allRdmIds = new ArrayList<>();
        allRdmIds.addAll(mo1RdmIds);
        allRdmIds.addAll(mo2RdmIds);
        allRdmIds.addAll(mo3RdmIds);

        listener.clear();
        smartTrackAssignmentManager.assignTSDtoRDMs(tsdId2, allRdmIds);
        assertThat(listener.assignments).hasSize(allRdmIds.size());

        validateAssignments(listener, tsdId2, allRdmIds, tsdId, rdmIds);

        // And test clearing them
        listener.clear();
        smartTrackAssignmentManager.assignTSDtoRDMs(null, allRdmIds);
        assertThat(listener.assignments).hasSize(allRdmIds.size());

        validateAssignments(listener, null, allRdmIds, tsdId2, allRdmIds);

        // Verify that override TSD assignments take precedence (use the original 3 rdmIds)

        for (Integer rdmId : rdmIds)
        {
            rdmsById.get(rdmId).setOverrideTrackStyleDefinition(tsd);
        }

        listener.clear();
        smartTrackAssignmentManager.assignTSDtoRDMs(tsdId2, rdmIds);
        assertThat(listener.assignments).hasSize(3);
        validateAssignments(listener, tsdId, rdmIds, null, null);

        smartTrackAssignmentManager.removeAssignmentListener(listener);

        // And clean things back up
        for (Integer rdmId : rdmIds)
        {
            rdmsById.get(rdmId).resetOverrideTrackStyleDefinition();
        }
    }

    /**
     * Validate that the assignment list from the listener is as we expect.
     *
     * @param listener  Listener holding assignment list
     * @param tsdId     The assigned TSD Id
     * @param rdmIds    The list of RDM Ids that were assigned
     * @param oldTsdId  The expected old TSD Id
     * @param oldRdmIds The collection of RDMs on which to expect oldTsdId
     */
    private void validateAssignments(STAListener listener, Integer tsdId, List<Integer> rdmIds, Integer oldTsdId, List<Integer> oldRdmIds)
    {
        for (Assignment assignment : listener.assignments)
        {
            Integer expectedOldTsdId = oldRdmIds != null && oldRdmIds.contains(assignment.rdmId) ? oldTsdId : null;
            assertThat(assignment.oldTsdId).isEqualTo(expectedOldTsdId);

            assertThat(assignment.tsdId).isEqualTo(tsdId);
            assertThat(assignment.moId).isEqualTo(rdmsById.get(assignment.rdmId).getMapObject().getId());
        }

        for (Integer rdmId : rdmIds)
        {
            final ITrackStyleDefinition activeTSD = stp.getActiveTSDforRDM(rdmId);
            assertThat(activeTSD != null ? activeTSD.getId() : null).isEqualTo(tsdId);
        }
    }

    /**
     * Validate most of the common default values on a TSD.
     *
     * @param tsd the TSD to validate
     * @since 2.30
     */
    private void validateCommonDefaultValues(ITrackStyleDefinition tsd)
    {
        String errorMessage = "Unexpected Default value for a TSD field";

        assertThat(tsd.getFamily()).withFailMessage(errorMessage).isEqualTo(TEST_FAMILY);
        assertThat(tsd.getType()).withFailMessage(errorMessage).isEqualTo(TSD);
        assertThat(tsd.getUniqueKey()).withFailMessage(errorMessage).isEqualTo("default");
        assertThat(tsd.isUserDefined()).withFailMessage(errorMessage).isFalse();
        assertThat(tsd.isShowingHeading()).withFailMessage(errorMessage).isFalse();
        assertThat(tsd.getTrackShapeConstantColor()).withFailMessage(errorMessage).isNull();
        assertThat(tsd.getTrackShapeConstantOpacity()).withFailMessage(errorMessage).isEqualTo(1.0f);
        assertThat(tsd.getTrackShapeConstantSize()).withFailMessage(errorMessage).isEqualTo(ITrackStyle.NORMAL);
        assertThat(tsd.isTrackShapeLinkedToHeading()).withFailMessage(errorMessage).isFalse();
        assertThat(tsd.getGlyphResource()).withFailMessage(errorMessage).isNull();
        assertThat(tsd.getGlyphVerticalOffset()).withFailMessage(errorMessage).isEqualTo(0.0);
        assertThat(tsd.isLinkGlyphToHeading()).withFailMessage(errorMessage).isFalse();
        assertThat(tsd.isGlyphLinkedToTrackOpacity()).withFailMessage(errorMessage).isTrue();
        assertThat(tsd.isGlyphLinkedToTrackSize()).withFailMessage(errorMessage).isFalse();
        assertThat(tsd.getGlyphConstantSize()).withFailMessage(errorMessage).isEqualTo(ISize.NORMAL);
        assertThat(tsd.isFadedOverTime()).withFailMessage(errorMessage).isFalse();
        assertThat(tsd.getTrackShape()).withFailMessage(errorMessage).isEqualTo(TrackShape.Sphere);
        assertThat(tsd.getGroupability()).withFailMessage(errorMessage).isEqualTo(Groupability.SimilarOnly);
        assertThat(tsd.getBucketMode()).withFailMessage(errorMessage).isEqualTo(BucketMode.None);

        assertThat(tsd.isVisible()).withFailMessage(errorMessage).isTrue();
        assertThat(tsd.isSnappedToSurface()).withFailMessage(errorMessage).isTrue();
        assertThat(tsd.getGlyphVerticalAlignment()).withFailMessage(errorMessage).isEqualTo(VerticalAlignment.DEFAULT);
    }

    /**
     * Simple listener to collect assignment notifications.
     */
    private static final class STAListener implements ISmartTrackAssignmentListener
    {
        final List<Assignment> assignments = new ArrayList<>();

        @Override
        public void onTSDAssignedToRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm)
        {
        }

        @Override
        public void onTSDAssignedToRDM(Integer oldTsdId, Integer tsdId, int moId, int rdmId)
        {
            assignments.add(new Assignment(oldTsdId, tsdId, moId, rdmId));
        }

        public void clear()
        {
            assignments.clear();
        }
    }

    private static final class Assignment
    {
        Integer oldTsdId;
        Integer tsdId;
        Integer moId;
        Integer rdmId;

        Assignment(Integer oldTsdId, Integer tsdId, Integer moId, Integer rdmId)
        {
            this.oldTsdId = oldTsdId;
            this.tsdId = tsdId;
            this.moId = moId;
            this.rdmId = rdmId;
        }
    }
}
