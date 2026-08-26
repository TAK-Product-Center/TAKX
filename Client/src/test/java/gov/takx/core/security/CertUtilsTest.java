package gov.takx.core.security;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link CertUtils}.
 *
 * @since 5.1
 */
public class CertUtilsTest
{
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
}
