package gov.takx.test.mock.plugin.io;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.IImportExportPluginDelegate;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.api.plugin.io.IWizardResultListener;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.ui.IImportExportSwingUIProvider;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.core.ioservice.ImportExportPluginDelegate;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Polyline;
import gov.takx.dataservices.shapes.Square;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generic import wizard for use with {@link TestImportPlugin}.
 *
 * @since 3.4 (formerly an inner class in {@link TestImportPlugin})
 */
public class TestImportWizard implements IImportWizard, IImportExportSwingUIProvider
{
    private static final String SHAPE_NAME = TestImportPlugin.FAMILY;
    private final IShapeService shapeService;
    private IImportExportPluginDelegate delegate;
    private boolean generateShapes;
    private int shapeCount;

    public TestImportWizard()
    {
        delegate = new ImportExportPluginDelegate(TestImportPlugin.FAMILY, TestImportPlugin.TYPE);
        shapeService = delegate.getShapeService();
    }

    /**
     * Configures the wizard to generate random shapes as part of the mocked import.
     *
     * @param generateShapes Whether the importer should generate shapes
     * @param shapeCount Number of shapes to generate (ignored if generateShapes is false)
     */
    public void setGenerateShapes(boolean generateShapes, int shapeCount)
    {
        this.generateShapes = generateShapes;
        this.shapeCount = shapeCount;
    }

    @Override
    public ImportExportResult doImport(IWizardProperties props, IImportExportProgressMonitor progressMonitor) throws Exception
    {
        ImportExportResult result = new ImportExportResult();

        if (generateShapes)
        {
            for (int i = 0; i < shapeCount; i++)
            {
                IShape nextShape = getRandomShape(i);
                result.getAffectedObjects().put(nextShape.getKey(), nextShape);
            }
        }

        return result;
    }

    private IShape getRandomShape(int index) throws Exception
    {
        // Random value between 0 and 2
        int type = (int) (Math.random() * 3);

        return switch (type)
        {
            case 0 -> shapeService.createPolylineMapObject(new Polyline(SHAPE_NAME + "_line_" + index, getRandomLocations(3)));
            case 1 -> shapeService.createCircleMapObject(new Circle(SHAPE_NAME + "_circle_" + index, getRandomLocation(), Math.random() * 10));
            default -> shapeService.createSquareMapObject(new Square(SHAPE_NAME + "_square_" + index, getRandomLocation(), Math.random() * 10, Math.random() * 10));
        };
    }

    /**
     * Generate a list of random locations.
     *
     * @param count The number of random locations to generate
     * @return A list of random locations.
     */
    private List<ILocation> getRandomLocations(int count)
    {
        List<ILocation> locations = new ArrayList<>();

        for (int i = 0; i < count; i++)
        {
            locations.add(getRandomLocation());
        }

        return locations;
    }

    private ILocation getRandomLocation()
    {
        return new RaptorLocation((Math.random() * 90), Math.random() * 180);
    }

    @Override
    public List<Format> getFormats()
    {
        return null;
    }

    @Override
    public void setFormat(Format format)
    {
    }

    @Override
    public void setFormats(List<Format> formats)
    {
    }

    @Override
    public Map<String, Object> getPreconfigurationSettings()
    {
        return null;
    }

    @Nonnull
    @Override
    public Map<String, IWizardPanelDescriptor> createWizardPanels()
    {
        return Collections.emptyMap();
    }

    @Override
    public String getFirstWizardPanel()
    {
        return null;
    }

    @Override
    public void setDelegate(IImportExportPluginDelegate delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public boolean allowCancel()
    {
        return false;
    }

    @Override
    public void cancel()
    {
    }

    @Override
    public void addResultListener(IWizardResultListener listener)
    {
    }

    @Override
    public void preconfigureWizard(Map<String, Object> configuration)
    {
    }
}
