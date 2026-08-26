package gov.takx.core.view.gui.preferences.network;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link NetworkInputOutputEditPanel}
 *
 * @since 5.0
 */
public class NetworkInputOutputEditPanelTest
{
    private static final PrintStream outputStreamGobbler = new PrintStream(new OutputStream()
    {
        @Override
        public void write(int b)
        {
        }
    });

    private static PrintStream originalSystemOutStream;

    /**
     * Use a stream gobbler to consume {@code System.out} calls and silence beeps during CL tests runs.
     * <p>
     * {@code HeadlessToolkit} uses {@code System.out.write(0x07);} for its {@code beep()} implementation, so ignoring
     * {@code System.out} calls effectively silences the audio alerts generated when testing invalid {@code JTextField}
     * text.
     *
     * @see javax.swing.LookAndFeel#provideErrorFeedback(Component)
     */
    @BeforeAll
    static void beforeAll()
    {
        originalSystemOutStream = System.out;
        System.setOut(outputStreamGobbler);
    }

    @AfterAll
    static void afterAll()
    {
        System.setOut(originalSystemOutStream);
    }

    @Test
    public void getUri_returnsUriInCorrectFormat()
    {
        final NetworkInputOutputEditPanel panel = new NetworkInputOutputEditPanel();

        panel.setProtocol("udp");
        panel.setHost("host");
        panel.setPort("1111");

        assertThat(panel.getUri()).isEqualTo("udp://host:1111");

        panel.setProtocol("tcp");
        panel.setHost("    host ");
        panel.setPort("2222");

        assertThat(panel.getUri()).isEqualTo("tcp://host:2222");
    }

    @Test
    public void setPort_onlyAllowsNumerics()
    {
        final NetworkInputOutputEditPanel panel = new NetworkInputOutputEditPanel();

        panel.setPort("1111");
        assertThat(panel.getPort()).isEqualTo("1111");

        panel.setPort("aaaa");
        assertThat(panel.getPort()).isEqualTo("");
    }

    @Test
    public void setDescription_onlyAllowsAlphanumericsAndSpaces()
    {
        final NetworkInputOutputEditPanel panel = new NetworkInputOutputEditPanel();

        panel.setDescription("This is a test");
        assertThat(panel.getDescription()).isEqualTo("This is a test");

        panel.setDescription("/");
        assertThat(panel.getDescription()).isEqualTo("");

        panel.setDescription("<");
        assertThat(panel.getDescription()).isEqualTo("");

        panel.setDescription(">");
        assertThat(panel.getDescription()).isEqualTo("");

        panel.setDescription("!");
        assertThat(panel.getDescription()).isEqualTo("");

        panel.setDescription("Hello!");
        assertThat(panel.getDescription()).isEqualTo("");
    }

    @Test
    public void setDescription_limitsInputLength()
    {
        final String maxChars = "x".repeat(Preferences.MAX_KEY_LENGTH);
        final String maxPlusOneChars = "x".repeat(Preferences.MAX_KEY_LENGTH + 1);

        final NetworkInputOutputEditPanel panel = new NetworkInputOutputEditPanel();

        panel.setDescription(maxChars);
        assertThat(panel.getDescription()).isEqualTo(maxChars);

        panel.setDescription(maxPlusOneChars);
        assertThat(panel.getDescription()).isEqualTo("");
    }
}