package gov.takx.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509KeyManager;

import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Our custom key manager.  In the event it is constructed with a X509Certificate, it will override
 * the method which returns the alias of the PKI key to use.  Otherwise, it will relegate calls to the
 * default KeyManager that it is constructed with.
 *
 */
class PKIKeyManager implements X509KeyManager
{
    private X509Certificate certificate;
    private X509KeyManager manager;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public PKIKeyManager(X509KeyManager manager, X509Certificate cert)
    {
        this.certificate = cert;
        this.manager = manager;
    }

    /**
     * Return the selected PKI key to the server.  If no key/certificate has been selected, let the default
     * Key manager try to pick the correct key.
     *
     * @param strings    the key algorithm type name(s), ordered with the most-preferred key type first.
     * @param principals the list of acceptable CA issuer subject names or {@code null} if it does not matter which issuers are used.
     * @param socket     the socket to be used for this connection. This parameter can be {@code null}, which indicates that implementations are free to select an alias applicable to any socket.
     * @return the alias name for the desired key, or {@code null} if there are no matches.
     */
    @Override
    public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket)
    {
        if (certificate == null)
        {
            return manager.chooseClientAlias(strings, principals, socket);
        } else
        {
            try
            {
                return CertUtils.getAliasFromCertificate(certificate);
            } catch (Exception e)
            {
                logger.error("Error getting alias from cert", e);
            }
        }
        return "";
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers)
    {
        return manager.getClientAliases(keyType, issuers);
    }

    @Override
    public String[] getServerAliases(String s, Principal[] principals)
    {
        return manager.getServerAliases(s, principals);
    }

    @Override
    public String chooseServerAlias(String s, Principal[] principals, Socket socket)
    {
        return manager.chooseServerAlias(s, principals, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias)
    {
        return manager.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias)
    {
        return manager.getPrivateKey(alias);
    }
}
