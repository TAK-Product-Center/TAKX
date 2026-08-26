package gov.tak.api.comms.security;

import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;

/**
 * Contract for service that manages the storage of SSL Certificates.
 *
 * @since 3.0
 */
@NoPluginImplementations
public interface ISslCertificateManager
{
    /**
     * Determine if a certificate with the given file name is present in our certificate store.
     *
     * @param fileName Name of certificate file (may be an absolute file path; if so, path will be removed before
     *                 checking the store)
     * @return {@code true} if a file with that name exists in the certificate store
     */
    boolean certExists(@Nonnull String fileName);

    /**
     * @param fileName Name of certificate file to obtain (may be an absolute file path; if so, path will be removed
     *                 before checking the store)
     * @return File referencing the certificate with the given name within the certificate store. The file may not exist.
     */
    @Nonnull
    File getCertFile(@Nonnull String fileName);

    /**
     * Copies the certificate at the provided path to the VFS with the same name as the provided cert, unless a
     * different cert already exists with that name, in which case a unique string is appended to the file name to avoid
     * overwriting the existing certificate.
     *
     * @param certFilePath The absolute path to the certificate file to be copied
     * @return The new file within the VFS certificate store (the file name may differ from the name of the provided
     * file to ensure uniqueness).
     * @throws IOException If a file doesn't exist for the provided path or if the file can't be written to the VFS
     */
    @Nonnull
    File copyCert(@Nonnull String certFilePath) throws IOException;

    /**
     * Create (or overwrite) a certificate file with the given name.
     *
     * @param fileName  Name of certificate file to create/overwrite, relative to the VFS certificate store
     * @param content   Content to write into the certificate file
     * @return The new file within the VFS certificate store, the file name will differ from the name provided to ensure
     * uniqueness by appending a short hash of the file content.
     * @throws IOException If the file can't be created or written
     */
    @Nonnull
    File createCert(@Nonnull String fileName, @Nonnull byte[] content) throws IOException;

    /**
     * Communicate with the indicated TAK Server to generate a client certificate for the user which will be used for
     * connecting to the TAK Server via common commo. As a result of this call, the provided
     * {@link StreamingConnectionConfiguration} will be updated with the new client certificate and truststore.
     *
     * @param connectionConfig The connection configuration containing the TAK Server host name, username, and password
     * @throws IOException if an error occurred enrolling for the client certificate.
     * @see <a Certificate+Enrollment">Certificate Enrollment confluence page</a>
     * for description of certificate enrollment process
     */
    void enrollForClientCertificate(@Nonnull StreamingConnectionConfiguration connectionConfig) throws IOException;
}
