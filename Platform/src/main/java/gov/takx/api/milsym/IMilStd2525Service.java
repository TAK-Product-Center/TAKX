package gov.takx.api.milsym;

import com.atakmap.map.layer.feature.Feature;
import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.symbology.Status;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Contract for a service offering MIL-STD-2525 symbology and graphic operations.
 *
 * @since 5.1
 */
@NoPluginImplementations
public interface IMilStd2525Service
{
    /**
     * @param symbolCode SIDC.
     *
     * @return Symbol wrapper for the requested symbol.
     */
    @Nullable
    SymbolWrapper getSymbol(@Nonnull String symbolCode);

    /**
     * @param symbolCode SIDC to validate.
     *
     * @return {@code true} when the SIDC correlates to a valid symbol.
     */
    boolean isValidSymbol(@Nonnull String symbolCode);

    /**
     * @return The 2525 variant of this service.
     * @since 5.5
     */
    @Nonnull
    MilStd2525Variant getVariant();

    /**
     * @param symbolCode Some arbitrary SIDC of any 2525 variant to convert.
     *
     * @return Adapted symbol code to this service's 2525 variant.
     * @since 5.5
     */
    @Nullable
    String convertFromOther2525Variant(@Nonnull String symbolCode);

    /**
     * Creates a new symbol chooser with the option to handle a selected symbol or a cancellation request.
     *
     * @return New symbol chooser.
     */
    @Nonnull
    ISymbolChooser newSymbolChooser();

    /**
     * @param symbolCode SIDC to extract affiliation from.
     *
     * @return Affiliation of the symbol. If an unexpected value is found then this falls back to {@link Affiliation#Unknown}.
     */
    @Nonnull
    Affiliation getAffiliation(@Nonnull String symbolCode);

    /**
     * @param symbolCode SIDC to modify.
     * @param affiliation Desired affiliation, or {@code null} to remove the affiliation from the code.
     *
     * @return Modified symbol code with the affiliation set.
     */
    @Nonnull
    String setAffiliation(@Nonnull String symbolCode, @Nullable Affiliation affiliation);

    /**
     * @param symbolCode SIDC to extract presence from.
     *
     * @return Presence amplifier of the symbol. If an unexpected value is found then this falls back to {@link Presence#UNSPECIFIED}.
     * @since 5.2
     * @see #getStatus(String)
     * @deprecated since = "5.5", forRemoval = true, removeAt = "5.8"
     */
    @Nonnull
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    Presence getPresence(@Nonnull String symbolCode);

    /**
     * @param symbolCode SIDC to modify.
     * @param presence Desired presence amplifier, or {@code null} to remove presence from the code.
     *
     * @return Modified symbol code with the presence set.
     * @since 5.2
     * @see #setStatus(String, Status)
     */
    @Nonnull
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    String setPresence(@Nonnull String symbolCode, @Nullable Presence presence);

    /**
     * @param symbolCode The SIDC to extract the {@link Status} from.
     * @return The {@link Status} from the provided SIDC.
     * @since 5.5
     */
    @Nonnull
    Status getStatus(@Nonnull String symbolCode);

    /**
     * @param symbolCode The SIDC to set the {@link Status} on.
     * @param status The {@link Status} to insert into the SIDC, or {@code null} to remove the status from the code.
     * @return The resultant SIDC.
     * @since 5.5
     */
    @Nonnull
    String setStatus(@Nonnull String symbolCode, @Nullable Status status);

    /**
     * @param symbolCode SIDC to extract echelon from.
     *
     * @return Echelon amplifier of the symbol. If an unexpected value is found then this falls back to {@link Echelon#UNSPECIFIED}.
     * @since 5.2
     * @see #getAmplifier(String)
     * @deprecated since = "5.5", forRemoval = true, removeAt = "5.8"
     */
    @Nonnull
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    Echelon getEchelon(@Nonnull String symbolCode);

    /**
     * @param symbolCode SIDC to modify.
     * @param echelon Desired echelon amplifier, or {@code null} to remove the echelon from the code.
     *
     * @return Modified symbol code with the echelon set.
     * @since 5.2
     * @see #setAmplifier(String, Amplifier)
     * @deprecated since = "5.5", forRemoval = true, removeAt = "5.8"
     */
    @Nonnull
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    String setEchelon(@Nonnull String symbolCode, @Nullable Echelon echelon);

    /**
     * @param symbolCode The SIDC to extract the {@link Amplifier} from.
     * @return The {@link Amplifier} from the provided SIDC.
     * @since 5.5
     */
    @Nullable
    Amplifier getAmplifier(@Nonnull String symbolCode);

