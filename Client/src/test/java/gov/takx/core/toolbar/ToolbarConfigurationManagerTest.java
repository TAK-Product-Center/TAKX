package gov.takx.core.toolbar;

import gov.takx.api.event.FinalizeToolbars;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarComponent;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.test.AContainerTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * Unit tests for the methods of {@link ToolbarConfigurationManager}.
 *
 * @since 2.18 (Renamed in 3.4)
 */
public class ToolbarConfigurationManagerTest extends AContainerTest
{
    private final TestDevicePlugin testDevicePlugin = new TestDevicePlugin();
    private ToolbarConfigurationManager configurationManager;
    private ToolbarConfiguration defaultConfiguration;

    @BeforeEach
    public void setup()
    {
        configurationManager = getBean(ToolbarConfigurationManager.class);

        // The default configuration will always exist, so go ahead and create it for more accurate tests
        getBean(ToolbarManager.class).createStandardTabs();
        defaultConfiguration = configurationManager.getDefaultConfiguration();

        container.event().select(FinalizeToolbars.class).fire(new FinalizeToolbars());
    }

    /**
     * @since 2.38
     */
    @AfterEach
    public void cleanup()
    {
        // Since we are deleting all the configurations (which doesn't happen outside of tests), we need to make sure
        // none are active, or it will get stuck trying to activate a backup config when the last one is deleted.
        CdiTestUtils.setField(configurationManager, "activeConfiguration", null);

        // Clear any configurations, tabs, or buttons left over from the tests
        configurationManager.getConfigurations().forEach(c -> configurationManager.removeConfiguration(c));
        configurationManager.getReferenceButtons().forEach(b -> configurationManager.removeReferenceButton(b, false));
        configurationManager.getReferenceTabs().forEach(t -> configurationManager.removeReferenceTab(t));
    }

    /**
     * @since 3.4
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void testImportConfiguration()
    {
        // Add two dynamic buttons ahead of the config to verify they are handled correctly.
        final ToolbarButton connectionButton1 = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "Dummy connection 1");
        final ToolbarButton connectionButton2 = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "Dummy connection 2");
        configurationManager.addReferenceButton(connectionButton1);
        configurationManager.addReferenceButton(connectionButton2);

        final URL url = getClass().getClassLoader().getResource("toolbar/testToolbarConfig.xml");
        final ToolbarConfiguration config = configurationManager.importConfiguration(url.getFile());

        // All tabs should reference the config, and all components should reference their parent tab.
        assertThat(config).isNotNull();
        assertThat(config.isDeletable()).isTrue();
        assertThat(config.getTabs()).allSatisfy(t -> assertThat(t.getConfiguration()).isEqualTo(config))
                .allSatisfy(t -> assertThat(t.getComponents()).allSatisfy(c -> assertThat(c.getTab()).isEqualTo(t)));

        // Dynamic buttons should be added as necessary. The test config is set to exclude Dummy connection 1, so only
        // Dummy connection 2 should be added to the Connect tab.
        assertThat(config.getTabs()).element(1).satisfies(t -> assertThat(t.getComponents())
                .extracting(AToolbarComponent::getId).containsExactly(connectionButton2.getId()));

        // Transient state should be copied from reference list. We can spot check this by checking for a valid button
        // with an image set.
        assertThat(config.getTabs()).flatMap(ToolbarTab::getComponents)
                .filteredOn(ToolbarButton.class::isInstance)
                .anySatisfy(b -> assertThat(b).asInstanceOf(type(ToolbarButton.class))
                        .extracting(ToolbarButton::getImage).isNotNull());

        // Buttons not in the reference list should be invalid. The test config has 2 bogus user favorites buttons that
        // should be invalid.
        assertThat(config.getTabs()).flatMap(ToolbarTab::getComponents)
                .filteredOn(ToolbarButton.class::isInstance)
                .filteredOn(c -> ((ToolbarButton) c).getType() == ButtonType.USER_FAVORITE)
                .allSatisfy(b -> assertThat(b.isValid()).isFalse());

        // Any missing reference tabs should have been added in correct spot. The test config has the Data tab commented
        // out, so it should be added in index 5.
        assertThat(config.getTabs()).element(5).extracting(ToolbarTab::getId).isEqualTo(TabType.DATA.getSingularId());
    }

    /**
     * @since 3.4
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void testImportConfiguration_noVersion()
    {
        final URL url = getClass().getClassLoader().getResource("toolbar/testToolbarConfig_noVersion.xml");
        assertThatIllegalArgumentException().isThrownBy(() -> configurationManager.importConfiguration(url.getFile()));
    }

    /**
     * @since 3.4
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void testImportConfiguration_oldVersion()
    {
        final URL url = getClass().getClassLoader().getResource("toolbar/testToolbarConfig_oldVersion.xml");
        assertThatIllegalArgumentException().isThrownBy(() -> configurationManager.importConfiguration(url.getFile()));
    }

    /**
     * @since 3.4
     */
    @Test
    public void testActivateConfiguration()
    {
        final ToolbarConfiguration config = createAndAddConfiguration("Test");
        configurationManager.activateConfiguration(config);

        assertThat(configurationManager.getActiveConfiguration()).isEqualTo(config);
    }

    /**
     * Creates a configuration with the provided name and adds it to the configuration manager.
     *
     * @param name The name of the configuration
     * @return The newly created and added configuration
     * @since 3.4
     */
    @Nonnull
    private ToolbarConfiguration createAndAddConfiguration(String name)
    {
        final ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration().setName(name);
        configurationManager.addConfiguration(configuration);
        return configuration;
    }

    /**
     * @since 3.4
     */
    @Test
    public void testAddConfiguration()
    {
        final ToolbarConfiguration config = createAndAddConfiguration("Test");

        assertThat(configurationManager.getConfiguration("Test")).isEqualTo(config);
        assertThat(configurationManager.getConfigurations()).containsOnly(defaultConfiguration, config);
        assertThat(configurationManager.getNonDefaultConfigurations()).containsExactly(config);
    }

