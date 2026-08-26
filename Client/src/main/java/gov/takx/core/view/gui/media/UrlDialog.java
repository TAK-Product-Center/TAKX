package gov.takx.core.view.gui.media;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.media.AudioBuilder;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.ImageBuilder;
import gov.takx.api.media.VideoBuilder;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog allowing user to add a URL
 *
 * @since 2.8
 */
public class UrlDialog extends AStandardDialog
{
    private static final String USER = "user"; // Media source key

    // Additional protocols not currently supported by URL that should be directly converted to URIs
    private static final List<String> ADDITIONAL_PROTOCOLS = List.of("rtsp", "udp");

    private final IMediaManager mediaManager;
    private final Consumer<IMedia> onAcceptMediaHandler;
    private final boolean updating;
    private String originalName;

    /**
     * Creates a dialog to input a name and URL pair. If {@code onAcceptMediaHandler} is not {@code null}, then when the
     * user clicks Ok, the media object created from the name and URL will be passed to it, otherwise the media object
     * will be passed to {@link IMediaManager#addMedia(IMedia)}.
     *
     * @param mediaManager The {@link IMediaManager} to facilitate the creation of the new {@link IMedia} object.
     * @param onAcceptMediaHandler A {@link Consumer} to accept the created media. If {@code null}, {@link IMediaManager#addMedia(IMedia)}
     *                             will be called to add the media directly.
     * @since 5.2
     */
    public UrlDialog(@Nonnull IMediaManager mediaManager, @Nullable Consumer<IMedia> onAcceptMediaHandler)
    {
        this(mediaManager, onAcceptMediaHandler != null ? onAcceptMediaHandler : mediaManager::addMedia, false);
    }

    /**
     * Creates a dialog to edit a name and URL pair. When the user clicks Ok, the media object created from the name and URL
     * will be passed to the {@code onAcceptMediaHandler} and that is responsible for adding the media to the {@link IMediaManager}.
     *
     * @param mediaManager The {@link IMediaManager} to facilitate the creation of the new {@link IMedia} object.
     * @param onAcceptMediaHandler A {@link Consumer} to accept the created media
     * @param updating if this dialog is being used to update an existing {@link IMedia} object ({@code true}) or not ({@code false})
     * @since 5.3
     */
    public UrlDialog(@Nonnull IMediaManager mediaManager, @Nonnull Consumer<IMedia> onAcceptMediaHandler, boolean updating)
    {
        this.mediaManager = mediaManager;
        this.onAcceptMediaHandler = onAcceptMediaHandler;
        this.updating = updating;

        initComponents();

        if (!updating)
        {
            // Auto-name "Stream (n)"
            String autoName = mediaManager.createUniqueName("Stream");
            nameTextField.setText(autoName);
            nameTextField.setSelectionStart(0);
            nameTextField.setSelectionEnd(autoName.length());
        }
    }

    /**
     * @param name  The name of the stream
     * @since 5.2
     */
    public void setName(@Nullable String name)
    {
        nameTextField.setText(name);
        originalName = name;
    }

    /**
     * @param url  The URL of the stream
     * @since 5.2
     */
    public void setUrl(@Nullable String url)
    {
        urlTextField.setText(url);
    }

    private void okButtonActionPerformed()
    {
        final String nameFieldText = nameTextField.getText().trim();
        final boolean needUniqueName = !updating || !nameFieldText.equals(originalName);
        final String name = needUniqueName ? mediaManager.createUniqueName(nameFieldText) : nameFieldText;
        final String url = urlTextField.getText().trim();

        final URI source;

        // Ensure it is a legal URL
        try
        {
            source = getUri(url);
        } catch (URISyntaxException | MalformedURLException e)
        {
            AlwaysOnTopJOptionPane.showErrorDialog(e.getMessage(), "Invalid URL");
            return;
        }

        // NOTE, most URLs for video streams do not have a file extension, so if the extension does not imply still image
        // or audio, assume it is a video stream
        final var media = switch (mediaManager.getType(url))
        {
            case Image -> new ImageBuilder(name, USER, source).build();
            case Audio -> new AudioBuilder(name, USER, source).build();
            case Video, Unknown -> new VideoBuilder(name, USER, source).build();
        };
        onAcceptMediaHandler.accept(media);
        dispose();
    }

    /**
     * Returns a URL string's URI. The URL is validated by first creating a URL object, unless the protocol is in the
     * exceptions list.
     *
     * @param url The URL String to convert
     * @return The URL's URI
     * @throws MalformedURLException Thrown if the protocol is unrecognized or string could not be parsed
     * @throws URISyntaxException Thrown if the string could not be parsed into a URI
     * @since 5.0
     */
    @VisibleForTesting
    static URI getUri(@Nonnull String url) throws MalformedURLException, URISyntaxException
    {
        return ADDITIONAL_PROTOCOLS.stream().anyMatch(url::startsWith) ? URI.create(url) : new URL(url).toURI();
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void inputUpdated()
    {
        final String name = nameTextField.getText();
        final String url = urlTextField.getText();

        okButton.setEnabled(!StringUtils.isEmpty(name, true) && !StringUtils.isEmpty(url, true));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        nameTextField = new JTextField();
        var label2 = new JLabel();
        urlTextField = new JTextField();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Add a URL");
        setMinimumSize(new Dimension(300, 165));
        setPreferredSize(new Dimension(500, 180));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setPreferredSize(new Dimension(400, 115));
            dialogPane.setMinimumSize(new Dimension(300, 115));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setPreferredSize(new Dimension(200, 50));
                contentPanel.setMinimumSize(new Dimension(200, 50));
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "default, $lgap, default"));

                //---- label1 ----
                label1.setText("Media Name:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //---- nameTextField ----
                nameTextField.setToolTipText("Select a name to help identify the stream");
                nameTextField.setName("nameTextField");
                nameTextField.addCaretListener(e -> inputUpdated());
                contentPanel.add(nameTextField, cc.xy(3, 1));

                //---- label2 ----
                label2.setText("URL:");
                label2.setName("label2");
                contentPanel.add(label2, cc.xy(1, 3));

                //---- urlTextField ----
                urlTextField.setToolTipText("The network address of the stream");
                urlTextField.setName("urlTextField");
                urlTextField.addCaretListener(e -> inputUpdated());
                contentPanel.add(urlTextField, cc.xy(3, 3));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setPreferredSize(new Dimension(200, 40));
                buttonBar.setMinimumSize(new Dimension(200, 40));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField nameTextField;
    private JTextField urlTextField;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
