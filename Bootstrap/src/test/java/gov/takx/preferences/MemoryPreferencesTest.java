package gov.takx.preferences;

import gov.takx.bootstrap.PreferencesFactoryConstants;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test our custom {@code MemoryPreferencesFactory}.  All we really need to test
 * are the custom aspects of our preferences implementation, such as path handling on get.  This test is not
 * intended to fully exercise the Preferences API.
 *
 * @since 2.0.0
 */
public class MemoryPreferencesTest
{
    static
    {
        // Configure our custom Preferences factory. Note: for execution, this is handled in the vmoptions. For other
        // tests, this is handled in AContainerTest. We don't want to extend AContainerTest for bootstrap, since there
        // really can't be the concept of a container, so just set the property directly.
        System.setProperty("java.util.prefs.PreferencesFactory", PreferencesFactoryConstants.PREFERENCES_FACTORY_CLASS);
    }

    @Test
    public void testSimpleValues() throws BackingStoreException
    {
        final Preferences userRoot = Preferences.userRoot();
        assertThat(userRoot).isInstanceOf(MemoryPreferences.class);

        assertThat(userRoot.nodeExists("TESTME")).isFalse();
        final Preferences node = userRoot.node("TESTME");

        final String key = "k1";

        assertThat(node.get(key, "default")).isEqualTo("default");

        node.put(key, "v1");
        assertThat(node.get(key, "")).isEqualTo("v1");

        node.remove(key);
        assertThat(node.get(key, "default")).isEqualTo("default");

        // Verify that inserting a value containing a slash is found as a value and does not create sub-nodes
        final String keyWithSlash = "foo/bar";
        node.put(keyWithSlash, "baz");
        assertThat(node.get(keyWithSlash, "")).isEqualTo("baz");
        assertThat(node.nodeExists("foo")).isFalse();
    }

    @Test
    public void testChildren() throws BackingStoreException
    {
        assertThat(Preferences.userRoot().nodeExists("TESTCHILDREN")).isFalse();
        final Preferences node = Preferences.userRoot().node("TESTCHILDREN");

        assertThat(node.childrenNames()).isEmpty();

        final Preferences n1 = node.node("N1");
        assertThat(n1).isNotNull();
        final Preferences n2 = node.node("N2");
        assertThat(n2).isNotNull();

        assertThat(node.childrenNames()).hasSize(2);
        assertThat(node.nodeExists("N1")).isTrue();
        assertThat(node.nodeExists("N2")).isTrue();

        assertThat(node.childrenNames())
                .hasSize(2)
                .containsOnly("N1", "N2");

        final Preferences subNode = node.node("N2/C1/C2/C3");
        subNode.put("k1", "v1");

        assertThat(node.nodeExists("N2")).isTrue();
        assertThat(node.nodeExists("N2/C1")).isTrue();
        assertThat(node.nodeExists("N2/C1/C2")).isTrue();
        assertThat(node.nodeExists("N2/C1/C2/C3")).isTrue();

        // Test value retrieval using a path
        assertThat(node.get("N2/C1/C2/C3/k1", "")).isEqualTo("v1");
    }
}