    /**
     * @since 3.4
     */
    @Test
    public void testRemoveConfiguration()
    {
        final ToolbarConfiguration config = createAndAddConfiguration("Test");
        configurationManager.activateConfiguration(config);
        assertThat(configurationManager.getConfiguration("Test")).isEqualTo(config);
        assertThat(configurationManager.getActiveConfiguration()).isEqualTo(config);

        configurationManager.removeConfiguration(config);

        assertThat(configurationManager.getConfiguration("Test")).isNull();
        assertThat(configurationManager.getActiveConfiguration()).isEqualTo(defaultConfiguration);
    }

    /**
     * @since 3.4
     */
    @Test
    public void testUpdateConfiguration()
    {
        final ToolbarConfiguration config = createAndAddConfiguration("Test");
        configurationManager.activateConfiguration(config);
        assertThat(configurationManager.getActiveConfiguration()).isEqualTo(config);

        config.setName("New Name");
        configurationManager.updateConfiguration("Test", config);
        assertThat(configurationManager.getConfiguration("Test")).isNull();
        assertThat(configurationManager.getConfiguration("New Name")).isEqualTo(config);
        assertThat(configurationManager.getActiveConfiguration()).isEqualTo(config);
    }

    @Test
    public void testAddReferenceTab_multipleTimes_isAddedOnce()
    {
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");

        // The new tab should be added as a reference tab
        configurationManager.addReferenceTab(tab);
        final UUID tabId = tab.getId();
        assertThat(configurationManager.getReferenceTabs()).extracting(ToolbarTab::getId).containsOnlyOnce(tabId);

        // Adding the same tab again should fail
        configurationManager.addReferenceTab(tab);
        assertThat(configurationManager.getReferenceTabs()).extracting(ToolbarTab::getId).containsOnlyOnce(tabId);

        // Adding a copy of the same tab should fail
        ToolbarTab tabCopy = tab.copy();
        configurationManager.addReferenceTab(tabCopy);
        assertThat(configurationManager.getReferenceTabs()).extracting(ToolbarTab::getId).containsOnlyOnce(tabId);
    }

    /**
     * @since 3.4
     */
    @Test
    public void testAddReferenceTab_withComponents_addsReferenceButton()
    {
        // Create a plugin tab with a plugin-created button and a core button
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test Tab");
        final ToolbarButton pluginButton = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Test Button");
        tab.addComponent(pluginButton, false);
        tab.addComponent(new ToolbarSeparator(testDevicePlugin, "Test Tab:1"), false);
        final ToolbarButton coreButton = configurationManager.getReferenceButton(ButtonType.FILE.requireSingularId());
        assertThat(coreButton).isNotNull();
        tab.addComponent(coreButton, true);

        configurationManager.addReferenceTab(tab);

        // The plugin button should be added to the reference buttons, but the core button should not be added again
        assertThat(configurationManager.getReferenceButtons()).contains(pluginButton);
        assertThat(configurationManager.getReferenceButtons()).extracting(AToolbarComponent::getId).containsOnlyOnce(coreButton.getId());
    }

    /**
     * @since 3.4
     */
    @Test
    public void testAddReferenceTab_updatesOtherConfigurations()
    {
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test Tab");
        final UUID tabId = tab.getId();

        // Create a configuration where the tab will be marked as invalid
        final ToolbarConfiguration configWithInvalidTab = ToolbarConfiguration.createConfiguration().setName("Invalid");
        configWithInvalidTab.addTab(tab, true);
        configWithInvalidTab.getTabs().forEach(t -> t.setValid(false));
        configurationManager.addConfiguration(configWithInvalidTab);
        assertThat(configWithInvalidTab.getTabs()).last().extracting(ToolbarTab::isValid, ToolbarTab::getId).containsExactly(false, tabId);

        // Create a configuration that will be missing the tab
        final ToolbarConfiguration configMissingTab = createAndAddConfiguration("Missing");
        assertThat(configMissingTab.getTabs()).extracting(ToolbarTab::getId).doesNotContain(tabId);

        configurationManager.addReferenceTab(tab);

        // The tab should be marked as valid in first config
        assertThat(configWithInvalidTab.getTabs()).last().extracting(ToolbarTab::isValid, ToolbarTab::getId).containsExactly(true, tabId);

        // The tab should be added to the second config
        assertThat(configMissingTab.getTabs()).last().extracting(ToolbarTab::getId).isEqualTo(tabId);
    }

    /**
     * @since 3.4
     */
    @Test
    public void testInsertReferenceTab()
    {
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");

        // The new tab should be added as a reference tab at the specified index
        configurationManager.insertReferenceTab(tab, 2);
        assertThat(configurationManager.getReferenceTabs()).element(2).extracting(ToolbarTab::getId).isEqualTo(tab.getId());
    }

    /**
     * @since 2.38
     */
    @Test
    public void testRemoveReferenceTab()
    {
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        configuration.addTab(tab, true);
        configurationManager.addReferenceTab(tab);
        configurationManager.addConfiguration(configuration);

        verifyTabAddedCorrectly(tab);

        configurationManager.removeReferenceTab(tab);

        verifyTabRemovedCorrectly(tab);

        // Reference tabs should be deleted w/o first marking them as invalid; otherwise, the deletion will be
        // attempted twice
        assertThat(tab.isValid()).isTrue();
    }

    /**
     * Verifies that the tab was correctly added to the configuration manager and any configurations.
     *
     * @param tab The tab that was added
     * @since 2.38
     */
    private void verifyTabAddedCorrectly(ToolbarTab tab)
    {
        // The tab should have been added directly to the default configuration
        assertThat(defaultConfiguration.getTabs()).last().isNotNull().isSameAs(tab);

        // A copy should have been added to any custom configurations
        assertThat(configurationManager.getNonDefaultConfigurations()).extracting(ToolbarConfiguration::getTabs)
                .allSatisfy(tabs -> assertThat(tabs).extracting(ToolbarTab::getId).contains(tab.getId()));

        // It should be in reference tabs exactly once
        assertThat(configurationManager.getReferenceTabs()).containsOnlyOnce(tab);
    }

