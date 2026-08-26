package gov.takx.core.globe.layers.worldwind;

import gov.takx.commons.utils.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A basic password manager for password-protected WMS servers.  Caches usernames and passwords in memory for WMS
 * retrieval.  The usernames and passwords are NOT encrypted in memory, so this class is not secure.  It is only
 * intended as a convenient means to access password-protected WMS servers.
 * <p/>
 * todo: possibly make this accessible only through project session
 *
 * @since 1.43
 */
public class HTTPPasswordManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Singleton instance of this class.
     */
    private static final HTTPPasswordManager instance = new HTTPPasswordManager();

    /**
     * A (non-encrypted) map of host/port key Strings to credentials.
     */
    private final Map<String, HTTPLoginInfo> userPassMap = new ConcurrentHashMap<>();

    /**
     * A set of sites (host and port combinations) that have been validated.  Validated means one of two things:
     * <p/>
     * 1) The site can be accessed with the current authentication data.
     * <p/>
     * 2) The site should just be ignored by this class even if it cannot be accessed.  In this case, the user may
     * not know the login information for this site and does not want to be prompted every time an attempt is made to
     * access it.
     */
    private final Set<String> validSites = ConcurrentHashMap.newKeySet();

    // Set of site urls that have received SC_UNAUTHORIZED responses, meaning we need to try adding authentication credentials
    private final Set<String> sitesNeedingAuthentication = ConcurrentHashMap.newKeySet();

    /**
     * Password manager view.  Responsible for displaying site login prompt and authentication editor.
     */
    private final IPasswordManagerView view = new PasswordManagerView();

    /**
     * @return the singleton instance of this class.
     */
    public static HTTPPasswordManager getInstance()
    {
        return instance;
    }

    /**
     * Singleton - private constructor
     */
    private HTTPPasswordManager()
    {
    }

    public void displayUserPrompt(URL url)
    {
        view.displayUserPrompt(getBaseURL(url));
    }

    /**
     * Display login information editor (add/remove arbitrary authentications).
     */
    public void displayEditor()
    {
        view.displayEditor();
    }

    /**
     * Get a set of the currently cached host/port/username combinations.  Password information is not included.
     *
     * @return a set of the currently cached host/port/username combinations.
     */
    public Set<HTTPLoginInfo> getLoginInfo()
    {
        return userPassMap.values().stream()
                .map(auth -> new HTTPLoginInfo(auth.url, auth.username))
                .collect(Collectors.toSet());
    }

    /**
     * Set authentication for a site (host/port).  Overwrites previous authentication for site if one exists.
     *
     * @param username authentication username.
     * @param password authentication password.
     */
    public void setAuthentication(String url, String username, String password)
    {
        if (StringUtils.isEmpty(url, true))
        {
            logger.warn("Unable to set authentication for empty URL.");
            return;
        }

        userPassMap.put(url, new HTTPLoginInfo(url, username, password));
    }

    /**
     * Remove authentication for a site (host/port).
     */
    public void removeAuthentication(String url)
    {
        userPassMap.remove(url);
        setSiteValidated(url, false);
    }

    /**
     * Set validity of a site (host/port).  Typically called internally by this class during configuration of an open
     * HTTP connection after testing whether the URL is accessible with the current authorizations.  However, a user
     * may sometimes be interested in manually modifying the validity of a site.  For example, a login prompt dialog
     * may include a "Do Not Ask Again" checkbox which is intended for users who do not know the login information for
     * a site and do not want to be pestered with more login prompts for the same site.
     *
     * @param validated {@code true} if the site is valid, or {@code false} otherwise.
     */
    public void setSiteValidated(String url, boolean validated)
    {
        if (StringUtils.isEmpty(url, true)) return;

        if (validated)
        {
            validSites.add(url);
        } else
        {
            validSites.remove(url);
        }
    }

    /**
     * Add the Base64-encoded authentication to the given connection, if there is any recorded.
     *
     * @param url base URL String for password protected host.
     * @param urlConnection Connection to update
     */
    private void addAuthorizationToConnection(String url, URLConnection urlConnection)
    {
        HTTPLoginInfo loginAuth = userPassMap.get(url);
        final String userPass = loginAuth != null ? loginAuth.authentication : null;
        if (userPass != null) urlConnection.setRequestProperty("Authorization", userPass);
    }

    /**
     * Mark a connection as requiring authentication.
     * @since 3.4
     */
    public void setNeedsAuthentication(URLConnection urlConnection)
    {
        sitesNeedingAuthentication.add(getBaseURL(urlConnection.getURL()));
    }

    /**
     * Update an HTTP URL Connection with authentication information.
     *
     * @param urlConnection connection to which authentication information will be added.
     */
    public void configureHTTPAuthorization(HttpURLConnection urlConnection)
    {
        final URL url = urlConnection.getURL();
        final String baseURL = getBaseURL(url);

        if (sitesNeedingAuthentication.contains(baseURL))
        {
            while (!validSites.contains(baseURL))
            {
                displayUserPrompt(url);

                if (isSiteAuthenticated(url))
                {
                    sitesNeedingAuthentication.remove(baseURL);
                    addAuthorizationToConnection(baseURL, urlConnection);
                    break;
                }
            }
        } else
        {
            addAuthorizationToConnection(baseURL, urlConnection);
        }
    }

    /**
     * Get a key base URL String for a site URL.  The key is a combination of the site's host and port.
     *
     * @param url site URL for which a key will be created.
     * @return key created from URL.
     */
    public static String getBaseURL(URL url)
    {
        String fullURL = url.toString();
        String query = url.getQuery();
        if (query != null && !query.isEmpty())
        {
            String stripped = fullURL.replace(query, "");
            if (stripped.endsWith("?"))
            {
                stripped = stripped.substring(0, stripped.length() - 1);
            }
            return stripped;
        }
        return fullURL;
    }

    /**
     * @return {@code false} if a 401 (Unauthorized) response is received from the server, otherwise
     * {@code true}.
     */
    private boolean isSiteAuthenticated(URL url)
    {
        try
        {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            addAuthorizationToConnection(getBaseURL(url), urlConnection);

            urlConnection.connect();
            if (urlConnection.getResponseCode() != HttpStatus.SC_UNAUTHORIZED)
            {
                validSites.add(getBaseURL(url));
                return true;
            }
            return false;
        } catch (Exception e)
        {
            logger.debug("Exception testing (creating) HTTP connection", e);
            return true;
        }
    }
}
