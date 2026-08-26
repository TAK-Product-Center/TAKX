package gov.takx.core.view.gui.navigation;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.commons.swing.FormButtonBar;
import gov.takx.core.mapobject.gui.MapObjectField;
import gov.takx.core.navigation.NavigationConfiguration;
import gov.takx.core.navigation.NavigationManager;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * Swing dialog for creating a navigation configuration.
 *
 * @since 5.6 (Renamed from EditBloodhoundConfigurationDialog in 5.7)
 */
@PseudoBean
public class NavigateToDialog extends AStandardDialog
{
    @Inject
    private NavigationManager navigationManager;

    private NavigationConfiguration navigationConfiguration;

    /**
     * Construct a new dialog with all fields empty.
     */
    public NavigateToDialog()
    {
        initComponents();
        // Prevent selection of anything other than PointMapItem and also prevent selecting the same map object in both fields
        sourceField.setFilter(e -> e instanceof IPointMapObject && e != targetField.getSelectedItem());
        buttonBar.attachFieldActionListener(sourceField, sourceField::addActionListener, e -> sourceField.getSelectedItem() == null ? "Must select a 'From' marker" : null);
        buttonBar.attachFieldActionListener(targetField, targetField::addActionListener, e -> targetField.getSelectedItem() == null ? "Must select a 'To' object" : null);
    }

    /**
     * @param from the object to select in the "From" field
     * @since 5.7
     */
    public void setFrom(@Nullable IPointMapObject from)
    {
        sourceField.setSelectedItem(from);
    }

    /**
     * @return the created or edited navigation configuration
     * @since 5.7
     */
    @Nullable
    public NavigationConfiguration getNavigationConfiguration()
    {
        return navigationConfiguration;
    }

    @PostConstruct
    private void postConstruct()
    {
        // Only allow navigable items to be selected
        targetField.setFilter(mapObject -> mapObject != sourceField.getSelectedItem() && navigationManager.isNavigable(mapObject));
    }

    private void buttonBarApply(ActionEvent ignored)
    {
        //noinspection DataFlowIssue source and target will always have values here
        navigationConfiguration = navigationManager.createNavigationConfiguration(sourceField.getSelectedItem(), targetField.getSelectedItem());
    }

    @SuppressWarnings({"Convert2MethodRef", "RedundantExplicitVariableType"})
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPanel = new JPanel();
        var formPanel = new JPanel();
        var sourceLabel = new JLabel();
        sourceField = new MapObjectField<>();
        var targetLabel = new JLabel();
        targetField = new MapObjectField<>();
        buttonBar = new FormButtonBar();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Navigate To");
        setMinimumSize(new Dimension(300, 75));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPanel ========
        {
            dialogPanel.setBorder(Borders.DIALOG_BORDER);
            dialogPanel.setName("dialogPanel");
            dialogPanel.setLayout(new BorderLayout());

            //======== formPanel ========
            {
                formPanel.setName("formPanel");
                formPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "default, $lgap, default"));

                //---- sourceLabel ----
                sourceLabel.setText("From:");
                sourceLabel.setName("sourceLabel");
                formPanel.add(sourceLabel, cc.xy(1, 1));

                //---- sourceField ----
                sourceField.setNewButtonVisible(false);
                sourceField.setName("sourceField");
                formPanel.add(sourceField, cc.xy(3, 1));

                //---- targetLabel ----
                targetLabel.setText("To:");
                targetLabel.setName("targetLabel");
                formPanel.add(targetLabel, cc.xy(1, 3));

                //---- targetField ----
                targetField.setNewButtonVisible(false);
                targetField.setName("targetField");
                formPanel.add(targetField, cc.xy(3, 3));
            }
            dialogPanel.add(formPanel, BorderLayout.CENTER);

            //---- buttonBar ----
            buttonBar.setDeleteButtonVisible(false);
            buttonBar.setApplyButtonVisible(false);
            buttonBar.setName("buttonBar");
            buttonBar.addApplyListener(e -> buttonBarApply(e));
            dialogPanel.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private MapObjectField<IPointMapObject> sourceField;
    private MapObjectField<IMapObject> targetField;
    private FormButtonBar buttonBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
