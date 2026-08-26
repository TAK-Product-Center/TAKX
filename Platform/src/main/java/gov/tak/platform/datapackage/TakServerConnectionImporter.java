package gov.tak.platform.datapackage;

import com.atakmap.comms.NetConnectString;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.security.ISslCertificateManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.util.TakPreferenceKeys;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.connection.IDelegatedConnectionAuthentication;
import gov.takx.commons.swing.AuthenticationDialog;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Process Data Package contents and create/update TAK Server connections.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class TakServerConnectionImporter
{
    private static final String CA_LOCATION = "caLocation";
    private static final String CERTIFICATE_LOCATION = "certificateLocation";
    private static final String CONNECT_STRING = "connectString";
    private static final String DESCRIPTION = "description";
    private static final String ENABLED = "enabled";
    private static final String ENROLL_FOR_CERTIFICATE = "enrollForCertificateWithTrust";

    @Inject
    private ISslCertificateManager certificateManager;

    @Inject
    private ITakServerConnectionManager connectionManager;

    private final PreferencesNode takPreferencesNode = PreferenceEntry.TAK_PREFERENCES_NODE.node();

    // Map of entries from the "cot_streams" preference
    private final Map<String, String> streamsPreferenceMap;

    // Map from cert name in the package to where it was placed by the certificate manager
    private final Map<String, File> certificateMap;

    /**
     * @param cotStreamsPreference "cot_streams" preference read from the package
     * @param certificateMap Map from cert name in the package to where it was placed by the certificate manager
     */
    TakServerConnectionImporter(@Nonnull PreferencesReader.Preference cotStreamsPreference, @Nonnull Map<String, File> certificateMap)
    {
        streamsPreferenceMap = cotStreamsPreference.toMap();
        this.certificateMap = Map.copyOf(certificateMap);
    }

    /**
     * Process all the TAK server connections present in our configured preferences.
     *
     * @throws IOException If processing fails
     */
    public void processServerConnections() throws IOException
    {
        final int count = ParseUtils.parseInt(streamsPreferenceMap.get("count"), 0);

        for (int i = 0; i < count; i++) processServerConnection(i);
    }

    /**
     * Process a single server connection.
     *
     * @param instance The instance number we are processing
     * @throws IOException If processing fails
     */
    private void processServerConnection(int instance) throws IOException
    {
        final String name = streamsPreferenceMap.get(DESCRIPTION + instance);
        if (name == null) throw new IOException("Missing 'description' in configuration");

        final String connectString = streamsPreferenceMap.get(CONNECT_STRING + instance);
        if (connectString == null) throw new IOException("Missing 'connectString' in configuration");

        final NetConnectString netConnectString = NetConnectString.fromString(connectString);
        if (netConnectString == null) throw new IOException("Couldn't parse connection details: '" + connectString + "'");

        final String proto = netConnectString.getProto();
        if (proto == null) throw new IOException("Missing protocol in connection details: '" + connectString + "'");
        if (!"ssl".equals(proto)) throw new IOException("Only SSL connections are supported: '" + connectString + "'");

        addOrUpdateConnection(instance, name, netConnectString);
    }

    /**
     * Create a new, or update an existing, TAK Server connection.
     *
     * @param instance The instance number we are processing
     * @param name The name to set on the connection
     * @param netConnectString Connection details
     * @throws IOException If processing fails
     * @since 3.4
     */
    private void addOrUpdateConnection(int instance, String name, NetConnectString netConnectString)
            throws IOException
    {
        // Get an existing connection, if it exists
        final ITakServerConnection connection = connectionManager.findConnectionByName(name);
        final UUID id = connection == null ? UUIDUtils.generateType1UUID() : connection.getId();

        final var configuration = new StreamingConnectionConfiguration(id);
        final boolean useEnrollment = Boolean.parseBoolean(streamsPreferenceMap.get(ENROLL_FOR_CERTIFICATE + instance));
        final boolean enabled = Boolean.parseBoolean(streamsPreferenceMap.get(ENABLED + instance));
        final String proto = netConnectString.getProto();
        final String host = netConnectString.getHost();
        final int port = netConnectString.getPort();

        configuration.setDisplayName(name);
        configuration.setEnabled(enabled);
        configuration.setUri(proto, host, port);

        try
        {
            if (useEnrollment)
            {
                configureEnrollment(configuration);
            } else
            {
                configureCerts(configuration, instance);
            }
        } catch (IOException e)
        {
            // If we created a new connection configuration above, then remove the node on failure
            if (connection == null) PreferenceEntry.COMMON_COMMO_TAK_SERVERS_NODE.node().removeChildNode(id.toString());

            throw e;
        }

        // Add or update the connection with this configuration
        if (connection == null)
        {
            connectionManager.addTakServerConnection(configuration);
        } else
        {
            connectionManager.updateTakServerConnection(connection.getId(), configuration);
        }
    }

    /**
     * Configure the connection for client certificate enrollment.  This requires prompting the user for their UN/PW.
     *
     * @param configuration Configuration to update
     * @throws IOException If processing fails
     * @since 3.4
     */
    private void configureEnrollment(StreamingConnectionConfiguration configuration) throws IOException
    {
        final CredentialsHandler credentialsHandler = new CredentialsHandler(configuration.getDisplayName());
        final AuthenticationDialog authenticationDialog = new AuthenticationDialog(credentialsHandler);
        authenticationDialog.setVisible(true);

        if (!authenticationDialog.isOkay()) throw new IOException("User canceled enrollment");

        configuration.setEnrollForClientCert(true);
        configuration.setUseAuthentication(true);
        configuration.setUsername(credentialsHandler.username);
        configuration.setPassword(credentialsHandler.password);
    }

    /**
     * Configure the CA and client certs on the configuration.
     *
     * @param configuration Configuration to update
     * @param instance      The instance number we are processing
     * @throws IOException If processing fails
     */
    private void configureCerts(StreamingConnectionConfiguration configuration, int instance) throws IOException
    {
        final File caCertFile = getCertFile(CA_LOCATION, instance);
        final String caCertPassword = getPrefWithFallback(TakPreferenceKeys.CA_PASSWORD, instance);
        final File clientCertFile = getCertFile(CERTIFICATE_LOCATION, instance);
        final String clientCertPassword = getPrefWithFallback(TakPreferenceKeys.CLIENT_PASSWORD, instance);

        if (!caCertFile.exists()) throw new IOException("CA Cert not found: " + caCertFile);
        if (!clientCertFile.exists()) throw new IOException("Client Cert not found: " + clientCertFile);

        configuration.setCertificate(clientCertFile.getName(), clientCertPassword);
        configuration.setCertificateAuthority(caCertFile.getName(), caCertPassword);
        configuration.setUseAuthentication(false);
    }

    /**
     * Find the certificate file according to the preferences.
     *
     * @param prefKey  Base key of preference to obtain
     * @param instance The instance number we are processing
     * @return Certificate file
     */
    private File getCertFile(String prefKey, int instance) throws IOException
    {
        final String certPref = getPrefWithFallback(prefKey, instance);

        if (certPref == null) throw new IOException("Invalid configuration; Missing preference '" + prefKey + "'");

        final String certName = FilenameUtils.getName(certPref);

        return certificateMap.containsKey(certName)
                ? certificateMap.get(certName)
                : certificateManager.getCertFile(certName);
    }

    /**
     * Get the preference entry value from the given map (using the instance number).  If it doesn't exist,
     * fall back to looking in the APP preferences using the base preference key name.
     *
     * @param prefKey  Base key of preference to obtain
     * @param instance The instance number we are processing
     * @return Preference entry value, if found
     */
    private String getPrefWithFallback(String prefKey, int instance)
    {
        // If the stream preferences contain the key, we're done, otherwise, look in app preferences
        final String value = streamsPreferenceMap.get(prefKey + instance);
        return value != null ? value : takPreferencesNode.get(prefKey, null);
    }

    /**
     * Handler to receive user-entered credentials.
     * @since 3.4
     */
    private static class CredentialsHandler implements IDelegatedConnectionAuthentication
    {
        private final String serverName;
        private String username = "";
        private String password = "";

        private CredentialsHandler(String serverName)
        {
            this.serverName = serverName;
        }

        @Nullable
        @Override
        public String getMessage()
        {
            return "Please provide your credentials for TAK Server: " + serverName;
        }

        @Override
        public void setUsername(@Nonnull String username)
        {
            this.username = username;
        }

        @Override
        public void setPassword(@Nonnull char[] password)
        {
            this.password = String.valueOf(password);
        }

        @Override
        public String validateContents()
        {
            if (username.isBlank()) return "You must provide a user name";
            if (password.isBlank()) return "You must provide a password";
            return null;
        }
    }
}
