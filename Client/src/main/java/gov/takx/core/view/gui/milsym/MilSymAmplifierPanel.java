package gov.takx.core.view.gui.milsym;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.symbology.Affiliation;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.milsym.Echelon;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.MilStd2525Variant;
import gov.takx.api.milsym.Presence;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * A panel to preview and manipulate amplifiers for symbol codes.
 *
 * @since 5.2
 */
@PseudoBean
public class MilSymAmplifierPanel extends JPanel
{
    // Arbitrary graphic with no details, makes for a good preview.
    private static final String PREVIEW_CODE_C = "O*O*CB----*****";
    private static final String PREVIEW_CODE_SUFFIX = "0110000013000000000000000000";
    private static final int PREVIEW_SIZE = 128;

    /**
     * Order differs from {@link Affiliation#values()} such that {@link Affiliation#Unknown} is first, along with the other common cases
     */
    private static final Affiliation[] AFFILIATIONS = new Affiliation[]{
            Affiliation.Unknown,
            Affiliation.Friend,
            Affiliation.Neutral,
            Affiliation.Hostile,
            Affiliation.Suspect,
            Affiliation.Joker,
            Affiliation.Faker,
            Affiliation.AssumedFriend,
            Affiliation.Pending,
            Affiliation.ExerciseUnknown,
            Affiliation.ExerciseAssumedFriend,
            Affiliation.ExerciseFriend,
            Affiliation.ExerciseNeutral,
            Affiliation.ExercisePending,
            Affiliation.SimulatedUnknown,
            Affiliation.SimulatedFriend,
            Affiliation.SimulatedNeutral,
            Affiliation.SimulatedHostile,
            Affiliation.SimulatedAssumedFriend,
            Affiliation.SimulatedSuspect,
            Affiliation.SimulatedPending
    };
    private final IMilStd2525Service milStd2525Service;
    private final Runnable refreshAction;

    /**
     * @param milStd2525Service MIL-STD-2525 service to render symbols with.
     * @param refreshAction Action to run when this panels values are updated.
     */
    public MilSymAmplifierPanel(@Nonnull IMilStd2525Service milStd2525Service, @Nonnull Runnable refreshAction)
    {
        this.milStd2525Service = milStd2525Service;
        this.refreshAction = refreshAction;

        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        // This has to happen in an @PostConstruct instead of in the constructor to avoid a race condition where the EDT
        // picks up the event *before* the injection processing happens at the end of the constructor.
        SwingUtilities.invokeLater(this::lateInit);
    }

    private void lateInit()
    {
        DefaultComboBoxModel<Affiliation> affiliationModel = new DefaultComboBoxModel<>(AFFILIATIONS);
        if (affiliationComboBox.getSelectedItem() instanceof Affiliation priorAffiliation) affiliationModel.setSelectedItem(priorAffiliation);
        affiliationComboBox.setModel(affiliationModel);
        affiliationComboBox.setRenderer(new AffiliationComboRenderer());

        DefaultComboBoxModel<Echelon> echelonModel = new DefaultComboBoxModel<>(Echelon.values());
        if (echelonComboBox.getSelectedItem() instanceof Echelon priorEchelon)
        {
            echelonModel.setSelectedItem(priorEchelon);
        } else
        {
            echelonModel.setSelectedItem(Echelon.UNSPECIFIED);
        }
        echelonComboBox.setModel(echelonModel);
        echelonComboBox.setRenderer(new EchelonComboRenderer());

        DefaultComboBoxModel<Presence> presenceModel = new DefaultComboBoxModel<>(Presence.values());
        if (presenceComboBox.getSelectedItem() instanceof Presence priorPresence)
        {
            presenceModel.setSelectedItem(priorPresence);
        } else
        {
            presenceModel.setSelectedItem(Presence.UNSPECIFIED);
        }
        presenceComboBox.setModel(presenceModel);
        presenceComboBox.setRenderer(new PresenceComboRenderer());

        // Now that the models are populated, create an initial preview.
        refreshPreview();
    }

    /**
     * @return Currently selected affiliation.
     */
    @Nonnull
    public Affiliation getAffiliation()
    {
        return Objects.requireNonNullElse((Affiliation) affiliationComboBox.getSelectedItem(), Affiliation.Unknown);
    }

    /**
     * @param affiliation Affiliation value to select.
     */
    public void setAffiliation(@Nonnull Affiliation affiliation)
    {
        affiliationComboBox.getModel().setSelectedItem(affiliation);
    }

    /**
     * @return Currently selected presence.
     */
    @Nonnull
    public Presence getPresence()
    {
        return Objects.requireNonNullElse((Presence) presenceComboBox.getSelectedItem(), Presence.UNSPECIFIED);
    }

    /**
     * @param presence Presence value to select.
     */
    public void setPresence(@Nonnull Presence presence)
    {
        presenceComboBox.getModel().setSelectedItem(presence);
    }

    /**
     * @return Currently selected echelon.
     */
    @Nonnull
    public Echelon getEchelon()
    {
        return Objects.requireNonNullElse((Echelon) echelonComboBox.getSelectedItem(), Echelon.UNSPECIFIED);
    }

