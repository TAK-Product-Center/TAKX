package gov.takx.core.milsym;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ShapeType;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.milsym.CotSidcAdapter;
import gov.takx.api.milsym.IExtendedMilStd2525Service;
import gov.takx.api.milsym.ISymbolChooser;
import gov.takx.api.milsym.ISymbolSelection;
import gov.takx.api.shapes.IShape;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.api.ui.UISpaceVisibilityEvent;
import gov.takx.core.view.gui.maptools.tacticalgraphics.SymbolChooserPanel;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Polyline;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;

import java.util.Map;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * {@link ISymbolChooser Symbol chooser} implementation that delegates to a {@link SymbolChooserPanel}.
 *
 * @since 5.1
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SymbolChooser implements ISymbolChooser
{
    private static final Map<Class<? extends IShape>, ShapeType> SHAPE_TO_TYPE = Map.of(
            Polyline.class, ShapeType.LineString,
            Polygon.class, ShapeType.Polygon,
            Square.class, ShapeType.Rectangle,
            Rectangle.class, ShapeType.Rectangle,
            Circle.class, ShapeType.Circle,
            Ellipse.class, ShapeType.Ellipse);
    private final IExtendedMilStd2525Service milStd2525Service;
    private final IUISpaceManager uiSpaceManager;
    private SymbolChooserPanel symbolChooserPanel;
    private Consumer<ISymbolSelection> symbolConsumer;
    private Runnable cancelAction;
    private ShapeType shapeType;
    private boolean cotSupport;
    private String objectName;
    private String initialSymbol;
    private Affiliation affiliation = Affiliation.Unknown;
    private boolean showing;
    private boolean selectionMade;

    SymbolChooser(@Nonnull IExtendedMilStd2525Service milStd2525Service, @Nonnull IUISpaceManager uiSpaceManager)
    {
        this.milStd2525Service = milStd2525Service;
        this.uiSpaceManager = uiSpaceManager;
    }

    @Nonnull
    @Override
    public ISymbolChooser initFromMapObject(@Nonnull IMapObject mapObject)
    {
        objectName = mapObject.getName();

        setInitialSelection(extractSidc(mapObject));

        if (mapObject instanceof IShapeMapObject<?> shapeMapObject)
        {
            ShapeType type = SHAPE_TO_TYPE.getOrDefault(shapeMapObject.getAssociatedShape().getClass(), ShapeType.LineString);
            setShapeType(type);
        }

        return this;
    }

    @Nonnull
    @Override
    public ISymbolChooser setInitialSelection(@Nullable String symbolCode)
    {
        initialSymbol = symbolCode;
        if (symbolCode != null) setAffiliation(milStd2525Service.getAffiliation(symbolCode));
        return this;
    }

    @Nonnull
    @Override
    public ISymbolChooser setAffiliation(@Nonnull Affiliation affiliation)
    {
        this.affiliation = affiliation;
        if (symbolChooserPanel != null) symbolChooserPanel.setAffiliation(affiliation);
        return this;
    }

    @Nonnull
    @Override
    public ISymbolChooser setShapeType(@Nullable ShapeType shapeType)
    {
        this.shapeType = shapeType;
        return this;
    }

    @Nonnull
    @Override
    public ISymbolChooser setRequireCotCompatibility(boolean cotSupport)
    {
        this.cotSupport = cotSupport;
        return this;
    }

    @Nonnull
    @Override
    public ISymbolChooser setSymbolConsumer(@Nonnull Consumer<ISymbolSelection> symbolConsumer)
    {
        // We need to track if a selection is made, because we don't want to accidentally trigger the cancel operation.
        // We'll just wrap the symbol consumer to handle this.
        this.symbolConsumer = t -> {
            selectionMade = true;
            symbolConsumer.accept(t);
        };
        return this;
    }

    @Nonnull
    @Override
    public ISymbolChooser setCancelAction(@Nonnull Runnable cancelAction)
    {
        this.cancelAction = cancelAction;
        return this;
    }

    @Override
    public void hide()
    {
        hide(true);
    }

    @Override
    public void show(boolean reset)
    {
        if (isShowing()) return;

        if (reset || symbolChooserPanel == null)
        {
            symbolChooserPanel = new SymbolChooserPanel(milStd2525Service);
            symbolChooserPanel.init(objectName, shapeType, initialSymbol, affiliation, cotSupport,
                    // Handle symbol selection
                    symbolSelection -> {
                        if (symbolConsumer != null) symbolConsumer.accept(symbolSelection);
                        hide(false);
                    },
                    // Handle cancel/close
                    event -> hide(true));
        } else
        {
            symbolChooserPanel.setAffiliation(affiliation);
        }

        final ComponentDescriptorBuilder componentDescriptorBuilder =
                new ComponentDescriptorBuilder(symbolChooserPanel, "MilSym Chooser")
                        .setComponentPosition(ComponentPosition.RIGHT)
                        .setCloseable(false)
                        .setExternalizable(false)
                        .setPinned(true)
                        .setHidePin(true)
                        .setToolbarButtonVisible(false)
                        .setIcon(UIResources.getUrl(PLATFORM_ICONS, "icon_tac_graphics.png"));

        uiSpaceManager.addComponent(componentDescriptorBuilder.build(), symbolChooserPanel);
        uiSpaceManager.showComponent(symbolChooserPanel);
        showing = true;
    }

    /**
     * If the user closes the panel instead of clicking the cancel button, we still need to invoke the cancel action.
     */
    @InvokeOnEDT
    private void panelVisibilityChanged(@Observes UISpaceVisibilityEvent uiSpaceVisibilityEvent)
    {
        if (!selectionMade && !uiSpaceVisibilityEvent.isVisible() && uiSpaceVisibilityEvent.getComponent() == symbolChooserPanel) hide(true);
    }

    @Override
    public boolean isShowing()
    {
        return showing;
    }

    /**
     * Removes the symbol chooser from the UI and optionally calls {@link #cancelAction}.
     *
     * @param callCancel {@code true} to call {@link #cancelAction} if defined.
     *                   Otherwise, the UI is hidden without the listener being called.
     */
    private void hide(boolean callCancel)
    {
        if (symbolChooserPanel != null)
        {
            showing = false;
            uiSpaceManager.removeComponent(symbolChooserPanel);
            if (callCancel && cancelAction != null) cancelAction.run();
        }
    }

    /**
     * @param mapObject Map object to extract a SIDC from.
     * @return SIDC associated with the map object, or {@code null} if none could be extracted.
     * @since 5.2
     */
    @Nullable
    private String extractSidc(@Nonnull IMapObject mapObject)
    {
        String code = milStd2525Service.getSymbolCode(mapObject);
        if (code == null)
        {
            String cotType = mapObject.getCotType();
            if (!CotTypes.EMPTY.equals(cotType)) code = CotSidcAdapter.mil2525CFromCotType(cotType);
        }
        return code;
    }
}
