package gov.takx.core.view.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Simple panel to display the current time, updated every second.
 *
 */
@PseudoBean(singletonObserver = true)
public class TimePanel extends JPanel
{
    @Inject
    private StandardTimeFormatting timeFormatting;

    private final Timer clockTimer;

    public TimePanel()
    {
        initComponents();
        clockTimer = new Timer(1000, e -> updateTime());
    }

    /**
     * Update our format when as needed.
     */
    private void preferenceChange(@Observes @UserPreferenceType(UserPreferenceEntry.DISPLAY_LOCAL_TIME) PreferenceEntryChange event)
    {
        SwingUtilities.invokeLater(this::updateTime);
    }

    /**
     * Update the time display, must be called on the EDT.
     */
    private void updateTime()
    {
        timeLabel.setText("  " + timeFormatting.formatWithCurrent(System.currentTimeMillis()) + " ");
    }

    public void stopClockTimer()
    {
        clockTimer.stop();
    }

    public void startClockTimer()
    {
        clockTimer.start();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        timeLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //---- timeLabel ----
        timeLabel.setText("Time");
        timeLabel.setName("timeLabel");
        add(timeLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel timeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
