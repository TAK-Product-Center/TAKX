package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nonnull;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Panel that allows for the selection of a time.
 *
 * @since 3.3
 */
public class TimePicker extends ATimePicker
{
    private final ChangeEvent changeEvent = new ChangeEvent(this);

    public TimePicker()
    {
        this(Instant.now().toEpochMilli());
    }

    /**
     * @param time The time (in milliseconds since epoch) to set the picker to initially be set to.
     */
    public TimePicker(long time)
    {
        initComponents();
        setTime(time);
    }

    @Override
    public long getTime()
    {
        return TimeUnit.HOURS.toMillis((Integer) hoursSpinner.getValue())
                + TimeUnit.MINUTES.toMillis((Integer) minutesSpinner.getValue())
                + TimeUnit.SECONDS.toMillis((Integer) secondsSpinner.getValue());
    }

    @Override
    public void setTime(long time)
    {
        final Duration duration = Duration.ofMillis(time);

        hoursSpinner.setValue(duration.toHoursPart());
        minutesSpinner.setValue(duration.toMinutesPart());
        secondsSpinner.setValue(duration.toSecondsPart());
    }

    /**
     * @param enabled true if this component should be enabled, false otherwise
     * @since 5.2
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);

        hoursSpinner.setEnabled(enabled);
        minutesSpinner.setEnabled(enabled);
        secondsSpinner.setEnabled(enabled);
    }

    /**
     * @param listener the listener to add
     * @since 5.6
     */
    public void addChangeListener(@Nonnull ChangeListener listener)
    {
        listenerList.add(ChangeListener.class, listener);
    }

    /**
     * @param listener the listener to remove
     * @since 5.6
     */
    @SuppressWarnings("unused")
    public void removeChangeListener(@Nonnull ChangeListener listener)
    {
        listenerList.remove(ChangeListener.class, listener);
    }

    private void hoursChanged(ChangeEvent ignored)
    {
        List.of(listenerList.getListeners(ChangeListener.class)).forEach(l -> l.stateChanged(changeEvent));
    }

    private void minutesChanged(ChangeEvent ignored)
    {
        List.of(listenerList.getListeners(ChangeListener.class)).forEach(l -> l.stateChanged(changeEvent));
    }

    private void secondsChanged(ChangeEvent ignored)
    {
        List.of(listenerList.getListeners(ChangeListener.class)).forEach(l -> l.stateChanged(changeEvent));
    }

    @SuppressWarnings("Convert2MethodRef")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var hoursLabel = new JLabel();
        var minutesLabel = new JLabel();
        var secondsLabel = new JLabel();
        hoursSpinner = new JSpinner();
        minutesSpinner = new JSpinner();
        secondsSpinner = new JSpinner();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "2*(default, $lcgap), default",
                "default, $lgap, default"));

        //---- hoursLabel ----
        hoursLabel.setText("Hours");
        hoursLabel.setName("hoursLabel");
        add(hoursLabel, cc.xy(1, 1));

        //---- minutesLabel ----
        minutesLabel.setText("Minutes");
        minutesLabel.setName("minutesLabel");
        add(minutesLabel, cc.xy(3, 1));

        //---- secondsLabel ----
        secondsLabel.setText("Seconds");
        secondsLabel.setName("secondsLabel");
        add(secondsLabel, cc.xy(5, 1));

        //---- hoursSpinner ----
        hoursSpinner.setModel(new SpinnerNumberModel(0, 0, 23, 1));
        hoursSpinner.setName("hoursSpinner");
        hoursSpinner.addChangeListener(e -> hoursChanged(e));
        add(hoursSpinner, cc.xy(1, 3));

        //---- minutesSpinner ----
        minutesSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));
        minutesSpinner.setName("minutesSpinner");
        minutesSpinner.addChangeListener(e -> minutesChanged(e));
        add(minutesSpinner, cc.xy(3, 3));

        //---- secondsSpinner ----
        secondsSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));
        secondsSpinner.setName("secondsSpinner");
        secondsSpinner.addChangeListener(e -> secondsChanged(e));
        add(secondsSpinner, cc.xy(5, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JSpinner hoursSpinner;
    private JSpinner minutesSpinner;
    private JSpinner secondsSpinner;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
