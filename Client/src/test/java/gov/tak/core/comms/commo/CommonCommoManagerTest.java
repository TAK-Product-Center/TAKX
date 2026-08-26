package gov.tak.core.comms.commo;

import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.commoncommo.Commo;
import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import com.atakmap.commoncommo.PhysicalNetInterface;
import com.atakmap.commoncommo.StreamingNetInterface;
import com.atakmap.commoncommo.StreamingTransport;
import com.atakmap.commoncommo.TcpInboundNetInterface;
import gov.tak.api.comms.commo.ConnectionConfiguration;
import gov.tak.api.comms.commo.ICotEventInterceptor;
import gov.tak.api.comms.commo.ICotSendListener;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.blacksmith.mock.MethodBehavior;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.comms.cot.CotEventReceiver;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommonCommoManagerTest extends AContainerTest
{
    @Mocked(methodBehavior = MethodBehavior.RETURN_REAL_METHOD, allowCaching = false)
    CommonCommoManager commonCommoManager;

    @BeforeEach
    public void beforeEach()
    {
        commonCommoManager.cotEventInterceptors = new ArrayList<>();
    }

    @Test
    void whenAddInput_withTcpDevice_addTcpInboundInterfaceCalled(@Mocked(allowCaching = false) Commo commo) throws URISyntaxException, CommoException
    {
        commonCommoManager.commo = commo;
        commonCommoManager.apiManagedInboundTcpInterfaces = new HashMap<>();
        URI tcpUri = new URI("tcp://localhost:5678");

        TcpInboundNetInterface mockInterface = mock(TcpInboundNetInterface.class);
        when(commo.addTcpInboundInterface(5678)).thenReturn(mockInterface);

        commonCommoManager.addInput(tcpUri, false);

        // since addInput is a void method, just verify what was internally called.
        verify(commo, atLeastOnce()).addTcpInboundInterface(5678);
    }

    @Test
    void whenAddInput_withUnsupportedProtocol_throws() throws URISyntaxException
    {
        URI unsupportedUri = new URI("ftp://localhost:1234");

        assertThatIllegalArgumentException().isThrownBy(() -> commonCommoManager.addInput(unsupportedUri, false))
                .withMessage("Unsupported protocol in connect string (ftp://localhost:1234)");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false}) // parameterize tests for device enabled = true, false
    void whenAddInput_withUdpEnabledDevice_addInboundInterfaceCalled(boolean enabled,
                                                                     @Mocked(allowCaching = false) NetworkDevice networkEnabledDevice,
                                                                     @Mocked(allowCaching = false) PhysicalNetInterface physicalNetInterface,
                                                                     @Mocked(allowCaching = false) INetworkDeviceManager networkDeviceManager,
                                                                     @Mocked(allowCaching = false) Commo commo)
            throws URISyntaxException, CommoException
    {
        commonCommoManager.commo = commo;
        commonCommoManager.apiManagedInboundUdpInterfaces = new HashMap<>();
        commonCommoManager.networkDeviceManager = networkDeviceManager;
        URI udpUri = new URI("udp://localhost:1234");

        when(networkEnabledDevice.getDisplayName()).thenReturn("enabledDevice");
        when(networkEnabledDevice.getMacAddress()).thenReturn("00:11:22:33:44:55".getBytes(StandardCharsets.UTF_8));
        when(networkDeviceManager.getNetworkDevices()).thenReturn(List.of(networkEnabledDevice));
        when(commonCommoManager.enabled(networkEnabledDevice)).thenReturn(enabled);

        String[] mcastAddresses = {udpUri.getHost()};
        boolean forGenericData = true;
        when(commo.addInboundInterface(networkEnabledDevice.getMacAddress(), udpUri.getPort(), mcastAddresses, forGenericData))
                .thenReturn(physicalNetInterface);

        commonCommoManager.addInput(udpUri, forGenericData);

        // Only udp schemes that are enabled will trigger addInboundInterface.
        if (enabled)
        {
            verify(commonCommoManager).addInboundUdpInterface(eq(networkEnabledDevice), eq(udpUri), eq(forGenericData), any());
        } else
        {
            // if device not enabled, never will call it
            verify(commonCommoManager, never()).addInboundUdpInterface(eq(networkEnabledDevice), eq(udpUri), eq(forGenericData), any());
        }
    }

    @Test
    void whenRemoveInput_invalidProtocol_throws()
    {
        URI inputUri = URI.create("http://localhost:1234");

        assertThatIllegalArgumentException().isThrownBy(() -> commonCommoManager.removeInput(inputUri))
                .withMessage("Unsupported protocol in connect string (http://localhost:1234)");
    }

    @Test
    void whenRemoveInput_withUdp_removeInboundInterface(@Mocked(allowCaching = false) NetworkDevice networkEnabledDevice,
                                                        @Mocked(allowCaching = false) Commo commo)
            throws Exception
    {
        commonCommoManager.commo = commo;

        URI inputUri = new URI("udp://localhost:1234");
        PhysicalNetInterface pnd = mock(PhysicalNetInterface.class);

        commonCommoManager.apiManagedInboundUdpInterfaces = new HashMap<>();
        commonCommoManager.apiManagedInboundUdpInterfaces.put(new CommonCommoManager.ApiManagedInput(inputUri, false), Map.of(networkEnabledDevice, pnd));

        commonCommoManager.removeInput(inputUri);

        verify(commo, times(1)).removeInboundInterface(pnd);
    }

    @Test
    void whenRemoveInput_withTcp_removeTcpInboundInterfaceCalled(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        commonCommoManager.commo = commo;

        URI inputUri = new URI("tcp://localhost:1234");
        TcpInboundNetInterface netInterface = mock(TcpInboundNetInterface.class);
        commonCommoManager.apiManagedInboundTcpInterfaces = new HashMap<>();
        commonCommoManager.apiManagedInboundTcpInterfaces.put("tcp://1234", netInterface);

        commonCommoManager.removeInput(inputUri);

        verify(commo, times(1)).removeTcpInboundInterface(netInterface);
    }

    /**
     * when ssl connection is enabled and all required parameters are provided, properly uses commo AddStreamingInterface.
     */
    @Test
    void whenAddStreamingConnection_commoAddStreamingInterface(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        commonCommoManager.commo = commo;
        StreamingConnectionConfiguration connectionConfig = getStreamingConnectionConfigurationHelper("username", "password");
        var spyConnectionConfig = Mockito.spy(connectionConfig);

        // Mocking all authentication so it does not throw exception
        when(spyConnectionConfig.getCertificateBytes()).thenReturn(new byte[]{1, 2, 3});
        when(spyConnectionConfig.getCertificateAuthorityBytes()).thenReturn(new byte[]{4, 5, 6});
        when(spyConnectionConfig.getCertificatePassword()).thenReturn("certPass");
        when(spyConnectionConfig.getCertificateAuthorityPassword()).thenReturn("caCertPass");
        when(spyConnectionConfig.isUseAuthentication()).thenReturn(true);

        StreamingNetInterface streamingNetInterface = mock(StreamingNetInterface.class);
        when(commo.addStreamingInterface(
                eq(StreamingTransport.SSL),
                eq("ts1.example.com"),
                eq(8089),
                any(),
                eq(new byte[]{1, 2, 3}),
                eq(new byte[]{4, 5, 6}),
                eq("certPass"),
                eq("caCertPass"),
                eq("username"),
                eq("password"))).thenReturn(streamingNetInterface);

        StreamingNetInterface result = commonCommoManager.addStreamingConnection(spyConnectionConfig);

        assertThat(streamingNetInterface).isEqualTo(result);

        verify(commo, times(1)).addStreamingInterface(
                eq(StreamingTransport.SSL),
                eq("ts1.example.com"),
                eq(8089),
                any(),
                eq(new byte[]{1, 2, 3}),
                eq(new byte[]{4, 5, 6}),
                eq("certPass"),
                eq("caCertPass"),
                eq("username"),
                eq("password"));
    }

    @Test
    void testAddStreamingConnection_disabledConnection(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        commonCommoManager.commo = commo;
        StreamingConnectionConfiguration connectionConfig = new StreamingConnectionConfiguration(UUID.randomUUID());
        connectionConfig.setEnabled(false);

        // when the connection is disabled the method should return null and not call commo.addStreamingInterface
        StreamingNetInterface result = commonCommoManager.addStreamingConnection(connectionConfig);
        assertThat(result).isNull();

        verify(commo, never()).addStreamingInterface(
                eq(StreamingTransport.SSL),
                eq("ts1.example.com"),
                eq(8089),
                any(),
                eq(new byte[]{1, 2, 3}),
                eq(new byte[]{4, 5, 6}),
                eq("certPass"),
                eq("caCertPass"),
                eq("username"),
                eq("password"));
    }

    @SuppressLogging(CommonCommoManager.class)
    @Test
    void whenAddStreamingConnection_sslButMissingCredentials_throws()
    {
        StreamingConnectionConfiguration connectionConfig = getStreamingConnectionConfigurationHelper("", "");
        var spyConnectionConfig = Mockito.spy(connectionConfig);

        // Mocking all authentication but not passwords. so it does throw exception for that
        when(spyConnectionConfig.getCertificateBytes()).thenReturn(new byte[]{1, 2, 3});
        when(spyConnectionConfig.getCertificateAuthorityBytes()).thenReturn(new byte[]{4, 5, 6});
        when(spyConnectionConfig.getCertificatePassword()).thenReturn("certPass");
        when(spyConnectionConfig.getCertificateAuthorityPassword()).thenReturn("caCertPass");

        String passwordErrorMessage = "Cannot create SSL connection using authentication if username and/or password is missing.";
        assertThatIOException().isThrownBy(() -> commonCommoManager.addStreamingConnection(spyConnectionConfig))
                .withMessage(passwordErrorMessage);
    }

    @SuppressLogging(CommonCommoManager.class)
    @Test
    void whenAddStreamingConnection_sslMissingCerts_throws()
    {
        StreamingConnectionConfiguration connectionConfig = getStreamingConnectionConfigurationHelper("username", "password");

        // don't mock certs so will throw exception
        String errorMessage = "Cannot create SSL connection if the CA cert, Client cert, or their passwords is missing.";
        assertThatIOException().isThrownBy(() -> commonCommoManager.addStreamingConnection(connectionConfig)).withMessage(errorMessage);
    }

    @Test
    void whenAddStreamingConnection_mockCommoThrows(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        StreamingConnectionConfiguration connectionConfig = getStreamingConnectionConfigurationHelper("username", "password");
        var spyConnectionConfig = Mockito.spy(connectionConfig);
        commonCommoManager.commo = commo;

        // this time simulate an error from within commo's method instead of commonCommoManager.
        String errorMessage = "Connection error";
        when(commo.addStreamingInterface(any(), anyString(), anyInt(), any(), any(), any(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new CommoException(errorMessage));

        // Mocking all authentication so it does not throw exception
        when(spyConnectionConfig.getCertificateBytes()).thenReturn(new byte[]{1, 2, 3});
        when(spyConnectionConfig.getCertificateAuthorityBytes()).thenReturn(new byte[]{4, 5, 6});
        when(spyConnectionConfig.getCertificatePassword()).thenReturn("certPass");
        when(spyConnectionConfig.getCertificateAuthorityPassword()).thenReturn("caCertPass");
        when(spyConnectionConfig.isUseAuthentication()).thenReturn(true);

        assertThatIOException().isThrownBy(() -> commonCommoManager.addStreamingConnection(spyConnectionConfig))
                .withMessage("Failed to connect to " + connectionConfig.getUri().getHost() + ": " + errorMessage);
    }

    @Test
    void testRemoveStreamingConnection_successful(@Mocked(allowCaching = false) Commo commo,
                                                  @Mocked(allowCaching = false) StreamingNetInterface streamingInterface)
    {
        commonCommoManager.commo = commo;

        // happy path- commo calls its own method which we should just mock
        doNothing().when(commo).removeStreamingInterface(streamingInterface);

        boolean result = commonCommoManager.removeStreamingConnection(streamingInterface);

        assertThat(result).isTrue();
        verify(commo, times(1)).removeStreamingInterface(streamingInterface);
    }

    @SuppressLogging(CommonCommoManager.class)
    @Test
    void whenCommoRemoveStreamingConnection_throws_commonCommoManagerReturnsFalse(@Mocked(allowCaching = false) Commo commo,
                                                                                  @Mocked(allowCaching = false) StreamingNetInterface streamingInterface)
    {
        commonCommoManager.commo = commo;

        // mock when commo's removeStreamingInterface throws an IllegalArgumentException
        doThrow(new IllegalArgumentException("Invalid argument")).when(commo).removeStreamingInterface(streamingInterface);

        boolean result = commonCommoManager.removeStreamingConnection(streamingInterface);

        // if exception, result of method is false
        assertThat(result).isFalse();
        verify(commo, times(1)).removeStreamingInterface(streamingInterface);
    }

    @Test
    void whenSendCotToMission_withUrl_commoSendsCotToServerMissionDest(@Mocked(allowCaching = false) Commo commo,
                                                                       @Mocked(allowCaching = false) ITakServerConnection connection)
            throws Exception
    {
        commonCommoManager.commo = commo;
        String cotMessage = "<cotMessage />";
        String missionName = "TestMission";
        URI uri = new URI("tcp://example.com:12345");

        //verifies the happy path where the connection URI is valid and commo.sendCoTToServerMissionDest is called and mocked.
        when(connection.getUri()).thenReturn(uri);
        doNothing().when(commo).sendCoTToServerMissionDest(anyString(), anyString(), anyString());

        commonCommoManager.sendCotToMission(connection, cotMessage, missionName);

        verify(commo).sendCoTToServerMissionDest("tcp:example.com:12345", missionName, cotMessage);
    }

    @Test
    void whenSendCotToMission_noScheme_commoThrows(
                                                   @Mocked(allowCaching = false) Commo commo,
                                                   @Mocked(allowCaching = false) ITakServerConnection connection)
            throws Exception
    {
        commonCommoManager.commo = commo;
        String cotMessage = "<cotMessage />";
        String missionName = "TestMission";

        // mock commo.sendCoTToServerMissionDest to throw a CommoException
        doThrow(new CommoException("URI scheme is missing")).when(commo).sendCoTToServerMissionDest(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> commonCommoManager.sendCotToMission(connection, cotMessage, missionName))
                .isInstanceOf(CommoException.class)
                .hasMessage("URI scheme is missing");
    }

    @Test
    void whenCommManager_broadcastCoT_commoBroadcastCoT(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        commonCommoManager.commo = commo;
        String cotMessage = "<cotMessage />";
        CoTSendMethod sendMethod = CoTSendMethod.TAK_SERVER;

        // happy path can mock out commo's call
        doNothing().when(commo).broadcastCoT(anyString(), any(CoTSendMethod.class));

        commonCommoManager.broadcastCoT(cotMessage, sendMethod);

        verify(commo).broadcastCoT(cotMessage, sendMethod);
    }

    @Test
    void whenCommoBroadcastCotThrows_commonCommoManager_throws(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        commonCommoManager.commo = commo;
        String cotMessage = "<cotMessage />";
        CoTSendMethod sendMethod = CoTSendMethod.TAK_SERVER;

        // mock when broadcastCot then commo.broadcastCoT throws a CommoException.
        doThrow(new CommoException("Broadcast failed")).when(commo).broadcastCoT(anyString(), any(CoTSendMethod.class));

        // in this case, exception not caught by inner method so rises up to here.
        assertThatThrownBy(() -> commonCommoManager.broadcastCoT(cotMessage, sendMethod))
                .isInstanceOf(CommoException.class)
                .hasMessage("Broadcast failed");
        verify(commo).broadcastCoT(cotMessage, sendMethod);
    }

    @Test
    @SuppressWarnings({"JdkObsolete", "unchecked"})
    void whenSendCoT_listOfContacts_commoSendCot(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        commonCommoManager.commo = commo;
        Vector<Contact> contacts = new Vector<>();
        Contact contact1 = mock(Contact.class);
        Contact contact2 = mock(Contact.class);
        contacts.add(contact1);
        contacts.add(contact2);
        String cotMessage = "<cotMessage />";
        CoTSendMethod sendMethod = CoTSendMethod.TAK_SERVER;

        doNothing().when(commo).sendCoT(any(Vector.class), anyString(), any(CoTSendMethod.class));

        commonCommoManager.sendCoT(contacts, cotMessage, sendMethod);

        verify(commo).sendCoT(contacts, cotMessage, sendMethod);
    }

    @Test
    @SuppressWarnings({"JdkObsolete", "unchecked"})
    void whenSendCoT_emptyListOfContacts_throws(@Mocked(allowCaching = false) Commo commo) throws CommoException
    {
        commonCommoManager.commo = commo;
        String cotMessage = "<cotMessage />";
        CoTSendMethod sendMethod = CoTSendMethod.TAK_SERVER;

        // when no contacts are provided, should throw an IllegalArgumentException.
        assertThatIllegalArgumentException().isThrownBy(() -> commonCommoManager.sendCoT(new Vector<>(), cotMessage, sendMethod))
                .withMessage("'contacts' must not be empty");
        verify(commo, never()).sendCoT(any(Vector.class), anyString(), any(CoTSendMethod.class));
    }

    @Test
    void whenSendMissionPackage_commoCallsPackageInitAndPackageSend(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        commonCommoManager.commo = commo;
        List<Contact> contacts = new ArrayList<>();
        Contact contact1 = mock(Contact.class);
        Contact contact2 = mock(Contact.class);
        contacts.add(contact1);
        contacts.add(contact2);
        File missionPackageFile = mock(File.class);
        when(missionPackageFile.getName()).thenReturn("package.zip");

        List<Contact> offlineContacts = new ArrayList<>(contacts);
        int transactionId = 123;

        when(commo.sendMissionPackageInit(offlineContacts, missionPackageFile, "package.zip", "package.zip", null, null, null)).thenReturn(transactionId);
        doNothing().when(commo).startMissionPackageSend(transactionId);

        commonCommoManager.sendMissionPackage(contacts, missionPackageFile);

        // when sendMissionPackage, commo.sendMissionPackageInit and commo.startMissionPackageSend are called successfully.
        verify(commo).sendMissionPackageInit(offlineContacts, missionPackageFile, "package.zip", "package.zip", null, null, null);
        verify(commo).startMissionPackageSend(transactionId);
    }

    @Test
    void testSendMissionPackage_withSingleContact_sendMissionPackage(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        // similar test but sends one contact instead of list, to use other sendMissionPackage method, also uses long file name and numbers.
        commonCommoManager.commo = commo;
        Contact contact = mock(Contact.class);
        File missionPackageFile = mock(File.class);
        String fileName = UUID.randomUUID() + ".zip";
        when(missionPackageFile.getName()).thenReturn(fileName);

        List<Contact> offlineContacts = new ArrayList<>(List.of(contact));
        int transactionId = 2147483647;

        when(commo.sendMissionPackageInit(offlineContacts, missionPackageFile, fileName, fileName, null, null, null)).thenReturn(transactionId);
        doNothing().when(commo).startMissionPackageSend(transactionId);

        commonCommoManager.sendMissionPackage(contact, missionPackageFile);

        verify(commo).sendMissionPackageInit(offlineContacts, missionPackageFile, fileName, fileName, null, null, null);
        verify(commo).startMissionPackageSend(transactionId);
    }

    @Test
    void whenCommoSendMissionPackage_throwsException(@Mocked(allowCaching = false) Commo commo) throws Exception
    {
        commonCommoManager.commo = commo;
        List<Contact> contacts = new ArrayList<>();
        Contact contact1 = mock(Contact.class);
        Contact contact2 = mock(Contact.class);
        contacts.add(contact1);
        contacts.add(contact2);
        File missionPackageFile = mock(File.class);
        when(missionPackageFile.getName()).thenReturn("package.zip");

        List<Contact> offlineContacts = new ArrayList<>(contacts);

        // mock when commo.sendMissionPackageInit throws a CommoException.
        when(commo.sendMissionPackageInit(offlineContacts, missionPackageFile, "package.zip", "package.zip", null, null, null)).thenThrow(new CommoException("Initialization failed"));

        // exception not caught by sendMissionPackageInit, rises here.
        assertThatThrownBy(() -> commonCommoManager.sendMissionPackage(contacts, missionPackageFile))
                .isInstanceOf(CommoException.class)
                .hasMessage("Initialization failed");
        verify(commo).sendMissionPackageInit(offlineContacts, missionPackageFile, "package.zip", "package.zip", null, null, null);
        verify(commo, never()).startMissionPackageSend(anyInt());
    }

    @Test
    void whenCotMessageReceived_InvalidEvent_noCotEventReceived(@Mocked() CotEventReceiver listener,
                                                                @Mocked(methodBehavior = MethodBehavior.RETURN_REAL_METHOD, allowCaching = false) CotEvent cotEvent)
    {
        String cotMessage = "<cotMessage />";
        String remoteEndpointId = "remoteEndpoint";
        commonCommoManager.cotEventListeners = new CopyOnWriteArrayList<>();
        commonCommoManager.addCotEventListener(listener);

        when(cotEvent.isValid()).thenReturn(false);

        commonCommoManager.cotMessageReceived(cotMessage, remoteEndpointId);

        verify(listener, never()).cotEventReceived(cotEvent);
    }

    /** @since 5.3 */
    @Test
    void whenCotMessageSent_listenersAreNotified(@Mocked(allowCaching = false) Commo commo,
                                                 @Mocked(allowCaching = false) ICotSendListener listener,
                                                 @Mocked Contact contact,
                                                 @Mocked ITakServerConnection connection)
            throws CommoException
    {
        commonCommoManager.commo = commo;
        commonCommoManager.cotSendListeners = new CopyOnWriteArrayList<>();
        commonCommoManager.addCotSendListener(listener);
        assertThat(commonCommoManager.cotSendListeners).hasSize(1).containsOnly(listener);

        final String cotMessage = "a CoT message";
        commonCommoManager.broadcastCoT(cotMessage);
        verify(listener).cotMessageSent(cotMessage);

        final String cm2 = cotMessage + "2";
        commonCommoManager.broadcastCoT(cm2, CoTSendMethod.ANY);
        verify(listener).cotMessageSent(cm2);

        final String cm3 = cotMessage + "3";
        final var contacts = new Vector<Contact>();
        contacts.add(contact);
        commonCommoManager.sendCoT(contacts, cm3, CoTSendMethod.ANY);
        verify(listener).cotMessageSent(cotMessage);

        final String cm4 = cotMessage + "4";
        when(connection.getUri()).thenReturn(URI.create("tcp://example.com:12345"));
        commonCommoManager.sendCotToMission(connection, cm4, "mission");
        verify(listener).cotMessageSent(cm4);
    }

    /** @since 5.8 */
    @Test
    void whenCotMessageSent_unregisteredListenersAreNotNotified(@Mocked(allowCaching = false) Commo commo,
                                                                @Mocked(allowCaching = false) ICotSendListener listener,
                                                                @Mocked Contact contact,
                                                                @Mocked ITakServerConnection connection)
            throws CommoException
    {
        commonCommoManager.commo = commo;
        commonCommoManager.cotSendListeners = new CopyOnWriteArrayList<>();
        commonCommoManager.addCotSendListener(listener);
        commonCommoManager.removeCotSendListener(listener);
        assertThat(commonCommoManager.cotSendListeners).isEmpty();

        final String cotMessage = "a CoT message";
        commonCommoManager.broadcastCoT(cotMessage);
        verify(listener, never()).cotMessageSent(any());

        commonCommoManager.broadcastCoT(cotMessage, CoTSendMethod.ANY);
        verify(listener, never()).cotMessageSent(any());

        final var contacts = new Vector<Contact>();
        contacts.add(contact);
        commonCommoManager.sendCoT(contacts, cotMessage, CoTSendMethod.ANY);
        verify(listener, never()).cotMessageSent(any());

        when(connection.getUri()).thenReturn(URI.create("tcp://example.com:12345"));
        commonCommoManager.sendCotToMission(connection, cotMessage, "mission");
        verify(listener, never()).cotMessageSent(any());
    }

    /** @since 5.8 */
    @Test
    void whenCotMessageSent_interceptorsAreCalled(@Mocked(allowCaching = false) Commo commo,
                                                  @Mocked(allowCaching = false) ICotEventInterceptor interceptor,
                                                  @Mocked Contact contact,
                                                  @Mocked ITakServerConnection connection)
            throws CommoException, IOException
    {
        commonCommoManager.commo = commo;
        commonCommoManager.cotEventInterceptors = new CopyOnWriteArrayList<>();
        commonCommoManager.addCotEventInterceptor(interceptor);
        assertThat(commonCommoManager.cotEventInterceptors).hasSize(1).containsOnly(interceptor);

        final String cotMessage = getResourceAsString("/cot/marker.cot");

        commonCommoManager.broadcastCoT(cotMessage);
        verify(interceptor).onSend(any(CotEvent.class));
        Mockito.reset(interceptor);

        commonCommoManager.broadcastCoT(cotMessage, CoTSendMethod.ANY);
        verify(interceptor).onSend(any(CotEvent.class));
        Mockito.reset(interceptor);

        final var contacts = new Vector<Contact>();
        contacts.add(contact);
        commonCommoManager.sendCoT(contacts, cotMessage, CoTSendMethod.ANY);
        verify(interceptor).onSend(any(CotEvent.class));
        Mockito.reset(interceptor);

        when(connection.getUri()).thenReturn(URI.create("tcp://example.com:12345"));
        commonCommoManager.sendCotToMission(connection, cotMessage, "mission");
        verify(interceptor).onSend(any(CotEvent.class));
        Mockito.reset(interceptor);
    }

    @Test
    void whenCotMessageReceived_cotInterceptorsAreCalled(@Mocked(allowCaching = false) Commo commo,
                                                         @Mocked(allowCaching = false) ICotEventInterceptor interceptor)
            throws IOException
    {
        commonCommoManager.commo = commo;
        commonCommoManager.cotEventListeners = new ArrayList<>();
        commonCommoManager.addCotEventInterceptor(interceptor);

        final String cotMessage = getResourceAsString("/cot/marker.cot");

        commonCommoManager.cotMessageReceived(cotMessage, "endpoint");
        verify(interceptor).onReceive(any(CotEvent.class));
    }

    @Test
    void updateUdpInputs_addsAndRemovesInterfaces(@Mocked(allowCaching = false) Commo commo,
                                                  @Mocked(allowCaching = false) INetworkDeviceManager networkDeviceManager,
                                                  @Mocked(allowCaching = false) NetworkDevice networkDevice)
            throws CommoException
    {
        final var prefNode = PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE.node().node("Default");

        initCommonCommoManagerForNetworkPrefs(commo, networkDeviceManager, networkDevice);
        commonCommoManager.inboundUdpInterfaces = new HashMap<>();

        //noinspection DataFlowIssue
        prefNode.put("enabled", true);

        commonCommoManager.updateUdpInputs();

        assertThat(commonCommoManager.inboundUdpInterfaces).containsKey(networkDevice);

        int enabledInterfaceCount = getEnabledInterfaceCount(PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE, "udp");
        verify(commo, times(enabledInterfaceCount)).addInboundInterface((byte[]) any(), anyInt(), any(), anyBoolean());
        assertThat(commonCommoManager.inboundUdpInterfaces.get(networkDevice)).hasSize(enabledInterfaceCount);

        prefNode.put("enabled", false);

        commonCommoManager.updateUdpInputs();

        verify(commo).removeInboundInterface(any());
        assertThat(commonCommoManager.inboundUdpInterfaces.get(networkDevice)).hasSize(enabledInterfaceCount - 1);

        prefNode.put("enabled", true);
    }

    @Test
    void updateUdpOutputs_addsAndRemovesInterfaces(@Mocked(allowCaching = false) Commo commo,
                                                   @Mocked(allowCaching = false) INetworkDeviceManager networkDeviceManager,
                                                   @Mocked(allowCaching = false) NetworkDevice networkDevice)
            throws CommoException
    {
        final var prefNode = PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE.node().node("Chat Output");

        initCommonCommoManagerForNetworkPrefs(commo, networkDeviceManager, networkDevice);
        commonCommoManager.outboundUdpInterfaces = new HashMap<>();

        //noinspection DataFlowIssue
        prefNode.put("enabled", true);

        commonCommoManager.updateUdpOutputs();

        assertThat(commonCommoManager.outboundUdpInterfaces).containsKey(networkDevice);

        int enabledInterfaceCount = getEnabledInterfaceCount(PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE, "udp");
        verify(commo, times(enabledInterfaceCount)).addBroadcastInterface((byte[]) any(), any(), anyString(), anyInt());
        assertThat(commonCommoManager.outboundUdpInterfaces.get(networkDevice)).hasSize(enabledInterfaceCount);

        prefNode.put("enabled", false);

        commonCommoManager.updateUdpOutputs();

        verify(commo).removeBroadcastInterface(any());
        assertThat(commonCommoManager.outboundUdpInterfaces.get(networkDevice)).hasSize(enabledInterfaceCount - 1);

        prefNode.put("enabled", true);
    }

    @Test
    void updateTcpInputs_addsAndRemovesInterfaces(@Mocked(allowCaching = false) Commo commo,
                                                  @Mocked(allowCaching = false) INetworkDeviceManager networkDeviceManager,
                                                  @Mocked(allowCaching = false) NetworkDevice networkDevice)
            throws CommoException
    {
        final var prefNode = PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE.node().node("Default TCP");

        initCommonCommoManagerForNetworkPrefs(commo, networkDeviceManager, networkDevice);
        commonCommoManager.inboundTcpInterfaces = new HashMap<>();

        //noinspection DataFlowIssue
        prefNode.put("enabled", true);

        commonCommoManager.updateTcpInputs();

        int enabledInterfaceCount = getEnabledInterfaceCount(PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE, "tcp");
        verify(commo, times(enabledInterfaceCount)).addTcpInboundInterface(anyInt());
        assertThat(commonCommoManager.inboundTcpInterfaces).hasSize(enabledInterfaceCount);

        prefNode.put("enabled", false);

        commonCommoManager.updateTcpInputs();

        verify(commo).removeTcpInboundInterface(any());
        assertThat(commonCommoManager.inboundTcpInterfaces).hasSize(enabledInterfaceCount - 1);

        prefNode.put("enabled", true);
    }

    private int getEnabledInterfaceCount(PreferenceEntry preferenceEntry, String scheme)
    {
        return (int) preferenceEntry.node().childrenToMap(ConnectionConfiguration::new)
                .values()
                .stream()
                .filter(config -> config.isEnabled() && scheme.equals(config.getUri().getScheme()))
                .count();
    }

    private static StreamingConnectionConfiguration getStreamingConnectionConfigurationHelper(String username, String password)
    {
        StreamingConnectionConfiguration connectionConfig = new StreamingConnectionConfiguration(UUID.randomUUID());
        connectionConfig.setDisplayName("Connection 1");
        connectionConfig.setUri("ssl", "ts1.example.com", 8089);
        connectionConfig.setEnabled(true);
        connectionConfig.setUseAuthentication(true);
        connectionConfig.setUsername(username);
        connectionConfig.setPassword(password);
        return connectionConfig;
    }

    private void initCommonCommoManagerForNetworkPrefs(Commo commo, INetworkDeviceManager networkDeviceManager,
                                                       NetworkDevice networkEnabledDevice)
    {
        commonCommoManager.commo = commo;
        commonCommoManager.networkDeviceManager = networkDeviceManager;
        when(networkDeviceManager.getNetworkDevices()).thenReturn(List.of(networkEnabledDevice));
    }
}
