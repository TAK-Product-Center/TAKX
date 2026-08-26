package gov.takx.core.mapobject.gui.alarms;

import gov.takx.commons.constants.State;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.IPanelStateListenerReceiver;
import gov.takx.commons.swing.IValidateAction;
import gov.takx.dataservices.persist.CustomDataMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.util.EnumMap;

/**
 * Abstract base class for an alarm configuration panel.
 *
 * @since 1.49
 */
public abstract class AAlarmPanel extends JPanel implements IPanelStateListenerReceiver, IValidateAction
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected static final State[] ALARM_STATES = {State.Active, State.Warning, State.Serious, State.Critical};
    protected static final State[] DEFAULT_STATES = {State.Inactive, State.Active};

    protected final CustomDataMonitor customDataMonitor;

    private IPanelStateListener listener = null;

    protected AAlarmPanel(CustomDataMonitor customDataMonitor)
    {
        this.customDataMonitor = customDataMonitor;
    }

    /**
     * Reads the CustomDataMonitor values and sets the fields in the alarm configuration panel accordingly.
     * It is acceptable to have the CustomDataMonitor parameter not yet have all the fields set up.
     */
    protected abstract void installDataMonitorState();

    /**
     * Checks to make sure all values entered by the user are valid.
     *
     * @return If all values are valid, a {@code null} String is returned.
     * If any errors are found, a String with the error message is returned.
     */
    @Override
    public abstract String validateContents();

    /**
     * Attempts to apply the values entered by the user.
     */
    public abstract void applyAction();

    @Override
    public void setPanelListener(IPanelStateListener listener)
    {
        this.listener = listener;
    }

    /**
     * Convenience method for extending classes to send a dirty notification to the panel state listener.
     */
    protected void setDirty()
    {
        if (listener != null)
        {
            listener.setPanelDirty();
        }
    }

    public abstract String getDisplayName();

    /**
     * List cell renderer to display the icon associated with an alarm level.
     *
     * @since 3.0
     */
    protected static class LevelComboBoxRenderer extends JLabel implements ListCellRenderer<State>
    {
        private static final EnumMap<State, ImageIcon> iconForState = new EnumMap<>(State.class);

        static
        {
            iconForState.put(State.Inactive, new ImageIcon(GraphicsUtils.createCircle(16, 100, Color.GRAY)));
            iconForState.put(State.Active, new ImageIcon(GraphicsUtils.createCircle(16, 100, Color.GREEN)));
            iconForState.put(State.Warning, new ImageIcon(GraphicsUtils.createTriangle(16, 100, Color.YELLOW)));
            iconForState.put(State.Serious, new ImageIcon(GraphicsUtils.createTriangle(16, 100, Color.YELLOW)));
            iconForState.put(State.Critical, new ImageIcon(GraphicsUtils.createOctagon(16, 100, Color.RED)));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends State> list, State value, int index, boolean isSelected, boolean cellHasFocus)
        {
            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setText(value.name());

            final ImageIcon icon = iconForState.get(value);
            if (icon != null)
            {
                setIcon(icon);
            } else
            {
                logger.error("Unknown alarm state = {}", value);
            }

            return this;
        }
    }
}
