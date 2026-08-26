package gov.takx.core.shapes;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.annotations.ForPlugin;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDelegate;
import gov.takx.api.plugin.io.IImportExportPluginDelegate;
import gov.takx.api.shapes.IShapeService;
import gov.takx.core.ioservice.ImportExportService;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.app.TestAppPlugin;
import gov.takx.test.mock.plugin.app.TestAppPlugin2;
import gov.takx.test.mock.plugin.device.TestDevicePluginWithOptions;
import gov.takx.test.mock.plugin.io.TestCancellableImportPlugin;
import gov.takx.test.mock.plugin.io.TestCancellableImportWizard;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link ShapeServiceFactory}.
 *
 * @since 2.38
 */
public class ShapeServiceFactoryTest extends AIntegrationTest
{
    private ShapeServiceFactory shapeServiceFactory;

    private final List<IAppPlugin> startedApps = new ArrayList<>();
    private IMapObjectManager mapObjectManager;

    public ShapeServiceFactoryTest()
    {
        setPluginsToRegister(TestAppPlugin.class, TestAppPlugin2.class, TestDevicePluginWithOptions.class,
                TestCancellableImportPlugin.class);
    }

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        shapeServiceFactory = getBean(ShapeServiceFactory.class);
    }

    @Override
    protected void onTeardown()
    {
        startedApps.forEach(this::stopApp);
        mapObjectManager.getMapObjectControllers().forEach(IMapObjectController::delete);
    }

    /**
     * Verify multiple calls to get the shape service for the TAKX core return the same instance.
     */
    @Test
    public void testGetOrCreateRaptorCoreShapeService()
    {
        ShapeService shapeService1 = shapeServiceFactory.getOrCreateCoreShapeService();
        assertThat(shapeService1).isNotNull();

        ShapeService shapeService2 = shapeServiceFactory.getOrCreateCoreShapeService();
        assertThat(shapeService2).isNotNull().isSameAs(shapeService1);
    }

    /**
     * Verify multiple calls to get the shape service for the same app delegate return the same instance, and calls for
     * a different instance should return a unique shape service.
     */
    @Test
    public void testGetOrCreateShapeService_app() throws Exception
    {
        TestAppPlugin appPlugin1 = startApp(TestAppPlugin.FAMILY, TestAppPlugin.TYPE, TestAppPlugin.VERSION);
        startedApps.add(appPlugin1);
        IAppPluginDelegate delegate1 = appPlugin1.getDelegate();
        IShapeService shapeServiceFromDelegate = delegate1.getShapeService();
        assertThat(shapeServiceFromDelegate).isNotNull();

        // If we request the shape service from the factory again with the same delegate, it should return the same instance
        ShapeService shapeServiceFromFactory = shapeServiceFactory.getOrCreateShapeService(delegate1);
        assertThat(shapeServiceFromFactory).isNotNull().isSameAs(shapeServiceFromDelegate);

        TestAppPlugin2 appPlugin2 = startApp(TestAppPlugin2.FAMILY, TestAppPlugin2.TYPE, TestAppPlugin2.VERSION);
        startedApps.add(appPlugin2);
        IAppPluginDelegate delegate2 = appPlugin2.getDelegate();
        IShapeService shapeService2 = delegate2.getShapeService();
        assertThat(shapeService2).isNotNull();

        // The shape service for a different app should be different
        assertThat(shapeService2).isNotSameAs(shapeServiceFromDelegate);
    }

    /**
     * Verify multiple calls to get the shape service for the same map object controller return the same instance, and
     * calls for a different instance should return a unique shape service.
     */
    @Test
    public void testGetOrCreateShapeService_device()
    {
        final MapObjectSpec spec1 = MapObjectSpec.builder(TestDevicePluginWithOptions.FAMILY, TestDevicePluginWithOptions.TYPE, "1")
                .location(new RaptorLocation(1, 1)).build();
        IMapObjectController mapObjectController1 = mapObjectManager.getOrCreateMapObject(spec1).getController();
        IShapeService shapeServiceFromController = mapObjectController1.getShapeService();
        assertThat(shapeServiceFromController).isNotNull();

        // If we request the shape service from the factory again with the same controller, it should return the same instance
        ShapeService shapeServiceFromFactory = shapeServiceFactory.getOrCreateShapeService(mapObjectController1);
        assertThat(shapeServiceFromFactory).isNotNull().isSameAs(shapeServiceFromController);

        final MapObjectSpec spec2 = MapObjectSpec.builder(TestDevicePluginWithOptions.FAMILY, TestDevicePluginWithOptions.TYPE, "2")
                .location(new RaptorLocation(1, 1)).build();
        IMapObjectController mapObjectController2 = mapObjectManager.getOrCreateMapObject(spec2).getController();
        IShapeService shapeService2 = mapObjectController2.getShapeService();
        assertThat(shapeService2).isNotNull();

        // The shape service for a different map object should be different
        assertThat(shapeService2).isNotSameAs(shapeServiceFromController);
    }

    /**
     * Verify multiple calls to get the shape service for the same import/export delegate return the same instance.
     */
    @Test
    public void testGetOrCreateShapeService_importExport()
    {
        ImportExportService importExportService = getBean(ImportExportService.class);
        TestCancellableImportWizard importer = (TestCancellableImportWizard) importExportService.createImporter(
                TestCancellableImportPlugin.FAMILY, TestCancellableImportPlugin.TYPE);
        IImportExportPluginDelegate delegate = importer.getDelegate();
        IShapeService shapeServiceFromDelegate = delegate.getShapeService();
        assertThat(shapeServiceFromDelegate).isNotNull();

        // If we request the shape service from the factory again with the same delegate, it should return the same instance
        ShapeService shapeServiceFromFactory = shapeServiceFactory.getOrCreateShapeService(delegate);
        assertThat(shapeServiceFromFactory).isNotNull().isSameAs(shapeServiceFromDelegate);
    }

    /**
     * Verify that disposing a service removes it from management by the factory.
     */
    @Test
    public void testDisposeShapeService()
    {
        ShapeService shapeService1 = shapeServiceFactory.getOrCreateCoreShapeService();
        assertThat(shapeService1).isNotNull();

        shapeServiceFactory.disposeShapeService(shapeService1);

        ShapeService shapeService2 = shapeServiceFactory.getOrCreateCoreShapeService();
        assertThat(shapeService2).isNotNull();

        // Now the shape services should be different since the original was disposed.
        assertThat(shapeService2).isNotSameAs(shapeService1);
    }

    /**
     * Verify shape services are injected correctly and that they match the instances in the associated plugins.
     */
    @Test
    @SuppressLogging(ShapeServiceFactory.class) // Don't log expected error for injecting device shape service
    public void testProduceShapeService() throws Exception
    {
        TestPseudoBean testPseudoBean = new TestPseudoBean();

        TestAppPlugin appPlugin = startApp(TestAppPlugin.FAMILY, TestAppPlugin.TYPE, TestAppPlugin.VERSION);
        startedApps.add(appPlugin);
        IShapeService shapeServiceFromApp = appPlugin.getDelegate().getShapeService();
        assertThat(shapeServiceFromApp).isNotNull();

        // The test class should have the same delegate as the app
        IShapeService injectedAppShapeService = testPseudoBean.getAppShapeService();
        assertThat(injectedAppShapeService).isNotNull().isSameAs(shapeServiceFromApp);

        // The device shape service should be null (since injecting shape services for device plugins isn't supported)
        IShapeService deviceShapeService = testPseudoBean.getDeviceShapeService();
        assertThat(deviceShapeService).isNull();

        ImportExportService importExportService = getBean(ImportExportService.class);
        TestCancellableImportWizard importer = (TestCancellableImportWizard) importExportService.createImporter(
                TestCancellableImportPlugin.FAMILY, TestCancellableImportPlugin.TYPE);
        IShapeService shapeServiceFromImporter = importer.getDelegate().getShapeService();
        assertThat(shapeServiceFromImporter).isNotNull();

        // The test class should have the same delegate as the feature
        IShapeService injectedImportExportShapeService = testPseudoBean.getImportExportShapeService();
        assertThat(injectedImportExportShapeService).isNotNull().isSameAs(shapeServiceFromImporter);
    }

    /**
     * PseudoBean class for testing injection of shape services.
     */
    @PseudoBean
    private static class TestPseudoBean
    {
        @Inject
        @ForPlugin(family = TestAppPlugin.FAMILY, type = TestAppPlugin.TYPE,
                category = PluginCategory.AppPlugin)
        private IShapeService appShapeService;

        @Inject
        @ForPlugin(family = TestDevicePluginWithOptions.FAMILY, type = TestDevicePluginWithOptions.TYPE,
                category = PluginCategory.DevicePlugin)
        private IShapeService deviceShapeService;

        @Inject
        @ForPlugin(family = TestCancellableImportPlugin.FAMILY, type = TestCancellableImportPlugin.TYPE,
                category = PluginCategory.ImportExportPlugin)
        private IShapeService importExportShapeService;

        public IShapeService getAppShapeService()
        {
            return appShapeService;
        }

        public IShapeService getDeviceShapeService()
        {
            return deviceShapeService;
        }

        public IShapeService getImportExportShapeService()
        {
            return importExportShapeService;
        }
    }
}
