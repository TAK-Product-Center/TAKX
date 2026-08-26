package gov.takx.core.view.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.WorldWind;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class NetworkStatusPanel extends JPanel
{
    private static final Color INVISIBLE = new Color(0, 0, 0, 0);

    // Check host availability every 10 seconds
    private static final long HOST_CHECK_FREQUENCY = TimeUnit.SECONDS.toMillis(10);

    // Update our status display every 2 seconds
    private static final int UPDATE_FREQUENCY = (int) TimeUnit.SECONDS.toMillis(2);

    private final AtomicBoolean isNetworkUnavailable = new AtomicBoolean(false);

    private final Timer downloadTimer = new Timer(UPDATE_FREQUENCY, this::updateLabelWithStatus);
    private final java.util.Timer networkCheckTimer = new java.util.Timer("Network Status Check", true);
    private TimerTask networkCheckTask;

    public NetworkStatusPanel()
    {
        initComponents();
    }

    public void stopTimers()
    {
        downloadTimer.stop();

        if (networkCheckTask != null)
        {
            networkCheckTask.cancel();
            networkCheckTask = null;
        }
    }

    public void startTimers()
    {
        downloadTimer.start();

        networkCheckTask = new NetworkCheckTask();
        networkCheckTimer.schedule(networkCheckTask, HOST_CHECK_FREQUENCY, HOST_CHECK_FREQUENCY);
    }

    /**
     * Using the current status info, update our label accordingly.
     * @since 3.4
     */
    private void updateLabelWithStatus(ActionEvent ignored)
    {
        final boolean networkUnavailable = isNetworkUnavailable.get();
        final String text = networkUnavailable ? "No Network" : "Downloading";
        final Color color = networkUnavailable || WorldWind.getRetrievalService().hasActiveTasks() ? Color.RED : INVISIBLE;

        heartBeatLabel.setText(text);
        heartBeatLabel.setForeground(color);
    }

    private class NetworkCheckTask extends TimerTask
    {
        @Override
        public void run()
        {
            isNetworkUnavailable.set(WorldWind.getNetworkStatus().isNetworkUnavailable(HOST_CHECK_FREQUENCY));
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        heartBeatLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "center:default",
                "default"));

        //---- heartBeatLabel ----
        heartBeatLabel.setText("Downloading");
        heartBeatLabel.setName("heartBeatLabel");
        add(heartBeatLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel heartBeatLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
