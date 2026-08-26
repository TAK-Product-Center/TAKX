package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jdesktop.swingx.JXDatePicker;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.time.Instant;
import java.util.Date;

/**
 * Panel that allows for the selection of a date and time.
 *
 * @since 3.3
 */
@SuppressWarnings("unused")
public class DateTimePicker extends ATimePicker
{
    public DateTimePicker()
    {
        this(Instant.now().toEpochMilli());
    }

    /**
     * @param time The time (in milliseconds since epoch) to have both the date and time picker to initially be set to.
     */
    public DateTimePicker(long time)
    {
        initComponents();
        setTime(time);

        setEnabled(false);
    }

    /**
     * @param enabled true if this component should be enabled, false otherwise
     * @since 5.2
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);

        datePicker.setEnabled(enabled);
        timePicker.setEnabled(enabled);
    }

    @Override
    public long getTime()
    {
        return datePicker.getDate().getTime() + timePicker.getTime();
    }

    @Override
    public void setTime(long time)
    {
        datePicker.setDate(new Date(time));
        timePicker.setTime(time);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var datePickerPanel = new JPanel();
        var dateLabel = new JLabel();
        datePicker = new JXDatePicker();
        timePicker = new TimePicker();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default",
                "default"));

        //======== datePickerPanel ========
        {
            datePickerPanel.setName("datePickerPanel");
            datePickerPanel.setLayout(new FormLayout(
                    "default",
                    "2*(default)"));

            //---- dateLabel ----
            dateLabel.setText("Date");
            dateLabel.setName("dateLabel");
            datePickerPanel.add(dateLabel, cc.xy(1, 1));

            //---- datePicker ----
            datePicker.setName("datePicker");
            datePickerPanel.add(datePicker, cc.xy(1, 2));
        }
        add(datePickerPanel, cc.xy(1, 1));

        //---- timePicker ----
        timePicker.setName("timePicker");
        add(timePicker, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JXDatePicker datePicker;
    private TimePicker timePicker;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
