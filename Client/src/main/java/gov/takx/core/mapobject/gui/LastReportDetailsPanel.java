package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import java.awt.Color;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A panel that displays the most recent communication time for a {@link IMapObject} instance, with a live age timer that 
 * increments every second.
 *
 * @since 5.8
 */
@PseudoBean
public class LastReportDetailsPanel extends JPanel implements IResourceCleaner
{
    private static final String DURATION_FORMAT = "[d'd' H'h' m'm' ]s's'";
    private static final DateTimeFormatter LAST_REPORT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss").withZone(ZoneOffset.UTC);

    private final ListenerManager listenerManager = new ListenerManager();

    @Inject
    private ICoreExecutorService executorService;
    private IMapObject mapObject;
    private ScheduledFuture<?> counter;
    private boolean empty = true;

    /**
     * Construct a new empty panel.
     */
    public LastReportDetailsPanel()
    {
        initComponents();
    }

    @Override
    public void releaseResources()
    {
        startOrStopCounter(false);
        listenerManager.unregisterAll();
    }

    /**
     * @param mapObject the map object to monitor and display communication time for.
     */
    public void setMapObject(@Nullable IMapObject mapObject)
    {
        if (this.mapObject == mapObject) return;
        if (this.mapObject != null) listenerManager.unregisterAll();
        this.mapObject = mapObject;
        if (this.mapObject != null) listenerManager.addPropertyChangeListener(mapObject, List.of(IMapObject.MOST_RECENT_COMMUNICATION_TIME), e -> update());
        update();
    }

    /**
     * @return {@code true} if there is no map object, or it does not contain a valid most recent communication time
     */
    public boolean isEmpty()
    {
        return empty;
    }

    @InvokeOnEDT
    private void update()
    {
        var ageString = "";
        var staleString = "";
        var staleLabelString = "Stale In:";
        var timeString = "";
        var empty = true;
        var staleForeground = UIManager.getColor("Label.foreground");
        if (mapObject != null)
        {
            var time = mapObject.getMostRecentCommunicationTime();
            if (time > -1)
            {
                timeString = LAST_REPORT_DATE_TIME_FORMAT.format(Instant.ofEpochMilli(time));
                ageString = DurationFormatUtils.formatDuration(System.currentTimeMillis() - time, DURATION_FORMAT);
                empty = false;
                var cotEvent = mapObject.getLatestCotEvent();
                if (cotEvent != null)
                {
                    var stale = cotEvent.getStale();
                    if (stale != null)
                    {
                        var staleDuration = stale.getMilliseconds() - System.currentTimeMillis();
                        if (staleDuration < 0)
                        {
                            staleLabelString = "Stale For:";
                            staleString = DurationFormatUtils.formatDuration(-staleDuration, DURATION_FORMAT);
                            staleForeground = Color.YELLOW;
                        } else
                        {
                            staleLabelString = "Stale In:";
                            staleString = DurationFormatUtils.formatDuration(staleDuration, DURATION_FORMAT);
                        }
                    }
                }
            }
        }

        this.empty = empty;
        timeField.setText(timeString);
        ageField.setText(ageString);
        staleField.setText(staleString);
        staleField.setForeground(staleForeground);
        staleLabel.setText(staleLabelString);
        startOrStopCounter(!empty);
    }

    private void startOrStopCounter(boolean start)
    {
        if (start && counter == null)
        {
            counter = executorService.scheduleAtFixedRate(() -> {
                Thread.currentThread().setName(getClass().getSimpleName() + "-Counter");
                update();
            }, 0, 1, TimeUnit.SECONDS);
        } else if (!start && counter != null)
        {
            counter.cancel(true);
            counter = null;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var timeLabel = new JLabel();
        timeField = new JLabel();
        var ageLabel = new JLabel();
        ageField = new JLabel();
        staleLabel = new JLabel();
        staleField = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "2*(default, $lgap), default"));

        //---- timeLabel ----
        timeLabel.setText("Time:");
        timeLabel.setName("timeLabel");
        add(timeLabel, cc.xy(1, 1));

        //---- timeField ----
        timeField.setName("timeField");
        add(timeField, cc.xy(3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- ageLabel ----
        ageLabel.setText("Age:");
        ageLabel.setName("ageLabel");
        add(ageLabel, cc.xy(1, 3));

        //---- ageField ----
        ageField.setName("ageField");
        add(ageField, cc.xy(3, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- staleLabel ----
        staleLabel.setText("Stale:");
        staleLabel.setName("staleLabel");
        add(staleLabel, cc.xy(1, 5));

        //---- staleField ----
        staleField.setName("staleField");
        add(staleField, cc.xy(3, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel timeField;
    private JLabel ageField;
    private JLabel staleLabel;
    private JLabel staleField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
