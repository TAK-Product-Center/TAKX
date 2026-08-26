package gov.takx.core.view.gui.media;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link UrlDialog}
 * 
 * @since 5.0
 */
public class UrlDialogTest
{
    @Test
    public void getUri_completesSuccessfully_whenProtocolValid() throws MalformedURLException, URISyntaxException
    {
        // Standard protocols accepted by Java's URL class
        UrlDialog.getUri("http://test");
        UrlDialog.getUri("https://test");
        UrlDialog.getUri("ftp://test");
        UrlDialog.getUri("file://test");
        UrlDialog.getUri("jar:file://test.jar!/file");

        // Additional protocols not accepted by Java's URL class
        UrlDialog.getUri("rtsp://test");
        UrlDialog.getUri("udp://test");
    }

    @Test
    public void getUri_throwsException_whenProtocolInvalid()
    {
        assertThatThrownBy(() -> UrlDialog.getUri("bad://test")).isExactlyInstanceOf(MalformedURLException.class);
    }

    @Test
    public void getUri_throwsException_whenContainsIllegalChars()
    {
        assertThatThrownBy(() -> UrlDialog.getUri("http://<test>")).isExactlyInstanceOf(URISyntaxException.class);
    }
}