    /**
     * @param symbolCode The SIDC to set the {@link Amplifier} on.
     * @param amplifier The {@link Amplifier} to insert into the SIDC, or {@code null} to remove the amplifier from the code.
     * @return Modified symbol code with the amplifier set.
     * @since 5.5
     */
    @Nonnull
    String setAmplifier(@Nonnull String symbolCode, @Nullable Amplifier amplifier);

    /**
     * @param symbolCode SIDC to get the default symbol shape of.
     *
     * @return Default shape of symbol, never {@code null}.
     * Unrecognized symbols will yield {@link ShapeType#LineString}.
     */
    @Nonnull
    ShapeType getDefaultShape(@Nonnull String symbolCode);

    /**
     * @param symbolCode SIDC.
     *
     * @return Modifiers applicable to the requested symbol.
     */
    @Nonnull
    Collection<Modifier> getModifiers(@Nonnull String symbolCode);

    /**
     * @return All modifiers without any relation to any specific symbol.
     *
     * @see #getModifiers(String) All modifiers related to a specific symbol.
     */
    @Nonnull
    Collection<Modifier> getAllModifiers();

    /**
     * @param modifierId ID of modifier to check.
     *
     * @return {@code true} if the given modifier ID is in the set of all known modifier IDs.
     */
    boolean isValidModifier(@Nonnull String modifierId);

    /**
     * @param symbolCode SIDC to validate against.
     * @param modifierId ID of modifier to check.
     *
     * @return {@code true} if the given modifier ID is supported on the given symbol.
     */
    boolean isValidModifierForSymbol(@Nonnull String symbolCode, @Nonnull String modifierId);

    /**
     * Set a modifier code for they MIL-STD-2525 symbol. If a value has already been set for the given modifier ID, it
     * will be overwritten with the value given here. Modifier codes alter/augment the way that the graphic is rendered
     * as specified in the MIL-STD-2525 standard.
     *
     * @param mapObject Map Object to update.
     * @param modifierId ID of modifier to add.
     * @param modifierValue Value of the modifier.
     * @throws IllegalArgumentException If the map object has a symbol code set and the given modifier ID is not supported
     *                                  by that symbol.
     * @see #hasSymbolCode(IMapObject)
     * @see #isValidModifierForSymbol(String, String)
     *
     * @implNote This is equivalent to {@code mapObject.setMetaString(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + modifierId, modifierValue);}
     */
    void setModifier(@Nonnull IMapObject mapObject, @Nonnull String modifierId, @Nonnull String modifierValue) throws IllegalArgumentException;

    /**
     * Get the value of the given modifier ID from the Map Object, if any.
     *
     * @param mapObject Map Object of interest.
     * @param modifierId ID of modifier to retrieve.
     *
     * @return Modifier value, if present, {@code null} if not.
     */
    @Nullable
    String getModifier(@Nonnull IMapObject mapObject, @Nonnull String modifierId);

    /**
     * Removes the specified modifier entry if it is present. If not present, no action is taken.
     *
     * @param mapObject The map object to update
     * @param modifierId The modifier id
     */
    void removeModifier(@Nonnull IMapObject mapObject, @Nonnull String modifierId);

    /**
     * Get the modifier ID contained within the property name
     *
     * @param propertyName The name of some property linked to a modifier.
     *                     Typically, in the form {@link MilSymStandardAttributes#MILSYM_MODIFIER_PREFIX}
     *                     + {@code "."} + {@code <id>}.
     *
     * @return Modifier ID, if present in the string. If an unexpected format is found then this returns {@code null}.
     */
    @Nullable
    String getModifierIdFromPropertyName(@Nonnull String propertyName);

    /**
     * @return {@code true} if a MIL-STD-2525 symbol code has been set on the given {@code mapObject}, {@code false} otherwise.
     */
    boolean hasSymbolCode(@Nonnull IMapObject mapObject);

    /**
     * @return MIL-STD-2525 symbol code set on the given {@code mapObject}, or {@code null} if one has not been set.
     */
    @Nullable
    String getSymbolCode(@Nonnull IMapObject mapObject);

    /**
     * Removes any MIL-STD-2525 symbol code + modifiers from the given {@code mapObject}.
     * @since 5.5
     */
    void clearSymbolCode(@Nonnull IMapObject mapObject);

    /**
     * Returns the base symbol code from which the provided symbol code was derived. Useful for comparing symbol codes
     * that have been updated with additional data.
     *
     * @param symbolCode The (potentially modified) symbol code
     * @return The base (unmodified) symbol code or {@code null} if no matching code is found
     */
    @Nullable
    String getBaseSymbolCode(@Nonnull String symbolCode);

