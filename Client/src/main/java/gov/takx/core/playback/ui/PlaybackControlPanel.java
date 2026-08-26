package gov.takx.core.playback.ui;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.playback.IPlaybackSystem;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.PlaybackConfigurationAdapter;
import gov.takx.api.playback.ui.IPlaybackControls;
import gov.takx.commons.swing.SimpleAction;
import gov.takx.commons.swing.SimpleAction.ActionPerformedListener;
import gov.takx.platform.playback.TimeSpanAdapter;
import gov.takx.platform.playback.TimelineUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Primary control panel for the playback system.  This panel contains the standard controls like start, pause, and stop,
 * as well as buttons to manipulate the timeline.
 * <p/>
 * Plugins can contribute custom components to be added into the toolbar.
 *
 * @since 1.47
 */
public class PlaybackControlPanel extends JPanel implements IPlaybackControls
{
    private static final ImageIcon PLAY_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "play.png");
    private static final ImageIcon PAUSE_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "pause.png");
    private static final ImageIcon STOP_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "stop.png");
    private static final ImageIcon FIT_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "playback/fit.svg");
    private static final ImageIcon SCALE_IN_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "playback/scale_in.svg");
    private static final ImageIcon SCALE_OUT_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "playback/scale_out.svg");
    private static final ImageIcon PAN_LEFT_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "arrow_left.svg");
    private static final ImageIcon PAN_RIGHT_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "arrow_right.svg");
    private static final ImageIcon LIVE_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "playback/live.svg");

    private static final CellConstraints CC = new CellConstraints();
    private static final Integer[] PLAYBACK_SPEEDS = {1, 2, 4, 8, 16, 32, 64, 128, 256};

    private final IPlaybackSystem playbackSystem;
    private final JMenuBar viewControlMenuBar = new JMenuBar();
    private final JMenu viewControlMenu = new JMenu("-View Control-");

    public PlaybackControlPanel(IPlaybackSystem playbackSystem)
    {
        this.playbackSystem = playbackSystem;

        initComponents();

        speedComboBox.setModel(new DefaultComboBoxModel<>(PLAYBACK_SPEEDS));
        speedComboBox.setRenderer(new SpeedListCellRenderer());

        // Any time the playback speed is changed via the API, we need to update the UI to match
        playbackSystem.getPlaybackConfiguration().addPlaybackConfigurationListener(new PlaybackSpeedChangeHandler());

        // Any time the time span changes, we need to update our controls
        playbackSystem.getTimeSpan().addListener(new TimeSpanChangeHandler());

        // Add an auto-timer mouse listener to the pan-left/right buttons
        AutoTimerButtonHandler.configure(panLeftButton);
        AutoTimerButtonHandler.configure(panRightButton);

        configureViewControls();
        refreshPlaybackSpeed();
        expandIndividualViewControlButtons();
        updateTimeLabels();
    }

    /**
     * Configure all of our view control buttons and the collapsed view control menu.
     * @since 5.7
     */
    private void configureViewControls()
    {
        configureControlButton(playButton, "Play", PLAY_ICON, l -> playbackSystem.play());
        configureControlButton(pauseButton, "Pause", PAUSE_ICON, l -> playbackSystem.pause());
        configureControlButton(stopButton, "Stop", STOP_ICON, l -> playbackSystem.stop());
        configureControlButton(scaleInButton, "Scale In", SCALE_IN_ICON, l -> playbackSystem.getTimeSpan().scaleTimeIn(ITimeSpan.DEFAULT_ZOOM_FACTOR, 0));
        configureControlButton(scaleOutButton, "Scale Out", SCALE_OUT_ICON, l -> playbackSystem.getTimeSpan().scaleTimeOut(ITimeSpan.DEFAULT_ZOOM_FACTOR, 0));
        configureControlButton(panLeftButton, "Pan Left", PAN_LEFT_ICON, l -> pan(-1));
        configureControlButton(panRightButton, "Pan Right", PAN_RIGHT_ICON, l -> pan(1));
        configureControlButton(fitButton, "Fit Timeline", FIT_ICON, l -> playbackSystem.fitTimeSpanToLanes());
        configureControlButton(liveButton, "Go Live", LIVE_ICON, l -> playbackSystem.goLive());

        // Create the collapsed view control menu
        List.of(scaleOutButton, scaleInButton, panLeftButton, panRightButton, fitButton, liveButton)
                .forEach(button -> viewControlMenu.add(new JMenuItem(button.getAction())));

        viewControlMenuBar.add(viewControlMenu);
    }

    /**
     * Configure a button with the given text, icon, and listener.
     * The tooltip text from the button is preserved in the created Action.  This mechanism is used so that the
     * action can also be added to our collapsed control menu {@link #viewControlMenu}.
     * @since 5.7
     */
    private void configureControlButton(JButton button, String text, ImageIcon icon, ActionPerformedListener listener)
    {
        final var action = new SimpleAction(text, icon, listener);
        action.setTooltipText(button.getToolTipText());
        button.setAction(action);
        button.setText(null); // Don't show the text on the button, it is only used in the collapsed menu
    }

    @Override
    public void addCustomControlComponent(JComponent component)
    {
        final var layout = (FormLayout) customComponentsPanel.getLayout();
        layout.appendColumn(FormFactory.DEFAULT_COLSPEC);
        layout.appendColumn(FormFactory.RELATED_GAP_COLSPEC);
        customComponentsPanel.add(component, CC.xy(layout.getColumnCount() - 1, 1));
    }

    @Override
    public void removeCustomControlComponent(JComponent component)
    {
        final var layout = (FormLayout) customComponentsPanel.getLayout();

        for (int i = 0, count = customComponentsPanel.getComponentCount(); i < count; i++)
        {
            if (customComponentsPanel.getComponent(i) == component)
            {
                // Remove the component, and then remove the component and gap columns to shift things left
                customComponentsPanel.remove(i);
                final int columnIndex = 2 * (i + 1); // Layout is 1 based
                layout.removeColumn(columnIndex);
                layout.removeColumn(columnIndex);
                break;
            }
        }

        revalidate();
        repaint();
    }

    /**
     * Hide/show the labels that show the time span.
     * @since 5.7
     */
    public void setTimeSpanLabelsVisible(boolean visible)
    {
        timeSpanLabel.setVisible(visible);
        timeSpanValueLabel.setVisible(visible);
    }

    /**
     * Collapse the row of individual buttons that control view/zoom.
     */
    @Override
    public void collapseIndividualViewControlButtons()
    {
        viewControlsHolderPanel.removeAll();
        viewControlsHolderPanel.add(viewControlMenuBar);
        revalidate();
    }

    /**
     * Expand the row of individual buttons that control view/zoom.
     */
    @Override
    public void expandIndividualViewControlButtons()
    {
        viewControlsHolderPanel.removeAll();
        viewControlsHolderPanel.add(viewControlButtonPanel);
        revalidate();
    }

    private void updateTimeLabels()
    {
        if (isShowing())
        {
            final var timeSpan = playbackSystem.getTimeSpan();

            long position = timeSpan.getPosition();
            if (position > 0) positionValueLabel.setText(TimelineUtils.getDateString(position));

            long totalTime = timeSpan.getTotalTime();
            timeSpanValueLabel.setText(TimelineUtils.elapsedTimeDays(totalTime));
        }
    }

    @InvokeOnEDT
    private void refreshPlaybackSpeed()
    {
        final int index = Arrays.binarySearch(PLAYBACK_SPEEDS, playbackSystem.getPlaybackSpeed());
        if (index > 0) speedComboBox.setSelectedIndex(index);
    }

    private void pan(int offsetDirection)
    {
        int offset = offsetDirection * ITimeSpan.DEFAULT_PAN_OFFSET;
        final var timeSpan = playbackSystem.getTimeSpan();
        int width = timeSpan.getEffectiveWidth();
        playbackSystem.stopLive();
        timeSpan.panTimeByXOffset((width * offset) / 100);
    }

    private void speedComboBoxActionPerformed()
    {
        playbackSystem.setPlaybackSpeed(speedComboBox.getItemAt(speedComboBox.getSelectedIndex()));
    }

    /**
     * A button event handler using a timer to repeat the button's action if the mouse button is held down for a period of time.
     */
    private static final class AutoTimerButtonHandler extends MouseAdapter
    {
        static void configure(@Nonnull JButton button)
        {
            button.addMouseListener(new AutoTimerButtonHandler(button));
        }

        private final Timer autoRepeatTimer;

        AutoTimerButtonHandler(@Nonnull JButton button)
        {
            autoRepeatTimer = new Timer(200, button.getAction());
            autoRepeatTimer.setInitialDelay(400);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            autoRepeatTimer.start();
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            autoRepeatTimer.stop();
        }
    }

    /**
     * Update our UI whenever the playback speed changes.
     * @since 5.7
     */
    private class PlaybackSpeedChangeHandler extends PlaybackConfigurationAdapter
    {
        @Override
        public void playbackSpeedChanged(int speed)
        {
            refreshPlaybackSpeed();
        }
    }

    /**
     * Update our view whenever the timespan changes.
     * @since 5.7
     */
    private class TimeSpanChangeHandler extends TimeSpanAdapter
    {
        @Override
        public void timeSpanChanged()
        {
            updateTimeLabels();
        }

        @Override
        public void timePositionChanged()
        {
            updateTimeLabels();
        }
    }

    /**
     * Render the speed values as "Nx" for a value of N.
     */
    private static final class SpeedListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            return super.getListCellRendererComponent(list, value + "x", index, isSelected, cellHasFocus);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        playButton = new JButton();
        pauseButton = new JButton();
        stopButton = new JButton();
        speedComboBox = new JComboBox<>();
        var statusPanel = new JPanel();
        positionValueLabel = new JLabel();
        timeSpanLabel = new JLabel();
        timeSpanValueLabel = new JLabel();
        viewControlsHolderPanel = new JPanel();
        customComponentsPanel = new JPanel();
        viewControlButtonPanel = new JPanel();
        scaleOutButton = new JButton();
        scaleInButton = new JButton();
        panLeftButton = new JButton();
        panRightButton = new JButton();
        fitButton = new JButton();
        liveButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "3*(default), 2*(10px, default), 15px, pref, $ugap, default:grow",
                "default:grow"));

        //---- playButton ----
        playButton.setInheritsPopupMenu(true);
        playButton.setToolTipText("Play");
        playButton.setName("playButton");
        add(playButton, cc.xy(1, 1));

        //---- pauseButton ----
        pauseButton.setToolTipText("Pause");
        pauseButton.setName("pauseButton");
        add(pauseButton, cc.xy(2, 1));

        //---- stopButton ----
        stopButton.setToolTipText("Stop");
        stopButton.setName("stopButton");
        add(stopButton, cc.xy(3, 1));

        //---- speedComboBox ----
        speedComboBox.setMaximumRowCount(10);
        speedComboBox.setName("speedComboBox");
        speedComboBox.addActionListener(e -> speedComboBoxActionPerformed());
        add(speedComboBox, cc.xy(5, 1));

        //======== statusPanel ========
        {
            statusPanel.setName("statusPanel");
            statusPanel.setLayout(new FormLayout(
                    "default, default:grow, 2*(default)",
                    "default"));

            //---- positionValueLabel ----
            positionValueLabel.setText("00:00:00");
            positionValueLabel.setName("positionValueLabel");
            statusPanel.add(positionValueLabel, cc.xy(1, 1));

            //---- timeSpanLabel ----
            timeSpanLabel.setText(" Time Span: ");
            timeSpanLabel.setName("timeSpanLabel");
            statusPanel.add(timeSpanLabel, cc.xy(3, 1));

            //---- timeSpanValueLabel ----
            timeSpanValueLabel.setText("00:00:00");
            timeSpanValueLabel.setName("timeSpanValueLabel");
            statusPanel.add(timeSpanValueLabel, cc.xy(4, 1));
        }
        add(statusPanel, cc.xy(7, 1));

        //======== viewControlsHolderPanel ========
        {
            viewControlsHolderPanel.setName("viewControlsHolderPanel");
            viewControlsHolderPanel.setLayout(new BorderLayout());
        }
        add(viewControlsHolderPanel, cc.xy(9, 1));

        //======== customComponentsPanel ========
        {
            customComponentsPanel.setName("customComponentsPanel");
            customComponentsPanel.setLayout(new FormLayout(
                    "default",
                    "default:grow"));
        }
        add(customComponentsPanel, cc.xy(11, 1));

        //======== viewControlButtonPanel ========
        {
            viewControlButtonPanel.setName("viewControlButtonPanel");
            viewControlButtonPanel.setLayout(new FormLayout(
                    "default, $lcgap, default, 8dlu, default, $lcgap, 2*(default, 8dlu), default",
                    "default"));

            //---- scaleOutButton ----
            scaleOutButton.setToolTipText("Scale time out (extend timeline duration)");
            scaleOutButton.setName("scaleOutButton");
            viewControlButtonPanel.add(scaleOutButton, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- scaleInButton ----
            scaleInButton.setToolTipText("Scale time in (decrease timeline duration)");
            scaleInButton.setName("scaleInButton");
            viewControlButtonPanel.add(scaleInButton, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- panLeftButton ----
            panLeftButton.setToolTipText("Pan time left (earlier start and end times)");
            panLeftButton.setName("panLeftButton");
            viewControlButtonPanel.add(panLeftButton, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- panRightButton ----
            panRightButton.setToolTipText("Pan time right (later start and end times)");
            panRightButton.setName("panRightButton");
            viewControlButtonPanel.add(panRightButton, cc.xy(7, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- fitButton ----
            fitButton.setToolTipText("Fit timeline to clips");
            fitButton.setName("fitButton");
            viewControlButtonPanel.add(fitButton, cc.xy(9, 1));

            //---- liveButton ----
            liveButton.setToolTipText("Go Live");
            liveButton.setName("liveButton");
            viewControlButtonPanel.add(liveButton, cc.xy(11, 1));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JComboBox<Integer> speedComboBox;
    private JLabel positionValueLabel;
    private JLabel timeSpanLabel;
    private JLabel timeSpanValueLabel;
    private JPanel viewControlsHolderPanel;
    private JPanel customComponentsPanel;
    private JPanel viewControlButtonPanel;
    private JButton scaleOutButton;
    private JButton scaleInButton;
    private JButton panLeftButton;
    private JButton panRightButton;
    private JButton fitButton;
    private JButton liveButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