    /**
     * Verifies that the tab was correctly removed from the configuration manager and any configurations.
     *
     * @param tab The tab that was removed
     * @since 2.38
     */
    private void verifyTabRemovedCorrectly(ToolbarTab tab)
    {
        // Tab should be completely removed from the default configuration
        assertThat(defaultConfiguration.getTabs()).doesNotContain(tab);

        // Tab should be present in any custom configurations but not valid
        assertThat(configurationManager.getNonDefaultConfigurations())
                .allSatisfy(c -> assertThat(c.getTabs()).last()
                        .extracting(ToolbarTab::getId, ToolbarTab::isValid)
                        .containsOnly(tab.getId(), false));

        // It should no longer be present as a reference tab
        assertThat(configurationManager.getReferenceTabs()).doesNotContain(tab);
    }

    @Test
    public void testAddReferenceButton_multipleTimes_isAddedOnce()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "My button");

        // A new button should be added
        configurationManager.addReferenceButton(button);
        final UUID buttonId = button.getId();
        assertThat(configurationManager.getReferenceButtons()).extracting(AToolbarComponent::getId).containsOnlyOnce(buttonId);

        // Adding the same button again should fail
        configurationManager.addReferenceButton(button);
        assertThat(configurationManager.getReferenceButtons()).extracting(AToolbarComponent::getId).containsOnlyOnce(buttonId);

        // Adding a copy of the same button should fail
        ToolbarButton buttonCopy = button.copy();
        configurationManager.addReferenceButton(buttonCopy);
        assertThat(configurationManager.getReferenceButtons()).extracting(AToolbarComponent::getId).containsOnlyOnce(buttonId);
    }

    /**
     * @since 3.4
     */
    @Test
    public void testAddReferenceButton_withDynamicType_addsButtonToTabs()
    {
        ToolbarTab tab = configurationManager.getReferenceTab(TabType.CONNECT.requireSingularId());
        assertThat(tab).isNotNull();
        final ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration().setName("Test Config");
        configuration.addTab(tab, true);
        configurationManager.addConfiguration(configuration);

        ToolbarButton button = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "My connection");
        configurationManager.addReferenceButton(button);

        final List<ToolbarTab> connectTabs = getAllToolbarTabs(TabType.CONNECT.requireSingularId());
        assertThat(connectTabs).hasSize(2); // One in default config and one in custom config
        assertThat(connectTabs).allSatisfy(t -> assertThat(t.getComponents()).extracting(AToolbarComponent::getId).containsOnlyOnce(button.getId()));
    }

    /**
     * @param tabId The tab id
     * @return List of all toolbar tabs in all configurations with the provided tab id.
     * @since 3.4
     */
    private List<ToolbarTab> getAllToolbarTabs(UUID tabId)
    {
        return configurationManager.getConfigurations().stream().flatMap(c -> c.getTabs().stream())
                .filter(t -> t.getId().equals(tabId)).toList();
    }

    /**
     * @since 3.4
     */
    @Test
    public void testAddReferenceButton_withInvalidButtonInConfig_updatesButtonValidityInConfig()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Test Button");
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test Tab");
        tab.addComponent(button, false);

        // Create a configuration where the button will be marked as invalid
        final ToolbarConfiguration configWithInvalidButton = ToolbarConfiguration.createConfiguration().setName("Invalid");
        configWithInvalidButton.addTab(tab, false);
        configWithInvalidButton.getTabs().stream().flatMap(t -> t.getComponents().stream())
                .filter(c -> c instanceof ToolbarButton)
                .map(ToolbarButton.class::cast)
                .forEach(b -> b.setValid(false));
        configurationManager.addConfiguration(configWithInvalidButton);

        configurationManager.addReferenceButton(button);

        // The button in the custom plugin tab should be marked as valid in the config
        assertThat(configWithInvalidButton.getTabs()).last()
                .satisfies(t -> assertThat(t.getComponents()).extracting(AToolbarComponent::isValid).containsExactly(true));
    }

    /**
     * Verifies that when {@link ToolbarConfigurationManager#removeReferenceButton(ToolbarButton, boolean)} is
     * called for a button in the default configuration, the button is completely removed from the configuration (aka hard deleted).
     *
     * @since 2.38
     */
    @Test
    public void testRemoveReferenceButton_hardDeletesFromDefaultConfiguration()
    {
        // Create tab with one button and add both elements to the configuration manager
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "My button");
        tab.addComponent(button, true);
        configurationManager.addReferenceTab(tab);

        verifyButtonAddedCorrectly(null, button);

        configurationManager.removeReferenceButton(button, true);

        verifyButtonRemovedCorrectly(null, button, true);
    }

    /**
     * Verifies that when {@link ToolbarConfigurationManager#removeReferenceButton(ToolbarButton, boolean)} is
     * called for a button in a custom configuration and soft delete is selected, the button is only invalidated within
     * the configuration (aka soft deleted).
     *
     * @since 2.38
     */
    @Test
    public void testRemoveReferenceButton_usingSoftDelete_softDeletesFromCustomConfiguration()
    {
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "My button");
        tab.addComponent(button, true);
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        configuration.addTab(tab, true);
        configurationManager.addConfiguration(configuration);
        configurationManager.addReferenceTab(tab);

        verifyButtonAddedCorrectly(configuration, button);

        configurationManager.removeReferenceButton(button, true);

        verifyButtonRemovedCorrectly(configuration, button, true);
    }

    /**
     * Verifies that when {@link ToolbarConfigurationManager#removeReferenceButton(ToolbarButton, boolean)} is
     * called for a button in a custom configuration and hard delete is selected, the button is completely removed.
     *
     * @since 2.38
     */
    @Test
    public void testRemoveReferenceButton_usingHardDelete_hardDeletesFromCustomConfiguration()
    {
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "My button");
        tab.addComponent(button, true);
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        configuration.addTab(tab, true);
        configurationManager.addConfiguration(configuration);
        configurationManager.addReferenceTab(tab);

        verifyButtonAddedCorrectly(configuration, button);

        configurationManager.removeReferenceButton(button, false);

        verifyButtonRemovedCorrectly(configuration, button, false);
    }

    /**
     * Verifies that the button was correctly added to the configuration manager and any configurations.
     *
     * @param configuration Custom configuration (may be {@code null})
     * @param button        The button that was added
     * @since 2.38
     */
    private void verifyButtonAddedCorrectly(ToolbarConfiguration configuration, @Nonnull ToolbarButton button)
    {
        final UUID buttonId = button.getId();

        // The button should have been added to the default configuration
        assertThat(defaultConfiguration.getTabs()).last()
                .withFailMessage("Expecting button to be added to default configuration but was not")
                .satisfies(t -> assertThat(t.getComponents()).extracting(AToolbarComponent::getId).containsExactly(buttonId));

        if (configuration != null)
        {
            // A copy should have been added to any custom configurations
            assertThat(configuration.getTabs()).last()
                    .withFailMessage("Expecting button to be added to custom configuration but was not")
                    .satisfies(t -> assertThat(t.getComponents()).extracting(AToolbarComponent::getId).containsExactly(buttonId));
        }

        assertThat(configurationManager.getReferenceButtons())
                .withFailMessage("Expecting button to be in reference buttons exactly once")
                .extracting(AToolbarComponent::getId)
                .containsOnlyOnce(buttonId);
    }

    /**
     * Verifies that the button was correctly removed from the configuration manager and any configurations.
     *
     * @param configuration Custom configuration (may be {@code null})
     * @param button        The button that was removed
     * @param softDelete    Indicates whether soft delete was used
     * @since 2.38
     */
    private void verifyButtonRemovedCorrectly(ToolbarConfiguration configuration, ToolbarButton button, boolean softDelete)
    {
        final UUID buttonId = button.getId();

        // Button should be completely removed from the default configuration
        assertThat(defaultConfiguration.getTabs()).last()
                .satisfies(t -> assertThat(t.getComponents()).extracting(AToolbarComponent::getId).isEmpty());

        if (configuration != null)
        {
            if (softDelete)
            {
                // Button should be present but not valid
                assertThat(configuration.getTabs()).last()
                        .satisfies(t -> assertThat(t.getComponents())
                                .extracting(AToolbarComponent::getId, AToolbarComponent::isValid)
                                .containsOnly(tuple(buttonId, false)));
            } else
            {
                assertThat(configuration.getTabs()).last()
                        .satisfies(t -> assertThat(t.getComponents()).extracting(AToolbarComponent::getId).isEmpty());
            }
        }

        // It should no longer be present as a reference button
        assertThat(configurationManager.getReferenceButtons())
                .extracting(AToolbarComponent::getId)
                .doesNotContain(buttonId);
    }

    /**
     * @since 3.4
     */
    @Test
    public void testGetIdOfButtonWithComponentDescriptorId()
    {
        // First try with no associated button
        final String componentDescriptorId = CoreComponentType.APPS_PANEL.getComponentDescriptorId();
        UUID buttonId = configurationManager.getIdOfButtonWithComponentDescriptorId(componentDescriptorId);
        assertThat(buttonId).isNull();

        // Next associate a button and try again
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        ToolbarButton button = ToolbarTestUtils.createPluginButtonBuilder(testDevicePlugin, "My button")
                .setComponentDescriptorId(componentDescriptorId)
                .build();
        tab.addComponent(button, false);
        configurationManager.addReferenceTab(tab);

        buttonId = configurationManager.getIdOfButtonWithComponentDescriptorId(componentDescriptorId);
        assertThat(buttonId).isEqualTo(button.getId());
    }

    /**
     * @since 3.4
     */
    @Test
    public void testAssociateButtonWithComponentDescriptor()
    {
        // Add the apps button to a user tab (in addition to the default config)
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        final ToolbarButton button = configurationManager.getReferenceButton(ButtonType.APPS.requireSingularId());
        assertThat(button).isNotNull();
        tab.addComponent(button, true);
        configurationManager.addReferenceTab(tab);

        final String componentDescriptorId = CoreComponentType.APPS_PANEL.getComponentDescriptorId();
        final UUID buttonId = ButtonType.APPS.requireSingularId();
        configurationManager.associateButtonWithComponentDescriptor(buttonId, componentDescriptorId);

        // There should be two buttons with the Apps button id, and both should now be associated with the provided
        // component descriptor id.
        final List<ToolbarButton> appsButtons = getAllToolbarButtons()
                .filter(c -> c.getId().equals(buttonId))
                .toList();

        assertThat(appsButtons).hasSize(2)
                .extracting(ToolbarButton::getComponentDescriptorId)
                .contains(componentDescriptorId, componentDescriptorId);
    }

    /**
     * @return Stream of all toolbar buttons in all configurations.
     * @since 3.4
     */
    private Stream<ToolbarButton> getAllToolbarButtons()
    {
        return configurationManager.getConfigurations().stream()
                .flatMap(c -> c.getTabs().stream())
                .flatMap(t -> t.getComponents().stream())
                .filter(c -> c instanceof ToolbarButton)
                .map(ToolbarButton.class::cast);
    }

    /**
     * @since 3.4
     */
    @Test
    public void testGetReferenceButtons()
    {
        final Set<ToolbarButton> referenceButtons = CdiTestUtils.getField(configurationManager, "referenceButtons");

        // All reference buttons should be returned
        assertThat(configurationManager.getReferenceButtons()).containsAll(referenceButtons);

        // List should be empty if no buttons exist for type
        assertThat(configurationManager.getReferenceButtons(ButtonType.CONNECTION_PLUGIN)).isEmpty();

        // Add two buttons for a type; list should contain both buttons
        final ToolbarButton connectionButton1 = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "Dummy connection 1");
        final ToolbarButton connectionButton2 = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "Dummy connection 2");
        configurationManager.addReferenceButton(connectionButton1);
        configurationManager.addReferenceButton(connectionButton2);
        assertThat(configurationManager.getReferenceButtons(ButtonType.CONNECTION_PLUGIN)).containsOnly(connectionButton1, connectionButton2);

        // Singular buttons should be returned for the type or by id
        assertThat(configurationManager.getReferenceButtons(ButtonType.CONNECTIONS)).hasSize(1).first()
                .isNotNull()
                .isEqualTo(configurationManager.getReferenceButton(ButtonType.CONNECTIONS.requireSingularId()));

        // Nothing should be returned if the id doesn't exist
        assertThat(configurationManager.getReferenceButton(UUID.randomUUID())).isNull();
    }

    /**
     * @since 3.4
     */
    @Test
    public void testGetReferenceTabs()
    {
        final List<ToolbarTab> referenceTabs = CdiTestUtils.getField(configurationManager, "referenceTabs");

        // All reference tabs should be returned
        assertThat(configurationManager.getReferenceTabs()).containsAll(referenceTabs);

        // List should be empty if no tabs exist for type
        assertThat(configurationManager.getReferenceTabs(TabType.PLUGIN_CREATED)).isEmpty();

        // Add tab of type; list should contain tab
        final ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test tab");
        configurationManager.addReferenceTab(tab);
        assertThat(configurationManager.getReferenceTabs(TabType.PLUGIN_CREATED)).containsOnly(tab);

        // Singular tabs should be returned for the type or by id
        assertThat(configurationManager.getReferenceTabs(TabType.CONNECT)).hasSize(1).first()
                .isNotNull()
                .isEqualTo(configurationManager.getReferenceTab(TabType.CONNECT.requireSingularId()));

        // Nothing should be returned if the id doesn't exist
        assertThat(configurationManager.getReferenceTab(UUID.randomUUID())).isNull();
    }

    /**
     * @since 3.4
     */
    @Test
    public void testAddButtonToTab_withIndex()
    {
        // Add a reference button normally so the next button can be added out of order
        ToolbarButton button = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "My connection");
        configurationManager.addReferenceButton(button);

        ToolbarButton button2 = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "My connection 2");
        configurationManager.addButtonToTab(TabType.CONNECT, button2, 0);

        final List<ToolbarTab> connectTabs = getAllToolbarTabs(TabType.CONNECT.getSingularId());
        assertThat(connectTabs).hasSize(1); // One in default config
        assertThat(connectTabs).extracting(ToolbarTab::getComponents)
                .allSatisfy(components -> assertThat(components).containsExactly(button2, button));
    }

    /**
     * @since 3.4
     */
    @Test
    public void testRemoveReferenceButton_withDynamicType_removesButtonFromTabs()
    {
        // Add two tabs with the PLUGINS type; only one has the button to be removed
        ToolbarTab tab = ToolbarTab.createSingularTab(TabType.PLUGINS);
        ToolbarButton button = ToolbarTestUtils.createCoreButton(ButtonType.PLUGIN_PANEL, "My button");
        tab.addComponent(button, true);
        final ToolbarConfiguration configWithButton = ToolbarConfiguration.createConfiguration().setName("With button");
        configWithButton.addTab(tab, false);
        configurationManager.addConfiguration(configWithButton);
        configurationManager.addReferenceButton(button);

        ToolbarTab tab2 = ToolbarTab.createSingularTab(TabType.PLUGINS);
        ToolbarButton button2 = ToolbarTestUtils.createCoreButton(ButtonType.PLUGIN_PANEL, "My button 2");
        tab2.addComponent(button2, true);
        final ToolbarConfiguration configWithOtherButton = ToolbarConfiguration.createConfiguration().setName("With other button");
        configWithOtherButton.addTab(tab2, false);
        configurationManager.addConfiguration(configWithOtherButton);
        configurationManager.addReferenceButton(button);

        configurationManager.removeReferenceButton(button, false);

        // Button should be removed from the tab that contained it; the other tab should be unchanged
        assertThat(tab.getComponents()).isEmpty();
        assertThat(tab2.getComponents()).extracting(AToolbarComponent::getId).containsExactly(button2.getId());

        // Button should be removed from default config
        assertThat(defaultConfiguration.getTabs()).extracting(ToolbarTab::getComponents)
                .allSatisfy(components -> assertThat(components).extracting(AToolbarComponent::getId).doesNotContain(button.getId()));
    }

    /**
     * @since 3.4
     */
    @Test
    public void testRemoveButton()
    {
        // Add button with the APPS type (so it will have the APPS singular button id)
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        ToolbarButton button = configurationManager.getReferenceButton(ButtonType.APPS.requireSingularId());
        assertThat(button).isNotNull();
        tab.addComponent(button, true);
        final ToolbarConfiguration config = ToolbarConfiguration.createConfiguration().setName("Custom");
        config.addTab(tab, false);
        configurationManager.addConfiguration(config);
        configurationManager.addReferenceButton(button);

        final UUID buttonId = button.getId();
        configurationManager.removeButton(buttonId);

        // Button should be set to invalid in custom config
        assertThat(config.getTabs()).last()
                .satisfies(t -> assertThat(t.getComponents()).extracting(AToolbarComponent::isValid).containsExactly(false));

        // Button should be removed from default config
        assertThat(defaultConfiguration.getTabs()).last()
                .satisfies(t -> assertThat(t.getComponents()).extracting(AToolbarComponent::getId).doesNotContain(buttonId));
    }

    /**
     * @since 3.4
     */
    @Test
    public void testHandleChangeEvent_tabTextChanged()
    {
        // Create two custom configs (which will have the home tab added automatically)
        final ToolbarConfiguration config1 = createAndAddConfiguration("Custom config 1");
        final ToolbarConfiguration config2 = createAndAddConfiguration("Custom config 2");

        final ToolbarTab homeTab1 = getTab(config1, TabType.HOME);
        final ToolbarTab homeTab2 = getTab(config2, TabType.HOME);
        final ToolbarTab defaultHomeTab = getTab(defaultConfiguration, TabType.HOME);
        final ToolbarTab refHomeTab = configurationManager.getReferenceTab(TabType.HOME.requireSingularId());
        assertThat(refHomeTab).isNotNull();

        // Changing the tab text in a custom config shouldn't affect the other custom config or the default config
        homeTab1.setDisplayText("Home 1");
        assertThat(homeTab2.getDisplayText()).isEqualTo(TabType.HOME.getDisplayText());
        assertThat(defaultHomeTab.getDisplayText()).isEqualTo(TabType.HOME.getDisplayText());
        assertThat(refHomeTab.getDisplayText()).isEqualTo(TabType.HOME.getDisplayText());

        // Changing the tab text of the reference tab should affect all configs
        refHomeTab.setDisplayText("Home 2");
        assertThat(homeTab1.getDisplayText()).isEqualTo("Home 2");
        assertThat(homeTab2.getDisplayText()).isEqualTo("Home 2");
        assertThat(defaultHomeTab.getDisplayText()).isEqualTo("Home 2");
    }

    /**
     * Gets the tab with the specified type from the provided configuration.
     *
     * @param configuration The configuration to get the tab from
     * @param tabType       The type of tab to get
     * @return The tab of the specified type for the provided configuration
     * @since 3.4
     */
    private ToolbarTab getTab(ToolbarConfiguration configuration, TabType tabType)
    {
        final ToolbarTab tab = configuration.getTabs().stream().filter(t -> t.getType() == tabType).findFirst().orElse(null);
        assertThat(tab).isNotNull();
        return tab;
    }

    /**
     * @since 3.4
     */
    @Test
    public void testHandleChangeEvent_tabVisibilityChanged()
    {
        // Create two custom configs (which will have the home tab added automatically)
        final ToolbarConfiguration config1 = createAndAddConfiguration("Custom config 1");
        final ToolbarConfiguration config2 = createAndAddConfiguration("Custom config 2");

        final ToolbarTab homeTab1 = getTab(config1, TabType.HOME);
        final ToolbarTab homeTab2 = getTab(config2, TabType.HOME);
        final ToolbarTab defaultHomeTab = getTab(defaultConfiguration, TabType.HOME);
        final ToolbarTab refHomeTab = configurationManager.getReferenceTab(TabType.HOME.requireSingularId());
        assertThat(refHomeTab).isNotNull();

        // Changing the tab visibility in a custom config shouldn't affect the other custom config or the default config
        homeTab1.setVisible(false, true);
        assertThat(homeTab2.isVisible()).isTrue();
        assertThat(defaultHomeTab.isVisible()).isTrue();
        assertThat(refHomeTab.isVisible()).isTrue();

        // Changing the tab visibility of the reference tab should affect all other configs (besides config 1; will test that below)
        refHomeTab.setVisible(false);
        assertThat(homeTab2.isVisible()).isFalse();
        assertThat(defaultHomeTab.isVisible()).isFalse();

        // Setting the reference tab to visible shouldn't override the user setting for hidden tabs in config 1
        refHomeTab.setVisible(true);
        assertThat(homeTab1.isVisible()).isFalse();
    }

    /**
     * @since 3.4
     */
    @Test
    public void testHandleChangeEvent_componentAdded()
    {
        // Create two custom configs (which will have the home tab added automatically)
        final ToolbarConfiguration config1 = createAndAddConfiguration("Custom config 1");
        final ToolbarConfiguration config2 = createAndAddConfiguration("Custom config 2");

        final ToolbarTab homeTab1 = getTab(config1, TabType.HOME);
        final ToolbarTab homeTab2 = getTab(config2, TabType.HOME);
        final ToolbarTab defaultHomeTab = getTab(defaultConfiguration, TabType.HOME);
        final ToolbarTab refHomeTab = configurationManager.getReferenceTab(TabType.HOME.requireSingularId());
        assertThat(refHomeTab).isNotNull();

        // Adding a button to a custom config shouldn't affect the other custom config or the default config
        final ToolbarButton button = configurationManager.getReferenceButton(ButtonType.ABOUT.requireSingularId());
        assertThat(button).isNotNull();
        homeTab1.addComponent(button);
        assertThat(homeTab2.getComponents()).extracting(AToolbarComponent::getId).doesNotContain(button.getId());
        assertThat(defaultHomeTab.getComponents()).extracting(AToolbarComponent::getId).doesNotContain(button.getId());
        assertThat(refHomeTab.getComponents()).extracting(AToolbarComponent::getId).doesNotContain(button.getId());

        // Adding a button to the reference tab should affect all other configs
        ToolbarButton button2 = ToolbarTestUtils.createPluginButton(testDevicePlugin, "My button 2");
        refHomeTab.addComponent(button2);
        assertThat(homeTab2.getComponents()).extracting(AToolbarComponent::getId).contains(button2.getId());
        assertThat(defaultHomeTab.getComponents()).extracting(AToolbarComponent::getId).contains(button2.getId());
        assertThat(refHomeTab.getComponents()).extracting(AToolbarComponent::getId).contains(button2.getId());

        // ... and the button should be added as a reference button
        assertThat(configurationManager.getReferenceButton(button2.getId())).isEqualTo(button2);
    }

    /**
     * @since 3.4
     */
    @Test
    public void testHandleChangeEvent_componentRemoved()
    {
        // Create two custom configs (which will have the home tab added automatically)
        final ToolbarConfiguration config1 = createAndAddConfiguration("Custom config 1");
        final ToolbarConfiguration config2 = createAndAddConfiguration("Custom config 2");

        final ToolbarTab homeTab1 = getTab(config1, TabType.HOME);
        final ToolbarTab homeTab2 = getTab(config2, TabType.HOME);
        final ToolbarTab defaultHomeTab = getTab(defaultConfiguration, TabType.HOME);
        final ToolbarTab refHomeTab = configurationManager.getReferenceTab(TabType.HOME.requireSingularId());
        assertThat(refHomeTab).isNotNull();

        // Add a plugin button to the reference home tab so it can be removed later
        ToolbarButton pluginButton = ToolbarTestUtils.createPluginButton(testDevicePlugin, "My button 2");
        refHomeTab.addComponent(pluginButton);

        // Removing a button from a custom config shouldn't affect the other custom config or the default config
        final ToolbarButton button = configurationManager.getReferenceButton(ButtonType.NAVIGATION_LOCK.requireSingularId());
        assertThat(button).isNotNull();
        homeTab1.removeComponent(button);
        assertThat(homeTab2.getComponents()).extracting(AToolbarComponent::getId).contains(button.getId());
        assertThat(defaultHomeTab.getComponents()).extracting(AToolbarComponent::getId).contains(button.getId());
        assertThat(refHomeTab.getComponents()).extracting(AToolbarComponent::getId).contains(button.getId());

        // Removing a button from the reference tab should affect all other configs
        refHomeTab.removeComponent(pluginButton);
        final UUID pluginButtonId = pluginButton.getId();
        assertThat(homeTab2.getComponents()).filteredOn(c -> c.getId().equals(pluginButtonId))
                .extracting(IToolbarComponent::isValid).containsOnly(false);
        assertThat(defaultHomeTab.getComponents()).extracting(AToolbarComponent::getId).doesNotContain(pluginButtonId);
        assertThat(refHomeTab.getComponents()).extracting(AToolbarComponent::getId).doesNotContain(pluginButtonId);

        // ... and if it is a plugin button that isn't present anywhere else, it should be removed as a reference button
        assertThat(configurationManager.getReferenceButton(pluginButtonId)).isNull();
    }

    /**
     * @since 3.4
     */
    @Test
    public void testHandleChangeEvent_buttonImageChanged()
    {
        // Create two custom configs (which will have the home tab with the overlays button added automatically)
        final ToolbarConfiguration config1 = createAndAddConfiguration("Custom config 1");
        final ToolbarConfiguration config2 = createAndAddConfiguration("Custom config 2");

        final ToolbarButton overlaysButton1 = getButton(config1, ButtonType.OVERLAYS);
        final ToolbarButton overlaysButton2 = getButton(config2, ButtonType.OVERLAYS);
        final ToolbarButton defaultOverlaysButton = getButton(defaultConfiguration, ButtonType.OVERLAYS);
        final ToolbarButton refOverlaysButton = configurationManager.getReferenceButton(ButtonType.OVERLAYS.requireSingularId());
        assertThat(refOverlaysButton).isNotNull();

        // Changing the button image in a custom config shouldn't affect the other custom config or the default config
        final BufferedImage defaultImage = ImageUtils.getDefaultImage();
        final BufferedImage overlaysImage = refOverlaysButton.getImage();
        overlaysButton1.setImage(defaultImage);
        assertThat(overlaysButton2.getImage()).isEqualTo(overlaysImage);
        assertThat(defaultOverlaysButton.getImage()).isEqualTo(overlaysImage);
        assertThat(refOverlaysButton.getImage()).isEqualTo(overlaysImage);

        // Changing the image of the reference button should affect all other configs
        refOverlaysButton.setImage(defaultImage);
        assertThat(overlaysButton1.getImage()).isEqualTo(defaultImage);
        assertThat(overlaysButton2.getImage()).isEqualTo(defaultImage);
        assertThat(defaultOverlaysButton.getImage()).isEqualTo(defaultImage);
    }

    /**
     * Gets the button with the specified singular type from the provided configuration.
     *
     * @param configuration The configuration to get the button from
     * @param buttonType    The type of button to get (must be singular)
     * @return The button of the specified type for the provided configuration
     * @since 3.4
     */
    private ToolbarButton getButton(ToolbarConfiguration configuration, ButtonType buttonType)
    {
        assertThat(buttonType.isSingular()).isTrue();
        final ToolbarButton button = configuration.getTabs().stream().flatMap(t -> t.getComponents().stream())
                .filter(c -> c instanceof ToolbarButton)
                .map(ToolbarButton.class::cast)
                .filter(c -> c.getType() == buttonType).findFirst().orElse(null);
        assertThat(button).isNotNull();
        return button;
    }

    /**
     * @since 3.4
     */
    @Test
    public void testHandleChangeEvent_buttonStateChanged()
    {
        // Create two custom configs (which will have the home tab with the overlays button added automatically)
        final ToolbarConfiguration config1 = createAndAddConfiguration("Custom config 1");
        final ToolbarConfiguration config2 = createAndAddConfiguration("Custom config 2");

        final ToolbarButton overlaysButton1 = getButton(config1, ButtonType.OVERLAYS);
        final ToolbarButton overlaysButton2 = getButton(config2, ButtonType.OVERLAYS);
        final ToolbarButton defaultOverlaysButton = getButton(defaultConfiguration, ButtonType.OVERLAYS);
        final ToolbarButton refOverlaysButton = configurationManager.getReferenceButton(ButtonType.OVERLAYS.requireSingularId());
        assertThat(refOverlaysButton).isNotNull();

        // Changing the button enabled state in a custom config shouldn't affect the other custom config or the default config
        overlaysButton1.setEnabled(false);
        assertThat(overlaysButton2.isEnabled()).isTrue();
        assertThat(defaultOverlaysButton.isEnabled()).isTrue();
        assertThat(refOverlaysButton.isEnabled()).isTrue();

        // Changing the image of the reference button should affect all other configs
        refOverlaysButton.setEnabled(false);
        assertThat(overlaysButton1.isEnabled()).isFalse();
        assertThat(overlaysButton2.isEnabled()).isFalse();
        assertThat(defaultOverlaysButton.isEnabled()).isFalse();
    }

    /**
     * @since 3.4
     */
    @Test
    public void testHandleChangeEvent_buttonTextChanged()
    {
        // Create two custom configs (which will have the home tab with the overlays button added automatically)
        final ToolbarConfiguration config1 = createAndAddConfiguration("Custom config 1");
        final ToolbarConfiguration config2 = createAndAddConfiguration("Custom config 2");

        final ToolbarButton overlaysButton1 = getButton(config1, ButtonType.OVERLAYS);
        final ToolbarButton overlaysButton2 = getButton(config2, ButtonType.OVERLAYS);
        final ToolbarButton defaultOverlaysButton = getButton(defaultConfiguration, ButtonType.OVERLAYS);
        final ToolbarButton refOverlaysButton = configurationManager.getReferenceButton(ButtonType.OVERLAYS.requireSingularId());
        assertThat(refOverlaysButton).isNotNull();

        // Changing the button text in a custom config shouldn't affect the other custom config or the default config
        overlaysButton1.setDisplayText("Overlays 2");
        assertThat(overlaysButton2.getDisplayText()).isEqualTo(ButtonType.OVERLAYS.getDisplayText());
        assertThat(defaultOverlaysButton.getDisplayText()).isEqualTo(ButtonType.OVERLAYS.getDisplayText());
        assertThat(refOverlaysButton.getDisplayText()).isEqualTo(ButtonType.OVERLAYS.getDisplayText());

        // Changing the text of the reference button should affect all other configs
        refOverlaysButton.setDisplayText("Overlays 3");
        assertThat(overlaysButton1.getDisplayText()).isEqualTo("Overlays 3");
        assertThat(overlaysButton2.getDisplayText()).isEqualTo("Overlays 3");
        assertThat(defaultOverlaysButton.getDisplayText()).isEqualTo("Overlays 3");
    }

    /**
     * @since 3.4
     */
    @Test
    public void testHandleChangeEvent_buttonTooltipChanged()
    {
        // Create two custom configs (which will have the home tab with the overlays button added automatically)
        final ToolbarConfiguration config1 = createAndAddConfiguration("Custom config 1");
        final ToolbarConfiguration config2 = createAndAddConfiguration("Custom config 2");

        final ToolbarButton overlaysButton1 = getButton(config1, ButtonType.OVERLAYS);
        final ToolbarButton overlaysButton2 = getButton(config2, ButtonType.OVERLAYS);
        final ToolbarButton defaultOverlaysButton = getButton(defaultConfiguration, ButtonType.OVERLAYS);
        final ToolbarButton refOverlaysButton = configurationManager.getReferenceButton(ButtonType.OVERLAYS.requireSingularId());
        assertThat(refOverlaysButton).isNotNull();

        // Changing the button tooltip in a custom config shouldn't affect the other custom config or the default config
        final String overlaysTooltip = refOverlaysButton.getTooltipText();
        overlaysButton1.setTooltipText("New tooltip");
        assertThat(overlaysButton2.getTooltipText()).isEqualTo(overlaysTooltip);
        assertThat(defaultOverlaysButton.getTooltipText()).isEqualTo(overlaysTooltip);
        assertThat(refOverlaysButton.getTooltipText()).isEqualTo(overlaysTooltip);

        // Changing the tooltip of the reference button should affect all other configs
        refOverlaysButton.setTooltipText("New tooltip 2");
        assertThat(overlaysButton1.getTooltipText()).isEqualTo("New tooltip 2");
        assertThat(overlaysButton2.getTooltipText()).isEqualTo("New tooltip 2");
        assertThat(defaultOverlaysButton.getTooltipText()).isEqualTo("New tooltip 2");
    }

    /**
     * Test reading in a config with deletable set to false maintains that property after import.  Also verify
     * the default config is not deletable.
     *
     * @since 5.5.1
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void importConfiguration_andDefault_notDeletable()
    {
        final URL url = getClass().getClassLoader().getResource("toolbar/testToolbarConfigNotDeletable.xml");
        final ToolbarConfiguration config = configurationManager.importConfiguration(url.getFile());
        assertThat(config).isNotNull();
        assertThat(config.isDeletable()).isFalse();

        final ToolbarConfiguration defaultConfig = configurationManager.getDefaultConfiguration();
        assertThat(defaultConfig).isNotNull();
        assertThat(defaultConfig.isDeletable()).isFalse();
    }

    /**
     * Test reading in a config with deletable set to true maintains that property after import.
     *
     * @since 5.5.1
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void importConfiguration_deletable()
    {
        final URL url = getClass().getClassLoader().getResource("toolbar/testToolbarConfigDeletable.xml");
        final ToolbarConfiguration config = configurationManager.importConfiguration(url.getFile());
        assertThat(config).isNotNull();
        assertThat(config.isDeletable()).isTrue();
    }

    /**
     * Create a Toolbar config with a tab, button and set to be non-deletable.  Next, export then re-import and
     * verify the properties are the same.
     *
     * @since 5.5.1
     */
    @Test
    public void export_thenImport_verifyProperties() throws IOException
    {
        final ToolbarConfiguration exportConfig = createAndAddConfiguration("Round-trip");
        exportConfig.setDeletable(false);
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test Device");
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "My Test button");
        tab.addComponent(button, true);
        exportConfig.addTab(tab, true);
        File exportFile = File.createTempFile("test-toolbar-round-trip", ".xml");
        exportFile.deleteOnExit();
        configurationManager.exportConfiguration(exportFile.getPath(), exportConfig);

        final ToolbarConfiguration importConfig = configurationManager.importConfiguration(exportFile.getPath());
        assertThat(importConfig).isNotNull();
        assertThat(importConfig.getName().equals("Round-trip")).isTrue();
        assertThat(importConfig.isDeletable()).isFalse();
        assertThat(importConfig.isDefault()).isFalse();

        List<String> tabNames = importConfig.getTabs().stream().map(ToolbarTab::getDisplayText).toList();
        assertThat(tabNames.contains("Test Device")).isTrue();

        Optional<ToolbarTab> tabOptional = importConfig.getTabs().stream().filter(toolbarTab -> toolbarTab.getDisplayText().equals("Test Device")).findFirst();
        assertThat(tabOptional).isPresent();
        ToolbarTab importTab = tabOptional.get();
        assertThat(importTab.getComponents()).isNotEmpty();
        AToolbarComponent component = importTab.getComponents().get(0);
        assertThat(component.getPluginUniqueKey()).isNotNull();
        assertThat(component instanceof ToolbarButton).isTrue();
        assertThat(((ToolbarButton) component).getDisplayText().equals("My Test button")).isTrue();
    }
}