    /**
     * Set a MIL-STD-2525 symbol code on the given Map Object.  This will result in its graphic representation (icon or
     * shape on the globe) to change according to the give {@code symbolCode}.
     *
     * @param mapObject Map Object to update
     * @param symbolCode SIDC to apply
     *
     * @implNote This is equivalent to {@code mapObject.setMetaString(StandardAttributeNames.MIL_SYM, symbolCode);}
     */
    void setSymbolCode(@Nonnull IMapObject mapObject, String symbolCode);

    /**
     * Set a MIL-STD-2525 symbol code and associated set of modifier codes on the given Map Object.  This will result in
     * its graphic representation (icon or shape on the globe) to change according to the give {@code symbolCode}.
     *
     * @param mapObject Map Object to update
     * @param symbolCode SIDC to apply
     * @param modifiers Map of modifier ID to modifier value to add
     *
     * @see #setSymbolCode(IMapObject, String)
     * @see #setModifier(IMapObject, String, String)
     */
    void setSymbolCode(@Nonnull IMapObject mapObject, @Nonnull String symbolCode, @Nonnull Map<String, String> modifiers);

    /**
     * @return Collection of all symbols.
     */
    @Nonnull
    Collection<SymbolWrapper> getAllSymbols();

    /**
     * @return Root of the symbol table tree.
     *
     * @see #getAllSymbols() To access a pre-computed collection of all terminal tree paths.
     * @see #traverse(Consumer) To pass a visitor which accepts all symbol paths in the tree.
     * @see #find(String, EnumSet) To collect a list of symbols of the given name and type.
     */
    @Nonnull
    ISymbolTable.Folder getSymbolTreeRoot();

    /**
     * Search for symbols by name <i>(case insensitive)</i> and type.
     *
     * @param searchString Search string to match against {@link ISymbolTable.Symbol#getName()} content <i>(case insensitive)</i>.
     *                     {@code null} to match against any content.
     * @param allowedTypes If non-{@code null}, only those symbols sharing one or more of the
     *                     specified {@link ShapeType}s will be returned. If {@code null} all
     *                     {@link ISymbolTable.Symbol}s matching {@code searchString} will be returned,
     *                     regardless of {@link ShapeType}.
     * @return {@link List} of all {@link ISymbolTable.Symbol}s matching the specified search criteria.
     *
     * @see #traverse(Consumer) For a tree-walking alternative where access to the {@code SymbolTable.Folder}
     *                          hierarchy of matched symbols is also available.
     */
    @Nonnull
    List<ISymbolTable.Symbol> find(@Nullable String searchString, @Nullable EnumSet<ShapeType> allowedTypes);

    /**
     * Visits the symbol table tree, passing along each symbol entry to the consumer.
     *
     * @param consumer Consumer to act on symbol entries.
     */
    void traverse(@Nonnull Consumer<SymbolWrapper> consumer);

    /**
     * @param symbol SIDC to render.
     * @param size Desired image size.
     *
     * @return Rendered symbol.
     */
    @Nonnull
    default BufferedImage renderSinglePointIcon(@Nonnull ISymbolTable.Symbol symbol, int size)
    {
        return renderSinglePointIcon(symbol.getCode(), size);
    }

    /**
     * @param symbol SIDC to render.
     *
     * @return Rendered symbol.
     */
    @Nonnull
    default BufferedImage renderSinglePointIcon(@Nonnull ISymbolTable.Symbol symbol)
    {
        return renderSinglePointIcon(symbol.getCode());
    }

    /**
     * @param symbolCode SIDC to render.
     *
     * @return Rendered symbol.
     */
    @Nonnull
    default BufferedImage renderSinglePointIcon(@Nonnull String symbolCode)
    {
        return renderSinglePointIcon(symbolCode, IMilSymIconBuilder.DEFAULT_PREVIEW_SIZE);
    }

    /**
     * @param symbolCode SIDC to render.
     * @param size Desired image size.
     *
     * @return Rendered symbol.
     */
    @Nonnull
    default BufferedImage renderSinglePointIcon(@Nonnull String symbolCode, int size)
    {
        return renderSinglePointIcon(symbolCode, null, size);
    }

    /**
     * @param symbolCode Symbol code (SIDC) to render.
     * @param attributes Optional attributes to pass down to the native renderer <i>(such as symbol modifiers)</i>.
     * @param size Desired image size.
     *
     * @return Rendered symbol.
     * @since 5.5
     */
    @Nonnull
    default BufferedImage renderSinglePointIcon(@Nonnull String symbolCode, @Nullable AttributeSet attributes, int size)
    {
        return newSymbolIconBuilder()
                .setCode(symbolCode)
                .addAttributes(attributes)
                .setResolution(size)
                .render();
    }

