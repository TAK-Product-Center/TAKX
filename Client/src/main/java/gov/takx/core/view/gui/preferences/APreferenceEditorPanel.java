package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.commons.swing.MouseClickedListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;

/**
 * Base for panels that edit a single preference entry.  The panel shows a title and description, holds a control to
 * display the current preference setting, and a hidden panel for showing a control to edit the value with ok/cancel
 * buttons.  The use of the hidden edit panel is controlled by appropriate subclasses.
 * <p>
 * Subclasses must implement {@link #handleMainPanelClick()}.
 *
 * @since 5.4
 */
@PseudoBean
public abstract class APreferenceEditorPanel extends JPanel
{
    @Inject
    protected ICoreExecutorService executorService;

    protected final IPreferenceEntry preferenceEntry;
    protected final IEditorComponent editorComponent;

    /**
     * Constructor for use cases that don't have a hidden edit control.
     * 
     * @param parent Parent scroll pane that will contain this panel
     * @param preferenceEntry Preference entry to edit
     */
    APreferenceEditorPanel(@Nonnull JScrollPane parent, @Nonnull IPreferenceEntry preferenceEntry)
    {
        this(parent, preferenceEntry, null);
    }

    /**
     * @param parent Parent scroll pane that will contain this panel
     * @param preferenceEntry Preference entry to edit
     * @param editorComponent Optional editor component
     */
    APreferenceEditorPanel(@Nonnull JScrollPane parent, @Nonnull IPreferenceEntry preferenceEntry, @Nullable IEditorComponent editorComponent)
    {
        this.preferenceEntry = preferenceEntry;
        this.editorComponent = editorComponent;

        initComponents();

        titleLabel.setText(preferenceEntry.getTitle());

        PreferencesComponentFactory.configureResizingTextArea(descriptionTextArea, parent);
        descriptionTextArea.setText(preferenceEntry.getDescription());

        if (editorComponent != null) editorControlHolder.add(editorComponent.getComponent());
    }

    /**
     * Set the component that will display the current value of the preference.
     */
    protected void setValueDisplayComponent(@Nonnull JComponent valueDisplayComponent)
    {
        valueDisplayComponent.setOpaque(false);
        valueDisplayHolder.add(valueDisplayComponent, BorderLayout.CENTER);
    }

    /**
     * Set the handler to invoke when the Ok button is clicked.
     */
    protected void setOkHandler(@Nonnull Runnable okHandler)
    {
        okButton.addMouseListener((MouseClickedListener) e -> okHandler.run());
    }

    /**
     * Set the handler to invoke when the Cancel button is clicked.
     */
    protected void setCancelHandler(@Nonnull Runnable cancelHandler)
    {
        cancelButton.addMouseListener((MouseClickedListener) e -> cancelHandler.run());
    }

    /**
     * To be called whenever a component in the main panel is clicked (the title, description, and value display).
     */
    protected void mainPanelMouseClicked(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1) handleMainPanelClick();
    }

    /**
     * Handle a click on the main panel components.
     */
    protected abstract void handleMainPanelClick();

    /**
     * Update the value display component with the current value of the preference.  Default does nothing.
     */
    protected void updateDisplayValue()
    {
    }

    /**
     * Reset the size of the description text area.  This allows it to properly grow/shrink when the containing panel
     * is resized.
     */
    protected void resetDescriptionSize()
    {
        descriptionTextArea.setSize(0, 0);
    }

    /**
     * Perform an action on a background thread.
     */
    protected CompletableFuture<Void> doInBackground(@Nonnull Runnable action)
    {
        return CompletableFuture.runAsync(() -> {
            Thread.currentThread().setName("Update preference " + preferenceEntry.name());
            action.run();
        }, executorService);
    }

    @Override
    public String toString()
    {
        return "APreferenceEditorPanel{preferenceEntry=" + preferenceEntry + '}';
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var panel1 = new JPanel();
        var separator1 = new JSeparator();
        var contentPanel = new JPanel();
        var mainPanel = new JPanel();
        titleLabel = new JLabel();
        descriptionTextArea = new JTextArea();
        valueDisplayHolder = new JPanel();
        editorPanel = new JPanel();
        editorControlHolder = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(4, 4, 0, 0));
        setName("this");
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                mainPanelMouseClicked(e);
            }
        });
        setLayout(new BorderLayout());

        //======== panel1 ========
        {
            panel1.setBorder(new EmptyBorder(4, 0, 0, 0));
            panel1.setName("panel1");
            panel1.setLayout(new BorderLayout());

            //---- separator1 ----
            separator1.setName("separator1");
            panel1.add(separator1, BorderLayout.CENTER);
        }
        add(panel1, BorderLayout.SOUTH);

        //======== contentPanel ========
        {
            contentPanel.setName("contentPanel");
            contentPanel.setLayout(new BorderLayout());

            //======== mainPanel ========
            {
                mainPanel.setName("mainPanel");
                mainPanel.setLayout(new BorderLayout());

                //---- titleLabel ----
                titleLabel.setText("Title");
                titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD, titleLabel.getFont().getSize() + 1f));
                titleLabel.setName("titleLabel");
                titleLabel.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        mainPanelMouseClicked(e);
                    }
                });
                mainPanel.add(titleLabel, BorderLayout.NORTH);

                //---- descriptionTextArea ----
                descriptionTextArea.setText("description");
                descriptionTextArea.setEditable(false);
                descriptionTextArea.setBorder(null);
                descriptionTextArea.setOpaque(false);
                descriptionTextArea.setName("descriptionTextArea");
                descriptionTextArea.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        mainPanelMouseClicked(e);
                    }
                });
                mainPanel.add(descriptionTextArea, BorderLayout.CENTER);
            }
            contentPanel.add(mainPanel, BorderLayout.CENTER);

            //======== valueDisplayHolder ========
            {
                valueDisplayHolder.setBorder(new EmptyBorder(0, 0, 0, 4));
                valueDisplayHolder.setOpaque(false);
                valueDisplayHolder.setName("valueDisplayHolder");
                valueDisplayHolder.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        mainPanelMouseClicked(e);
                    }
                });
                valueDisplayHolder.setLayout(new BorderLayout());
            }
            contentPanel.add(valueDisplayHolder, BorderLayout.EAST);

            //======== editorPanel ========
            {
                editorPanel.setName("editorPanel");
                editorPanel.setLayout(new FormLayout(
                        "default:grow, 2*($lcgap, default)",
                        "default, $lgap, default"));

                //======== editorControlHolder ========
                {
                    editorControlHolder.setName("editorControlHolder");
                    editorControlHolder.setLayout(new BorderLayout());
                }
                editorPanel.add(editorControlHolder, cc.xywh(1, 1, 5, 1));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                editorPanel.add(okButton, cc.xy(3, 3));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                editorPanel.add(cancelButton, cc.xy(5, 3));
            }
            contentPanel.add(editorPanel, BorderLayout.SOUTH);
        }
        add(contentPanel, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel titleLabel;
    protected JTextArea descriptionTextArea;
    protected JPanel valueDisplayHolder;
    protected JPanel editorPanel;
    private JPanel editorControlHolder;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
