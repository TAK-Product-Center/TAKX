package gov.takx.core.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.PlaybackEvent;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Globe map object to represent a Map Object that is participating in playback.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobePlaybackIconMapObject extends GlobeIconMapObject
{
    @Inject
    private IPlaybackManager playbackManager;

    private ILocation location; // Tracks our location as playback occurs

    public GlobePlaybackIconMapObject(IMapObject mapObject)
    {
        super(mapObject);

        location = mapObject.getLocation();
    }

    @Override
    protected BufferedImage makeMapObjectImage(@Nonnull BufferedImage originalImage)
    {
        return makePlaybackIcon(originalImage);
    }

    private void onPlaybackEvent(@Observes PlaybackEvent playbackEvent)
    {
        if (!playbackManager.isLive()
                && (playbackEvent == PlaybackEvent.PlayPositionChanged || playbackEvent == PlaybackEvent.ScrubPositionChanged))
        {
            // Fire a property change event so the feature provider will know that our state has changed
            final var playbackLocation = playbackManager.getCurrentLocation(getMapObject());
            propertyChangeSupport.updateAndFire(IMapObject.LOCATION, location, playbackLocation, () -> location = playbackLocation);
        }
    }

    @Override
    @RemoveObserver
    public void peerRemoved()
    {
        super.peerRemoved();
    }

    /**
     * @return The location of the map object that we represent (according to the playback system)
     */
    @Nonnull
    @Override
    public ILocation getLocation()
    {
        return location;
    }

    /**
     * @return A copy of the original image at 70% opacity, and with a 2 pixel green border
     * @since 5.7
     */
    @Nonnull
    private static BufferedImage makePlaybackIcon(@Nonnull BufferedImage originalImage)
    {
        final var baseImage = ImageUtils.getSquareImage(originalImage, ImageUtils.DEFAULT_ICON_WIDTH);
        final var playbackImage = ImageUtils.createTransparentImage(baseImage, .7f);

        final var g2 = playbackImage.createGraphics();
        g2.setColor(Color.GREEN);
        g2.setStroke(new BasicStroke(2));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawRect(1, 1, playbackImage.getWidth() - 3, playbackImage.getHeight() - 3); // 2 pixel border
        g2.dispose();

        return playbackImage;
    }
}
