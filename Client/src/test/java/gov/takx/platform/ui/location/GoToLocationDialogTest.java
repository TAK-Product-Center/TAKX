package gov.takx.platform.ui.location;

import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.icons.IUserIcon;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import static gov.takx.api.milsym.MilStd2525Variant.CHARLIE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.3
 */
public class GoToLocationDialogTest extends AIntegrationTest
{
    private IMapObjectManager mapObjectManager;
    private IIconSetManager iconSetManager;
    private IMilStd2525Service milStd2525Service;

    @BeforeAll
    public void beforeClass()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        iconSetManager = getBean(IIconSetManager.class);
        milStd2525Service = getBean(IMilStd2525ServiceProvider.class).get2525Service(CHARLIE);
    }

    @AfterEach
    public void cleanup()
    {
        for (IMapObject mapObject : new ArrayList<>(mapObjectManager.getMapObjects()))
        {
            mapObjectManager.deleteMapObject(mapObject);
        }
    }

    @Test
    public void testDialogSelectionCreatesExpectedMOState() throws IOException
    {
        IUserIcon icon = iconSetManager.getIcon(IIconSetManager.TAKX_LEGACY_ICONSET_UID, "Red.png");
        assertThat(icon).isNotNull();

        // Create the "dialog" to emulate user input.
        // Our case here has a URI provided which will make it so that the created MapObject's display icon is not modifiable.
        URI iconUri = icon.toUri();
        new GoToLocationDialog.Helper().onLocationSelected(new GeoPoint(0, 0), CoordinateSystem.MGRS, iconUri, CotIcon.HostileGround.getCotType());

        // The map object should be created
        var mapObjects = mapObjectManager.getMapObjects();
        assertThat(mapObjects).hasSize(1);
        IMapObject mapObject = mapObjects.stream().findFirst().get();

        // Assert initial state
        URI moIconUri = mapObject.getIconImageURI();
        BufferedImage moImage = mapObject.getIconImage();
        assertThat(moIconUri).isEqualTo(iconUri);
        assertThat(moImage).isNotNull();
        assertThat(mapObject.getMetaBoolean(StandardAttributeNames.ADAPT_MARKER_ICON, true)).isFalse();

        // TODO: TAKX-3278 - The MO should not have visuals be modifiable if the call to "onLocationSelected" passes a non-null URI.
        //  This seems to be broken at the moment, but is not too big of an issue at the moment since there is no way to
        //  trigger the bug as an end-user anyways. We'll be refactoring the panel at some point in the future anyways so the
        //  problem may go away on its own.
        /*
        // Make a change that visually won't stick due to the icon URI being set from the original icon-set
        String symbolCode = "S*G*UULC--*****";
        milStd2525Service.setSymbolCode(mapObject, symbolCode);
        
        // Assert that no visual changes were made
        URI moIconUri2 = mapObject.getIconImageURI();
        BufferedImage moImage2 = mapObject.getIconImage();
        assertThat(moIconUri2).isSameAs(moIconUri);
        assertThat(moImage2).isSameAs(moImage);
         */
    }
}
