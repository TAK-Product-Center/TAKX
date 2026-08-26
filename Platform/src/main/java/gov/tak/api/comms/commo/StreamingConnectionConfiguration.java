package gov.tak.api.comms.commo;

import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import gov.tak.api.comms.security.ISslCertificateManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Class to describe a streaming connection configuration to be used with the Common Commo library to send CoT messages
 * to other TAK clients connected to the same streaming connection, usually a TAK Server.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class StreamingConnectionConfiguration extends ConnectionConfiguration
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CERT_FILE_NAME_KEY = "certFileName";
    private static final String CA_CERT_FILE_NAME_KEY = "caCertFileName";
    private static final String USERNAME_KEY = "username";
    private static final String USE_AUTHENTICATION_KEY = "useAuthentication";
    private static final String ENROLL_FOR_CLIENT_CERT_KEY = "enrollForClientCert";
    private static final String USE_TOKEN_AUTHENTICATION_KEY = "useTokenAuthentication";
    private static final String CREATION_TIMESTAMP_MS_KEY = "creationTimestampMs";

    private static final String CA_CERT_CREDENTIALS_TYPE = AtakAuthenticationCredentials.TYPE_caPassword;
    private static final String CERT_CREDENTIALS_TYPE = AtakAuthenticationCredentials.TYPE_clientPassword;
    private static final String USER_CREDENTIALS_TYPE = AtakAuthenticationCredentials.TYPE_COT_SERVICE;

    // No longer used, but maintained for migrating passwords from preferences into secure storage
    private static final String CA_CERT_PASSWORD_KEY = "caCertPassword";
    private static final String CERT_PASSWORD_KEY = "certPassword";
    private static final String PASSWORD_KEY = "password";

    private static final byte[] EMPTY_BYTES = new byte[0];
    private final UUID id;

    private String certFileName;
    private byte[] certBytes = EMPTY_BYTES;
    private String caCertFileName;
    private byte[] caCertBytes = EMPTY_BYTES;
    private String username;
    private boolean useAuthentication;
    private boolean enrollForClientCert;
    private boolean useTokenAuthentication;
    private long creationTimestampMs;

    @Inject
    private ISslCertificateManager certificateManager;

    /**
     * Constructor for creating a new, unconfigured instance.
     *
     * @param id Unique id for the newly created node
     */
    public StreamingConnectionConfiguration(@Nonnull UUID id)
    {
        super(PreferenceEntry.COMMON_COMMO_TAK_SERVERS_NODE.node().node(id.toString()));
        this.id = id;
        this.creationTimestampMs = System.currentTimeMillis();
        put(CREATION_TIMESTAMP_MS_KEY, creationTimestampMs);
    }

    /**
     * Constructor for pulling configuration from preferences.
     *
     * @param node Preferences node from which to load our configuration
     */
    public StreamingConnectionConfiguration(@Nonnull PreferencesNode node)
    {
        super(node);

        id = UUID.fromString(name());
        certFileName = get(CERT_FILE_NAME_KEY, "");
        caCertFileName = get(CA_CERT_FILE_NAME_KEY, "");
        username = get(USERNAME_KEY, "");
        enrollForClientCert = getBoolean(ENROLL_FOR_CLIENT_CERT_KEY, false);
        useAuthentication = getBoolean(USE_AUTHENTICATION_KEY, false);
        useTokenAuthentication = getBoolean(USE_TOKEN_AUTHENTICATION_KEY, false);
        creationTimestampMs = getLong(CREATION_TIMESTAMP_MS_KEY, System.currentTimeMillis());

        // Migrate old password storage into secure storage
        migratePassword(CA_CERT_PASSWORD_KEY, CA_CERT_CREDENTIALS_TYPE);
        migratePassword(CERT_PASSWORD_KEY, CERT_CREDENTIALS_TYPE);
        migratePassword(PASSWORD_KEY, USER_CREDENTIALS_TYPE);
    }

    @PostConstruct
    private void postConstruct()
    {
        certBytes = getCertBytesFromFile(getOrCopyCertFile(certFileName));
        caCertBytes = getCertBytesFromFile(getOrCopyCertFile(caCertFileName));
    }

    /**
     * Check if a given password is stored in preferences and, if so, migrate it into our secure/encrypted storage.
     * The value is removed from the preferences node if found.
     *
     * @param key Preferences key
     * @param credentialsType The type to store the password under
     * @since 5.0
     */
    private void migratePassword(String key, String credentialsType)
    {
        final String password = get(key, null);
        if (password != null)
        {
            AtakAuthenticationDatabase.saveCredentials(credentialsType, id.toString(), username, password, false);
            remove(key);
        }
    }

    /**
     * Attempts to retrieve the cert bytes from the provided file.
     *
     * @param certFile The cert file
     * @return The bytes from the file, or an empty array if the file doesn't exist or an error occurred.
     */
    @Nonnull
    private byte[] getCertBytesFromFile(@Nullable File certFile)
    {
        if (certFile != null && certFile.exists())
        {
            try
            {
                return Files.readAllBytes(certFile.toPath());
            } catch (Exception e)
            {
                logger.warn("Error reading from cert file {}", certFile, e);
            }
        }

        return EMPTY_BYTES;
    }

    /**
     * Retrieves the cert file from the certificate store or copies it to the store if a valid cert file name was
     * provided and it isn't already in the VFS certificate store.
     *
     * @param certFileName The name of the cert file to get or copy
     * @return The cert file, or {@code null} if the provided cert name wasn't valid
     */
    @Nullable
    private File getOrCopyCertFile(@Nullable String certFileName)
    {
        if (!StringUtils.isEmpty(certFileName))
        {
            if (certificateManager.certExists(certFileName))
            {
                return certificateManager.getCertFile(certFileName);
            } else
            {
                try
                {
                    return certificateManager.copyCert(certFileName);
                } catch (IOException e)
                {
                    logger.warn("Error copying cert to store", e);
                }
            }
        }

        return null;
    }

    /**
     * @return The unique id used as a key for this preferences node.
     */
    @Nonnull
    public UUID getId()
    {
        return id;
    }

    /**
     * @return The byte array data for a certificate file.
     */
    @Nonnull
    public byte[] getCertificateBytes()
    {
        return certBytes.clone();
    }

    /**
     * @return The file name for a certificate file, relative to the certificate store in the VFS.
     */
    @Nullable
    public String getCertificateFileName()
    {
        return certFileName;
    }

    /**
     * @return The certificate password
     */
    @Nullable
    public String getCertificatePassword()
    {
        return getPasswordFromStorage(CERT_CREDENTIALS_TYPE);
    }

    /**
     * Set the client certificate file and password, adding the certificate to the certificate store if a valid cert was
     * provided and it wasn't in the store.
     *
     * @param certFileName name of certificate file, which should either be the absolute path to a cert external to the
     *                     certificate store or the file name relative to the VFS certificate store
     * @param certPassword password for certificate
     */
    public void setCertificate(@Nullable String certFileName, @Nullable String certPassword)
    {
        File certFile = getOrCopyCertFile(certFileName);
        certBytes = getCertBytesFromFile(certFile);

        this.certFileName = certFile == null ? null : certFile.getName();
        updatePreferencesNode(CERT_FILE_NAME_KEY, this.certFileName);

        updatePasswordInStorage(CERT_CREDENTIALS_TYPE, certPassword);
    }

    /**
     * @param credentialsType The type the password is stored under
     * @return The password of the given type for this connection
     * @since 5.0
     */
    @Nullable
    private String getPasswordFromStorage(String credentialsType)
    {
        final var credentials = AtakAuthenticationDatabase.getCredentials(credentialsType, id.toString());
        return credentials != null ? credentials.password : null;
    }

    /**
     * Save or delete the password under the given credentials type.  If {@code password} is {@code null}, then the
     * credential entry will be deleted.  Otherwise, the password will be stored under the given type.
     *
     * @param credentialsType The type to store the password under
     * @param password Password to process, {@code null} to delete the credentials entry
     * @since 5.0
     */
    private void updatePasswordInStorage(@Nonnull String credentialsType, @Nullable String password)
    {
        if (password != null)
        {
            AtakAuthenticationDatabase.saveCredentials(credentialsType, id.toString(), username, password, false);
        } else
        {
            AtakAuthenticationDatabase.delete(credentialsType, id.toString());
        }
    }

    /**
     * Updates the specified preference node by either setting or removing the key as appropriate.
     *
     * @param key   The preference key
     * @param value The preference value (may be {@code null} to remove)
     */
    private void updatePreferencesNode(@Nonnull String key, @Nullable String value)
    {
        if (value != null)
        {
            put(key, value);
        } else
        {
            remove(key);
        }
    }

    /**
     * @return The byte array data for a certificate authority file
     */
    @Nonnull
    public byte[] getCertificateAuthorityBytes()
    {
        return caCertBytes.clone();
    }

    /**
     * @return The file name for a certificate authority file, relative to the certificate store in the VFS.
     */
    @Nullable
    public String getCertificateAuthorityFileName()
    {
        return caCertFileName;
    }

    /**
     * @return The certificate authority password
     */
    @Nullable
    public String getCertificateAuthorityPassword()
    {
        return getPasswordFromStorage(CA_CERT_CREDENTIALS_TYPE);
    }

    /**
     * Set the certificate authority file and password, adding the certificate to the certificate store if a valid cert
     * was provided and it wasn't in the store.
     *
     * @param caCertFileName name of certificate authority file, which should either be the absolute path to a cert
     *                       external to the certificate store or the file name relative to the VFS certificate store
     * @param caCertPassword password for certificate authority
     */
    public void setCertificateAuthority(@Nullable String caCertFileName, @Nullable String caCertPassword)
    {
        File caCertFile = getOrCopyCertFile(caCertFileName);
        caCertBytes = getCertBytesFromFile(caCertFile);

        this.caCertFileName = caCertFile == null ? null : caCertFile.getName();
        updatePreferencesNode(CA_CERT_FILE_NAME_KEY, this.caCertFileName);

        updatePasswordInStorage(CA_CERT_CREDENTIALS_TYPE, caCertPassword);
    }

    /**
     * @return The username (may be {@code null} if this connection does not use authentication).
     */
    @Nullable
    public String getUsername()
    {
        return username;
    }

    /**
     * Sets the username (may be {@code null} if this connection does not use authentication).
     */
    public void setUsername(@Nullable String username)
    {
        this.username = username;
        updatePreferencesNode(USERNAME_KEY, username);
    }

    /**
     * @return The password (may be {@code null} if this connection does not use authentication).
     */
    @Nullable
    public String getPassword()
    {
        return getPasswordFromStorage(USER_CREDENTIALS_TYPE);
    }

    /**
     * Sets the password (may be {@code null} if this connection does not use authentication).
     */
    public void setPassword(@Nullable String password)
    {
        updatePasswordInStorage(USER_CREDENTIALS_TYPE, password);
    }

    /**
     * @return True if the connection utilizes LDAP authentication.
     */
    public boolean isUseAuthentication()
    {
        return useAuthentication;
    }

    /**
     * Sets whether the connection utilizes LDAP authentication.
     */
    public void setUseAuthentication(boolean useAuthentication)
    {
        put(USE_AUTHENTICATION_KEY, this.useAuthentication = useAuthentication);
    }

    /**
     * @return True if the connection should enroll for a client certificate.
     */
    public boolean isEnrollForClientCert()
    {
        return enrollForClientCert;
    }

    /**
     * Sets whether the connection should enroll for a client certificate.
     */
    public void setEnrollForClientCert(boolean enrollForClientCert)
    {
        put(ENROLL_FOR_CLIENT_CERT_KEY, this.enrollForClientCert = enrollForClientCert);
    }

    /**
     * @return {@code true} if an OAuth token should be used for authentication when enrolling for a client cert.
     * @since 3.5.0
     */
    public boolean isUseTokenAuthentication()
    {
        return useTokenAuthentication;
    }

    /**
     * Sets whether an OAuth token should be used for authentication when enrolling for a client certificate. This
     * value is used if (and only if) {@code isEnrollForClientCert} is also set to true.
     * @param useTokenAuthentication {@code true} if a token should be used for cert enrollment. {@code false} if a token
     *                                           should not be used for cert enrollment.
     * @since 3.5.0
     */
    public void setUseTokenAuthentication(boolean useTokenAuthentication)
    {
        this.useTokenAuthentication = useTokenAuthentication;
        put(USE_TOKEN_AUTHENTICATION_KEY, this.useTokenAuthentication);
    }

    /**
     * @return The timestamp in epoch millis that the connection was created at.
     * @since 5.5.1
     */
    public long getCreationTimestampMs()
    {
        return creationTimestampMs;
    }
}
