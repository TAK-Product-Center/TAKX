package gov.takx.core.security;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.SwingUtilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SSL Certificate utility class
 *
 */
public class CertUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TRUSTSTORE = "conf" + File.separator + "truststore";
    private static final String PASS_PHRASE = "changeit";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy");
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final long CERT_EXPIRY_WARNING_DURATION = 7 * 24 * 3600 * 1000; // one week
    private static final String WINDOWS_ROOT_KEYSTORE = "Windows-ROOT";
    private static final String LINUX_CA_CERTS_PATH = "/etc/ssl/certs/ca-certificates.crt";

    public static final String INVALID_CERT = "Received fatal alert: unknown_ca";
    public static final String UNSUPPORTED_CERT = "Received fatal alert: unsupported_certificate";
    public static final String HANDSHAKE_CERT = "Received fatal alert: handshake_failure";

    /**
     * Install 1 or more (usually 1) SSL certificate (non-trusted) with user interaction, unless of course,
     * the certificate appears to already be installed.  Some improvements to this routine would be to allow the
     * user to install the cert temporarily through the DisplayCertInfoDialog, rather than permanently
     *
     * @param host     host/server
     * @param listener listener to provide results to
     * @throws Exception exception installing the certificate
     */
    public static void installCert(final String host, final ICertificateInstallerListener listener) throws Exception
    {
        File file = new File(TRUSTSTORE);
        if (file.isFile())
        {
            logger.debug("Loading KeyStore {}...", file);
            InputStream in = new FileInputStream(file);
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(in, PASS_PHRASE.toCharArray());
            in.close();

            SSLContext context = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            X509TrustManager defaultTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            SavingReloadableTrustManager savingTrustManager = new SavingReloadableTrustManager(defaultTrustManager);
            context.init(null, new TrustManager[]{savingTrustManager}, null);
            SSLSocketFactory factory = context.getSocketFactory();

            logger.debug("Opening connection to {}:" + 443 + "...", host);
            SSLSocket socket = (SSLSocket) factory.createSocket(host, 443);
            socket.setSoTimeout(10000);
            try
            {
                logger.debug("Starting SSL handshake...");
                socket.startHandshake();
                socket.close();
                logger.debug("No errors, certificate is already trusted");
                if (listener != null)
                {
                    listener.certificateInstalled(ICertificateInstallerListener.RESULTS.INSTALLED, host);
                }
                return;
            } catch (SSLException e)
            {
                logger.debug("SSL Exception", e);
                logger.info("Certificate is not yet installed in the keystore");
                // If we are here... we need to install the cert.
            }

            final X509Certificate[] chain = savingTrustManager.getCertificateChain();
            if (chain == null)
            {
                logger.debug("Could not obtain server certificate chain");
                if (listener != null)
                {
                    listener.certificateInstalled(ICertificateInstallerListener.RESULTS.FAILURE, host);
                }
                return;
            }

            logger.debug("Server sent {} certificate(s):", chain.length);
            final MessageDigest sha512 = MessageDigest.getInstance("SHA-512");

            // Install all of the certs sent by the server.  In all likelihood, there will only be 1.
            for (int i = 0; i < chain.length; i++)
            {
                final X509Certificate cert = chain[i];
                logger.debug("{} Subject {}", i + 1, cert.getSubjectX500Principal());
                logger.debug("Issuer  {}", cert.getIssuerX500Principal());
                sha512.update(cert.getEncoded());
                logger.debug("sha-512    {}", toHexString(sha512.digest()));

                SwingUtilities.invokeAndWait(() -> {
                    DisplayCertInfoDialog dlg = new DisplayCertInfoDialog(cert, sha512);
                    dlg.setModal(true);
                    dlg.setVisible(true);
                    if (dlg.isOkay())
                    {
                        int k = 0;  // Just install the first certificate for now.

                        X509Certificate cert1 = chain[k];
                        String alias = host + "-" + (k + 1);
                        try
                        {
                            keyStore.setCertificateEntry(alias, cert1);

                            OutputStream out = new FileOutputStream(TRUSTSTORE);
                            keyStore.store(out, PASS_PHRASE.toCharArray());
                            out.close();

                            logger.debug(cert1.toString());
                            logger.debug("Added certificate to keystore 'truststore' using alias '{}'", alias);
                            if (listener != null)
                            {
                                listener.certificateInstalled(ICertificateInstallerListener.RESULTS.SUCCESS, host);
                                setWorldWindSSLContext(TRUSTSTORE);
                            }
                        } catch (Exception e)
                        {
                            logger.error("Error adding to keystore");
                            if (listener != null)
                            {
                                listener.certificateInstalled(ICertificateInstallerListener.RESULTS.FAILURE, host);
                            }
                        }
                    } else if (listener != null)
                    {
                        listener.certificateInstalled(ICertificateInstallerListener.RESULTS.CANCELLED, host);
                    }
                });
            }
        }
    }

    /**
     * Initialize use of PKI keys by setting the WW SSLContext, which will add the windows Keystore to the
     * key manager used to create the SSLContext.
     *
     * @see #setWorldWindSSLContext(String)
     */
    public static void enablePKIKeyUse()
    {
        try
        {
            setWorldWindSSLContext(TRUSTSTORE);
        } catch (Exception e)
        {
            logger.error("Error selecting SSL context from user PKI key", e);
        }
    }

    /**
     * Get the user preferred PKI key from the user preferences.
     *
     * @return A string representing the user PKI key, empty string if not set
     * @see #createEntryStringFromCert(java.security.cert.X509Certificate)
     */
    public static String getUserSelectedKey()
    {
        return UserPreferenceEntry.PKI_USER_KEY.getStringValue();
    }

    /**
     * Set the user preferred PKI key as a string in the user preferences.  The "entry" is a string which is a
     * human readable form of the PKI key, and not related to the key alias.
     *
     * @param entry the user PKI key in string form
     * @see #createEntryStringFromCert(java.security.cert.X509Certificate)
     */
    public static void selectUserKey(String entry)
    {
        UserPreferenceEntry.PKI_USER_KEY.putAndSave(entry);

        try
        {
            setWorldWindSSLContext(TRUSTSTORE);
        } catch (Exception e)
        {
            logger.error("Error setting WW SSLContext for key: {}", entry, e);
        }
    }

    /**
     * Get the X509Certificate that corresponds to the String value of the user key.  The entry string is a human
     * readable form of the PKI key, and not related to the key alias.  However, the best way to iterate through
     * the keys is to iterate through the list of aliases in the Keystore, get the certificates from the aliases,
     * and then compare to what they translate to in their human readable text form, as calculated from
     * {@link #createEntryStringFromCert(java.security.cert.X509Certificate)}.
     *
     * @param userKeyEntry String representation of the user PKI key
     * @return the corresponding X509Certificate
     * @throws Exception exception getting key from keystore entry string
     */
    public static X509Certificate getKeyFromEntryString(String userKeyEntry) throws Exception
    {
        KeyStore keyStore = getWindowsKeystore();
        if (keyStore != null)
        {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements())
            {
                String keyAlias = aliases.nextElement();
                Certificate cert = keyStore.getCertificate(keyAlias);
                if (cert instanceof X509Certificate x509Certificate)
                {
                    String entry = createEntryStringFromCert(x509Certificate);
                    if (userKeyEntry.equals(entry)) return x509Certificate;
                }
            }
        }
        return null;
    }

    /**
     * Create a restorable string representation of a X509Certificate to be used for user preferences.  Will be of
     * the format:  KeyName, Issuer: (issuer), Valid date to/from, SN: (serial number).
     * Should result in unique strings to identify/store/restore the key preference with.  Chose this as a unique
     * identifying string because it is essentially what IE presents the user with to identify their key when
     * needed.
     *
     * @param cert the X509 Cert
     * @return the String representation of the X509Certificate.
     */
    public static String createEntryStringFromCert(X509Certificate cert)
    {
        StringBuilder sb = new StringBuilder();
        String subject = cert.getSubjectX500Principal().toString();
        String[] parts = subject.split(",");
        if (parts.length > 0)
        {
            int index = parts[0].indexOf("CN=");
            if (index != -1)
            {
                sb.append(parts[0].substring(index + 3)).append(", ");
            }
        }

        String issuer = cert.getIssuerX500Principal().toString();
        parts = issuer.split(",");
        if (parts.length > 0)
        {
            int index = parts[0].indexOf("CN=");
            if (index != -1)
            {
                sb.append("Issuer: ").append(parts[0].substring(index + 3)).append(", ");
            }
        }

        sb.append("Valid From: ").append(DATE_FORMAT.format(cert.getNotBefore())).append(" to ").append(DATE_FORMAT.format(cert.getNotAfter()));
        sb.append(", SN: ").append(toHexString(cert.getSerialNumber().toByteArray()));

        return sb.toString();
    }

    /**
     * Basically, we always use the windows IE keystore, since that's the place users will be installing their
     * PKI keys for use with GE and other browsers.
     *
     * @return the windows keystore
     * @throws Exception exception getting the windows keystore.
     */
    public static KeyStore getWindowsKeystore() throws Exception
    {
        KeyStore keyStore = null;
        try
        {
            keyStore = KeyStore.getInstance("Windows-MY");  // This will throw an exception if not on windows.
        } catch (KeyStoreException ignored)
        {
        }
        if (keyStore != null)
        {
            keyStore.load(null, null);
        }
        return keyStore;
    }

    /**
     * Gets the key alias from the certificate.  Used by our {@link PKIKeyManager} to return an alias
     * for the currently selected key/certificate.
     *
     * @param certificate the X509Certificate to get the alias for
     * @return the key alias
     * @throws Exception exception getting alias
     */
    static String getAliasFromCertificate(X509Certificate certificate) throws Exception
    {
        KeyStore keyStore = getWindowsKeystore();   // this will only work on windows
        if (keyStore != null)
        {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements())
            {
                String keyAlias = aliases.nextElement();
                Certificate cert = keyStore.getCertificate(keyAlias);
                if (cert instanceof X509Certificate x509Certificate)
                {
                    if (x509Certificate.getSerialNumber().equals(certificate.getSerialNumber()))
                    {
                        return keyAlias;
                    }
                }
            }
        }
        return "";
    }

    /**
     * Creates an SSL context from the reloadable trust manager and the custom keystore manager using
     * the windows PKI keystore.
     *
     * @param path the path to the truststore file
     * @throws Exception exception loading SSL context
     */
    private static void setWorldWindSSLContext(String path) throws Exception
    {
        // Don't try to install the trust store if it is missing
        if (!new File(path).exists())
        {
            logger.debug("trust store is missing: {}", path);
            return;
        }

        KeyStore keystore = getWindowsKeystore();   // This will only work on windows...
        if (keystore != null)
        {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            factory.init(keystore, null);
            final X509KeyManager defaultKeyManager = (X509KeyManager) factory.getKeyManagers()[0];
            final PKIKeyManager pkiKeyManager;

            String entry = getUserSelectedKey();
            if (!entry.isEmpty())
            {
                X509Certificate cert = getKeyFromEntryString(entry);
                pkiKeyManager = new PKIKeyManager(defaultKeyManager, cert);
                logger.debug("Using PKI key: {}", entry);
            } else
            {
                pkiKeyManager = new PKIKeyManager(defaultKeyManager, null);
                logger.debug("Not manually specifying a PKI key.");
            }

            TrustManager[] trustManagers = {new SavingReloadableTrustManager(path)};
            sslContext.init(new KeyManager[]{pkiKeyManager}, trustManagers, null);
            WorldWind.setValue(AVKey.HTTP_SSL_CONTEXT, sslContext);
            logger.debug("Set SSL Context: {}", sslContext);
        }
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    /**
     * Utility class turning a byte array into a hex string
     *
     * @param bytes bytes to convert
     * @return hex string result
     */
    public static String toHexString(byte[] bytes)
    {
        boolean firstChar = true;
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int b : bytes)
        {
            if (!firstChar)
            {
                sb.append(':');
            }
            firstChar = false;
            b &= 0xff;
            sb.append(HEX_DIGITS[b >> 4]);
            sb.append(HEX_DIGITS[b & 15]);
        }
        return sb.toString();
    }

    /**
     * @param certName Name of the cert, for enhanced warning messages.
     * @param certPath Path to cert file.
     * @param password Password to open the cert file with.
     * @return Validation result summarizing any issues opening the cert with the given password.
     * @since 3.4
     */
    @Nonnull
    public static CertValidation validateCert(@Nonnull String certName, @Nonnull File certPath, @Nonnull char[] password)
    {
        return validateCert(certName, certPath, password, System.currentTimeMillis());
    }

    /**
     * @param certName Name of the cert, for enhanced warning messages.
     * @param certPath Path to cert file.
     * @param password Password to open the cert file with.
     * @param now Time to check against for validity.
     * @return Validation result summarizing any issues opening the cert with the given password.
     * @since 3.4
     */
    @Nonnull
    public static CertValidation validateCert(@Nonnull String certName, @Nonnull File certPath, @Nonnull char[] password, long now)
    {
        // Read from the keystore
        try (InputStream is = new FileInputStream(certPath))
        {
            // Load keystore
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(is, password);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password);

            // Ensure all contained certificates are still valid
            String error = null;
            String warning = null;
            Enumeration<String> aliases = ks.aliases();
            List<CertEntry> certEntries = new ArrayList<>();
            while (aliases.hasMoreElements())
            {
                String alias = aliases.nextElement();
                Certificate certificate = ks.getCertificate(alias);
                CertEntry entry = new CertEntry(certName, alias, certificate);
                entry.validate(now);
                certEntries.add(entry);
            }

            // TAKX-1837: Need to make sure there are actually certs in the keystore. A user provided a CA cert that
            // didn't have any aliases and it was falsely reporting a cert error since the number of invalid certs was
            // equal to the number of certs (0). The user confirmed that the cert loaded correctly in WinTAK and ATAK,
            // so even though it seems weird to have a certificate file with no entries, hopefully this change will
            // mirror the behavior of the other products.
            if (!certEntries.isEmpty())
            {
                // If there are multiple certificates in a file, we cannot know which will be used by an application
                // just by looking at the certificate entries. Because of this, we will allow files with at least one
                // valid certificate to pass, but with a warning.
                int invalidCertsCount = (int) certEntries.stream().filter(c -> c.errorMessage != null).count();
                int warnCertsCount = (int) certEntries.stream().filter(c -> c.warningMessage != null).count();
                if (invalidCertsCount == certEntries.size())
                {
                    // None of the contained certificates are valid.
                    error = "All entries in '" + certName + "' are invalid:\n" + certEntries.stream()
                            .filter(c -> c.errorMessage != null)
                            .map(c -> c.errorMessage)
                            .collect(Collectors.joining("\n - "));
                } else if (invalidCertsCount > 0)
                {
                    // Some contained certificates are valid.
                    warning = "Some entries in '" + certName + "' are invalid:\n" + certEntries.stream()
                            .filter(c -> c.errorMessage != null)
                            .map(c -> c.errorMessage)
                            .collect(Collectors.joining("\n - "));
                } else if (warnCertsCount > 0)
                {
                    // All contained certificates are valid, but have warnings.
                    warning = "Some entries in '" + certName + "' have warnings:\n" + certEntries.stream()
                            .filter(c -> c.warningMessage != null)
                            .map(c -> c.warningMessage)
                            .collect(Collectors.joining("\n - "));
                }
            }

            return new CertValidation(null, error, warning);
        } catch (IOException e)
        {
            // The cause exception is wrapped in an IO exception, which we can check the message of
            // to validate that the cause was due to the wrong password being specified.
            return new CertValidation(e, "keystore password was incorrect".equals(e.getMessage()) ? "Incorrect password for " + certName.toLowerCase() : "Could not find or load " + certName.toLowerCase() + " keystore from path",
                    null);
        } catch (UnrecoverableKeyException e)
        {
            // Typically thrown when no password is provided when an entry requires one.
            return new CertValidation(e, "Error reading " + certName.toLowerCase() + " - Could not recover keystore entries", null);
        } catch (CertificateException e)
        {
            // Typically thrown when parsing key-store that is malformed.
            return new CertValidation(e, "Error reading " + certName.toLowerCase() + " - Certificate is likely malformed", null);
        } catch (KeyStoreException e)
        {
            // Typically thrown when key-store is not initialized, or api is used incorrectly.
            return new CertValidation(e, "Error reading " + certName.toLowerCase() + " - Internal handling of keystore is incorrect", null);
        } catch (NoSuchAlgorithmException e)
        {
            return new CertValidation(e, "Error reading " + certName.toLowerCase() + " - Signed with unsupported algorithm", null);
        }
    }

    /**
     * @return A PCKS12 key store containing the system CA certificates or {@code null} if there are no system certs or
     * there were errors retrieving them.
     * @since 5.1
     */
    @Nullable
    public static KeyStore getSystemCaCertificates()
    {
        try
        {
            // Get the certs from the OS-specific cert store
            final Stream<X509Certificate> certificateStream =
                    SystemUtils.IS_OS_WINDOWS ? getWindowsCaCertificates()
                            : SystemUtils.IS_OS_LINUX ? getLinuxCaCertificates()
                            : Stream.empty();

            // Prepare an empty temp key store to hold CA certs
            final KeyStore keyStore = KeyStore.getInstance("pkcs12");
            keyStore.load(null, null);

            // Add valid certs to the temp key store
            final Instant now = Instant.now();
            certificateStream
                    .filter(certificate -> now.isAfter(certificate.getNotBefore().toInstant())
                            && now.isBefore(certificate.getNotAfter().toInstant()))
                    .forEach(certificate -> setCertificateEntry(keyStore, null, certificate));

            logger.debug("Added {} system CA certificates to the temp key store", keyStore.size());

            return keyStore.size() > 0 ? keyStore : null;
        } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e)
        {
            logger.error("Error retrieving system CA certificates.", e);
            return null;
        }
    }

    /**
     * @param keyStore The key store to convert to a byte array
     * @param password The key store password
     * @return A byte array containing the output of the call to {@code KeyStore.store()} or an empty byte array if there
     * were errors converting the key store
     * @since 5.1
     */
    @Nonnull
    public static byte[] getBytes(@Nonnull KeyStore keyStore, @Nullable char[] password)
    {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            keyStore.store(outputStream, password);
            return outputStream.toByteArray();
        } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e)
        {
            logger.error("Error converting key store to byte array", e);
            return new byte[0];
        }
    }

    /**
     * @param keyStore The key store to update
     * @param alias The alias to use for the certificate entry. If {@code null}, a unique alias will be generated
     * @param bytes The X509 certificate bytes
     * @since 5.1
     */
    public static void setCertificateEntry(@Nonnull KeyStore keyStore, @Nullable String alias, @Nonnull byte[] bytes)
    {
        try
        {
            var certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(bytes));
            setCertificateEntry(keyStore, alias, certificate);
        } catch (CertificateException e)
        {
            logger.warn("Unable to add certificate to key store", e);
        }
    }

    /**
     * @param keyStore The key store to update
     * @param alias The alias to use for the certificate entry. If {@code null}, a unique alias will be generated
     * @param certificate The X509 certificate
     * @since 5.1
     */
    private static void setCertificateEntry(@Nonnull KeyStore keyStore, @Nullable String alias, @Nonnull X509Certificate certificate)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Adding cert with principal {} and serial number {}",
                    certificate.getIssuerX500Principal().getName(), certificate.getSerialNumber().toString());
        }

        try
        {
            keyStore.setCertificateEntry(alias != null ? alias : getUniqueAlias(keyStore), certificate);
        } catch (KeyStoreException e)
        {
            logger.warn("Unable to add certificate to key store", e);
        }
    }

    /**
     * Returns a String value that has not been used as an alias in the provided key store.
     *
     * @param keyStore The key store
     * @return A unique alias
     * @throws KeyStoreException thrown if there are errors checking existing aliases
     * @since 5.1
     */
    private static String getUniqueAlias(@Nonnull KeyStore keyStore) throws KeyStoreException
    {
        int i = keyStore.size();
        String alias;

        do
        {
            alias = String.valueOf(i++);
        } while (keyStore.containsAlias(alias));

        return alias;
    }

    /**
     * @return A stream of Windows system CA certificates
     * @throws CertificateException thrown on errors creating an empty key store
     * @throws IOException thrown on errors creating an empty key store
     * @throws KeyStoreException thrown on errors initializing a TrustManagerFactory from a key store
     * @throws NoSuchAlgorithmException thrown on errors creating an empty key store or retrieving a TrustManagerFactory
     * instance
     * @since 5.1
     */
    @Nonnull
    private static Stream<X509Certificate> getWindowsCaCertificates()
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException
    {
        KeyStore keyStore = null;

        // Get the Windows CA keystore
        try
        {
            keyStore = KeyStore.getInstance(WINDOWS_ROOT_KEYSTORE);
        } catch (KeyStoreException ignored)
        {
        }

        if (keyStore == null) return Stream.empty();

        keyStore.load(null, null);

        // Get the trust factory and initialize with the keystore
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // Map the managers to the relevant certs and return a stream
        return Arrays.stream(trustManagerFactory.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .map(trustManager -> Arrays.asList(trustManager.getAcceptedIssuers()))
                .flatMap(Collection::stream);
    }

    /**
     * @return A stream of Linux system CA certificates.
     * @throws CertificateException thrown on errors retrieving a CertificateFactrory or generating a certificate from
     * an input stream
     * @throws IOException thrown on errors reading the CRT file
     * @since 5.1
     */
    @Nonnull
    private static Stream<X509Certificate> getLinuxCaCertificates()
            throws CertificateException, IOException
    {
        final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        final List<X509Certificate> certificates = new ArrayList<>();

        // Try to open the CA certs file and add its certs to the list
        try (FileInputStream fis = new FileInputStream(LINUX_CA_CERTS_PATH))
        {
            X509Certificate certificate;

            while (fis.available() > 0 && (certificate = (X509Certificate) certFactory.generateCertificate(fis)) != null)
            {
                certificates.add(certificate);
            }
        }

        return certificates.stream();
    }

    /**
     * @param validationError            Error thrown by validation of the trust-store.
     *                                   {@code null} indicates success. See {@link #isValid()}
     * @param validationErrorExplanation Explanation of the error, for UI display.
     * @param validationWarning          Additional warning for UI display when there is no error,
     *                                   but something noteworthy should be mentioned.
     *
     * @since 3.4
     */
    public record CertValidation(@Nullable Exception validationError,
            @Nullable String validationErrorExplanation,
            @Nullable String validationWarning)
    {
        /**
         * @return {@code true} when no {@link #validationError()} exists.
         */
        public boolean isValid()
        {
            return validationError == null && validationErrorExplanation == null;
        }
    }

    /**
     * Used to track certificate validation state.
     *
     * @since 3.4
     */
    private static class CertEntry
    {
        private final String certFileName;
        private final String alias;
        private final Certificate certificate;
        private String errorMessage;
        private String warningMessage;

        /**
         * @param certFileName Name of certificate file containing this entry.
         * @param alias        Name of certificate entry.
         * @param certificate  Certificate entry value.
         */
        public CertEntry(@Nonnull String certFileName, @Nonnull String alias, @Nonnull Certificate certificate)
        {
            this.certFileName = certFileName;
            this.alias = alias;
            this.certificate = certificate;
        }

        /**
         * @param now Time to check against for validity.
         */
        public void validate(long now)
        {
            if (certificate instanceof X509Certificate x509Certificate)
            {
                long start = x509Certificate.getNotBefore().getTime();
                if (now < start)
                {
                    errorMessage = certFileName + " entry '" + alias + "' is not yet available, first valid date is " + DATE_TIME_FORMAT.format(start);
                    return;
                }

                long expiration = x509Certificate.getNotAfter().getTime();
                if (now > expiration)
                {
                    errorMessage = certFileName + " entry '" + alias + "' expired on " + DATE_TIME_FORMAT.format(expiration);
                    return;
                }

                // May as well let them know the cert will expire soon.
                // This should help users prepare to request for new certs in advance when shown.
                if (now + CERT_EXPIRY_WARNING_DURATION > expiration)
                {
                    warningMessage = certFileName + " entry '" + alias + "' will expire soon: " + DATE_TIME_FORMAT.format(expiration);
                }
            }
        }
    }
}