    /**
     * @return New symbol icon builder.
     */
    @Nonnull
    IMilSymIconBuilder newSymbolIconBuilder();

    /**
     * Create a collection of features that represent the specified multipoint symbol. Returns {@code null} if
     * symbol can not be represented.
     *
     * @param symbolCode SIDC to render.
     * @param geoPoints Globe coordinates to render the vertices of the symbol at.
     * @param attrs User specified attributes for the symbol. Currently supported attributes include:
     *              <ul>
     *                <li>{@code "milsym.mod.<modifier-id>"}, where {@code <modifier-id>} is the
     *                modifier ID per {@link Modifier#getId()} and the value is a string. For
     *                modifiers supporting multiple fields, the field values shall be comma
     *                delimited.</li>
     *              </ul>
     * @param hints Hints for the symbol renderer.
     *
     * @return A collection of map features composed of geometry, style and attributes for the multipoint symbol or {@code null}
     */
    @Nullable
    Collection<Feature> renderMultipointSymbol(@Nonnull String symbolCode, @Nonnull IGeoPoint[] geoPoints,
                                               @Nullable AttributeSet attrs, @Nullable ISymbologyProvider.RendererHints hints);

    /**
     * This method is used to check if the render for a given symbol results in a valid image.
     * For optimal usage it is actually recommended to use this as an after-the-fact check when
     * a custom resolution is specified.
     * <p/>
     * This usage is <b>optimal</b>:
     * <pre>
     * {@code
     * if (isUnrenderableSymbol(symbolCode)) return;
     * BufferedImage image = renderSinglePointIcon(symbolCode); // default resolution
     * // Use of 'image'
     * }
     * </pre>
     * This usage is <b>optimal</b>:
     * <pre>
     * {@code
     * BufferedImage image = renderSinglePointIcon(symbolCode, 64); // custom resolution
     * if (isUnrenderableSymbol(symbolCode)) return;
     * // Use of 'image'
     * }
     * </pre>
     * This usage is <b>not optimal</b>:
     * <pre>
     * {@code
     * if (isUnrenderableSymbol(symbolCode)) {
     *     BufferedImage image = renderSinglePointIcon(symbolCode, 64); // custom resolution
     *     // Use of 'image'
     * }
     * }
     * </pre>
     * The reason for this is that this check needs to attempt rendering for the given symbol
     * code at least once to know if it is valid or not. This is done by invoking {@link #newSymbolIconBuilder()}
     * with just the symbol code provided and then checking if the internal {@link IMilSymIconCache}
     * recorded the render as a success or failure. Thus, the <i>default resolution</i> is used for this check
     * if the symbol has not been rendered previously.
     * <p/>
     * If you are specifying a non-default resolution in the {@link IMilSymIconBuilder} or via
     * {@link #renderSinglePointIcon(ISymbolTable.Symbol, int)} or {@link #renderSinglePointIcon(String, int)}
     * then the non-optimal example will render the image <i>(and cache it)</i> twice which is a CPU and memory waste.
     * This method will use the default resolution and then user's call to {@link #renderSinglePointIcon(String, int)}
     * will use a custom resolution which will result in two of the same image at different resolutions in the
     * {@link IMilSymIconCache}. Calling this method <i>after</i> rendering an icon will use the result of the prior call.
     *
     * @param symbolCode SIDC to check.
     * @return {@code true} when the symbol cannot be rendered, 
     * typically due to some internal failure in the symbols rendering logic or the specification of an invalid symbol code.
     */
    boolean isUnrenderableSymbol(@Nullable String symbolCode);

    /**
     * Initiates the drawing of a shape of the requested {@link ShapeType} <i>(Or point-dropping when {@link ShapeType#Point} is specified)</i>
     * and applies the given {@code symbolCode} to the created {@link IMapObject}.
     * <p/>
     * The drawn shape or dropped point's {@link IMapObject} is returned asynchronously by the {@link CompletableFuture} return value.
     * If the user cancels the drawing or dropping operation the future is completed with a {@code null} value.
     *
     * @param symbolCode Symbol code to apply to the point or shape drawn.
     * @param objectName Name template for drawn points and shapes.
     *                   Specifying {@code "foo"} will result in {@code "foo 1", "foo 2", "foo 3"} for three drawn items.
     * @param shapeType Desired shape type to draw the symbol with.
     *                  Can be {@code null} to auto-select based on {@link #getDefaultShape(String)} for the {@code symbolCode}.
     * @return Future of the created map object generated from the user's drawing. The future contents will be {@code null} if the draw was cancelled.
     * @since 5.2
     */
    @Nonnull
    CompletableFuture<IMapObject> initiateDraw(@Nonnull String symbolCode, @Nullable String objectName, @Nullable ShapeType shapeType);
}
