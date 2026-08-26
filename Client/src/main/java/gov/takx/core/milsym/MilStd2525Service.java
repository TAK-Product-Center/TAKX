package gov.takx.core.milsym;

import ArmyC2.C2SD.Utilities.SymbolUtilities;
import com.atakmap.map.layer.feature.Feature;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ISymbologyProvider2;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.symbology.Status;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525;
import gov.tak.platform.symbology.milstd2525.MilStd2525cSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525dSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilStd2525eSymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.milsym.Echelon;
import gov.takx.api.milsym.IExtendedMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.IMilSymIconBuilder;
import gov.takx.api.milsym.ISymbolChooser;
import gov.takx.api.milsym.MilStd2525Variant;
import gov.takx.api.milsym.Presence;
import gov.takx.api.milsym.SymbolWrapper;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.api.pointdropper.IPointDropperService;
import gov.takx.api.pointdropper.PointDropperConfiguration;
import gov.takx.api.shapes.IShapeDrawingService;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.plugin.synthetic.SyntheticPluginDefinition;
import gov.takx.dataservices.persist.AShapeMapObject;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation of {@link IExtendedMilStd2525Service} / {@link IMilStd2525Service}.
 * <p/>
 * <b>Note:</b> Intentionally not an injectable service;
 * this is accessed via {@link IMilStd2525ServiceProvider}
 *
 * @since 5.1
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "removal"})
@PseudoBean
public class MilStd2525Service implements IExtendedMilStd2525Service
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final EnumMap<ShapeType, gov.takx.api.shapes.ShapeType> SHAPE_TYPE_ALIAS_MAP = new EnumMap<>(ShapeType.class);
    private static final EnumMap<ShapeType, String> SHAPE_MESSAGE_MAP = new EnumMap<>(ShapeType.class);

    // Used for creating single point graphic markers
    private static final ISyntheticPluginDefinition PLUGIN_DEFINITION = new SyntheticPluginDefinition(
            KeyUtils.TOOL_FAMILY, CotIcon.TYPE, MapObjectGroupNames.DEVICES_GROUP,
            null, null, MapObjectOptions.STANDARD_RESOURCE_OPTIONS, null);

    private final MilSymIconCache symbolCache = new MilSymIconCache();
    private final IMilStd2525ServiceProvider milSymServiceProvider;
    private final ISymbologyProvider2 symProvider;

    private volatile Map<String, Modifier> allModifiers;
    private volatile Map<String, SymbolWrapper> allSymbols;

    @Inject
    private IUISpaceManager uiSpaceManager;
    @Inject
    private IExtendedGlobeManager globeManager;
    @Inject
    private IShapeDrawingService shapeDrawingService;
    @Inject
    private IMapObjectManager mapObjectManager;
    @Inject
    private IPointDropperService pointDropperService;

    static
    {
        SHAPE_TYPE_ALIAS_MAP.put(ShapeType.Point, gov.takx.api.shapes.ShapeType.NONE);
        SHAPE_TYPE_ALIAS_MAP.put(ShapeType.LineString, gov.takx.api.shapes.ShapeType.POLYLINE);
        SHAPE_TYPE_ALIAS_MAP.put(ShapeType.Polygon, gov.takx.api.shapes.ShapeType.POLYGON);
        SHAPE_TYPE_ALIAS_MAP.put(ShapeType.Rectangle, gov.takx.api.shapes.ShapeType.RECTANGLE);
        SHAPE_TYPE_ALIAS_MAP.put(ShapeType.Circle, gov.takx.api.shapes.ShapeType.CIRCLE);
        SHAPE_TYPE_ALIAS_MAP.put(ShapeType.Ellipse, gov.takx.api.shapes.ShapeType.ELLIPSE);
        SHAPE_MESSAGE_MAP.put(ShapeType.Point, "Click on the globe to place the symbol");
        SHAPE_MESSAGE_MAP.put(ShapeType.LineString, "Click to place points, double click to finish the shape");
        SHAPE_MESSAGE_MAP.put(ShapeType.Polygon, "Click to place points, double click to finish the shape");
        SHAPE_MESSAGE_MAP.put(ShapeType.Rectangle, "Click and Drag to draw a rectangle");
        SHAPE_MESSAGE_MAP.put(ShapeType.Circle, "Click and Drag to draw a circle");
        SHAPE_MESSAGE_MAP.put(ShapeType.Ellipse, "Click and Drag to draw an ellipse");
    }

    /**
     * Constructor for a specified symbology provider.  Registers the given provider with the kernel {@link SymbologyProvider}.
     *
     * @param milSymServiceProvider Provider for all 2525 service implementations.
     * @param symProvider Provider for 2525 graphics.
     */
    MilStd2525Service(@Nonnull IMilStd2525ServiceProvider milSymServiceProvider, @Nonnull ISymbologyProvider2 symProvider)
    {
        this.milSymServiceProvider = milSymServiceProvider;
        this.symProvider = symProvider;
        SymbologyProvider.registerProvider(symProvider, 0);
    }

    @Nonnull
    @Override
    public ISymbologyProvider getSymbologyProvider()
    {
        return symProvider;
    }

    @Nonnull
    @Override
    public MilSymIconCache getSymbolCache()
    {
        return symbolCache;
    }

    @Nonnull
    @Override
    public Affiliation getAffiliation(@Nonnull String symbolCode)
    {
        return symProvider.getAffiliation(symbolCode);
    }

    @Nonnull
    @Override
    public String setAffiliation(@Nonnull String symbolCode, @Nullable Affiliation affiliation)
    {
        return SymbolUtilities.isWeather(symbolCode) ? symbolCode : symProvider.setAffiliation(symbolCode, affiliation);
    }

    @Nonnull
    @Override
    public Presence getPresence(@Nonnull String symbolCode)
    {
        return Presence.fromStatus(getStatus(symbolCode));
    }

    @Nonnull
    @Override
    public String setPresence(@Nonnull String symbolCode, @Nullable Presence presence)
    {
        return setStatus(symbolCode, Presence.toStatus(presence));
    }

    @Nonnull
    @Override
    public Status getStatus(@Nonnull String symbolCode)
    {
        return symProvider.getStatus(symbolCode);
    }

    @Nonnull
    @Override
    public String setStatus(@Nonnull String symbolCode, @Nullable Status status)
    {
        return SymbolUtilities.isWeather(symbolCode) ? symbolCode : symProvider.setStatus(symbolCode, status);
    }

    @Nonnull
    @Override
    public Echelon getEchelon(@Nonnull String symbolCode)
    {
        return Objects.requireNonNullElse(Echelon.fromAmplifier(getAmplifier(symbolCode)), Echelon.UNSPECIFIED);
    }

    @Nonnull
    @Override
    public String setEchelon(@Nonnull String symbolCode, @Nullable Echelon echelon)
    {
        return SymbolUtilities.isWeather(symbolCode) ? symbolCode : setAmplifier(symbolCode, Echelon.toAmplifier(echelon));
    }

    @Nullable
    @Override
    public Amplifier getAmplifier(@Nonnull String symbolCode)
    {
        return symProvider.getAmplifier(symbolCode);
    }

    @Nonnull
    @Override
    public String setAmplifier(@Nonnull String symbolCode, @Nullable Amplifier amplifier)
    {
        return SymbolUtilities.isWeather(symbolCode) ? symbolCode : symProvider.setAmplifier(symbolCode, amplifier);
    }

    @Nonnull
    @Override
    public ShapeType getDefaultShape(@Nonnull String symbolCode)
    {
        return symProvider.getDefaultSourceShape(symbolCode);
    }

    @Nonnull
    @Override
    public Collection<Modifier> getModifiers(@Nonnull String symbolCode)
    {
        return Objects.requireNonNullElse(symProvider.getModifiers(symbolCode), Collections.emptySet());
    }

    @Nonnull
    @Override
    public Collection<Modifier> getAllModifiers()
    {
        ensureModifiersMapIsInitialized();
        return allModifiers.values();
    }

    @Override
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValidModifier(@Nonnull String modifierId)
    {
        ensureModifiersMapIsInitialized();
        return allModifiers.containsKey(modifierId);
    }

    @Override
    public boolean isValidModifierForSymbol(@Nonnull String symbolCode, @Nonnull String modifierId)
    {
        if (!isValidModifier(modifierId)) return false;

        final Set<String> supportedModifierIds = symProvider.getModifiers(symbolCode).stream()
                .map(Modifier::getId)
                .collect(Collectors.toSet());

        return supportedModifierIds.contains(modifierId);
    }

    @Override
    public void setModifier(@Nonnull IMapObject mapObject, @Nonnull String modifierId, @Nonnull String modifierValue)
            throws IllegalArgumentException
    {
        final String milSym = getSymbolCode(mapObject);
        if (milSym != null && !isValidModifierForSymbol(milSym, modifierId))
        {
            throw new IllegalArgumentException("Symbol '" + milSym + "' does not support a modifier with id '" + modifierId + "'");
        }

        // If the modifier value is empty we will remove it from the attribute set.
        if (modifierValue.isEmpty())
        {
            mapObject.setMetaString(getModifierKey(modifierId), null);
        } else
        {
            mapObject.setMetaString(getModifierKey(modifierId), modifierValue);
        }
    }

    @Nullable
    @Override
    public String getModifier(@Nonnull IMapObject mapObject, @Nonnull String modifierId)
    {
        final String key = getModifierKey(modifierId);
        return mapObject.hasMetaValue(key)
                ? mapObject.getMetaString(key, "")
                : null;
    }

    @Override
    public void removeModifier(@Nonnull IMapObject mapObject, @Nonnull String modifierId)
    {
        final String key = getModifierKey(modifierId);

        // Check existence to prevent unnecessary PCEs
        if (mapObject.hasMetaValue(key)) mapObject.getAttributeSet().removeAttribute(key);
    }

    @Nullable
    @Override
    public String getModifierIdFromPropertyName(@Nonnull String propertyName)
    {
        if (propertyName.startsWith(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX))
        {
            // The modifier id will appear after the last "."
            return propertyName.substring(propertyName.lastIndexOf(".") + 1);
        }

        return null;
    }

    @Override
    public boolean hasSymbolCode(@Nonnull IMapObject mapObject)
    {
        return !StringUtils.isEmpty(mapObject.getMetaString(StandardAttributeNames.MIL_SYM, ""));
    }

    @Nullable
    @Override
    public String getSymbolCode(@Nonnull IMapObject mapObject)
    {
        return hasSymbolCode(mapObject)
                ? mapObject.getMetaString(StandardAttributeNames.MIL_SYM, "")
                : null;
    }

    @Override
    public void clearSymbolCode(@Nonnull IMapObject mapObject)
    {
        String symbolCode = getSymbolCode(mapObject);

        if (symbolCode != null)
        {
            setSymbolCode(mapObject, null);
            for (Modifier modifier : getModifiers(symbolCode))
            {
                removeModifier(mapObject, modifier.getId());
            }
        }
    }

    @Nullable
    @Override
    public String getBaseSymbolCode(@Nonnull String symbolCode)
    {
        final SymbolWrapper symbolWrapper = getSymbol(symbolCode);
        return symbolWrapper != null ? symbolWrapper.getSymbol().getCode() : null;
    }

    @Override
    public void setSymbolCode(@Nonnull IMapObject mapObject, String symbolCode)
    {
        mapObject.setMetaString(StandardAttributeNames.MIL_SYM, symbolCode);
    }

    @Override
    public void setSymbolCode(@Nonnull IMapObject mapObject, @Nonnull String symbolCode, @Nonnull Map<String, String> modifiers)
    {
        setSymbolCode(mapObject, symbolCode);

        modifiers.forEach((id, value) -> setModifier(mapObject, id, value));
    }

    @Nullable
    @Override
    public SymbolWrapper getSymbol(@Nonnull String symbolCode)
    {
        // Ensure the code is recognized by the provider.
        ISymbolTable table = symProvider.getSymbolTable();
        ISymbolTable.Symbol symbol = table.getSymbol(symbolCode);
        if (symbol != null)
        {
            // Users can give in symbol codes with affiliations and other data set.
            // Fetching the symbol from the table then asking for the symbol back
            // provides us with the 'default' state for the symbol.
            symbolCode = symbol.getCode();

            // The 'default' symbol code is what we use for keys in 'allSymbols'.
            // We can now do a simple lookup.
            ensureSymbolsMapIsInitialized();
            return allSymbols.get(symbolCode);
        }

        // Code not recognized.
        return null;
    }

    @Override
    public boolean isValidSymbol(@Nonnull String symbolCode)
    {
        return getSymbol(symbolCode) != null;
    }

    @Nonnull
    @Override
    public MilStd2525Variant getVariant()
    {
        if (symProvider instanceof MilStd2525cSymbologyProvider)
        {
            return MilStd2525Variant.CHARLIE;
        } else if (symProvider instanceof MilStd2525dSymbologyProvider)
        {
            return MilStd2525Variant.DELTA;
        } else if (symProvider instanceof MilStd2525eSymbologyProvider)
        {
            return MilStd2525Variant.ECHO;
        }
        return MilStd2525Variant.CHARLIE;
    }

    @Nullable
    @Override
    public String convertFromOther2525Variant(@Nonnull String symbolCode)
    {
        // Extract the base symbol code + the amplifier/status of the given symbol code.
        IMilStd2525Service service = milSymServiceProvider.get2525ServiceForSymbol(symbolCode);
        Amplifier amplifier = service.getAmplifier(symbolCode);
        Status status = service.getStatus(symbolCode);
        Affiliation affiliation = service.getAffiliation(symbolCode);
        String symbolCodeBase = Objects.requireNonNullElse(service.getBaseSymbolCode(symbolCode), symbolCode);

        // Convert using the base symbol (kernel lookups can fail if you have amplifier/status present...)
        char c = symbolCode.charAt(1);
        String symbolCodeC;
        String symbolCodeD;
        String symbolCodeE;
        if (Character.isDigit(c))
        {
            if (c == '0')
            {
                // Incoming is delta
                symbolCodeD = symbolCodeBase;
                symbolCodeC = MilStd2525.get2525CFrom2525D(symbolCodeD);
                symbolCodeE = MilStd2525.get2525DFrom2525E(symbolCodeD);
            } else
            {
                // Incoming is echo
                symbolCodeE = symbolCodeBase;
                symbolCodeD = MilStd2525.get2525DFrom2525E(symbolCodeE);
                symbolCodeC = symbolCodeD == null ? null : MilStd2525.get2525CFrom2525D(symbolCodeD);
            }
        } else
        {
            // Incoming is charlie
            symbolCodeC = symbolCodeBase;
            symbolCodeD = MilStd2525.get2525DFrom2525C(symbolCodeC);
            symbolCodeE = symbolCodeD == null ? null : MilStd2525.get2525EFrom2525D(symbolCodeD);
        }

        // Return the appropriate code variant if it is valid.
        // - Prior conversions may be 'null' if there is not a mapping
        // - Prior conversions may have been made, but are not "real" (the conversion table provided by kernel has "discrepancies")
        String mappedSymbolCode = switch (getVariant())
        {
            case CHARLIE -> symbolCodeC;
            case DELTA -> symbolCodeD;
            case ECHO -> symbolCodeE;
        };

        // If we have a mapped code given back to us, put the amplifier/status back into it.
        return (mappedSymbolCode != null && getSymbol(mappedSymbolCode) != null) ? setAffiliation(setStatus(setAmplifier(mappedSymbolCode, amplifier), status), affiliation) : null;
    }

    @Nonnull
    @Override
    public ISymbolChooser newSymbolChooser()
    {
        return new SymbolChooser(this, uiSpaceManager);
    }

    @Nonnull
    @Override
    public Collection<SymbolWrapper> getAllSymbols()
    {
        ensureSymbolsMapIsInitialized();
        return new TreeSet<>(allSymbols.values());
    }

    @Nonnull
    @Override
    public ISymbolTable.Folder getSymbolTreeRoot()
    {
        return symProvider.getSymbolTable().getRoot();
    }

    @Nonnull
    @Override
    public List<ISymbolTable.Symbol> find(@Nullable String searchString, @Nullable EnumSet<ShapeType> allowedTypes)
    {
        return symProvider.getSymbolTable()
                .find(Objects.requireNonNullElse(searchString, ""), allowedTypes);
    }

    @Override
    public void traverse(@Nonnull Consumer<SymbolWrapper> consumer)
    {
        ISymbolTable.Folder root = symProvider.getSymbolTable().getRoot();
        traverse(root, consumer);
    }

    /**
     * Visits the symbol table tree, passing along each symbol entry to the consumer.
     *
     * @param folder Current folder being traversed.
     * @param consumer Consumer to act on symbol entries.
     */
    private void traverse(@Nonnull ISymbolTable.Folder folder, @Nonnull Consumer<SymbolWrapper> consumer)
    {
        traverse(List.of(folder), folder, consumer);
    }

    /**
     * Visits the symbol table tree, passing along each symbol entry to the consumer.
     *
     * @param folderPath Complete path of folders up to this point.
     * @param folder Current folder being traversed.
     * @param consumer Consumer to act on symbol entries.
     */
    private void traverse(@Nonnull List<ISymbolTable.Folder> folderPath,
                          @Nonnull ISymbolTable.Folder folder,
                          @Nonnull Consumer<SymbolWrapper> consumer)
    {
        Collection<ISymbolTable.Folder> children = folder.getChildren();
        for (ISymbolTable.Folder subfolder : children)
        {
            List<ISymbolTable.Folder> subfolderPath = new ArrayList<>(folderPath);
            subfolderPath.add(subfolder);
            traverse(subfolderPath, subfolder, consumer);
        }
        for (ISymbolTable.Symbol symbol : folder.getSymbols())
        {
            consumer.accept(new SymbolWrapper(folderPath, symbol));
        }
    }

    @Nonnull
    @Override
    public IMilSymIconBuilder newSymbolIconBuilder()
    {
        return new MilSymIconBuilder(this, symbolCache);
    }

    @Nullable
    @Override
    public Collection<Feature> renderMultipointSymbol(@Nonnull String symbolCode, @Nonnull IGeoPoint[] geoPoints,
                                                      @Nullable AttributeSet attrs, @Nullable ISymbologyProvider.RendererHints hints)
    {
        if (attrs == null) attrs = new AttributeSet();
        return symProvider.renderMultipointSymbol(symbolCode, geoPoints, attrs, hints);
    }

    @Override
    public boolean isUnrenderableSymbol(@Nullable String symbolCode)
    {
        // Skip if no code or an invalid symbol were provided.
        if (symbolCode == null || !isValidSymbol(symbolCode)) return true;

        if (!symbolCache.isCached(symbolCode))
        {
            // Not cached, can't tell until we try to render it.
            newSymbolIconBuilder().setCode(symbolCode).render();
        }

        return symbolCache.isFailure(symbolCode);
    }

    @Nonnull
    @Override
    public CompletableFuture<IMapObject> initiateDraw(@Nonnull String symbolCode, @Nullable String objectName, @Nullable ShapeType shapeType)
    {
        // Skip if the shape drawing service hasn't been injected.
        // This exists for setups that run the UI outside a TAKX instance (no injection)
        if (shapeDrawingService == null) return CompletableFuture.completedFuture(null);

        // If the user has requested drawing something while the shape service is already drawing cancel it.
        // Should the user want to switch which symbol they're drawing this will allow the new symbol shape to
        // be drawn instead of erroring out.
        shapeDrawingService.cancelDraw();

        // Skip if there is no focused globe.
        IApplicationGlobe globe = globeManager.getLastFocusedGlobe();
        if (globe == null) return CompletableFuture.completedFuture(null);

        // Map kernel shape-type to TAKX shape-type
        if (shapeType == null) shapeType = getDefaultShape(symbolCode);
        var adaptedShapeType = SHAPE_TYPE_ALIAS_MAP.get(shapeType);
        var drawMessage = SHAPE_MESSAGE_MAP.get(shapeType);

        // Ensure object name is specified
        if (objectName == null)
        {
            SymbolWrapper wrapper = getSymbol(symbolCode);
            objectName = wrapper == null ? "Unknown" : wrapper.getName();
        }
        final String finalObjectName = objectName;

        // Future to hold the result, completed by the point/shape draw operations.
        CompletableFuture<IMapObject> result = new CompletableFuture<>();

        // Request the creation of the shape/point on the globe.
        if (adaptedShapeType == gov.takx.api.shapes.ShapeType.NONE)
        {
            final var configuration = PointDropperConfiguration.builder(PLUGIN_DEFINITION)
                    .sidc(symbolCode)
                    .nameGenerator(() -> finalObjectName)
                    .dropHandler(mo -> {
                        result.complete(mo);
                        pointDropperService.deactivatePointDropper(); // Only allow a single point to be dropped
                    })
                    .deactivationHandler(() -> {
                        // If the result hasn't been completed yet, it implies the user has cancelled the point
                        // dropping operation, so we should complete the future with null to indicate the cancellation.
                        if (!result.isDone()) result.complete(null);
                    })
                    .build();

            pointDropperService.activatePointDropper(configuration);
        } else
        {
            // Use the shape drawing service to draw a shape of the selected type, then replace it with
            // a customized shape with the appropriate 2525 attributes.
            try
            {
                shapeDrawingService.requestDraw(globe, adaptedShapeType, drawMessage).whenComplete((shape, error) -> {
                    if (shape != null)
                    {
                        AttributeSet attributes = new AttributeSet();
                        attributes.setAttribute(StandardAttributeNames.MIL_SYM, symbolCode);
                        shapeDrawingService.removeDrawnShape(shape);
                        shape.setName(finalObjectName);
                        MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, adaptedShapeType.getShapeType())
                                .location(shape.getLocation())
                                .name(finalObjectName)
                                .attributeSet(attributes)
                                .initializer(mo -> {
                                    if (mo instanceof AShapeMapObject<?> asmo) asmo.setAssociatedShape(shape);
                                })
                                .build();
                        mapObjectManager.getOrCreateMapObject(spec);
                        result.complete(shape.getShapeMapObject());
                    } else
                    {
                        result.complete(null);
                    }
                });
            } catch (IllegalStateException ex)
            {
                logger.error("Cannot draw shape for '{}' because draw-service is already armed", objectName, ex);
                result.completeExceptionally(ex);
            }
        }

        return result;
    }

    /**
     * Populate {@link #allModifiers} if not already populated.
     */
    private void ensureModifiersMapIsInitialized()
    {
        if (allModifiers == null)
        {
            synchronized (this)
            {
                if (allModifiers == null) allModifiers = symProvider.getModifiers(null).stream()
                        .collect(Collectors.toMap(Modifier::getId, Function.identity()));
            }
        }
    }

    /**
     * Populate {@link #allSymbols} if not already populated.
     */
    private void ensureSymbolsMapIsInitialized()
    {
        if (allSymbols == null)
        {
            synchronized (this)
            {
                if (allSymbols == null)
                {
                    Map<String, SymbolWrapper> map = new HashMap<>();
                    traverse(wrapper -> map.put(wrapper.getSymbol().getCode(), wrapper));
                    allSymbols = Collections.unmodifiableMap(map);
                }
            }
        }
    }

    /**
     * @param modifierId The modifier id
     * @return The modifier's AttributeSet key
     */
    @Nonnull
    private String getModifierKey(@Nonnull String modifierId)
    {
        return MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + modifierId;
    }
}
