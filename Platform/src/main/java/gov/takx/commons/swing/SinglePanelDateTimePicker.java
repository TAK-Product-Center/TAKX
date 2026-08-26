package gov.takx.commons.swing;

import com.formdev.flatlaf.FlatClientProperties;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.calendar.SingleDaySelectionModel;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;

import java.awt.Color;
import java.awt.FlowLayout;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * An augmented {@link JXDatePicker} which provides support for entering a time in the month view.
 *
 */
public class SinglePanelDateTimePicker extends JXDatePicker
{
    private JSpinner timeSpinner;
    private JPanel timePanel;
    private DateFormat timeFormat;
    private boolean okButtonPressed = false;

    public SinglePanelDateTimePicker()
    {
        super();
        setMonthView(new OKButtonMonthView());
        getMonthView().setSelectionModel(new SingleDaySelectionModel());
    }

    public SinglePanelDateTimePicker(Date d)
    {
        this();
        setDate(d);
    }

    @Override
    public void commitEdit() throws ParseException
    {
        // If the ok button caused the commit, the spinners may have changed, so commit the time first so it isn't
        // over-written by the date.
        if (okButtonPressed)
        {
            commitTime();
        }

        super.commitEdit();

        // If the spinners haven't changed, they need to be updated with the new date
        if (!okButtonPressed)
        {
            setTimeSpinners();
        }
    }

    @Override
    public void cancelEdit()
    {
        super.cancelEdit();
        setTimeSpinners();
    }

    @Override
    public JPanel getLinkPanel()
    {
        if (timePanel == null)
        {
            timePanel = createTimePanel();
        }
        setTimeSpinners();
        return timePanel;
    }

    private JPanel createTimePanel()
    {
        JPanel newPanel = new JPanel();
        newPanel.setLayout(new FlowLayout());

        SpinnerDateModel dateModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(dateModel);
        if (timeFormat == null)
        {
            timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        }

        updateTextFieldFormat();

        newPanel.add(new JLabel("Time:"));

        newPanel.add(timeSpinner);

        JButton okButton = new JButton("OK");

        // Tell the LAF to ignore the default minimum size for this button
        okButton.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);

        okButton.addActionListener(e -> {
            okButtonPressed = true;
            getMonthView().commitSelection();
            okButtonPressed = false;
        });
        newPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");

        // Tell the LAF to ignore the default minimum size for this button
        cancelButton.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);
        cancelButton.addActionListener(e -> {
            getMonthView().setSelectionDate(getDate());
            getMonthView().cancelSelection();
        });
        newPanel.add(cancelButton);

        newPanel.setBackground(Color.WHITE);

        return newPanel;
    }

    private void updateTextFieldFormat()
    {
        if (timeSpinner == null)
        {
            return;
        }
        JFormattedTextField tf = ((JSpinner.DefaultEditor) timeSpinner.getEditor()).getTextField();
        DefaultFormatterFactory factory = (DefaultFormatterFactory) tf.getFormatterFactory();
        DateFormatter formatter = (DateFormatter) factory.getDefaultFormatter();

        // Change the date format to only show the hours
        formatter.setFormat(timeFormat);
    }

    private void commitTime()
    {
        Date date = getDate();
        if (date != null)
        {
            Date time = (Date) timeSpinner.getValue();
            GregorianCalendar timeCalendar = new GregorianCalendar();
            timeCalendar.setTimeZone(getTimeZone());
            timeCalendar.setTime(time);

            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeZone(getTimeZone());
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, 0);

            Date newDate = calendar.getTime();
            setDate(newDate);
        }
    }

    private void setTimeSpinners()
    {
        Date date = getDate();
        if (date != null)
        {
            timeSpinner.setValue(date);
        }
    }

    public DateFormat getTimeFormat()
    {
        return timeFormat;
    }

    public void setTimeFormat(DateFormat timeFormat)
    {
        this.timeFormat = timeFormat;
        updateTextFieldFormat();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setTitle("Date Time Picker");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            SinglePanelDateTimePicker dateTimePicker = new SinglePanelDateTimePicker();

            TimeZone timeZone = TimeZone.getTimeZone("UTC");

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            dateFormat.setTimeZone(timeZone);
            DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss z");
            timeFormat.setTimeZone(timeZone);

            dateTimePicker.setTimeZone(timeZone);
            dateTimePicker.setFormats(dateFormat);
            dateTimePicker.setTimeFormat(timeFormat);

            dateTimePicker.setDate(new Date());

            frame.getContentPane().add(dateTimePicker);
            frame.pack();
            frame.setVisible(true);
        });
    }

    private class OKButtonMonthView extends JXMonthView
    {
        private OKButtonMonthView()
        {
            super();
            this.setTraversable(true);
        }

        @Override
        public void commitSelection()
        {
            // Prevents the month view from closing on a date click.  The ok or cancel buttons have to be pressed.
            if (okButtonPressed)
            {
                super.commitSelection();
            }
        }
    }
}
