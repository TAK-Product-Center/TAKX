package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.protocol.IChatProtocolHandlerDelegate;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * Unit test class for the {@link GeoChatMessageAssembler}.
 *
 * @since 3.0.1
 */
@ExtendWith(BlacksmithExtension.class)
public class GeoChatMessageAssemblerTest
{
    private static final URL EVENT_SCHEMA_URL =
            Objects.requireNonNull(
                    GeoChatMessageAssemblerTest.class.getClassLoader().getResource("chat/schemas/event.xsd"),
                    "Could not locate event.xsd");
    private static final long currentTime = System.currentTimeMillis();
    private static final double LAT = 39.739752;
    private static final double LON = -104.945318;
    private static final double ALT = 4.5;
    private static final double CE90 = 78.3;
    private static final double LE90 = 86.4;

    private static final String UID = "senderUid";
    private static final String CALL_SIGN = "senderCallSign";
    private static final String MESSAGE_ID = "304bfca7-3ef2-4b3f-8b06-8e07ed31f6c9";
    private static final String GROUP_UID = "8b94efd3-c823-4642-946b-71607d40737b";
    private static final String SUBGROUP_UID = "7a83dec2-b712-3531-835a-60506c30626c";
    private static final String ROOM_NAME = "Room Name";
    private static final String RECIPIENT_UID = "9fdcef10-466e-4ae1-95f8-46761fc2bc4c";
    private static final String MESSAGE_TEXT = "Test message text.";
    private static final String RECIPIENT_CALL_SIGN = "BRAVO";

    @SuppressWarnings("unused")
    @Mocked(forBeanUpdating = false)
    private IChatMessage message;

    @SuppressWarnings("unused")
    @Mocked(forBeanUpdating = false)
    private IChatProtocolHandlerDelegate chatServiceProviderDelegate;

    @SuppressWarnings("unused")
    @Mocked(forBeanUpdating = false)
    private IIndividualContact localContact;

    @SuppressWarnings("unused")
    @Mocked(allowCaching = false, forBeanUpdating = false)
    private IGroupContact groupContact;

    @SuppressWarnings("unused")
    @Mocked(allowCaching = false, forBeanUpdating = false)
    private IGroupContact subgroupContact;

    @SuppressWarnings("unused")
    @Mocked(allowCaching = false, forBeanUpdating = false)
    private IGroupContact parentGroupContact;

