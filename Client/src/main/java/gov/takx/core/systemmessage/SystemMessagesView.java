package gov.takx.core.systemmessage;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.logging.SystemLogLevel;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.systemmessage.ISystemMessageManagerListener;
import gov.takx.api.systemmessage.MessageAction;
import gov.takx.api.systemmessage.SystemMessage;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.UISpaceVisibilityEvent;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractCellEditor;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This component provides a view of all the system messages/events that have been recorded.  The messages are
 * timestamped, and actionable elements are represented as hyperlinks in the text.
 * <p>
 * Every time this panel becomes visible, the outstanding message count is reset using
 * {@link ISystemMessageManager#resetOutstandingCount()}
 * <p>
 * Care has been taken to perform minimal work when the panel is not visible as to reduce the CPU overhead.  Also, to
 * assist with performance, the number of messages in the panel is limited by the
 * {@link PreferenceEntry#MAX_VISIBLE_SYSTEM_MESSAGES} preference.  This prevents UI lockups when trying to display
 * thousands of messages.
 *
 * @see ISystemMessageManager
 * @since complete rework as of 2.30
 */
@PseudoBean
public class SystemMessagesView extends ATitledPanel implements ISystemMessageManagerListener //, SkinChangeListener // TODO KMB: Fix for FlatLAF (TAKX-739)
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TITLE = ButtonType.SYSTEM_MESSAGES.getDisplayText();

    private static final String ACTION_CMD = "action";
    private static final String EXCEPTION_CMD = "exception";

    private static final String HEAD_HTML = "<html><font face=\"courier\">";
    private static final String TAIL_HTML = "</font></html>";

    private static final Map<SystemLogLevel, String> LIGHT_THEME_COLORS = Map.of(
            SystemLogLevel.SYSTEM_ERROR, "#ff4400",
            SystemLogLevel.SYSTEM_WARN, "blue",
            SystemLogLevel.SYSTEM_INFO, "black");

    private static final Map<SystemLogLevel, String> DARK_THEME_COLORS = Map.of(
            SystemLogLevel.SYSTEM_ERROR, "#ff4400",
            SystemLogLevel.SYSTEM_WARN, "yellow",
            SystemLogLevel.SYSTEM_INFO, "white");

    private static Map<SystemLogLevel, String> levelColors = DARK_THEME_COLORS; // One of LIGHT_THEME_COLORS or DARK_THEME_COLORS

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("kk:mm:ss.sss");

    @Inject
    private ISystemMessageManager messageManager;

    private final boolean showStackTraces = PreferenceEntry.SYSTEM_MESSAGE_SHOW_EXCEPTION_STACK.getBooleanValue();

    private final SystemMessageTableModel tableModel;
    private final CellEditController cellEditController = new CellEditController();
    private final AtomicBoolean newMessages = new AtomicBoolean(false);
    private final CircularMessageCollection messagesWaitingToBeVisible;
    private final SystemMessageQueue messageBatchQueue;
    private volatile boolean shutdown;

    public SystemMessagesView()
    {
        super(TITLE);
        initComponents();

        final int maxSystemMessages = PreferenceEntry.MAX_VISIBLE_SYSTEM_MESSAGES.getIntegerValue();

        messagesWaitingToBeVisible = new CircularMessageCollection(maxSystemMessages);
        messageBatchQueue = new SystemMessageQueue(this, maxSystemMessages);

        tableModel = new SystemMessageTableModel(maxSystemMessages, this::getHtmlForMessage);

        messageTable.setModel(tableModel);
        messageTable.setTableHeader(null);

        final TableColumn column = messageTable.getColumnModel().getColumn(0);

        column.setCellRenderer(new HtmlCellRenderer());
        column.setCellEditor(new LinkHandlingTableCellEditor());

        messageTable.addMouseMotionListener(cellEditController);

        // TODO KMB: Fix for FlatLaf (TAKX-739)
//        SubstanceLookAndFeel.registerSkinChangeListener(this);
//        skinChanged();
    }

    @PostConstruct
    private void postConstruct()
    {
        messageManager.addSystemMessageManagerListener(this);

        // Install all the pending messages
        final List<SystemMessage> pendingMessages = messageManager.takePendingMessages();
        if (!pendingMessages.isEmpty()) messageBatchQueue.addAll(pendingMessages);
    }

    /**
     * Shut down our message queue when we are shutting down.
     *
     * @since 2.31
     */
    private void shutdown(@Observes ShuttingDown ignored)
    {
        shutdown = true;
        messageBatchQueue.shutdown();
    }

    /**
     * Generate the HTML for rendering a message, any exception, and all actions.
     *
     * @param message The System Message object to process
     * @return HTML to render this message
     */
    private String getHtmlForMessage(SystemMessage message)
    {
        final StringBuilder sb = new StringBuilder();

        // Add the Start HTML Tag:
        sb.append(HEAD_HTML);

        // Add the timestamp
        sb.append(TIME_FMT.format(new Date(message.getTimestamp()))).append(" ");

        // Set the color according to the level
        final String colorString = levelColors.get(message.getLevel());

        sb.append("<font color=\"").append(colorString).append("\">")
                .append(message.getMessage())
                .append("</font>");

        // Append any exception info
        if (message.hasException())
        {
            final String exceptionName = message.getException().getClass().getSimpleName();

            if (showStackTraces)
            {
                // Show as link to open the exception in a dialog
                sb.append(" ").append("<a href=\"").append(EXCEPTION_CMD).append("\">")
                        .append("Threw Exception: ").append(exceptionName)
                        .append("</a>");
            } else
            {
                // Just show the exception, no link
                sb.append("; Threw Exception: ").append(exceptionName);
            }
        }

        // Append the action links
        if (message.hasActions())
        {
            final List<MessageAction> actions = message.getActions();

            for (int i = 0, size = actions.size(); i < size; i++)
            {
                sb.append(" ")
                        .append("<a href=\"").append(ACTION_CMD).append(':').append(i).append("\">")
                        .append(actions.get(i).getMessage())
                        .append("</a>");
            }
        }

        sb.append(TAIL_HTML);

        return sb.toString();
    }

    /**
     * Override so we can clear all outstanding messages whenever the user opens the panel.
     */
    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

        if (visible)
        {
            handleShowing();
        }
    }

    public void visibilityChanged(@Observes UISpaceVisibilityEvent event)
    {
        if (event.getComponent() instanceof SystemMessagesView && event.isVisible())
        {
            handleShowing();
        }
    }

    /**
     * Handle becoming visible. Reset the count and add any waiting messages to our table model.
     */
    private void handleShowing()
    {
        messageManager.resetOutstandingCount();

        // If we've had new text inserted since we were last visible, then update the UI widget
        if (newMessages.compareAndSet(true, false))
        {
            tableModel.addMessages(messagesWaitingToBeVisible);
            messagesWaitingToBeVisible.clear();
        }
    }

    /**
     * Add a message to the content.
     *
     * @param message The message to add
     */
    @Override
    public void newSystemMessage(SystemMessage message)
    {
        if (!shutdown) messageBatchQueue.add(message);
    }

    /**
     * Add a batch of queues messages to the view.
     *
     * @param messages Batch of messages to add
     */
    void addNewMessages(Collection<SystemMessage> messages)
    {
        // Only update the actual widget if it's visible
        if (messageTable.isVisible())
        {
            cellEditController.cancelEdit(); // If we're visible, ensure that no cell editor is active

            tableModel.addMessages(messages);
        } else
        {
            messagesWaitingToBeVisible.addAll(messages);
            newMessages.set(true); // Note we have new data
        }
    }

// TODO KMB: Fix for FlatLaf (TAKX-739)
    /**
     * Only update system messages' colors if the skin changed from a light to dark theme, or vice versa.
     *
     * @since 2.36
     */
//    @Override
//    public void skinChanged()
//    {
//        //noinspection AssignmentToStaticFieldFromInstanceMethod
//        levelColors = SubstanceLookAndFeel.getCurrentSkin().getActiveColorScheme(DecorationAreaType.GENERAL).isDark()
//                ? DARK_THEME_COLORS
//                : LIGHT_THEME_COLORS;
//
//        tableModel.refresh();
//    }

    /**
     * Mouse listener to control the automatic editing of table cells when the mouse moves into a new row.
     */
    private final class CellEditController extends MouseAdapter
    {
        int lastRow = -1;

        @Override
        public void mouseMoved(MouseEvent e)
        {
            Point p = e.getPoint();
            int row = messageTable.rowAtPoint(p);

            if (row == -1)
            {
                cancelEdit();
            } else if (row != lastRow)
            {
                lastRow = row;

                cancelEdit();

                messageTable.editCellAt(row, 0);
                Component editor = messageTable.getEditorComponent();
                editor.requestFocusInWindow();
            }
        }

        private void cancelEdit()
        {
            if (messageTable.isEditing())
            {
                messageTable.getCellEditor().cancelCellEditing();
            }
        }
    }

    /**
     * Cell renderer to deal with HTML and potential line wrapping.
     */
    private static final class HtmlCellRenderer extends JEditorPane implements TableCellRenderer
    {
        HtmlCellRenderer()
        {
            setEditable(false);
            setContentType("text/html");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            setText(value.toString());

            // Need to set the width so we can properly calculate the needed rendering height (in case of line wrapping)
            setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);

            final int preferredHeight = getPreferredSize().height;

            if (table.getRowHeight(row) != preferredHeight)
            {
                table.setRowHeight(row, preferredHeight);
            }
            return this;
        }
    }

    /**
     * Table cell editor that uses a {@link JEditorPane} so that hyperlinks are active.
     */
    private static final class LinkHandlingTableCellEditor extends AbstractCellEditor implements TableCellEditor
    {
        private final JEditorPane editor = new JEditorPane();
        private final LinkHandler linkHandler = new LinkHandler();

        public LinkHandlingTableCellEditor()
        {
            editor.putClientProperty("FlatLaf.styleClass", "monospaced");
            editor.setEditable(false);
            editor.setContentType("text/html");
            editor.setBorder(new EmptyBorder(2, 2, 0, 0));

            editor.addHyperlinkListener(linkHandler);

            // Add a listener to cancel the edit when the mouse moves out of our editor component
            editor.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseExited(MouseEvent e)
                {
                    cancelCellEditing();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col)
        {
            linkHandler.setCurrentMessage(((SystemMessageTableModel.SystemMessageElement) value).getSystemMessage());
            editor.setText(value.toString());
            return editor;
        }

        @Override
        public Object getCellEditorValue()
        {
            return null; // we don't actually edit
        }
    }

    /**
     * Special hyperlink listener to handle the links we embed in the message display for exceptions and actions.
     * The links use an href that looks like either:
     * <ul>
     * <li>action:&lt;index&gt; - where index refers to an action on the message</li>
     * <li>exception - which means display the exception from the message</li>
     * </ul>
     */
    private static class LinkHandler implements HyperlinkListener
    {
        private SystemMessage currentMessage;

        /**
         * Set the message being edited so hyperlink clicks can be properly interpreted and dispatched.
         *
         * @param currentMessage Message being edited
         */
        void setCurrentMessage(SystemMessage currentMessage)
        {
            this.currentMessage = currentMessage;
        }

        @Override
        public void hyperlinkUpdate(HyperlinkEvent event)
        {
            if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
            {
                return; // Only care about activations (clicks)
            }

            final String link = event.getDescription(); // We don't use valid URLs, so grab the text

            if (logger.isDebugEnabled()) logger.debug("activated link: '{}'", link);

            if (link.equals(EXCEPTION_CMD))
            {
                displayException(currentMessage.getException());
            } else
            {
                // Split on the ':' to get the command and the argument
                final int index = link.indexOf(':');
                if (index <= 0 || index >= link.length() - 1)
                {
                    logger.error("Malformed action link: '{}'", link);
                } else
                {
                    final int actionIndex = Integer.parseInt(link.substring(index + 1));
                    invokeAction(currentMessage.getActions().get(actionIndex));
                }
            }
        }

        /**
         * Display a message dialog showing the exception and stack trace.
         *
         * @param exception The exception to display
         */
        private void displayException(Throwable exception)
        {
            final StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            final String msg = sw.toString();

            final JTextArea area = new JTextArea(msg);
            area.setEditable(false);

            final JScrollPane jsp = new JScrollPane(area);
            jsp.setPreferredSize(new Dimension(700, 450));

            JOptionPane optionPane = new JOptionPane(jsp, JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null, null, null);

            JDialog dialog = optionPane.createDialog(JFrame.getFrames()[0], "Exception Reported");
            dialog.setResizable(true);
            dialog.setVisible(true);
            dialog.dispose();
        }

        /**
         * Invoke the provide action, and catch any exceptions that occur.
         *
         * @param action The action to invoke.
         */
        private void invokeAction(MessageAction action)
        {
            try
            {
                action.invoke();
            } catch (Exception e)
            {
                logger.error("Failed to invoke action: {}", action, e);
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var messageScrollPane = new JScrollPane();
        messageTable = new JTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "fill:default:grow"));

        //======== messageScrollPane ========
        {
            messageScrollPane.setName("messageScrollPane");

            //---- messageTable ----
            messageTable.setModel(new DefaultTableModel(
                    new Object[][]{
                            {null},
                            {null},
                    },
                    new String[]{
                            null
                    })
            {
                final Class<?>[] columnTypes = new Class<?>[]{
                        String.class
                };

                @Override
                public Class<?> getColumnClass(int columnIndex)
                {
                    return columnTypes[columnIndex];
                }
            });
            messageTable.setShowHorizontalLines(false);
            messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            messageTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
            messageTable.setName("messageTable");
            messageScrollPane.setViewportView(messageTable);
        }
        add(messageScrollPane, cc.xy(3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable messageTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
