package gov.takx.core.view.gui.preferences;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.core.globe.tak.layer.FrameRateOverlay;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.takx.platform.preferences.PreferenceEntry.DEV_MODE_CONTINUOUS_RENDERING_ENABLED;
import static gov.takx.platform.preferences.PreferenceEntry.DEV_MODE_COT_MESSAGE_INSPECTION_ENABLED;
import static gov.takx.platform.preferences.PreferenceEntry.DEV_MODE_SETTINGS_ENABLED;

/**
 * Preference page for managing Developer Mode settings.
 * 
 * @since 5.4
 */
@PseudoBean
class DeveloperPreferencesPage extends APreferencesPage
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IRaptorLayerManager layerManager;

    DeveloperPreferencesPage()
    {
        super("Developer Preferences");
        addCheckboxEditor(DEV_MODE_SETTINGS_ENABLED);
        addCheckboxEditor(DEV_MODE_COT_MESSAGE_INSPECTION_ENABLED);
        addCheckboxEditor(DEV_MODE_CONTINUOUS_RENDERING_ENABLED, this::setContinuousRendering);
    }

    /**
     * Set the visibility of the Frame Rate Overlay layer based on {@code enabled}.
     */
    private void setContinuousRendering(boolean enabled)
    {
        final var raptorLayer = layerManager.getLayer(FrameRateOverlay.layerConfiguration.name());
        if (raptorLayer != null)
        {
            raptorLayer.setVisible(enabled);
        } else
        {
            logger.warn("Could not find '{}' layer", FrameRateOverlay.layerConfiguration.name());
        }
    }
}
