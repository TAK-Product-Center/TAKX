package gov.tak.core.comms.security;

import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.EnrollmentIO;
import com.atakmap.commoncommo.EnrollmentIOUpdate;
import com.atakmap.commoncommo.EnrollmentStep;
import com.atakmap.commoncommo.SimpleFileIOStatus;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.security.ISslCertificateManager;
import gov.tak.core.comms.commo.CommonCommoManager;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.bootstrap.Version;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.security.CertUtils;
import gov.takx.dataservices.utils.HashUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.api.filesystem.VfsConstants.FILES_DIR;

/**
 * Service to manage SSL certificates, maintained within Global VFS.
 *
 * @since 3.0
 */
@ApplicationScoped
public class SslCertificateManager implements ISslCertificateManager, EnrollmentIO
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String SSL_CERTS_DIR = "SslCerts";

    private static final String CLIENT_CERT_FILE_NAME_SUFFIX = "_Client.p12";
    private static final String TRUST_STORE_FILE_NAME_SUFFIX = "_TrustStore.p12";
    private static final int PRIVATE_KEY_LENGTH = 4096; // Length of generated private key during enrollment

    private final Map<Integer, EnrollmentHandler> activeEnrollments = new ConcurrentHashMap<>();

    private final CommonCommoManager commoManager;
    private final File rootDir;

    @Inject
    SslCertificateManager(CommonCommoManager commoManager, @VFSManagerScope(VFSScope.Global) IVirtualFileSystemManager vfsManager)
    {
        this.commoManager = commoManager;
        rootDir = vfsManager.getRoot(new FilePath(FILES_DIR, SSL_CERTS_DIR).toString());
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices event)
    {
        if (!rootDir.exists() && !rootDir.mkdirs()) logger.error("Failed to create dir: {}", rootDir);
    }

    @Nonnull
    @Override
    public File getCertFile(@Nonnull String fileName)
    {
        // Strip off the path (if present) before getting the file to make this method more generally useful
        return new File(rootDir, FilenameUtils.getName(fileName));
    }

    @Override
    public boolean certExists(@Nonnull String fileName)
    {
        return getCertFile(fileName).exists();
    }

    @Nonnull
    @Override
    public File copyCert(@Nonnull String certFilePath) throws IOException
    {
        final File certFile = new File(certFilePath);

        if (!certFile.exists()) throw new IOException("Cert file does not exist: " + certFilePath);

        return createCert(certFile.getName(), Files.readAllBytes(certFile.toPath()));
    }

    @Nonnull
    @Override
    public File createCert(@Nonnull String fileName, @Nonnull byte[] content) throws IOException
    {
        // Construct a unique file name by appending a hash of the file contents
        final String nameWithHash = String.format("%s_%s.%s", FilenameUtils.getBaseName(fileName),
                HashUtils.getShortHash(content), FilenameUtils.getExtension(fileName));

        final File hashedFile = new File(rootDir, nameWithHash);
        hashedFile.getParentFile().mkdirs();
        Files.write(hashedFile.toPath(), content);

        return hashedFile;
    }

    @Override
    public void enrollForClientCertificate(@Nonnull StreamingConnectionConfiguration connectionConfig) throws IOException
    {
        try
        {
            // Crank up the async operation, and wait for it to complete.
            final ClientCertAndTruststore certAndTruststore = startEnrollment(connectionConfig).join();

            // To match WinTAK, store the certs to the certificate store using the connection name as the file name prefix.
            final String safeConnectionName = FileUtils.makeSafeName(connectionConfig.getDisplayName());
            File certFile = createCert(safeConnectionName + CLIENT_CERT_FILE_NAME_SUFFIX, certAndTruststore.certBytes);
            File trustStoreFile = createCert(safeConnectionName + TRUST_STORE_FILE_NAME_SUFFIX, certAndTruststore.trustStoreBytes);

            logger.debug("Writing cert to {} and trust store to {}", certFile, trustStoreFile);

            // TODO KMB: WinTAK and ATAK encrypt and store the passwords to .dat files in the same directory as the certs
            //  instead of storing them in the preferences.

            connectionConfig.setCertificate(certFile.getName(), certAndTruststore.certPassword);
            connectionConfig.setCertificateAuthority(trustStoreFile.getName(), certAndTruststore.trustStorePassword);
        } catch (CommoException e)
        {
            throw new IOException("Error initiating certificate enrollment: " + e.getMessage(), e);
        } catch (CompletionException e)
        {
            final Throwable cause = e.getCause(); // Unwrap the CompletionException
            throw new IOException(cause.getMessage(), cause);
        }
    }

    /**
     * Initiate the Cert Signing Request (enrollment) process via Common Commo.
     *
     * @param connectionConfig Connection on which to request enrollment
     * @return CompletableFuture to provide the result of the enrollment
     * @throws CommoException If the enrollment couldn't be started
     * @since 3.3
     */
    private CompletableFuture<ClientCertAndTruststore> startEnrollment(@Nonnull StreamingConnectionConfiguration connectionConfig)
            throws CommoException
    {
        // Generate a random password since we don't ever need to use it again
        final String password = UUIDUtils.generateType1UUID().toString();

        // Initialize the enrollment operation with commo
        final URI hostUri = connectionConfig.getUri();

        //noinspection ConstantConditions
        final int enrollmentId = commoManager.enrollmentInit(hostUri.getHost(),
                PreferenceEntry.COMMON_COMMO_TAK_SERVER_CERT_ENROLLMENT_API_PORT.getIntegerValue(), false,
                connectionConfig.getUsername(), connectionConfig.getPassword(), connectionConfig.isUseTokenAuthentication(),
                getCaCertificateBytes(connectionConfig), connectionConfig.getCertificateAuthorityPassword(),
                password, password, password, PRIVATE_KEY_LENGTH, Version.getFullVersionString());

        // Create a new handler for this specific enrollment
        final EnrollmentHandler enrollmentHandler = new EnrollmentHandler(connectionConfig, enrollmentId, password);
        activeEnrollments.put(enrollmentId, enrollmentHandler);

        try
        {
            logger.debug("Starting enrollment to {}; id {}", hostUri, enrollmentId);
            commoManager.enrollmentStart(enrollmentId); // And crank it up
            return enrollmentHandler.enrollmentFuture;
        } catch (CommoException e)
        {
            activeEnrollments.remove(enrollmentId); // Clean up after failed start
            throw e;
        }
    }

    @Override
    public void enrollmentUpdate(EnrollmentIOUpdate update)
    {
        final int transferId = update.transferId;

        final EnrollmentHandler handler = activeEnrollments.get(transferId);
        if (handler != null)
        {
            handler.enrollmentUpdate(update);
        } else
        {
            logger.error("Received update for unknown enrollment id {}: step {}, status {}, info {}",
                    transferId, update.step, update.status, update.additionalInfo);
        }
    }

    /**
     * @param connectionConfig The streaming connection configuration
     * @return A byte array containing the PCKS12 certificates or {@code null} if there are no certs to include
     * @since 5.1
     */
    @Nullable
    private byte[] getCaCertificateBytes(@Nonnull StreamingConnectionConfiguration connectionConfig)
    {
        // Start with the CA cert bytes from the config
        byte[] caBytes = connectionConfig.getCertificateAuthorityBytes();

        // Add the system CA cert bytes to the array if needed
        if (PreferenceEntry.COMMON_COMMO_TAK_SERVER_USE_SYSTEM_CERT_AUTHORITIES.getBooleanValue())
        {
            // Get the system CA bytes in a key store
            final KeyStore systemCaKeyStore = CertUtils.getSystemCaCertificates();

            if (systemCaKeyStore != null)
            {
                // Add the cert bytes retrieved from the config
                if (caBytes.length > 0) CertUtils.setCertificateEntry(systemCaKeyStore, null, caBytes);

                final String password = connectionConfig.getCertificateAuthorityPassword();
                caBytes = CertUtils.getBytes(systemCaKeyStore, password != null ? password.toCharArray() : null);
            }
        }

        return caBytes.length > 0 ? caBytes : null;
    }

    /**
     * Handler to manage the asynchronous callbacks from the enrollment process, providing a {@link CompletableFuture}
     * for consumers to observe the completion status.
     *
     * @since 3.3
     * @see ClientCertAndTruststore
     */
    private class EnrollmentHandler implements EnrollmentIO
    {
        private final StreamingConnectionConfiguration configuration;
        private final int enrollmentId;
        private final String password;
        private final CompletableFuture<ClientCertAndTruststore> enrollmentFuture = new CompletableFuture<>();

        private EnrollmentHandler(StreamingConnectionConfiguration connectionConfiguration, int enrollmentId, String password)
        {
            this.configuration = connectionConfiguration;
            this.enrollmentId = enrollmentId;
            this.password = password;
        }

        @Override
        public void enrollmentUpdate(EnrollmentIOUpdate update)
        {
            logger.debug("enrollment {} update step {}, status {}, info {}", enrollmentId, update.step, update.status, update.additionalInfo);

            final SimpleFileIOStatus status = update.status;

            // We only care about the SIGN step to get our results
            if (update.step == EnrollmentStep.SIGN && status == SimpleFileIOStatus.SUCCESS)
            {
                // We're done!  Capture the private and CA certs and complete our future.
                enrollmentFuture.complete(new ClientCertAndTruststore(Arrays.copyOf(update.privResult, update.privResult.length),
                        password, Arrays.copyOf(update.caResult, update.caResult.length), password));
                activeEnrollments.remove(enrollmentId);
            } else if (status != SimpleFileIOStatus.INPROGRESS && status != SimpleFileIOStatus.SUCCESS)
            {
                // Anything other than SUCCESS and INPROGRESS is some form of failure
                final String msg = "Enrollment process failed on " + configuration.getUri().getHost() + "; status " + status;
                logger.error(msg);
                activeEnrollments.remove(enrollmentId);
                enrollmentFuture.completeExceptionally(new RuntimeException(msg));
            }
        }
    }

    /**
     * Simple wrapper for returning the generated client cert and trust store to the calling method.
     *
     * @param certBytes          The bytes for the client certificate
     * @param certPassword       The password used to encrypt the client certificate
     * @param trustStoreBytes    The bytes for the trust store (aka CA certificate)
     * @param trustStorePassword The password used to encrypt the trust store
     */
    private record ClientCertAndTruststore(byte[] certBytes, String certPassword, byte[] trustStoreBytes,
            String trustStorePassword)
    {
    }
}
