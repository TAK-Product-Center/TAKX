package gov.takx.core.view.gui.breadcrumb;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.time.Instant;

/**
 * Panel that displays dates in the {@link TrackSearchPanel}.
 *
 * @since 5.4
 */
@PseudoBean
public class TrackDatePanel extends JPanel
{
    @Inject // Must have a no-arg constructor for use in other JFormDesigner forms
    private StandardTimeFormatting timeFormatting;
    private Instant instant;

    public TrackDatePanel()
    {
        initComponents();
    }

    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void onTimePreferenceChange(@Observes @UserPreferenceType(UserPreferenceEntry.DISPLAY_LOCAL_TIME) PreferenceEntryChange ignored)
    {
        update();
    }

    /**
     * @param instant The time to display
     */
    public void setDate(Instant instant)
    {
        this.instant = instant;
        update();
    }

    /**
     * Sets the displayed day/month/year values based on the selected time zone.
     */
    private void update()
    {
        if (timeFormatting == null) return; // null while prototyping in JFormDesigner

        dayLabel.setText(timeFormatting.formatDay(instant));
        monthLabel.setText(timeFormatting.formatMonth(instant));
        yearLabel.setText(timeFormatting.formatYear(instant));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        monthLabel = new JLabel();
        dayLabel = new JLabel();
        yearLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "20dlu",
                "3*(default)"));

        //---- monthLabel ----
        monthLabel.setText("Jan");
        monthLabel.setHorizontalAlignment(SwingConstants.CENTER);
        monthLabel.setBackground(Color.red);
        monthLabel.setOpaque(true);
        monthLabel.setFont(monthLabel.getFont().deriveFont(monthLabel.getFont().getSize() - 1f));
        monthLabel.setName("monthLabel");
        add(monthLabel, cc.xy(1, 1));

        //---- dayLabel ----
        dayLabel.setText("00");
        dayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dayLabel.setBackground(Color.white);
        dayLabel.setOpaque(true);
        dayLabel.setForeground(Color.black);
        dayLabel.setFont(dayLabel.getFont().deriveFont(dayLabel.getFont().getSize() + 3f));
        dayLabel.setName("dayLabel");
        add(dayLabel, cc.xy(1, 2));

        //---- yearLabel ----
        yearLabel.setText("2000");
        yearLabel.setHorizontalAlignment(SwingConstants.CENTER);
        yearLabel.setBackground(Color.red);
        yearLabel.setOpaque(true);
        yearLabel.setFont(yearLabel.getFont().deriveFont(yearLabel.getFont().getSize() - 1f));
        yearLabel.setName("yearLabel");
        add(yearLabel, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel monthLabel;
    private JLabel dayLabel;
    private JLabel yearLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
