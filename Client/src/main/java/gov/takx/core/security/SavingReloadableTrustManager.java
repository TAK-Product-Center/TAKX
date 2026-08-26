package gov.takx.core.security;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Custom trust manager to manage checking if a certificate is trusted and to store the certificate chain.  Also
 * provides the ability to reload the trust manager from a keystore that may have had a cert added to it.
 */
final class SavingReloadableTrustManager implements X509TrustManager
{
    private X509TrustManager x509TrustManager;
    private X509Certificate[] chain;

    SavingReloadableTrustManager(X509TrustManager trustManager)
    {
        x509TrustManager = trustManager;
    }

    SavingReloadableTrustManager(String path) throws Exception
    {
        reloadTrustManager(path);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        return x509TrustManager.getAcceptedIssuers();
    }

    // Not applicable when running as a client.
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
    {
        this.chain = chain;
        x509TrustManager.checkServerTrusted(chain, authType);
    }

    protected X509Certificate[] getCertificateChain()
    {
        return chain;
    }

    private void reloadTrustManager(String trustStorePath) throws Exception
    {
        // load keystore from specified cert store (or default)
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream(trustStorePath))
        {
            keyStore.load(in, null);
        }

        // initialize a new TMF with the ts we just loaded
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // acquire X509 trust manager from factory
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        for (TrustManager trustManager : trustManagers)
        {
            if (trustManager instanceof X509TrustManager)
            {
                x509TrustManager = (X509TrustManager) trustManager;
                return;
            }
        }

        throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");
    }
}
