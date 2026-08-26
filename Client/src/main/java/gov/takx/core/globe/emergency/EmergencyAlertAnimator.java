package gov.takx.core.globe.emergency;

import gov.takx.api.emergency.event.EmergencyAlertAdded;
import gov.takx.api.emergency.event.EmergencyAlertRemoved;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeMapObjectManager;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides animation of a marker background to indicate an Emergency Alert.
 * 
 * @since 5.7
 */
@ApplicationScoped
public class EmergencyAlertAnimator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final double SCALE_FACTOR = 0.25; // The factor that the pulsing images scale is increased by each frame
    private static final int NUM_FRAMES = 10; // The number of frames it takes to cycle through a pulsing image
    private static final int FRAME_RATE = 150; // millis between animation frames

    private static final BufferedImage BASE_IMAGE = UIResources.getImage(ResourceDirectory.PLATFORM_ICONS, "map_ping_flash.png");

    private final IExtendedGlobeManager globeManager;
    private final IGlobeMapObjectManager globeMapObjectManager;

    private final Timer animationTimer = new Timer(FRAME_RATE, new AnimationHandler());
    private final Map<String, IGlobeMarkerMapObject> emergencyMarkers = new ConcurrentHashMap<>();

    @Inject
    EmergencyAlertAnimator(IExtendedGlobeManager globeManager)
    {
        this.globeManager = globeManager;
        globeMapObjectManager = globeManager.getPrimaryGlobeDelegate().getGlobeMapObjectManager();
    }

    /**
     * When an alert is added, add the marker to our tracked set, and start our animation timer.
     */
    private void onAlertAdded(@Observes EmergencyAlertAdded event)
    {
        final String key = event.alert().mapItem().getKey();
        final var globeMapObject = (IGlobeMarkerMapObject) globeMapObjectManager.getGlobeMapObject(key);

        if (globeMapObject != null)
        {
            emergencyMarkers.put(key, globeMapObject);
            globeMapObject.setBackgroundImage(BASE_IMAGE);
            globeMapObject.setBackgroundScale(1);

            animationTimer.start();
        } else
        {
            logger.warn("No Globe Map Object found for emergency alert on {}", key);
        }
    }

    /**
     * When an alert is removed, remove the marker from our tracked set, and if that is now empty, stop our animation timer.
     */
    private void onAlertRemoved(@Observes EmergencyAlertRemoved event)
    {
        emergencyMarkers.remove(event.alert().mapItem().getKey());
        if (emergencyMarkers.isEmpty()) animationTimer.stop();
    }

    /**
     * Wrap up operations when the project is closing.
     */
    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        animationTimer.stop();
        emergencyMarkers.clear();
    }

    /**
     * Action listener to animate the scale of the emergency markers background image.
     */
    private class AnimationHandler implements ActionListener
    {
        int frameIndex;

        @Override
        public void actionPerformed(ActionEvent e)
        {
            final double scale = 1 + frameIndex * SCALE_FACTOR;
            for (var marker : emergencyMarkers.values()) marker.setBackgroundScale(scale);

            frameIndex = (frameIndex + 1) % NUM_FRAMES;
            globeManager.getAllGlobeDelegates().forEach(IGlobeDelegate::redraw);
        }
    }
}
