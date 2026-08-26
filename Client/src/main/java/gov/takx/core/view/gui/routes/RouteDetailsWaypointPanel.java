package gov.takx.core.view.gui.routes;

import com.jgoodies.forms.layout.CellConstraints;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IRoute;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A panel that contains the information and interactions for a single route waypoint, used inside {@link RouteDetailsPanel}.  There is no associated
 * .jfd for this, because we want to use a provided {@link LayoutManager}, which JFormDesigner does not like.
 *
 * @since 3.3
 */
public class RouteDetailsWaypointPanel extends JPanel
{
    private static final ImageIcon DELETE_IMAGE = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "delete.png");

    // TODO When these feature are implemented, find correct images.
    private static final ImageIcon ATTACHMENT_IMAGE = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "add.png");
    private static final ImageIcon CUE_IMAGE = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "add.png");

    private final GeoPointMetaData waypoint;

    private final Color hoverColor = UIManager.getLookAndFeelDefaults().getColor("Button.hoverBackground");
    private final Color backgroundColor = UIManager.getLookAndFeelDefaults().getColor("Panel.background");

    /**
     * Creates a panel displaying details and buttons for a route waypoint.
     *
     * @param waypoint The waypoint to use to populate this panel
     * @param layoutManager The layout manager to use.  This should be a shared across the {@link RouteDetailsWaypointPanel} to keep sizing consistent
     * @param enableDeleteButton true if the delete button should be visible and enabled
     * @param deleteButtonConsumer Consumer to be called when the delete button is pressed, will be provided the waypoint UID
     * @param itemSelectConsumer Consumer to be called when this row is selected, will be provided the location of the waypoint represented
     */
    public RouteDetailsWaypointPanel(@Nonnull GeoPointMetaData waypoint, @Nonnull LayoutManager layoutManager, boolean enableDeleteButton,
                                     @Nullable Consumer<String> deleteButtonConsumer, @Nonnull Consumer<ILocation> itemSelectConsumer)
    {
        if (enableDeleteButton)
        {
            Objects.requireNonNull(deleteButtonConsumer, "'deleteButtonConsumer' may not be null if the delete button is enabled");
        }

        this.waypoint = waypoint;
        setLayout(layoutManager);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        initComponents();

        // TODO TAKX-1256 add attachment button and support for attaching items to each waypoint
        attachmentButton.setIcon((ATTACHMENT_IMAGE));
        attachmentButton.setEnabled(false);
        attachmentButton.setVisible(false);

        // TODO TAKX-1257 add support for setting cues for each waypoint
        cueButton.setIcon((CUE_IMAGE));
        cueButton.setEnabled(false);
        cueButton.setVisible(false);

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                // Go to waypoint on the globe
                itemSelectConsumer.accept(waypoint.get());
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                // getComponentAt returns null if we leave the panel, but will return a value if we are over a child object (e.g. button)
                if (getComponentAt(e.getPoint()) == null) setBackground(backgroundColor);

            }
        });
        loadWaypointData(enableDeleteButton, deleteButtonConsumer);
    }

    private void loadWaypointData(boolean enableDeleteButton, Consumer<String> deleteButtonConsumer)
    {
        final AttributeSet attributeSet = waypoint.getMetaData();
        final String waypointUid = attributeSet.getStringAttribute(StandardAttributeNames.UID);

        // add rename button
        String displayName = attributeSet.getStringAttribute(IRoute.DISPLAY_NAME, "");
        renameButton.setText(displayName);
        renameButton.setToolTipText(displayName);
        renameButton.addActionListener(action -> {
            // Open input box
            String message = "Rename " + displayName;
            String newName = JOptionPane.showInputDialog(this, null, message, JOptionPane.PLAIN_MESSAGE);
            if (newName != null)
            {
                // If an empty string is entered, clear the stored callsign
                if (newName.isBlank())
                {
                    attributeSet.removeAttribute(StandardAttributeNames.CALLSIGN);
                    attributeSet.removeAttribute(IRoute.DISPLAY_NAME); // TODO TAKX-4273 decide which attribute to use
                } else
                {
                    attributeSet.setAttribute(StandardAttributeNames.CALLSIGN, newName);
                    attributeSet.setAttribute(IRoute.DISPLAY_NAME, newName); // TODO TAKX-4273 decide which attribute to use
                }
            }
        });

        if (enableDeleteButton)
        {
            // add delete button
            deleteButton.setIcon((DELETE_IMAGE));
            deleteButton.addActionListener(action -> {
                final int dialogResult = AlwaysOnTopJOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete " + attributeSet.getStringAttribute(IRoute.DISPLAY_NAME, "this waypoint") + "?",
                        "Delete Waypoint", JOptionPane.OK_CANCEL_OPTION);

                if (dialogResult == JOptionPane.OK_OPTION) deleteButtonConsumer.accept(waypointUid);
            });
        } else
        {
            deleteButton.setVisible(false);
        }
    }

    private void attachmentButtonPressed()
    {
        // TODO TAKX-1256
    }

    private void cueButtonPressed()
    {
        // TODO TAKX-1257
    }

    /**
     * @param distanceString Sets the string to be displayed in the distance label
     */
    @InvokeOnEDT
    public void setDistanceValue(String distanceString)
    {
        distanceLabel.setText(distanceString);
    }

    public GeoPointMetaData getWaypoint()
    {
        return waypoint;
    }

    private void initComponents()
    {
        renameButton = new JButton();
        distanceLabel = new JLabel();
        attachmentButton = new JButton();
        cueButton = new JButton();
        deleteButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setName("this");

        //---- renameButton ----
        renameButton.setName("renameButton");
        add(renameButton, cc.xy(1, 1));

        //---- distanceLabel ----
        distanceLabel.setName("distanceLabel");
        add(distanceLabel, cc.xy(3, 1));

        //---- attachmentButton ----
        attachmentButton.setName("attachmentButton");
        attachmentButton.setToolTipText("Add attachment");
        attachmentButton.addActionListener(action -> attachmentButtonPressed());
        add(attachmentButton, cc.xy(5, 1));

        //---- cueButton ----
        cueButton.setName("cueButton");
        cueButton.setToolTipText("Set cue");
        cueButton.addActionListener(action -> cueButtonPressed());
        add(cueButton, cc.xy(7, 1));

        //---- deleteButton ----
        deleteButton.setName("deleteButton");
        deleteButton.setToolTipText("Delete");
        add(deleteButton, cc.xy(9, 1));
    }

    private JButton renameButton;
    private JLabel distanceLabel;
    private JButton attachmentButton;
    private JButton cueButton;
    private JButton deleteButton;
}