    private Validator schemaValidator;
    private GeoChatMessageAssembler assemblerUnderTest;

    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes)
                .withMockStrategy(mockStrategy));
    }

    @BeforeEach
    public void setupGeoChatMessageAssembler() throws SAXException
    {
        when(chatServiceProviderDelegate.getClientUid()).thenReturn(UID);
        when(chatServiceProviderDelegate.getClientName()).thenReturn(CALL_SIGN);
        when(chatServiceProviderDelegate.getClientLocation()).thenReturn(new GeoPoint(LAT, LON, ALT, CE90, LE90));
        when(message.getText()).thenReturn(MESSAGE_TEXT);
        when(message.getReceiveTime()).thenReturn(currentTime);
        when(message.getSendTime()).thenReturn(currentTime);
        when(message.getUid()).thenReturn(MESSAGE_ID);
        when(parentGroupContact.getUid()).thenReturn(GeoChatRootGroupContact.GROUPS_ROOT_UID);
        when(parentGroupContact.getName()).thenReturn(GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME);
        when(groupContact.getParentContact()).thenReturn(parentGroupContact);
        when(groupContact.getUid()).thenReturn(GROUP_UID);
        when(groupContact.getName()).thenReturn(ROOM_NAME);
        when(groupContact.getGroupMembers()).thenReturn(Set.of(localContact));

        when(subgroupContact.getParentContact()).thenReturn(groupContact);
        when(subgroupContact.getUid()).thenReturn(SUBGROUP_UID);
        when(subgroupContact.getName()).thenReturn(ROOM_NAME);
        when(subgroupContact.getGroupMembers()).thenReturn(Set.of(localContact));

        when(localContact.getUid()).thenReturn(UID);
        when(localContact.getName()).thenReturn(CALL_SIGN);

        schemaValidator =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(EVENT_SCHEMA_URL).newValidator();

        assemblerUnderTest = new GeoChatMessageAssembler(chatServiceProviderDelegate);
    }

    @Test
    public void assembleGroupChatMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleGroupChatMessage(RECIPIENT_UID, groupContact, message, true);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, GeoChatRootGroupContact.GROUPS_ROOT_UID, ROOM_NAME, GROUP_UID, true);
        validateChatGroup(document, GROUP_UID, RECIPIENT_UID);
        validateHierarchy(document);
        validateGroup(document);
        validateLink(document);
        validateRemarks(document, MESSAGE_TEXT, currentTime);
        validateContacts(document, Set.of(localContact));
    }

    @Test
    public void assembleGroupCreationMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleGroupCreationMessage(RECIPIENT_UID, groupContact, MESSAGE_ID);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, GeoChatRootGroupContact.GROUPS_ROOT_UID, ROOM_NAME, GROUP_UID, true);
        validateChatGroup(document, GROUP_UID, RECIPIENT_UID);
        validateHierarchy(document);
        validateGroup(document);
        validateLink(document);
        validateRemarks(document, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, -1);
        validateContacts(document, Set.of(localContact));
    }

    @Test
    public void assembleSubgroupCreationMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleGroupCreationMessage(RECIPIENT_UID, subgroupContact, MESSAGE_ID);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, GROUP_UID, ROOM_NAME, SUBGROUP_UID, true);
        validateChatGroup(document, SUBGROUP_UID, RECIPIENT_UID);
        validateHierarchy(document);
        validateGroup(document, 3);
        validateLink(document);
        validateRemarks(document, GeoChatConstants.UPDATED_CONTACTS_MESSAGE, -1);
        validateContacts(document, List.of(localContact, localContact));
    }

    @Test
    public void assembleGroupDeletionMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleGroupDeletionMessage(RECIPIENT_UID, groupContact, MESSAGE_ID);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, DetailConstants.CHAT_ROOT_CONTACT_GROUP, GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME,
                GeoChatRootGroupContact.GROUPS_ROOT_UID, false, GROUP_UID);
        validateChatGroup(document, GeoChatRootGroupContact.GROUPS_ROOT_UID, RECIPIENT_UID);
        validateLink(document);
        validateRemarks(document, "Deleted group: " + ROOM_NAME, -1);
    }

    @Test
    public void assembleSubgroupDeletionMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleGroupDeletionMessage(RECIPIENT_UID, subgroupContact, MESSAGE_ID);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, DetailConstants.CHAT_ROOT_CONTACT_GROUP, GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME,
                GeoChatRootGroupContact.GROUPS_ROOT_UID, false, SUBGROUP_UID);
        validateChatGroup(document, GeoChatRootGroupContact.GROUPS_ROOT_UID, RECIPIENT_UID);
        validateLink(document);
        validateRemarks(document, "Deleted group: " + ROOM_NAME, -1);
    }

    @Test
    public void assembleGroupEvictionMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleGroupMemberEvictionMessage(RECIPIENT_UID, groupContact, MESSAGE_ID);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, GeoChatRootGroupContact.GROUPS_ROOT_UID, ROOM_NAME, GROUP_UID, true);
        validateChatGroup(document, GROUP_UID, "");
        validateHierarchy(document);
        validateGroup(document);
        validateLink(document);
        validateRemarks(document, GeoChatConstants.KICKED_FROM_GROUP_MESSAGE, -1);
    }

    @Test
    public void assembleGroupsChatMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleTopLevelGroupChatMessage(GeoChatRootGroupContact.GROUPS_ROOT_UID,
                GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME, RECIPIENT_UID, message);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, DetailConstants.CHAT_ROOT_CONTACT_GROUP,
                GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME, GeoChatRootGroupContact.GROUPS_ROOT_UID, false);
        validateLink(document);
        validateRemarks(document, MESSAGE_TEXT, currentTime);
    }

    @Test
    public void assembleTeamsChatMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleTopLevelGroupChatMessage(GeoChatTeamsContact.TEAMS_GROUP_UID,
                GeoChatTeamsContact.TEAMS_GROUP_NAME, RECIPIENT_UID, message);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, DetailConstants.CHAT_ROOT_CONTACT_GROUP, GeoChatTeamsContact.TEAMS_GROUP_NAME,
                GeoChatTeamsContact.TEAMS_GROUP_UID, false);
        validateLink(document);
        validateRemarks(document, MESSAGE_TEXT, currentTime);
    }

    @Test
    public void assemblePeerToPeerChatMessage_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assemblePeerToPeerChatMessage(RECIPIENT_UID, RECIPIENT_CALL_SIGN, message);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document);
        validatePoint(document);
        validateDetail(document);
        validateChat(document, DetailConstants.CHAT_ROOT_CONTACT_GROUP, RECIPIENT_CALL_SIGN, RECIPIENT_UID, false);
        validateLink(document);
        validateRemarks(document, MESSAGE_TEXT, currentTime);
    }

    @Test
    public void assembleReceipt_ValidatesWithSchemaAndValues()
            throws ParserConfigurationException, SAXException, IOException
    {
        String messageXml = assemblerUnderTest.assembleReceiptMessage(RECIPIENT_UID, RECIPIENT_CALL_SIGN, MESSAGE_ID, false);
        validateXmlAgainstSchema(messageXml);

        Document document = SafeXmlDocumentUtils.parseXML(messageXml);

        validateEvent(document, uid -> assertThat(uid).isEqualTo(MESSAGE_ID), CotTypes.FREE_TEXT_DELIVERED,
                CotConstants.HOW_MACHINE_GENERATED_PREFIX);
        validatePoint(document);
        validateDetail(document);
        validateLink(document);
    }

    private void validateXmlAgainstSchema(@Nonnull String messageXml)
    {
        try
        {
            schemaValidator.validate(new StreamSource(new StringReader(messageXml)));
        } catch (SAXException | IOException e)
        {
            fail("Message failed schema validation: " + messageXml, e);
        }
    }

    private void validateEvent(@Nonnull Document document)
    {
        validateEvent(document,
                uid -> {
                    assertThat(uid).contains(UID);
                    assertThat(uid).contains(MESSAGE_ID);
                },
                CotTypes.PREFIX_FREE_TEXT,
                CotConstants.HOW_HUMAN_GARBAGE_IN_GARBAGE_OUT);
    }

    private void validateEvent(@Nonnull Document document, Consumer<String> uidChecker, String cotType, String how)
    {
        NodeList nodeList = document.getElementsByTagName("event");
        assertThat(nodeList.getLength()).isEqualTo(1);
        Element eventElement = (Element) nodeList.item(0);

        String uid = eventElement.getAttribute(CotEvent.UID);
        uidChecker.accept(uid);
        assertThat(eventElement.getAttribute(CotEvent.TYPE)).isEqualTo(cotType);

        assertThat(eventElement.hasAttribute(CotEvent.TIME)).isEqualTo(true);

        String startString = eventElement.getAttribute(CotEvent.START);
        assertThat(startString).isNotEmpty();

        Instant start = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startString));
        assertThat(Instant.parse(eventElement.getAttribute(CotEvent.STALE))).isEqualTo(start.plus(Duration.ofDays(1)));

        assertThat(eventElement.getAttribute(CotEvent.HOW)).isEqualTo(how);
    }

    private void validatePoint(@Nonnull Document document)
    {
        NodeList nodeList = document.getElementsByTagName("point");
        assertThat(nodeList.getLength()).isEqualTo(1);
        Element pointElement = (Element) nodeList.item(0);

        assertThat(pointElement.getAttribute("lat")).isEqualTo(String.valueOf(LAT));
        assertThat(pointElement.getAttribute("lon")).isEqualTo(String.valueOf(LON));
        assertThat(pointElement.getAttribute("hae")).isEqualTo(String.valueOf(ALT));
        assertThat(pointElement.getAttribute("ce")).isEqualTo(String.valueOf(CE90));
        assertThat(pointElement.getAttribute("le")).isEqualTo(String.valueOf(LE90));
    }

    private void validateDetail(@Nonnull Document document)
    {
        NodeList nodeList = document.getElementsByTagName(CotDetail.DETAIL);
        assertThat(nodeList.getLength()).isEqualTo(1);
    }

    private void validateChat(@Nonnull Document document, @Nonnull String parent, @Nonnull String chatroom, String id,
                              boolean owner)
    {
        validateChat(document, parent, chatroom, id, owner, null);
    }

    private void validateChat(@Nonnull Document document, @Nonnull String parent, @Nonnull String chatroom, String id,
                              boolean owner, @Nullable String deleteChildUid)
    {
        NodeList nodeList = document.getElementsByTagName(DetailConstants.CHAT);
        assertThat(nodeList.getLength()).isEqualTo(1);
        Element chatElement = (Element) nodeList.item(0);

        assertThat(chatElement.getAttribute(DetailConstants.ATTR_PARENT)).isEqualTo(parent);
        assertThat(chatElement.getAttribute(DetailConstants.ATTR_GROUP_OWNER)).isEqualTo(owner ? "true" : "false");
        assertThat(chatElement.getAttribute(DetailConstants.ATTR_MESSAGE_ID)).isEqualTo(MESSAGE_ID);
        assertThat(chatElement.getAttribute(DetailConstants.ATTR_CHATROOM)).isEqualTo(chatroom);
        assertThat(chatElement.getAttribute(DetailConstants.ATTR_ID)).isEqualTo(id);
        assertThat(chatElement.getAttribute(DetailConstants.ATTR_SENDER_CALLSIGN)).isEqualTo(CALL_SIGN);

        if (deleteChildUid != null) assertThat(chatElement.getAttribute(DetailConstants.ATTR_DELETE_CHILD)).isEqualTo(deleteChildUid);
    }

    private void validateChatGroup(@Nonnull Document document, String id, String uid1)
    {
        NodeList nodeList = document.getElementsByTagName(DetailConstants.CHAT_GROUP);
        assertThat(nodeList.getLength()).isEqualTo(1);
        Element chatGroupElement = (Element) nodeList.item(0);

        assertThat(chatGroupElement.getAttribute(DetailConstants.ATTR_ID)).isEqualTo(id);
        assertThat(chatGroupElement.getAttribute(DetailConstants.ATTR_UID_0)).isEqualTo(UID);
        assertThat(chatGroupElement.getAttribute(DetailConstants.ATTR_UID_1)).isEqualTo(uid1);
    }

    private void validateHierarchy(@Nonnull Document document)
    {
        NodeList nodeList = document.getElementsByTagName(DetailConstants.HIERARCHY);
        assertThat(nodeList.getLength()).isEqualTo(1);
    }

    private void validateGroup(@Nonnull Document document)
    {
        // default group node count is 2: "Groups" and 1 test group; other setups may have more nodes
        validateGroup(document, 2);
    }

    private void validateGroup(@Nonnull Document document, int groupNodeCount)
    {
        NodeList nodeList = document.getElementsByTagName(DetailConstants.HIERARCHY_GROUP);
        assertThat(nodeList.getLength()).isEqualTo(groupNodeCount);

        // Root group
        Element group0Element = (Element) nodeList.item(0);
        assertThat(group0Element.getAttribute(DetailConstants.ATTR_UID)).isEqualTo(GeoChatRootGroupContact.GROUPS_ROOT_UID);
        assertThat(group0Element.getAttribute(DetailConstants.ATTR_NAME)).isEqualTo(GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME);

        // Custom group
        Element group1Element = (Element) nodeList.item(1);
        assertThat(group1Element.getAttribute(DetailConstants.ATTR_UID)).isEqualTo(GROUP_UID);
        assertThat(group1Element.getAttribute(DetailConstants.ATTR_NAME)).isEqualTo(ROOM_NAME);
    }

    private void validateLink(@Nonnull Document document)
    {
        NodeList nodeList = document.getElementsByTagName(DetailConstants.LINK);
        assertThat(nodeList.getLength()).isEqualTo(1);
        Element linkElement = (Element) nodeList.item(0);

        assertThat(linkElement.getAttribute(DetailConstants.ATTR_UID)).isEqualTo(UID);
        assertThat(linkElement.getAttribute(DetailConstants.ATTR_TYPE)).isEqualTo(CotTypes.FRIENDLY_GROUND_COMBAT_UNIT);
        assertThat(linkElement.getAttribute(DetailConstants.ATTR_RELATION)).isEqualTo(DetailConstants.LINK_RELATION_PARENT_PRODUCER);
    }

    private void validateRemarks(@Nonnull Document document, String messageText, long messageTime)
    {
        NodeList nodeList = document.getElementsByTagName(DetailConstants.REMARKS);
        assertThat(nodeList.getLength()).isEqualTo(1);
        Element remarksElement = (Element) nodeList.item(0);

        assertThat(remarksElement.getAttribute(DetailConstants.ATTR_SOURCE)).isEqualTo(UID);
        if (messageTime >= 0)
        {
            final var messageTimeInstant = Instant.ofEpochMilli(messageTime);
            final var attrTimeInstant = Instant.parse(remarksElement.getAttribute(DetailConstants.ATTR_TIME));
            assertThat(attrTimeInstant).isEqualTo(messageTimeInstant);
        }
        assertThat(remarksElement.getAttribute(DetailConstants.ATTR_TO)).isEqualTo(RECIPIENT_UID);
        assertThat(remarksElement.getTextContent()).isEqualTo(messageText);
    }

    private void validateContacts(@Nonnull Document document, Collection<IContact> expectedContacts)
    {
        NodeList nodeList = document.getElementsByTagName(DetailConstants.CONTACT);
        int actualContactsCount = nodeList.getLength();
        int expectedContactsCount = expectedContacts != null ? expectedContacts.size() : 0;
        assertThat(actualContactsCount).isEqualTo(expectedContactsCount);

        if (expectedContactsCount == 0) return;

        Map<String, String> contacts = new HashMap<>();

        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Element contact = (Element) nodeList.item(0);
            contacts.put(contact.getAttribute(DetailConstants.ATTR_UID), contact.getAttribute(DetailConstants.ATTR_NAME));
        }

        expectedContacts.forEach(contact -> {
            String name = contacts.get(contact.getUid());
            assertThat(name).isNotNull();
            assertThat(name).isEqualTo(contact.getName());
        });
    }
}