    /**
     * @param echelon Echelon value to select.
     */
    public void setEchelon(@Nonnull Echelon echelon)
    {
        echelonComboBox.getModel().setSelectedItem(echelon);
    }

    /**
     * Called when any of the amplifier inputs are modified.
     */
    private void onStateChanged()
    {
        refreshPreview();
        refreshAction.run();
    }

    /**
     * Refresh the preview graphic to showcase the current amplifiers specified.
     */
    private void refreshPreview()
    {

        String symbolCode;
        if (milStd2525Service.getVariant() == MilStd2525Variant.CHARLIE)
        {
            symbolCode = PREVIEW_CODE_C;
        } else
        {
            symbolCode = milStd2525Service.getVariant().version() + PREVIEW_CODE_SUFFIX;
        }

        BufferedImage render = milStd2525Service.newSymbolIconBuilder()
                .setCode(symbolCode)
                .setAffiliation(getAffiliation())
                .setEchelon(getEchelon())
                .setPresence(getPresence())
                .setResolution(PREVIEW_SIZE)
                .render();
        ImageIcon icon = new ImageIcon(render);
        previewDisplayLabel.setIcon(icon);
    }

    /**
     * ComboBox renderer for {@link Affiliation} values.
     */
    private static class AffiliationComboRenderer extends BasicComboBoxRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Affiliation affiliation)
            {
                setText(affiliation.name());
            } else
            {
                setText("Unknown affiliation value: " + value);
            }

            return this;
        }
    }

    /**
     * ComboBox renderer for {@link Echelon} values.
     */
    private static class EchelonComboRenderer extends BasicComboBoxRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Echelon echelon)
            {
                setText(echelon.getDisplay());
            } else
            {
                setText("Unknown echelon value: " + value);
            }

            return this;
        }
    }

    /**
     * ComboBox renderer for {@link Presence} values.
     */
    private static class PresenceComboRenderer extends BasicComboBoxRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Presence presence)
            {
                setText(presence.getDisplay());
            } else
            {
                setText("Unknown presence value: " + value);
            }

            return this;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var previewLabel = new JLabel();
        var previewWrapper = new JPanel();
        previewDisplayLabel = new JLabel();
        Dimension previewDimensions = new Dimension(PREVIEW_SIZE, PREVIEW_SIZE);
        previewDisplayLabel.setMinimumSize(previewDimensions);
        previewDisplayLabel.setPreferredSize(previewDimensions);
        var affiliationLabel = new JLabel();
        affiliationComboBox = new JComboBox<>();
        var presenceLabel = new JLabel();
        presenceComboBox = new JComboBox<>();
        var echelonLabel = new JLabel();
        echelonComboBox = new JComboBox<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new CompoundBorder(
                new LineBorder(UIManager.getColor("Component.borderColor")),
                new EmptyBorder(10, 10, 10, 10)));
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "3*(default, $lgap), default"));

        //---- previewLabel ----
        previewLabel.setText("Preview");
        previewLabel.setName("previewLabel");
        add(previewLabel, cc.xy(1, 1));

        //======== previewWrapper ========
        {
            previewWrapper.setName("previewWrapper");
            previewWrapper.setLayout(new FormLayout(
                    "center:default:grow",
                    "default:grow"));

            //---- previewDisplayLabel ----
            previewDisplayLabel.setBorder(new LineBorder(UIManager.getColor("Component.borderColor"), 1, true));
            previewDisplayLabel.setName("previewDisplayLabel");
            previewWrapper.add(previewDisplayLabel, cc.xy(1, 1));
        }
        add(previewWrapper, cc.xy(3, 1));

        //---- affiliationLabel ----
        affiliationLabel.setText("Affiliation");
        affiliationLabel.setName("affiliationLabel");
        add(affiliationLabel, cc.xy(1, 3));

        //---- affiliationComboBox ----
        affiliationComboBox.setName("affiliationComboBox");
        affiliationComboBox.addItemListener(e -> onStateChanged());
        add(affiliationComboBox, cc.xy(3, 3));

        //---- presenceLabel ----
        presenceLabel.setText("Presence");
        presenceLabel.setName("presenceLabel");
        add(presenceLabel, cc.xy(1, 5));

        //---- presenceComboBox ----
        presenceComboBox.setName("presenceComboBox");
        presenceComboBox.addItemListener(e -> onStateChanged());
        add(presenceComboBox, cc.xy(3, 5));

        //---- echelonLabel ----
        echelonLabel.setText("Echelon");
        echelonLabel.setName("echelonLabel");
        add(echelonLabel, cc.xy(1, 7));

        //---- echelonComboBox ----
        echelonComboBox.setName("echelonComboBox");
        echelonComboBox.addItemListener(e -> onStateChanged());
        add(echelonComboBox, cc.xy(3, 7));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel previewDisplayLabel;
    private JComboBox<Affiliation> affiliationComboBox;
    private JComboBox<Presence> presenceComboBox;
    private JComboBox<Echelon> echelonComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
