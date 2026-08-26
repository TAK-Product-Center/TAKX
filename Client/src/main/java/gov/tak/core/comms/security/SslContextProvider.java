package gov.tak.core.comms.security;

import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.security.ISslCertificateManager;
import gov.tak.api.comms.security.ISslContextProvider;
import gov.tak.api.engine.net.ICertificateStore;
import gov.tak.api.engine.net.ICredentialsStore;
import gov.tak.platform.commons.resources.JavaAssetManager;
import gov.tak.platform.engine.net.CentralTrustManager;
import gov.tak.platform.engine.net.CertificateManager;
import gov.tak.platform.engine.net.CertificateStore;
import gov.tak.platform.engine.net.CredentialsStore;
import gov.takx.api.annotation.InjectableService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to provide easy access to {@link SSLContext} instances based on provided
 * {@link StreamingConnectionConfiguration streaming configurations}.
 *
 * @since 3.5
 */
@InjectableService
@ApplicationScoped
public class SslContextProvider implements ISslContextProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Map<URI, SSLContext> sslContexts = new ConcurrentHashMap<>();
    private final ISslCertificateManager certificateManager;

    @Inject
    public SslContextProvider(@Nonnull ISslCertificateManager certificateManager)
    {
        this.certificateManager = certificateManager;
    }

    @Nullable
    @Override
    public SSLContext getSslContext(@Nonnull StreamingConnectionConfiguration config)
    {
        return sslContexts.computeIfAbsent(config.getUri(), uri -> makeSslContext(config));
    }

    @Override
    public boolean hasRequiredCertificates(@Nonnull StreamingConnectionConfiguration config)
    {
        final String certificateFileName = config.getCertificateFileName();
        final String certificatePassword = config.getCertificatePassword();
        if (certificateFileName == null || certificatePassword == null) return false;

        final File certFile = certificateManager.getCertFile(certificateFileName);
        return certFile.exists() && !certFile.isDirectory();
    }

    /**
     * Create the SSL context necessary to connect to a TAK Server.
     *
     * @param config Connection config, providing authentication details.
     *
     * @return {@link SSLContext}, or {@code null} if one couldn't be constructed
     */
    @Nullable
    private SSLContext makeSslContext(@Nonnull StreamingConnectionConfiguration config)
    {
        final String certificateFileName = config.getCertificateFileName();
        final String certificatePassword = config.getCertificatePassword();

        URI uri = config.getUri();
        if (certificateFileName == null || certificatePassword == null)
        {
            logger.error("Missing cert file or password for server {}", uri);
            return null;
        }

        final File certFile = certificateManager.getCertFile(certificateFileName);
        if (!certFile.exists() || certFile.isDirectory())
        {
            logger.error("Missing cert file (No local file) for server {}", uri);
            return null;
        }

        final char[] passwordChars = certificatePassword.toCharArray();

        try (InputStream is = new FileInputStream(certFile))
        {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(is, passwordChars);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, passwordChars);

            final String host = uri.getHost();
            final int port = uri.getPort();

            // configure credentials for connection
            ICredentialsStore credStore = new CredentialsStore(null, null);
            credStore.saveCredentials(ICredentialsStore.Credentials.TYPE_caPassword, host, null, config.getCertificateAuthorityPassword(), -1);
            credStore.saveCredentials(ICredentialsStore.Credentials.TYPE_clientPassword, host, null, config.getCertificatePassword(), -1);

            // configure certificates for connection
            ICertificateStore certStore = new CertificateStore(null, null);
            certStore.saveCertificate(ICertificateStore.TYPE_TRUST_STORE_CA, host, port, config.getCertificateAuthorityBytes());
            certStore.saveCertificate(ICertificateStore.TYPE_CLIENT_CERTIFICATE, host, port, config.getCertificateBytes());

            CentralTrustManager centralTrustManager = new CentralTrustManager(
                    new CertificateManager(certStore, credStore, new JavaAssetManager()));
            centralTrustManager.setIgnoreSystemCerts(true);
            centralTrustManager.setSystemCertificatePrompt(new PermissiveCertificatePrompt(host));

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), new TrustManager[]{centralTrustManager}, null);

            return context;
        } catch (Exception e)
        {
            logger.error("Failed to build SSLContext for server: {}", uri, e);
            return null;
        }
    }

    /**
     * Custom implementation to allow usage of self-signed server certificates.
     */
    private record PermissiveCertificatePrompt(String host) implements CentralTrustManager.CertificatePrompt
    {
        @Override
        public boolean promptToUseSystemTrustManager(X509Certificate[] certs)
        {
            logger.debug("using permissive prompt for system trust manager access");
            return true;
        }

        @Override
        public boolean promptToAccept(X509Certificate[] certs)
        {
            logger.warn("Trusting self-signed CA certificate on host: {}", host);
            return true;
        }
    }
}
