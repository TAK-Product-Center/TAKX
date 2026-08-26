package gov.takx.api.plugin.util;

import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IPrePersistRaptorMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.device.ADataStructureFactory;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.commons.date.DateFormatter;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
@SuppressWarnings("JavaUtilDate")
public class MessageSerializerTest extends AContainerTest
{
    private static final String MESSAGE_TYPE = "testMessage";

    protected PersistTestSetup test;
    private IPrePersistRaptorDataMessage rdm;
    private IRaptorDataStructure struct;

    /**
     * Get the data structure for our test messages.
     *
     * @return The IRaptorDataStructure for our test messages.
     * @since 2.36
     */
    private IRaptorDataStructure getTestDataStructure()
    {
        return new DataStructureBuilder(KeyUtils.EMPTY_FAMILY, "testObject", "testVersion", MESSAGE_TYPE)
                .addBoolean("testBoolean")
                .addString("testString")
                .addFloat("testFloat")
                .addDouble("testDouble")
                .addInt("testInt")
                .addShort("testShort")
                .addLong("testLong")
                .addDate("testDate")
                .addByteArray("testBytes")
                .build();
    }

    @BeforeAll
    public void setup()
    {
        test = new PersistTestSetup();
        test.init();
        struct = getTestDataStructure();

        rdm = struct.createRaptorDataMessage("testUnit");

        rdm.setLocation(89.0, 88.0, 1.0);
        rdm.setOrientation(1.0, 2.0, 3.0);
        rdm.setSpeedInMps(5.0);
        rdm.setTime(System.currentTimeMillis());
        rdm.setSourceType(CommServiceType.SERIAL);
        rdm.setSourceName("SomePort");
        setFields(rdm);
    }

    private void setFields(IPrePersistRaptorMessage msg)
    {
        msg.setBoolean("testBoolean", true);
        msg.setString("testString", "this is a test");
        msg.setFloat("testFloat", 42.42f);
        msg.setDouble("testDouble", 84.84);
        msg.setInt("testInt", 99);
        msg.setShort("testShort", (short) 44);
        msg.setLong("testLong", 444524L);
        msg.setDate("testDate", DateFormatter.parseDate("2009-04-22 08:08:00", "yyyy-MM-dd HH:mm:ss", "PST"));
        msg.setByteArray("testBytes", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    }

    @AfterAll
    public void teardown()
    {
        test.shutdown();
    }

    @Test
    public void serializeAndDeserializeRdm()
    {
        String xml = MessageSerializer.toXml(rdm);

        IPrePersistRaptorDataMessage newMessage = MessageSerializer.fromXml(xml, struct);

        assertDataMessageFields(newMessage, rdm);
    }

    /**
     * Test messagesFromXML - setting up the data structure factory as frequently used in the field.
     *
     * @since 2.36
     */
    @Test
    public void deserializeRdms()
    {
        IDataStructureFactory factory = new ADataStructureFactory()
        {
            @Override
            protected void createStructureMap()
            {
                defineDataStructure(MESSAGE_TYPE, getTestDataStructure());
            }
        };
        factory.resetStructures();

        // Save times so we can ensure we get the same messages back
        long[] times = new long[3];

        times[0] = rdm.getTime();
        times[1] = times[0] + 1000;
        times[2] = times[1] + 1000;

        String xmlRdms = "<dataMessages>";
        xmlRdms += MessageSerializer.toXml(rdm) + System.lineSeparator();
        rdm.setTime(times[1]);
        xmlRdms += MessageSerializer.toXml(rdm) + System.lineSeparator();
        rdm.setTime(times[2]);
        xmlRdms += MessageSerializer.toXml(rdm) + System.lineSeparator();
        xmlRdms += "</dataMessages>";

        rdm.setTime(times[0]);

        List<IPrePersistRaptorDataMessage> messages = MessageSerializer.messagesFromXml(xmlRdms, factory);
        assertThat(messages).hasSize(3);

        for (int i = 0; i < 3; i++)
        {
            IPrePersistRaptorDataMessage newMessage = messages.get(i);
            rdm.setTime(times[i]);
            assertDataMessageFields(newMessage, rdm);
        }
        rdm.setTime(times[0]);
    }

    private void assertCommonFields(IPrePersistRaptorMessage actual, IPrePersistRaptorMessage expected)
    {
        assertThat(actual.getUnitID()).isEqualTo(expected.getUnitID());
        assertThat(actual.getType()).isEqualTo(expected.getType());
        assertThat(actual.getMessageType()).isEqualTo(expected.getMessageType());
        assertThat(actual.getVersion()).isEqualTo(expected.getVersion());
        assertThat(actual.getUUID()).isEqualTo(expected.getUUID());

        assertThat(actual.getBoolean("testBoolean")).isEqualTo(expected.getBoolean("testBoolean"));
        assertThat(actual.getString("testString")).isEqualTo(expected.getString("testString"));
        assertThat(actual.getFloat("testFloat")).isEqualTo(expected.getFloat("testFloat"));
        assertThat(actual.getDouble("testDouble")).isEqualTo(expected.getDouble("testDouble"));
        assertThat(actual.getInt("testInt")).isEqualTo(expected.getInt("testInt"));
        assertThat(actual.getLong("testLong")).isEqualTo(expected.getLong("testLong"));
        assertThat(actual.getShort("testShort")).isEqualTo(expected.getShort("testShort"));

        assertThat(actual.getDate("testDate").getTime()).isEqualTo(expected.getDate("testDate").getTime());
        assertThat(Arrays.equals(expected.getByteArray("testBytes"), actual.getByteArray("testBytes"))).isTrue();
    }

    /**
     * Validate fields in deserialized raptor data messages against the original message.
     *
     * @param actual   The deserialized message
     * @param expected The original message
     * @since 2.36
     */
    private void assertDataMessageFields(IPrePersistRaptorDataMessage actual, IPrePersistRaptorMessage expected)
    {
        assertThat(actual.getSourceName()).isEqualTo(rdm.getSourceName());

        assertThat(actual.getLatitude()).isEqualTo(rdm.getLatitude());
        assertThat(actual.getLongitude()).isEqualTo(rdm.getLongitude());
        assertThat(actual.getElevation()).isEqualTo(rdm.getElevation());
        assertThat(actual.getHeading()).isEqualTo(rdm.getHeading());
        assertThat(actual.getPitch()).isEqualTo(rdm.getPitch());
        assertThat(actual.getRoll()).isEqualTo(rdm.getRoll());
        assertThat(actual.getSpeedInMps()).isEqualTo(rdm.getSpeedInMps());
        assertThat(actual.getTime()).isEqualTo(rdm.getTime());

        assertCommonFields(actual, expected);
    }
}
