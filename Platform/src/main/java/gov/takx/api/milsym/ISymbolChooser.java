package gov.takx.api.milsym;

import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ShapeType;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * Manages a pop-up docked panel which prompts the user to select a {@link gov.tak.api.symbology.ISymbolTable.Symbol}.
 * The chooser can be configured to filter by a specific shape type and inherit traits <i>(such as the supported shape type)</i>
 * from a given shape map object.
 *
 * @since 5.1
 */
@NoPluginImplementations
public interface ISymbolChooser
{
    /**
     * Prepares the symbol chooser to initialize with symbol data attached to the given map object.
     * Automatically {@link #setInitialSelection(String) assigns the initial selection}.
     * If the map object represents a shape, also automatically {@link #setShapeType(ShapeType) assigns the target shape type}.
     *
     * @param mapObject Map object to pull information from.
     *
     * @return Self.
     * @since 5.2
     */
    @Nonnull
    ISymbolChooser initFromMapObject(@Nonnull IMapObject mapObject);

    /**
     * Prepares the symbol chooser with an initial symbol selected.
     *
     * @param symbolCode Symbol to display as the initial selection.
     *
     * @return Self.
     * @since 5.2
     */
    @Nonnull
    ISymbolChooser setInitialSelection(@Nullable String symbolCode);

    /**
     * Prepares the symbol chooser with a given affiliation selected.
     *
     * @param affiliation Affiliation state to use. Defaults is {@link Affiliation#Unknown}.
     *
     * @return Self.
     * @since 5.3
     */
    @Nonnull
    ISymbolChooser setAffiliation(@Nonnull Affiliation affiliation);

    /**
     * Prepares the symbol chooser to initialize only showing symbols supporting the given shape type.
     *
     * @param shapeType Shape type to limit selections to.
     *
     * @return Self.
     */
    @Nonnull
    ISymbolChooser setShapeType(@Nullable ShapeType shapeType);

    /**
     * Prepares the symbol chooser to initialize only with symbols that can support representation in CoT.
     *
     * @param cotSupport {@code true} for showing only symbols with CoT support. {@code false} to not consider CoT support.
     *
     * @return Self.
     */
    @Nonnull
    ISymbolChooser setRequireCotCompatibility(boolean cotSupport);

    /**
     * Prepares the symbol chooser to initialize with a listener to handle symbol selection.
     *
     * @param symbolConsumer Consumer to handle the symbol a user selects.
     *
     * @return Self.
     */
    @Nonnull
    ISymbolChooser setSymbolConsumer(@Nonnull Consumer<ISymbolSelection> symbolConsumer);

    /**
     * Prepares the symbol chooser to initialize with a listener to handle cancellation.
     *
     * @param cancelAction Runnable to handle cancellation or closure of the chooser dialog.
     *
     * @return Self.
     *
     * @see #hide() For pre-emptive closing of the chooser.
     */
    @Nonnull
    ISymbolChooser setCancelAction(@Nonnull Runnable cancelAction);

    /**
     * Shows the symbol chooser panel, resetting any state if this chooser was shown previously.
     *
     * @see #show(boolean) Calls show(true).
     */
    default void show()
    {
        show(true);
    }

    /**
     * Show the symbol chooser panel and optionally resets any state if this chooser was shown previously.
     * Not resetting the state will result in the symbol tree keeping the same paths expanded as they were
     * when the panel was last shown.
     * <p/>
     * While the chooser panel is shown the setters <i>({@link #setShapeType(ShapeType)},
     * {@link #setRequireCotCompatibility(boolean)}, etc)</i> will not have any affect until the panel is shown
     * again with {@code reset} set to {@code true}.
     *
     * @param reset {@code true} to reset the state of the panel if it was previously shown.
     * {@code false} to not apply state changes if it was already shown.
     */
    void show(boolean reset);

    /**
     * Hides the symbol chooser panel. Will call the cancel action provided to {@link #setCancelAction(Runnable)}.
     */
    void hide();

    /**
     * @return {@code true} when the symbol chooser panel is visible.
     * {@code false} if the panel is not shown.
     */
    boolean isShowing();
}
