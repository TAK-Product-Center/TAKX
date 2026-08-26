package gov.takx.core.placemarkoverlay.gui;

import gov.tak.api.icons.IIconSetManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.ui.IconSelectionPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Panel to allow the user to select an icon to use for creating point placemarks.
 */
@PseudoBean
public class PlacemarkIconSelectionPanel extends IconSelectionPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_ICON_NAME = "Blue.png";

    @Inject
    private IIconSetManager iconSetManager;

    public PlacemarkIconSelectionPanel()
    {
        setBrowseButtonVisible(false);
        setGroupSelectionButtonVisible(true);
    }

    @PostConstruct
    private void postConstruct()
    {
        final var iconset = iconSetManager.getIconSetByUid(IIconSetManager.TAKX_LEGACY_ICONSET_UID);

        if (iconset != null)
        {
            final var imagesAndAddresses = getScaledImagesAndAddresses();
            final var defaultIconRef = new AtomicReference<ImageIcon>();

            iconset.getIcons().forEach(userIcon -> {
                final var imageIcon = new ImageIcon(iconSetManager.getIconImage(userIcon));

                if (DEFAULT_ICON_NAME.equals(userIcon.getFileName())) defaultIconRef.set(imageIcon);

                imagesAndAddresses.put(userIcon.toUri().toString(), imageIcon);
            });

            initIconList();

            final ImageIcon defaultIcon = defaultIconRef.get();
            if (defaultIcon != null) selectIcon(defaultIcon);
        } else
        {
            logger.error("Legacy icon set is missing: {}", IIconSetManager.TAKX_LEGACY_ICONSET_UID);
        }
    }

    @Nonnull
    @Override
    protected String extractGroupFromPath(@Nonnull String imagePath)
    {
        final var userIcon = iconSetManager.getIcon(URI.create(imagePath));
        return userIcon != null ? userIcon.getGroup() : "";
    }

    public ImageIcon getSelectedImage()
    {
        return selectedImage;
    }
}
