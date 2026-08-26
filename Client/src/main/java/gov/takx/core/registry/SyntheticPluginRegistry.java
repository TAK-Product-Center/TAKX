package gov.takx.core.registry;

import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.synthetic.ISyntheticPlugin;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.registry.ISyntheticPluginRegistry;
import gov.takx.api.shapes.ShapeType;
import gov.takx.core.mapobject.PolygonPluginController;
import gov.takx.core.plugin.FilePluginController;
import gov.takx.module.PluginDefinitionFactory;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static gov.takx.api.mapobject.MapObjectOptions.STANDARD_RESOURCE_OPTIONS;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Registry for synthetic plugins.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class SyntheticPluginRegistry extends APluginRegistry<ISyntheticPlugin, ISyntheticPluginDefinition> implements ISyntheticPluginRegistry
{
    // Map of MO type to group
    public static final Map<String, String> COMPOUND_MO_TYPES_AND_GROUPS = Map.of(
            IMultiPolylineMapObject.MULTI_POLYLINE_TYPE, MapObjectGroupNames.MAP_TOOLS);

    // Map of MO type to the icon name in ImageUtils.DEFAULT_ICON_PATH
    private static final Map<String, String> TYPE_TO_ICON_NAME = new HashMap<>();

    // Map tools to exclude when creating synthetic plugins.
    //  - Polygon since it needs a special controller
    //  - both "freehand" types since they have the same underlying type as the non-freehand versions
    //  - both annotation types since they don't have a ShapeType
    private static final Set<MapToolType> EXCLUDED_MAP_TOOLS = Set.of(
            MapToolType.NONE, MapToolType.DRAWING_POLYGON, MapToolType.DRAWING_FREEHAND_POLYGON, MapToolType.DRAWING_FREEHAND_PATH,
            MapToolType.DRAWING_ANNOTATION, MapToolType.DRAWING_FLAT_ANNOTATION);

    static
    {
        TYPE_TO_ICON_NAME.put(IMultiPolylineMapObject.MULTI_POLYLINE_TYPE, "multi_polyline.png");
        TYPE_TO_ICON_NAME.put(IAnnotationMapObject.ANNOTATION_TYPE, "text_callout.png");
        TYPE_TO_ICON_NAME.put(IFlatAnnotationMapObject.FLAT_ANNOTATION_TYPE, "text_box.png");

        Stream.of(MapToolType.values())
                .map(MapToolType::getShapeType)
                .filter(shapeType -> shapeType != ShapeType.NONE)
                .forEach(shapeType -> TYPE_TO_ICON_NAME.put(shapeType.getShapeType(), shapeType.getIconName()));
    }

    @Inject
    private PluginDefinitionFactory pluginDefinitionFactory;

    /**
     * Construct the Device plugin registry.
     */
    public SyntheticPluginRegistry()
    {
        super(PluginCategory.SyntheticPlugin, false);
    }

    @Override
    public void generateSyntheticPlugins()
    {
        generateMapObjectPlugins();
        processMapTools();
    }

    /**
     * Generate the synthetic plugins backing Map Object types that need custom options and/or controllers.
     *
     * @since 3.0
     */
    private void generateMapObjectPlugins()
    {
        // Create a synthetic plugin for our "File" Map Object type
        addPlugin(pluginDefinitionFactory.syntheticDefinitionFor(KeyUtils.FILE_FAMILY, IFileMapObject.FILE_TYPE,
                MapObjectGroupNames.FILES_GROUP, null, STANDARD_RESOURCE_OPTIONS,
                FilePluginController::new));

        // Create synthetic plugins for the different compound map object types
        COMPOUND_MO_TYPES_AND_GROUPS.forEach(
                (type, group) -> addPlugin(pluginDefinitionFactory.syntheticDefinitionFor(KeyUtils.COMPOUND_FAMILY, type,
                        group, getIconUri(type), STANDARD_RESOURCE_OPTIONS, null)));
    }

    /**
     * Create synthetic plugin definitions for all the map tools.
     */
    private void processMapTools()
    {
        Stream.of(MapToolType.values())
                .filter(mtt -> !EXCLUDED_MAP_TOOLS.contains(mtt))
                .map(mtt -> makeMapToolDefinition(mtt.getShapeTypeName(), null))
                .forEach(this::addPlugin);

        // Add Polygon with its custom device controller
        addPlugin(makeMapToolDefinition(ShapeType.POLYGON.getShapeType(), PolygonPluginController::new));

        // And the annotation type
        addPlugin(makeMapToolDefinition(IAnnotationMapObject.ANNOTATION_TYPE, null));

        // And the flat annotation type
        addPlugin(makeMapToolDefinition(IFlatAnnotationMapObject.FLAT_ANNOTATION_TYPE, null));
    }

    /**
     * Create a synthetic plugin definition in the {@link KeyUtils#TOOL_FAMILY} using standard resource map options.
     * The resource name is constructed from the given type by converting it to lower case and replacing spaces with
     * underscores.
     *
     * @param type                Tool type
     * @param controllerGenerator Function to generate a device plugin controller, may be {@code null}
     * @return plugin definition
     * @see MapObjectOptions#STANDARD_RESOURCE_OPTIONS
     * @since 2.29
     */
    private ISyntheticPluginDefinition makeMapToolDefinition(String type, Function<String, IDevicePluginController> controllerGenerator)
    {
        return pluginDefinitionFactory.syntheticDefinitionFor(KeyUtils.TOOL_FAMILY, type, MapObjectGroupNames.MAP_TOOLS,
                getIconUri(type), STANDARD_RESOURCE_OPTIONS, controllerGenerator);
    }

    /**
     * Get icon from {@link gov.takx.platform.ui.ResourceDirectory#PLATFORM_ICONS} associated with the provided type string.
     *
     * @param type The map object type for which to get the icon
     * @return The URI of the icon associated with the provided type.
     * @since 3.0 (renamed for more broad use)
     */
    private URI getIconUri(String type)
    {
        // Note: This used to be calculated dynamically from the type name. However, that led to two problems:
        //  1. When searching for usages of one of the icons used for a synthetic MO, there wouldn't be any explicit
        //     references, and it would look like it wasn't being used anywhere.
        //  2. When a new map object type is added, it wasn't obvious that an icon whose name matched the type must
        //     be added to the default icon path in order for the MO to be represented correctly in the explorer. In
        //     fact, this led to missing icons when adding the route type because looking at another MO for reference
        //     didn't make it obvious that this type to icon name association was happening here.
        // By creating an explicit map to associate the MO type with the icon name, it will be easier to see where the
        // icon is used and if any MO types are added in the future, it should be clear that a new icon is needed.
        final String resourceName = TYPE_TO_ICON_NAME.get(type);
        return resourceName == null ? null : UIResources.getUri(PLATFORM_ICONS, resourceName);
    }
}
