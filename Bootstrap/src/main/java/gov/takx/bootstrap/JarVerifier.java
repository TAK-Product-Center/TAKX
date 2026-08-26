package gov.takx.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This class offers methods to verify jar signatures.
 *
 * @since 5.2 (renamed from MilVerifier in 5.5)
 */
public final class JarVerifier
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DIGEST_MANIFEST_SUFFIX = "-Digest-Manifest";
    private static final String META_INF = "META-INF/";

    // Accepted jar signing public certificates. Defined here in base64 because it is usually easier to export base64 via
    // key utilities e.g. openssl and keytool

    // The TPC cert public keys.  We use this to verify that a jar was in fact signed by the TPC.
    // Obtained from Signing+Keys
    private static final String TRUSTED_KEY_BASE64 = "MIICxDCCAkqgAwIBAgIUZDNSAx8dojhOrbnq8drtQI6sE9owCgYIKoZIzj0EAwIwgZcxCzAJBgNVBAYTAlVTMREwDwYDVQQIDAhWaXJnaW5pYTEVMBMGA1UEBwwMRm9ydCBCZWx2b2lyMQwwCgYDVQQKDANUQUsxFzAVBgNVBAsMDlByb2R1Y3QgQ2VudGVyMTcwNQYDVQQDDC5UQUsgUHJvZHVjdCBDZW50ZXIgQVRBSyBUcnVzdGVkIFBsdWdpbiBSZWxlYXNlMCAXDTIwMDUyMTA5MzUwNloYDzIwNTAwNTE0MDkzNTA2WjCBlzELMAkGA1UEBhMCVVMxETAPBgNVBAgMCFZpcmdpbmlhMRUwEwYDVQQHDAxGb3J0IEJlbHZvaXIxDDAKBgNVBAoMA1RBSzEXMBUGA1UECwwOUHJvZHVjdCBDZW50ZXIxNzA1BgNVBAMMLlRBSyBQcm9kdWN0IENlbnRlciBBVEFLIFRydXN0ZWQgUGx1Z2luIFJlbGVhc2UwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAARP61S66vmiTz3AGB2viihxhA8sMgmx8hNdpyte41agauNspbVULHsh2lt+eo0Xr5Ow5tSeimB2+YjksBGhBuaMD3QO7OnxvXElTQsUmO6SNZjdXIrW7vOFbwJLJPzNtSijUzBRMB0GA1UdDgQWBBTCK1eo4A4KMuvBKNeDN2K/ZdvEnjAfBgNVHSMEGDAWgBTCK1eo4A4KMuvBKNeDN2K/ZdvEnjAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMCA2gAMGUCMFXOpKlC4NbUcQo/pQbrEWP205zsKJzztayTaKcJVkvQQmhQxFUXi6NXtA399GwtXwIxAPW0k8mO3Ah0TUJiXbAECzPgUzSjmkHTN1n8j0JwCJkyUyCFkEy+7d3NS/NxkhPffg==";
    private static final String UNTRUSTED_KEY_BASE64 = "MIICxzCCAk6gAwIBAgIUPv/zyfxraGXCm7I68H+nZFcn5lAwCgYIKoZIzj0EAwIwgZkxCzAJBgNVBAYTAlVTMREwDwYDVQQIDAhWaXJnaW5pYTEVMBMGA1UEBwwMRm9ydCBCZWx2b2lyMQwwCgYDVQQKDANUQUsxFzAVBgNVBAsMDlByb2R1Y3QgQ2VudGVyMTkwNwYDVQQDDDBUQUsgUHJvZHVjdCBDZW50ZXIgQVRBSyBVbnRydXN0ZWQgUGx1Z2luIFJlbGVhc2UwIBcNMjAxMDA4MTU0NjE3WhgPMjA1MDEwMDExNTQ2MTdaMIGZMQswCQYDVQQGEwJVUzERMA8GA1UECAwIVmlyZ2luaWExFTATBgNVBAcMDEZvcnQgQmVsdm9pcjEMMAoGA1UECgwDVEFLMRcwFQYDVQQLDA5Qcm9kdWN0IENlbnRlcjE5MDcGA1UEAwwwVEFLIFByb2R1Y3QgQ2VudGVyIEFUQUsgVW50cnVzdGVkIFBsdWdpbiBSZWxlYXNlMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEu/nbpVU/qu5FWHiIBUlMGj2LwKXspMWbzmL8touXmZOHf1xlGQRU4XAMmBhBY9AiuNkWSMpomLQbLPVrJs4Z43lKS96xx8CPAh2Pt7JYuQTZTFKrH/siOXW9Y2UScIPPo1MwUTAdBgNVHQ4EFgQUiTollL0194AYOognMdGA3gVrgyYwHwYDVR0jBBgwFoAUiTollL0194AYOognMdGA3gVrgyYwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNnADBkAjBqC35V+y60ZYS/edusmXIO02jND55MMziTqqF2MQRHLOYMiZE2yz+zqEfufW3JApYCMAr7xCdCCSsfSZy3/r0zVDwKJK/iGu5oING5Fzdb0CBEBnUcZH12BxzWHhbBBC15ag==";

    // The developer cert public key (to allow development of plugins without access to the TPC signing service),
    // which is only available in a CI build.
    private static final String DEV_KEY_BASE64 = "MIIDYDCCAkigAwIBAgIJAPSQcOPKuOd9MA0GCSqGSIb3DQEBCwUAMF4xCzAJBgNVBAYTAlVTMQwwCgYDVQQIEwNUQUsxDDAKBgNVBAcTA1RBSzEMMAoGA1UEChMDVEFLMQwwCgYDVQQLEwNUQUsxFzAVBgNVBAMTDlRBS1ggRGV2ZWxvcGVyMB4XDTI0MDcwMjE2NTQ1NVoXDTM0MDYzMDE2NTQ1NVowXjELMAkGA1UEBhMCVVMxDDAKBgNVBAgTA1RBSzEMMAoGA1UEBxMDVEFLMQwwCgYDVQQKEwNUQUsxDDAKBgNVBAsTA1RBSzEXMBUGA1UEAxMOVEFLWCBEZXZlbG9wZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCvHcXiYuf2dau+rW01JpWpj3jApATsKLJ33iTMru2I57x3Oy9LVQyz22rbHv7rakmIY+SYhJ6OxWKRZcRS1cDC7531IBXijIwdtw2E8j8c2UUeFKjDTXECqBfnKWc3q98eaKKh/Bz+qtBAfG7WIsRYI0mk2Tx9vLb7Fmg19H+wbT8+boc3HV6xSeqPgMDAP4vRDyl0FgAkuL+rVcBtrijej63O5YqydGKnbVv8WOkK4+XWqLbYAuE+hzd2HAHLWd3Dh5ZU3rajREZTt9rToujtJDSz2aPOQCxHzqjHeB62tnwHhQWuVlkdyKktDk7++Rv+LEMtXitRTMEFfWxCRO9nAgMBAAGjITAfMB0GA1UdDgQWBBTnrTEJ15zO/PdLgqfQCtVadcllSTANBgkqhkiG9w0BAQsFAAOCAQEANDuCyEaeV8OuEwBK4fWvxy5cLbJtNTc2UZi8A+ZNBsjmPiUnwfUo6sBQt+iIjO5pkyhvO5b55iU4g6kNle7X3mAiVsMDDUR1tloo1oV/oKmbm41+WQEBHHIaFymg7WeY2MZ9tmMwGYczxqv3d/kiRUuindFk5WMEpMarefSEH4C8giByikaJ8yEMV3Iv7xtv1UFFctejXUB3J613PdSQJPN8C4jRghjyl0IrvjIq856256YCYWfwU8Yaf9kXu1e2FPWsnKIAAKbiC2tnxhyHkPl/I2vWS1dX9TLHEtMJQdzBiK/eEmoqSVJUeyilauRjqeN07/cNA5zSQ3kFHhahQQ==";

    // Byte array version of keys because comparison is faster
    private static final byte[] TRUSTED_KEY = Base64.getDecoder().decode(TRUSTED_KEY_BASE64);
    private static final byte[] UNTRUSTED_KEY = Base64.getDecoder().decode(UNTRUSTED_KEY_BASE64);
    private static final byte[] DEV_KEY = Base64.getDecoder().decode(DEV_KEY_BASE64);

    private JarVerifier()
    {

    }

    /**
     * Verify that the given jar is properly signed by one of our accepted certificates.
     * <p/>
     * Reading every entry forces the jar loader to validate any signatures present, so we don't have to
     * actually validate the signature or digest hashes.  The caveat is that if all the signature related
     * jar entries are removed, and also removed from the manifest, then the JVM will not have anything to
     * verify, so we also ensure that the signature files are present.
     *
     * @return {@code true} if the required signature files are present and the main manifest is signed
     */
    public static Verification verify(JarFile jarFile) throws IOException
    {
        Verification verification = null;
        final var jarFileName = jarFile.getName();
        final var manifest = jarFile.getManifest();

        JarEntry signatureFileEntry = null;
        Attributes signatureFileMainAttributes = null;

        boolean signatureBlockFileFound = false;

        final var entries = jarFile.entries();
        while (verification != Verification.Developer && entries.hasMoreElements())
        {
            final var jarEntry = entries.nextElement();
            final var name = jarEntry.getName();

            // Just reading the jar file entry causes any signature to be validated by the JVM, so this will throw a
            // SecurityException if the entry is not properly signed.  If the entry is not signed at all, then this
            // will succeed, and we'll catch that condition below.

            try (var inputStream = jarFile.getInputStream(jarEntry))
            {
                if (isSigningRelated(name))
                {
                    if (name.endsWith(".SF"))
                    {
                        signatureFileEntry = jarEntry;
                        signatureFileMainAttributes = new Manifest(inputStream).getMainAttributes();
                    } else
                    {
                        signatureBlockFileFound = true;
                    }
                } else
                {
                    // Ensure that the entry is signed and has an entry in the manifest (ignoring the manifest itself)
                    if (!JarFile.MANIFEST_NAME.equalsIgnoreCase(name))
                    {
                        // Opening the input stream is one verification of signatures, but to actually get access
                        // to the signers, you have to read the entry, so do that now.
                        final byte[] bytes = inputStream.readAllBytes();

                        if (jarEntry.isDirectory())
                        {
                            // If an entry indicates it is a directory (which just means that it has a name ending
                            // with a '/'), yet it has a non-zero size, it indicates that someone might be trying
                            // to use the "fake directory" technique to spoof verification.
                            // See: https://github.com/x4e/fakedirectory

                            if (bytes.length != 0)
                            {
                                logger.error("Apparent 'fake directory' hack: [{}] in [{}]", name, jarFileName);
                                return Verification.None;
                            } else
                            {
                                continue; // Directories are not signed, so skip them
                            }
                        }

                        Verification entryVerification = validateSigner(jarFile, jarEntry);
                        if (entryVerification == Verification.None)
                        {
                            return Verification.None;
                        } else if (verification == null)
                        {
                            verification = entryVerification;
                        } else if (verification != entryVerification)
                        {
                            logger.error("Entry [{}] verification: {} did not match prior verification: {}", name, entryVerification, verification);
                            return Verification.None;
                        }

                        if (!hasManifestEntry(manifest, name))
                        {
                            logger.error("Missing manifest entry for [{}] in [{}]", name, jarFileName);
                            return Verification.None;
                        }
                    }
                }
            } catch (SecurityException | CertificateEncodingException e)
            {
                logger.error("Verification failed for [{}]: {}", jarFileName, e.getMessage(), e);
                return Verification.None;
            }
        }

        // We can short-circuit here if verification is dev
        if (verification == Verification.Developer) return verification;

        // Verify that we found the signature file and signature block file, and the signature file has attributes
        if (!signatureBlockFileFound || signatureFileMainAttributes == null)
        {
            logger.error("Signature files not found in [{}]", jarFileName);
            return Verification.None;
        }

        // Finally validate that the signature file contains a hash for the manifest, ignoring the algorithm used
        Optional<Object> manifestHash = signatureFileMainAttributes.keySet().stream().filter(key -> key.toString().endsWith(DIGEST_MANIFEST_SUFFIX)).findFirst();
        if (manifestHash.isPresent())
        {
            return verification;
        } else
        {
            logger.error("Signature file [{}] in [{}] does not contain '{}'", signatureFileEntry.getName(), jarFileName, DIGEST_MANIFEST_SUFFIX);
            return Verification.None;
        }
    }

    /**
     * @return {@code true} if the given name is a supported Signature File or PKCS7 block file name.
     *         Copied mostly from {@code sun.security.util.SignatureFileVerifier}.
     */
    private static boolean isSigningRelated(String name)
    {
        name = name.toUpperCase(Locale.ENGLISH);

        if (!name.startsWith(META_INF)) return false;

        name = name.substring(META_INF.length());
        return name.indexOf('/') == -1 &&
                (name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA") || name.endsWith(".EC"));
    }

    /**
     * @return {@code true} if the manifest has an attribute for the given entry name
     */
    private static boolean hasManifestEntry(Manifest manifest, String name)
    {
        return manifest.getAttributes(name) != null
                || manifest.getAttributes("./" + name) != null
                || manifest.getAttributes("/" + name) != null;
    }

    /**
     * @return the verification level of the jar entry
     */
    private static Verification validateSigner(JarFile jarFile, JarEntry jarEntry) throws CertificateEncodingException
    {
        final String jarFileName = jarFile.getName();
        final String name = jarEntry.getName();
        final var codeSigners = jarEntry.getCodeSigners();

        if (codeSigners == null)
        {
            logger.debug("Unsigned entry [{}] in [{}]", name, jarFileName);
            return Verification.None;
        }

        if (codeSigners.length != 1)
        {
            logger.debug("Should have exactly 1 signer on [{}] in [{}]", name, jarFileName);
            return Verification.None;
        }

        final var certificates = codeSigners[0].getSignerCertPath().getCertificates();
        if (certificates.isEmpty())
        {
            logger.debug("No signer certificates on [{}] in [{}]", name, jarFileName);
            return Verification.None;
        }

        final var certificate = certificates.get(0);
        if (certificate instanceof X509Certificate x509Certificate)
        {
            byte[] raw = x509Certificate.getEncoded();

            if (Arrays.equals(raw, DEV_KEY))
            {
                return Verification.Developer;
            } else if (Arrays.equals(raw, TRUSTED_KEY))
            {
                return Verification.Trusted;
            } else if (Arrays.equals(raw, UNTRUSTED_KEY))
            {
                return Verification.Untrusted;
            }
        } else
        {
            if (logger.isDebugEnabled()) logger.debug("Not an X.509 certificate on [{}] in [{}]: got {}", name, jarFileName, certificate.getType());
            return Verification.None;
        }

        return Verification.None;
    }
}
