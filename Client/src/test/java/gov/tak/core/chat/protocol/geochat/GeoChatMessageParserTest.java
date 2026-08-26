package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.platform.chat.ChatReceipt;
import gov.tak.platform.contact.GroupContact;
import gov.takx.test.AContainerTest;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests {@link GeoChatMessageParser} using real WinTAK and ATAK chat data. The the XML data for each message being
 * tested must be inspected to determine the expected values, so constants are not used even though some values repeat.
 *
 * @since 3.4
 */
public class GeoChatMessageParserTest extends AContainerTest
{
    private static final String FILES_DIR = "chat/xml/";
    private static final String CLIENT_UID = "clientUid";
    private static final GeoChatTeamsContact TEAMS_CONTACT = new GeoChatTeamsContact();

    @SuppressWarnings("unused")
    @Mocked
    private GeoChatContactProvider geoChatContactProvider;

    @SuppressWarnings("unused")
    @Mocked
    private GroupContact groupContact;

    @SuppressWarnings("unused")
    @Mocked
    private IIndividualContact individualContact;

    @SuppressWarnings("unused")
    private GeoChatMessageParser parser;

    private long startTime;

    @BeforeEach
    public void setup()
    {
        when(individualContact.getUid()).thenReturn("individualUid");
        when(individualContact.getName()).thenReturn("individualDisplayName");
        when(individualContact.getAttributes()).thenReturn(new AttributeSet());
        when(groupContact.getUid()).thenReturn("groupUid");
        when(groupContact.getName()).thenReturn("groupDisplayName");
        when(groupContact.getAttributes()).thenReturn(new AttributeSet());

        parser = new GeoChatMessageParser(geoChatContactProvider);
        startTime = System.currentTimeMillis();
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesDirectWintakMessage() throws IOException
    {
        final var senderUid = "S-1-5-21-2739727742-677502536-3530350016-1000";
        setGetContactExpectation(individualContact, senderUid);

        CotEvent cotEvent = getCotEvent("direct_message_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1661286737770L,
                "473ea506-f47c-4541-baa0-4c3e2c9a0913", senderUid, "OPTIMUS WT", senderUid);
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesDirectAtakMessage() throws IOException
    {
        final var senderUid = "ANDROID-R32M700HZ2X";
        setGetContactExpectation(individualContact, senderUid);

        CotEvent cotEvent = getCotEvent("direct_message_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "at breach", 1660766441048L,
                "7d073764-3871-4542-8210-da47e40ea166", senderUid, "OPTIMUS ATAK", senderUid);
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesDirectWebtakMessage() throws IOException
    {
        final var senderUid = "432230f8-47ae-2e45-8234-5c2a6a2e4625";
        setGetContactExpectation(individualContact, senderUid);

        CotEvent cotEvent = getCotEvent("direct_message_webtak_4.10.1.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "hello", 1712169443000L,
                "60356107-eec2-e768-a515-f66343b7b477", senderUid, "OPTIMUS WebTAK M1", senderUid);
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesAllChatRoomsWintakMessage() throws Exception
    {
        setGetContactExpectation(individualContact);

        CotEvent cotEvent = getCotEvent("all_chat_rooms_message_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1661287800360L,
                "a0ab7162-5a20-49d8-a977-6db7be5208fd",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT",
                GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME);
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesAllChatRoomsAtakMessage() throws Exception
    {
        setGetContactExpectation(individualContact);

        CotEvent cotEvent = getCotEvent("all_chat_rooms_message_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1041981355896L,
                "993ee7e4-6a4a-4536-999b-d2703dc5a7d2",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK",
                GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME);
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesAllChatRoomsWebtakMessage() throws Exception
    {
        setGetContactExpectation(individualContact);

        CotEvent cotEvent = getCotEvent("all_chat_rooms_message_webtak_4.10.1.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "hello", 1712169366000L,
                "e4a09281-1efa-0239-85a1-73513d06af32",
                "432230f8-47ae-2e45-8234-5c2a6a2e4625", "OPTIMUS WebTAK M1",
                GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME);
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesAllChatRoomsWintakSimulatorMessage() throws Exception
    {
        setGetContactExpectation(individualContact);

        CotEvent cotEvent = getCotEvent("all_chat_rooms_message_wintak_simulator.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Test from WinTAK: 2025-05-21T23:25:50.000Z", 1747869950000L,
                "1747869950595",
                "448fca89-278f-4701-b6c2-e12b5f936ce9.NMEA.SA", "JS_WinTAK",
                GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME);
    }

    @Test
    public void parseGroupCotEvent_CorrectlyParsesGroupWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact, "6104ccc9-eb97-4852-9125-3c2ab0658b70");

        CotEvent cotEvent = getCotEvent("group_message_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseGroupCotEvent(cotEvent, CLIENT_UID, false);

        validateGeoChatMessage(geoChatMessage, "Roger", 1660768789190L,
                "3dc99d39-cf29-42b6-990b-44b95c261147",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT",
                "6104ccc9-eb97-4852-9125-3c2ab0658b70");
    }

    @Test
    public void parseGroupCotEvent_CorrectlyParsesGroupAtakMessage() throws IOException
    {
        setGetContactExpectation(groupContact, "6104ccc9-eb97-4852-9125-3c2ab0658b70");

        CotEvent cotEvent = getCotEvent("group_message_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseGroupCotEvent(cotEvent, CLIENT_UID, false);

        validateGeoChatMessage(geoChatMessage, "Roger", 1660766301845L,
                "4ce326c0-5d5b-49ec-b774-95c6beca4aa8",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK",
                "6104ccc9-eb97-4852-9125-3c2ab0658b70");
    }

    @Test
    public void parseGroupCotEvent_CorrectlyParsesGroupsWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact, GeoChatRootGroupContact.GROUPS_ROOT_UID);

        CotEvent cotEvent = getCotEvent("groups_message_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseGroupCotEvent(cotEvent, CLIENT_UID, false);

        validateGeoChatMessage(geoChatMessage, "Roger", 1661288136030L,
                "be0935ef-588c-4bf7-977c-dde4b5f07fb3",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT",
                GeoChatRootGroupContact.GROUPS_ROOT_UID);
    }

    @Test
    public void parseGroupCotEvent_CorrectlyParsesGroupsAtakMessage() throws IOException
    {
        setGetContactExpectation(groupContact, GeoChatRootGroupContact.GROUPS_ROOT_UID);

        CotEvent cotEvent = getCotEvent("groups_message_atak_4.6.xml");

        // No hierarchy element in ATAK's message, so it gets processed as a direct event
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1041981992711L,
                "b526d823-64b1-411b-9973-87fa554d803e",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK",
                GeoChatRootGroupContact.GROUPS_ROOT_UID);
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupDeletedWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_deleted_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Deleted group: New Group", 1660777924510L,
                "de44e561-4c88-43ce-b849-9ae8ad01aa01",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT");
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupDeletedAtakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_deleted_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Deleted group: z", 1660776432408L,
                "31520e3d-6c8a-4a8a-9f62-4d3246af1d6e",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK");
    }

    @Test
    public void parseGroupDeletionEvent_CorrectlyParsesSubgroupDeletedWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("subgroup_deleted_wintak_4.8.xml");
        IGroupContact deletedGroupContact = parser.parseGroupDeletionEvent(cotEvent);

        assertThat(deletedGroupContact).isNotNull();
    }

    @Test
    public void parseGroupDeletionEvent_CorrectlyParsesSubgroupDeletedAtakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("subgroup_deleted_atak_4.6.xml");
        IGroupContact deletedGroupContact = parser.parseGroupDeletionEvent(cotEvent);

        assertThat(deletedGroupContact).isNotNull();
    }

    @Test
    public void parseGroupCotEvent_CorrectlyParsesGroupCreatedWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_created_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseGroupCotEvent(cotEvent, CLIENT_UID, false);

        validateGeoChatMessage(geoChatMessage, "Roger", 1661296798490L,
                "4d9dc882-f693-4d48-bacc-07e2361b0567",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT");
    }

    @Test
    public void parseGroupCotEvent_CorrectlyParsesGroupCreatedAtakMessage(@Mocked(allowCaching = false) IIndividualContact client,
                                                                          @Mocked(allowCaching = false) IIndividualContact sender,
                                                                          @Mocked(allowCaching = false) GroupContact groupContact)
            throws IOException
    {
        final String clientUid = "ebc53795-403f-540f-99c6-983696f0bc16";
        final String senderUid = "ANDROID-R32M700HZ2X";
        final String senderName = "OPTIMUS ATAK";
        final String groupUid = "6104ccc9-eb97-4852-9125-3c2ab0658b70";

        configureContact(client, clientUid, "client");
        configureContact(sender, senderUid, senderName);
        configureContact(groupContact, groupUid, "group");

        CotEvent cotEvent = getCotEvent("group_created_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseGroupCotEvent(cotEvent, clientUid, false);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1660766209563L,
                "2799e571-a802-4813-98c6-fe227508c24e",
                senderUid, senderName);
    }

    @Test
    public void parseGroupCotEvent_CorrectlyParsesSubgroupCreatedWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("subgroup_created_wintak_4.8.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseGroupCotEvent(cotEvent, CLIENT_UID, false);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1681243659850L,
                "7ce9bc1c-a487-4392-87e0-8b77b77a2640",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT");
    }

    @Test
    public void parseGroupCotEvent_CorrectlyParsesSubgroupCreatedAtakMessage(@Mocked(allowCaching = false) IIndividualContact client,
                                                                             @Mocked(allowCaching = false) IIndividualContact sender,
                                                                             @Mocked(allowCaching = false) GroupContact groupContact,
                                                                             @Mocked(allowCaching = false) GroupContact subgroupContact)
            throws IOException
    {
        final String clientUid = "a6848264-d5e2-58e2-adec-fec4d0ee77b5";
        final String senderUid = "ANDROID-R32M700HZ2X";
        final String senderName = "OPTIMUS ATAK";
        final String groupUid = "67109f0a-0b38-4301-bae7-9d1e39010497";
        final String subgroupUid = "96d602ee-e461-4ecd-945f-74130c628226";

        configureContact(client, clientUid, "client");
        configureContact(sender, senderUid, senderName);
        configureContact(groupContact, groupUid, "group");
        configureContact(subgroupContact, subgroupUid, "subgroup");

        CotEvent cotEvent = getCotEvent("subgroup_created_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseGroupCotEvent(cotEvent, clientUid, false);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1061847262883L,
                "dc43d3b9-3cd8-4031-bca6-cd5ac09a2915",
                senderUid, senderName);
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupAddOtherWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_add_other_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1661297794900L,
                "8bb65fdc-b469-4bec-a986-e83ca376fedd",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT");
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupAddOtherAtakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_add_other_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1041982787949L,
                "7f0a333c-99ed-4368-b7dd-2c17ac1d11a3",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK");
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupAddMeWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_add_me_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1661296997770L,
                "af68510a-5f48-464f-8375-1fdfc0d1c5f4",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT");
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupAddMeAtakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_add_me_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1660768548574L,
                "3605e4b5-34a1-4bda-b7ff-fb487f0848d2",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK");
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupRemoveMeWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_remove_me_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.KICKED_FROM_GROUP_MESSAGE, 1661296944870L,
                "ebb1b001-7bed-4705-8498-47375c065569",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT");
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupRemoveMeAtakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_remove_me_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.KICKED_FROM_GROUP_MESSAGE, 1660766819290L,
                "0f0393b5-76cc-48fa-85c7-08ce19d1109d",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK");
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupRemoveOtherWintakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_remove_other_wintak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1661297727350L,
                "de7171ba-76dd-44be-89a9-3c0029b775e3",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WT");
    }

    @Test
    public void parseDirectCotEvent_CorrectlyParsesGroupRemoveOtherAtakMessage() throws IOException
    {
        setGetContactExpectation(groupContact);

        CotEvent cotEvent = getCotEvent("group_remove_other_atak_4.6.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseDirectCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, 1041982747221L,
                "114c70ea-b7f8-4b5d-95cb-9cb9014aa5dd",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK");
    }

    @Test
    public void parseReceipt_CorrectlyParsedReceiptDeliveredWintakMessage() throws IOException
    {
        setGetContactExpectation(individualContact);

        CotEvent cotEvent = getCotEvent("chat_receipt_delivered_wintak_4.6.xml");
        ChatReceipt receipt = parser.parseReceipt(cotEvent);

        assertThat(receipt).isNotNull();

        assertThat(receipt.messageUid()).isEqualTo("afe1d15d-e31e-45d9-8564-b09263ba4d88");
        assertThat(receipt.contact()).isEqualTo(individualContact);
        assertThat(receipt.cotType()).isEqualTo(CotTypes.FREE_TEXT_DELIVERED);
    }

    @Test
    public void parseReceipt_CorrectlyParsedReceiptDeliveredAtakMessage() throws IOException
    {
        setGetContactExpectation(individualContact);

        CotEvent cotEvent = getCotEvent("chat_receipt_delivered_atak_4.6.xml");
        ChatReceipt receipt = parser.parseReceipt(cotEvent);

        assertThat(receipt).isNotNull();

        assertThat(receipt.messageUid()).isEqualTo("956a2c3d-dd37-43db-b1a2-70db5c861240");
        assertThat(receipt.contact()).isEqualTo(individualContact);
        assertThat(receipt.cotType()).isEqualTo(CotTypes.FREE_TEXT_DELIVERED);
    }

    @Test
    public void parseReceipt_CorrectlyParsedReceiptReadWintakMessage() throws IOException
    {
        setGetContactExpectation(individualContact);

        CotEvent cotEvent = getCotEvent("chat_receipt_read_wintak_4.6.xml");
        ChatReceipt receipt = parser.parseReceipt(cotEvent);

        assertThat(receipt).isNotNull();

        assertThat(receipt.messageUid()).isEqualTo("afe1d15d-e31e-45d9-8564-b09263ba4d88");
        assertThat(receipt.contact()).isEqualTo(individualContact);
        assertThat(receipt.cotType()).isEqualTo(CotTypes.FREE_TEXT_READ);
    }

    @Test
    public void parseReceipt_CorrectlyParsedReceiptReadAtakMessage() throws IOException
    {
        setGetContactExpectation(individualContact);

        CotEvent cotEvent = getCotEvent("chat_receipt_read_atak_4.6.xml");
        ChatReceipt receipt = parser.parseReceipt(cotEvent);

        assertThat(receipt).isNotNull();

        assertThat(receipt.messageUid()).isEqualTo("956a2c3d-dd37-43db-b1a2-70db5c861240");
        assertThat(receipt.contact()).isEqualTo(individualContact);
        assertThat(receipt.cotType()).isEqualTo(CotTypes.FREE_TEXT_READ);
    }

    @Test
    public void parseTeamCotEvent_CorrectlyParsesTeamWintakMessage(@Mocked IGroupContact teamContact) throws IOException
    {
        when(teamContact.getUid()).thenReturn("Yellow");

        GeoChatTeamsContact teamsContact = new GeoChatTeamsContact();
        teamsContact.setGroupMembers(Set.of(teamContact));
        setGetContactExpectation(teamsContact);

        CotEvent cotEvent = getCotEvent("team_message_wintak_4.8.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseTeamCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "at VDO", 1684882488660L,
                "cc74714b-17be-4b13-832a-fdbe69657c7e",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WINTAK");
    }

    @Test
    public void parseTeamCotEvent_CorrectlyParsesTeamAtakMessage(@Mocked IGroupContact teamContact) throws IOException
    {
        when(teamContact.getUid()).thenReturn("Yellow");

        GeoChatTeamsContact teamsContact = new GeoChatTeamsContact();
        teamsContact.setGroupMembers(Set.of(teamContact));
        setGetContactExpectation(teamsContact);

        CotEvent cotEvent = getCotEvent("team_message_atak_4.8.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseTeamCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1684864664614L,
                "30f70bab-428d-4438-8727-0ec0c117c379",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK 4.8");
    }

    @Test
    public void parseTeamCotEvent_CorrectlyParsesTeamsWintakMessage() throws IOException
    {
        setGetContactExpectation(TEAMS_CONTACT);

        CotEvent cotEvent = getCotEvent("teams_message_wintak_4.8.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseTeamCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1685470923320L,
                "054c4c66-fe8d-42d0-a987-7acc09c32794",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WINTAK");
    }

    @Test
    public void parseTeamCotEvent_CorrectlyParsesTeamsAtakMessage() throws IOException
    {
        setGetContactExpectation(TEAMS_CONTACT);

        CotEvent cotEvent = getCotEvent("teams_message_atak_4.8.xml");

        GeoChatParsedMessage geoChatMessage = parser.parseTeamCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1685463372312L,
                "b2f0af52-619c-4bf9-b90d-c5d92ea4ae77",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK 4.8");
    }

    @Test
    public void parseRoleCotEvent_CorrectlyParsesRoleWintakMessage() throws IOException
    {
        CotEvent cotEvent = getCotEvent("role_message_wintak_4.8.xml");
        GeoChatParsedMessage geoChatMessage = parser.parseRoleCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1690313932260L,
                "9d05bf98-bfd4-4520-8152-a275cca14d56",
                "S-1-5-21-2739727742-677502536-3530350016-1000", "OPTIMUS WINTAK");
    }

    @Test
    public void parseRoleCotEvent_CorrectlyParsesRoleAtakMessage() throws IOException
    {
        CotEvent cotEvent = getCotEvent("role_message_atak_4.8.xml");

        GeoChatParsedMessage geoChatMessage = parser.parseRoleCotEvent(cotEvent);

        validateGeoChatMessage(geoChatMessage, "Roger", 1689724413324L,
                "05762203-81b1-4ea1-b63a-9a7ecb74b73d",
                "ANDROID-R32M700HZ2X", "OPTIMUS ATAK");
    }

    /**
     * Returns a CoT event based on the specified XML file.
     *
     * @param fileName The full XML file name
     * @return A CoT event
     * @throws IOException if there are errors retrieving the XML file
     */
    private CotEvent getCotEvent(String fileName) throws IOException
    {
        String filePath = FILES_DIR + fileName;
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(filePath), "Could not locate " + filePath);

        try (InputStream is = url.openStream())
        {
            String xml = new String(is.readAllBytes(), Charset.defaultCharset());
            return CotEvent.parse(xml);
        }
    }

    /**
     * @param contact The contact to return for all calls to {@link GeoChatContactProvider#getContact(String)}
     * @since 3.5
     */
    private void setGetContactExpectation(IContact contact)
    {
        when(geoChatContactProvider.getContact(anyString())).thenReturn(contact);
    }

    /**
     * @param contact The contact to return for all calls to {@link GeoChatContactProvider#getContact(String)}
     * @param contactUid The contact's UID
     * @since 3.5
     */
    private void setGetContactExpectation(IContact contact, String contactUid)
    {
        when(contact.getUid()).thenReturn(contactUid);
        when(geoChatContactProvider.getContact(contactUid)).thenReturn(contact);
    }

    /**
     * @param contact The contact to return for calls to {@link GeoChatContactProvider#getContact(String)} where the UID
     *                matches the {@code uid} parameter
     * @param uid The contact's UID 
     * @param name The contact's name
     * @since 3.5
     */
    private void configureContact(IContact contact, String uid, String name)
    {
        when(contact.getUid()).thenReturn(uid);
        when(contact.getName()).thenReturn(name);
        when(geoChatContactProvider.getContact(uid)).thenReturn(contact);
    }

    /**
     * Validates a chat message. The expected sender UID will also be used as the expected conversation id.
     *
     * @param geoChatMessage     The chat message
     * @param expectedMessage    The expected message, or {@code null} if no message is expected
     * @param expectedSendTime   The expected send time
     * @param expectedMessageUid The expected message UID
     * @param expectedSenderUid  The expected sender UID
     * @param expectedSenderName The expected sender display name
     */
    private void validateGeoChatMessage(GeoChatParsedMessage geoChatMessage, @Nullable String expectedMessage, long expectedSendTime,
                                        String expectedMessageUid, String expectedSenderUid, String expectedSenderName)
    {
        validateGeoChatMessage(geoChatMessage, expectedMessage, expectedSendTime, expectedMessageUid, expectedSenderUid,
                expectedSenderName, null);
    }

    /**
     * Validates a chat message
     *
     * @param geoChatMessage     The chat message
     * @param expectedMessage    The expected message, or {@code null} if no message is expected
     * @param expectedSendTime   The expected send time
     * @param expectedMessageUid The expected message UID
     * @param expectedSenderUid  The expected sender UID
     * @param expectedSenderName The expected sender display name
     * @param expectedConversationUid The expected conversation UID
     * @since 5.4
     */
    private void validateGeoChatMessage(GeoChatParsedMessage geoChatMessage, @Nullable String expectedMessage, long expectedSendTime,
                                        String expectedMessageUid, String expectedSenderUid, String expectedSenderName,
                                        @Nullable String expectedConversationUid)
    {
        assertThat(geoChatMessage).isNotNull();

        IChatMessage message = geoChatMessage.getMessage();
        assertThat(message).isNotNull();

        String messageString = message.getText();
        if (expectedMessage != null)
        {
            assertThat(messageString).isNotNull();
            assertThat(messageString.trim()).isEqualTo(expectedMessage);
        }

        assertThat(message.getSendTime())
                .as("Incorrect send time")
                .isEqualTo(expectedSendTime);

        assertThat(message.getReceiveTime())
                .as("Receive time out of expected range")
                .isGreaterThanOrEqualTo(startTime)
                .isLessThanOrEqualTo(System.currentTimeMillis());

        assertThat(message.getUid())
                .as("Incorrect message UID")
                .isEqualTo(expectedMessageUid);

        assertThat(message.getSenderUid())
                .as("Incorrect sender UID")
                .isEqualTo(expectedSenderUid);

        assertThat(message.getSenderName())
                .as("Incorrect display name")
                .isEqualTo(expectedSenderName);

        if (expectedConversationUid != null)
        {
            assertThat(message.getConversationUid())
                    .as("Incorrect conversation UID")
                    .isEqualTo(expectedConversationUid);
        }
    }
}