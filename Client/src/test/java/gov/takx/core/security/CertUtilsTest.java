package gov.takx.core.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link CertUtils}.
 *
 * @since 5.1
 */
public class CertUtilsTest
{
    @TempDir
    Path tempDir;

    @Test
    public void getSystemCaCertificates_returnsCerts()
    {
        final KeyStore systemCaCerts = CertUtils.getSystemCaCertificates();

        assertThat(systemCaCerts).isNotNull();
        assertThat(systemCaCerts.getType()).isEqualToIgnoringCase("pkcs12");
    }

    @Test
    public void getBytes_returnsValidBytes() throws Exception
    {
        final char[] password = "password".toCharArray();

        final KeyStore systemCaCerts = CertUtils.getSystemCaCertificates();

        // Convert to bytes
        //noinspection DataFlowIssue
        final byte[] bytes = CertUtils.getBytes(systemCaCerts, password);

        // Convert back to key store
        final KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(new ByteArrayInputStream(bytes), password);

        assertThat(keyStore.size()).isEqualTo(systemCaCerts.size());

        // Confirm that the original certs exist in the new store with the same encoded bytes
        systemCaCerts.aliases().asIterator().forEachRemaining(alias -> {
            try
            {
                final Certificate certificate1 = systemCaCerts.getCertificate(alias);
                final Certificate certificate2 = keyStore.getCertificate(alias);

                assertThat(certificate2).isNotNull();
                assertThat(certificate1.getEncoded()).isEqualTo(certificate2.getEncoded());
            } catch (CertificateException | KeyStoreException e)
            {
                throw new RuntimeException("Failed to retrieve certificate from key store", e);
            }
        });
    }

    @Test
    public void getLinuxCaCertificates_usesFirstReadableBundle() throws Exception
    {
        final List<X509Certificate> certificates = getJdkCaCertificates(3);
        final Path missing = tempDir.resolve("missing.crt");
        final Path first = writeBundle(tempDir.resolve("first.crt"), certificates, "");
        final Path second = writeBundle(tempDir.resolve("second.pem"), certificates.subList(0, 1), "");

        final List<Path> paths = List.of(missing, first, second);

        final List<X509Certificate> loaded = CertUtils.getLinuxCaCertificates(paths).toList();

        assertThat(loaded).containsExactlyElementsOf(certificates);
    }

    @Test
    public void getLinuxCaCertificates_readsBundleWithTextBetweenCertificates() throws Exception
    {
        final List<X509Certificate> certificates = getJdkCaCertificates(3);
        final Path bundle = writeBundle(tempDir.resolve("tls-ca-bundle.pem"), certificates, "# Certificate label\n");

        final List<X509Certificate> loaded = CertUtils.getLinuxCaCertificates(List.of(bundle)).toList();

        assertThat(loaded).containsExactlyElementsOf(certificates);
    }

    @Test
    public void getLinuxCaCertificates_returnsEmptyWhenNoBundleIsReadable() throws Exception
    {
        final List<Path> paths = List.of(tempDir.resolve("a.crt"), tempDir.resolve("b.pem"));

        assertThat(CertUtils.getLinuxCaCertificates(paths)).isEmpty();
    }

    private static List<X509Certificate> getJdkCaCertificates(int count) throws Exception
    {
        final File cacerts = new File(System.getProperty("java.home"), "lib/security/cacerts");
        final KeyStore keyStore = KeyStore.getInstance(cacerts, "changeit".toCharArray());
        final List<X509Certificate> certificates = new ArrayList<>();

        for (String alias : Collections.list(keyStore.aliases()))
        {
            if (certificates.size() == count) break;
            if (keyStore.getCertificate(alias) instanceof X509Certificate certificate) certificates.add(certificate);
        }

        assertThat(certificates).hasSize(count);
        return certificates;
    }

    private static Path writeBundle(Path path, List<X509Certificate> certificates, String textBeforeEach)
            throws Exception
    {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII));
        final StringBuilder bundle = new StringBuilder();

        for (X509Certificate certificate : certificates)
        {
            bundle.append(textBeforeEach)
                    .append("-----BEGIN CERTIFICATE-----\n")
                    .append(encoder.encodeToString(certificate.getEncoded()))
                    .append("\n-----END CERTIFICATE-----\n\n");
        }

        return Files.writeString(path, bundle.toString(), StandardCharsets.US_ASCII);
    }
}
