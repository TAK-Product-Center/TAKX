package gov.takx.core.globe.widgets;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.widgets.DrawableWidget;
import gov.tak.platform.widgets.LinearLayoutWidget;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.tracks.ITrackStyle;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentListener;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentManager;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.globe.render.GlobeMarkerAttributes;
import gov.takx.core.globe.render.RenderingContext;
import gov.takx.core.tracks.TrackStylingContext;
import gov.takx.platform.tracks.BucketMode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

/**
 * Widget implementation of a color scale/legend for track differentiation, based on the previously existing `TrackColorScaleLayer`
 * <p/>
 *
 * Display color scales for each {@link BucketMode}. The color scales will show/hide based
 * on whether there are map objects or tracks on the screen that are using each {@link BucketMode}.
 *
 * @since 3.5
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TrackColorScaleWidget extends AWidget implements ISmartTrackAssignmentListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BUCKET_MODE_CLEARED = ITrackStyleDefinition.BUCKET_MODE + ITrackStyleDefinition.CLEARED;

    // The TSD properties to monitor to know when to hide/show the color scales.
    private static final List<String> TSC_PROPERTY_LIST = Arrays.asList(ITrackStyleDefinition.VISIBILITY, ITrackStyleDefinition.BUCKET_MODE, BUCKET_MODE_CLEARED);

    // The set of legends that can be used
    private static final BucketMode[] BUCKET_MODES = {BucketMode.HourOfDay, BucketMode.MonthOfYear, BucketMode.DayOfWeek};

    // The legend widget to use for each Bucket Mode
    private static final Map<BucketMode, LinearLayoutWidget> WIDGET_MAP = new EnumMap<>(BucketMode.class);

    private static final int TRANSLUCENT_BLACK = new Color(0, 0, 0, 125).getRGB();

    private static final int ENTRY_HEIGHT = 15; // Height of an entry within a legend
    private static final int ENTRY_MARGIN = 2; // Margin of an entry within a legend

    private final Map<IMapObject, Set<ITrackStylingContext>> mapObjectToStylingContextMap = new HashMap<>();
    private final PropertyChangeListener mapObjectVisibilityChanged = this::mapObjectVisibilityChanged;
    private final PropertyChangeListener trackStylingChanged = this::trackStylingChanged;
    private final IApplicationGlobe globe;

    private final int[] bucketReferences = new int[BucketMode.values().length]; // Reference count for each bucket type
    private final BitSet moVisible = new BitSet(); //track the last known visibility state of MOs

    @Inject
    private ISmartTrackAssignmentManager smartTrackAssignmentManager;

    @Inject
    private ISmartTrackManager smartTrackManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    private LinearLayoutWidget currentWidget;
    private RenderingContext renderingContext;

    static
    {
        WIDGET_MAP.put(BucketMode.HourOfDay, createHoursLegendWidget());
        WIDGET_MAP.put(BucketMode.DayOfWeek, createDaysLegendWidget());
        WIDGET_MAP.put(BucketMode.MonthOfYear, createMonthLegendWidget());
    }

    /**
     * A Widget to display the currently applied {@link TrackStylingContext} color scheme
     * @param globe The globe this is applied to
     */
    public TrackColorScaleWidget(@Nonnull IApplicationGlobe globe)
    {
        this.globe = globe;
        setOrientation(VERTICAL);
        setName(CoreWidgetNames.TRACK_COLOR_SCALE);
    }

    @PostConstruct
    private void postConstruct()
    {
        renderingContext = globe.getRenderingContext();
        mapObjectManager.getMapObjects().forEach(this::onMapObjectAdded);// Add any map objects that are visible on the map.
        updateColorScales();
        setVisible(false);
    }

    @Override
    public boolean setVisible(boolean visible)
    {
        if (super.setVisible(visible))
        {
            if (visible) updateLegend(currentWidget);
            return true;
        }
        return false;

    }

    @RemoveObserver
    @Override
    public void releaseResources()
    {
        smartTrackAssignmentManager.removeAssignmentListener(this);
        mapObjectToStylingContextMap.forEach((mo, tsc) -> {
            mo.removePropertyChangeListener(IMapObject.VISIBILITY, mapObjectVisibilityChanged);
            tsc.forEach(e -> e.removePropertyChangeListener(trackStylingChanged, TSC_PROPERTY_LIST));
        });
    }

    private void trackStylingChanged(PropertyChangeEvent evt)
    {
        //noinspection SwitchStatementWithoutDefaultBranch
        switch (evt.getPropertyName())
        {
            // TSC notification that the bucket changed
            case ITrackStylingContext.BUCKET_MODE, BUCKET_MODE_CLEARED ->
            {
                BucketMode oldMode = (BucketMode) evt.getOldValue();

                // Check to see if there was a previous bucket. If not, use the global.
                // todo: This still isn't right. We need some way to track the previous MO-level bucket.
                if (oldMode == null)
                {
                    oldMode = smartTrackManager.getGlobalTrackStyleDefinition().getBucketMode();
                }

                onTrackStyleContextBucketChange((ITrackStylingContext) evt.getSource(), oldMode);
            }

            // TSC notification that visibility changed
            case ITrackStylingContext.VISIBILITY ->
            {
                ITrackStylingContext tsc = (ITrackStylingContext) evt.getSource();
                Boolean newValue = (Boolean) evt.getNewValue();

                // If visibility value has been cleared, new value will be null, defer to TSC
                boolean visible = newValue != null ? newValue : tsc.isVisible();

                onTrackStyleContextVisibilityChange(tsc, visible);
            }

            // TSC notification that TSD changed
            case ITrackStylingContext.TRACK_STYLE_DEFINITION ->
            {
                ITrackStylingContext tsc = (ITrackStylingContext) evt.getSource();
                ITrackStyleDefinition oldTsd = (ITrackStyleDefinition) evt.getOldValue();
                BucketMode oldMode;

                // Check to see if the old TSD defined a bucket, which would have
                // overridden all other settings.
                // todo: This still isn't right. We need some way to track the previous MO-level bucket.
                if (oldTsd != null && oldTsd.hasDefinedBucketMode())
                {
                    oldMode = oldTsd.getBucketMode();
                } else
                {
                    // Otherwise, only the global setting would have been used by the
                    // MO that didn't define its own bucket setting.
                    oldMode = smartTrackManager.getGlobalTrackStyleDefinition().getBucketMode();
                }

                onTrackStyleContextBucketChange(tsc, oldMode);
            }
        }
    }

    /**
     * Respond to TSC bucket changes.
     *
     * @param tsc      The {@link TrackStylingContext} that has changed.
     * @param oldBucketMode The old bucket value.
     */
    private void onTrackStyleContextBucketChange(ITrackStylingContext tsc, BucketMode oldBucketMode)
    {
        // Find the map object that owns this TSC
        IMapObject mapObject = findMapObject(tsc);

        // If all RDM-level TSCs have been removed, then it will not be in this map, so we just ignore them
        if (mapObject == null) return;

        // Only update the buckets if the map object is currently viewable.
        if (mapObject.isVisible() && tsc.isVisible())
        {
            if (oldBucketMode != null) bucketReferences[oldBucketMode.ordinal()]--;
            bucketReferences[tsc.getBucketMode().ordinal()]++;
            updateColorScales();
        }
    }

    /**
     * Respond to TSC visibility changes.
     *
     * @param tsc       The {@link TrackStylingContext} that has changed.
     * @param visible Indicates if this current track is visible.
     */
    private void onTrackStyleContextVisibilityChange(ITrackStylingContext tsc, boolean visible)
    {
        // Find the map object that owns this TSC.
        IMapObject mapObject = findMapObject(tsc);

        // Only update the buckets if the map object is currently viewable.
        if (mapObject != null && mapObject.isVisible())
        {
            bucketReferences[tsc.getBucketMode().ordinal()] += visible ? 1 : -1;
            updateColorScales();
        }
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     *
     * @param event The event providing the added map object
     */
    private void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        onMapObjectAdded(event.getMapObject());
        updateColorScales();
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     *
     * @param event The event providing the removed map object
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        onMapObjectRemoved(event.getMapObject());
        updateColorScales();
    }

    /**
     * Add a map object, adding all required listeners and updating its bucket references.
     *
     * @param mo The map object to add.
     */
    private void onMapObjectAdded(IMapObject mo)
    {
        // Only devices with styled tracks affect color scales.
        if (mo.canHaveStyledTrackData())
        {
            final Integer moId = mo.getId();

            moVisible.set(moId, mo.isVisible()); // Initialize the current visibility of the MO
            mo.addPropertyChangeListener(IMapObject.VISIBILITY, mapObjectVisibilityChanged); // Listen to visibility changes

            // If this is the first MO, then add ourselves as a TSD assignment listener
            if (mapObjectToStylingContextMap.isEmpty()) smartTrackAssignmentManager.addAssignmentListener(this);

            mapObjectToStylingContextMap.put(mo, new HashSet<>()); // Prepare the map object to ITrackStylingContext set entry.
            addTSC(mo, smartTrackManager.getTrackStylingContext(mo)); // Start monitoring the new TSC.
        }
    }

    /**
     * Remove a map object, removing all listeners and updating the bucket references.
     *
     * @param mo The map object to remove.
     */
    private void onMapObjectRemoved(IMapObject mo)
    {
        if (mapObjectToStylingContextMap.containsKey(mo))
        {
            // Stop listening for visibility changes.
            mo.removePropertyChangeListener(IMapObject.VISIBILITY, mapObjectVisibilityChanged);

            // Remove each TSC associated with this map object
            // Using List.copyOf to avoid ConcurrentModificationExceptions since removeTSC modifies the list
            List.copyOf(mapObjectToStylingContextMap.get(mo)).forEach(tsc -> removeTSC(mo, tsc));

            // This must be done at the end because the call to removeTSC() above will use mapObjectToStylingContextMap.
            mapObjectToStylingContextMap.remove(mo);

            // If we are the last MO, then remove our TSD assignment listener
            if (mapObjectToStylingContextMap.isEmpty()) smartTrackAssignmentManager.removeAssignmentListener(this);
        }
    }

    /**
     * Change the visibility of a map object. Remove or add a references to buckets, but
     * leave all listeners alone. Note that the call to {@link #updateColorScales()} does
     * NOT happen in this method, as it may be used in a loop and {@link #updateColorScales()}
     * can be called once at the end of it.
     *
     * @param mo   The map object whose visibility should be updated.
     * @param show Boolean indicating if the map object's bucket reference should be counted.
     */
    private void onMapObjectVisible(IMapObject mo, boolean show)
    {
        // Update the bucket references for each TrackStylingContext. Only change the bucket references if the TSC is visible.
        mapObjectToStylingContextMap.get(mo).stream()
                .filter(ITrackStyle::isVisible)
                .forEach(tsc -> bucketReferences[tsc.getBucketMode().ordinal()] += (show ? 1 : -1));
    }

    /**
     * Update our control data based on the new visibility of a MO.
     */
    private void mapObjectVisibilityChanged(PropertyChangeEvent pce)
    {
        final IMapObject mapObject = (IMapObject) pce.getSource();
        final Integer moId = mapObject.getId();
        final boolean visible = (Boolean) pce.getNewValue();

        if (moVisible.get(moId) != visible)
        {
            onMapObjectVisible(mapObject, visible);
            updateColorScales();
            moVisible.set(moId, visible); // And update our records
        }
    }

    /**
     * Find a {@link IMapObject} by {@link ITrackStylingContext}.
     *
     * @param tsc The {@link ITrackStylingContext} for which the owning {@link IMapObject} needs to be found.
     * @return The {@link IMapObject} that owns the specified {@link ITrackStylingContext}.
     */
    private IMapObject findMapObject(ITrackStylingContext tsc)
    {
        return mapObjectToStylingContextMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(tsc))
                .findFirst()
                .map(Map.Entry::getKey).orElse(null);
    }

    /**
     * Find the {@link ITrackStylingContext} that owns the specified {@link ITrackStyleDefinition}.
     *
     * @param mapObject The {@link IMapObject} that owns the desired {@link ITrackStylingContext}.
     * @param tsdId The id of the {@link ITrackStyleDefinition} for which the owning {@link ITrackStylingContext}
     *              needs to be found.
     * @return The {@link ITrackStylingContext} that contains the specified {@link ITrackStyleDefinition}.
     */
    private ITrackStylingContext findTrackStylingContext(IMapObject mapObject, Integer tsdId)
    {
        for (ITrackStylingContext tsc : mapObjectToStylingContextMap.get(mapObject))
        {
            if (tsc.getTrackStyleDefinition().getId().equals(tsdId)) return tsc;
        }

        return null;
    }

    /**
     * Monitor a new {@link TrackStylingContext}, setting up the required mapping (MO->TSC) and making this class
     * listen for bucket mode change events. Note that the call to {@link #updateColorScales()} does NOT happen in
     * this method, as it may be used in a loop and {@link #updateColorScales()} can be called once at the end of it.
     *
     * @param mapObject The map object the {@link ITrackStylingContext} is owned by.
     * @param tsc  The new {@link ITrackStylingContext}.
     */
    private void addTSC(IMapObject mapObject, ITrackStylingContext tsc)
    {
        // Update the bucket references if the map object is currently viewable.
        if (mapObject.isVisible() && tsc.isVisible())
        {
            bucketReferences[tsc.getBucketMode().ordinal()]++;
        }

        // Map the map object to the TSC key.
        if (mapObjectToStylingContextMap.get(mapObject) != null)
        {
            mapObjectToStylingContextMap.get(mapObject).add(tsc);
            tsc.addPropertyChangeListener(trackStylingChanged, TSC_PROPERTY_LIST); // Listen for changes to the track style's bucket and visibility.
        } else
        {
            logger.error("ERROR - mapObjectToStylingContextMap is empty.  Can't add TSC.  Most likely this Layer was not cleaned up when a new project was opened");
        }
    }

    /**
     * Stop monitoring a {@link TrackStylingContext}, removing the mappings (MO->TSC)  and stopping this class from
     * listening to bucket mode change events on the {@link TrackStylingContext}. Note that the call to
     * {@link #updateColorScales()} does NOT happen in this method, as it may be used in a loop and
     * {@link #updateColorScales()} can be called once at the end of it.
     *
     * @param mapObject The map object the {@link ITrackStylingContext} is owned by.
     * @param tsc The track styling context to remove.
     */
    private void removeTSC(IMapObject mapObject, ITrackStylingContext tsc)
    {
        if (tsc == null) return;

        if (mapObject.isVisible() && tsc.isVisible())
        {
            bucketReferences[tsc.getBucketMode().ordinal()]--;
        }

        // Stop listening to TSC events.
        tsc.removePropertyChangeListener(trackStylingChanged, TSC_PROPERTY_LIST);

        // Remove the mapping from the map object to the old TSC key.
        mapObjectToStylingContextMap.get(mapObject).remove(tsc);
    }

    /**
     * Determine which bucket scales need to be drawn and the dimensions of
     * the resulting image. Draw the image.
     */
    private void updateColorScales()
    {
        List<LinearLayoutWidget> widgetList = new ArrayList<>();

        for (BucketMode bucketMode : BUCKET_MODES)
        {
            if (bucketReferences[bucketMode.ordinal()] > 0) widgetList.add(WIDGET_MAP.get(bucketMode));
        }

        updateLegend(stitchWidgets(widgetList));
    }

    @InvokeOnEDT
    private void updateLegend(LinearLayoutWidget widget)
    {
        if (currentWidget != null) removeChildWidget(currentWidget);
        currentWidget = widget;
        if (currentWidget == null) return;
        addChildWidget(currentWidget);
    }

    /**
     * Append multiple widgets together in a vertical layout.
     *
     * @param widgetList The list of widgets to append
     * @return A single widget that combines the widgets in {@code widgetList}.
     */
    private LinearLayoutWidget stitchWidgets(List<LinearLayoutWidget> widgetList)
    {
        if (widgetList.isEmpty()) return null;
        LinearLayoutWidget finalWidget = new LinearLayoutWidget();
        finalWidget.setOrientation(LinearLayoutWidget.VERTICAL);
        widgetList.forEach(e -> {
            e.setParent(null); // prevents an NPE in the kernel with reference to releasing textures of a non-existent widget (gov.tak.platform.commons.opengl.JOGLGLES.gl2() == null if not)
            finalWidget.addChildWidget(e);
        });
        return finalWidget;
    }

    @Override
    public void onTSDAssignedToRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm)
    {
        onTSDAssignedToRDM(oldTsdId, tsd != null ? tsd.getId() : null, rdm.getMapObject().getId(), rdm.getIdAsInt());
    }

    @Override
    public void onTSDAssignedToRDM(Integer oldTsdId, Integer tsdId, int moId, int rdmId)
    {
        final IMapObject mapObject = mapObjectPersist.findMapObjectById(moId);

        if (oldTsdId != null)
        {
            // Stop monitoring the old TSC.
            removeTSC(mapObject, findTrackStylingContext(mapObject, oldTsdId));
        }

        if (tsdId != null)
        {
            // Start monitoring the new TSC.
            final ITrackStylingContext tsc = renderingContext.getStyleContextManager().getTrackStylingContext(moId, tsdId);

            if (tsc != null)
            {
                addTSC(mapObject, tsc);
            } else
            {
                logger.debug("Looks like the MO was deleted: {}", mapObject);
            }
        }

        updateColorScales();
    }

    /**
     * @return The legend that will be used for the hour-of-day color scale.
     */
    private static LinearLayoutWidget createHoursLegendWidget()
    {
        return createLegendWidget(GlobeMarkerAttributes.COLOR_SCALE_HOUR_OF_DAY, i -> String.format("%02d:00", i), i -> i < 12);
    }

    /**
     * @return The legend that will be used for the day-of-week color scale.
     */
    private static LinearLayoutWidget createDaysLegendWidget()
    {
        final String[] weekdays = DateFormatSymbols.getInstance().getShortWeekdays();
        return createLegendWidget(GlobeMarkerAttributes.COLOR_SCALE_DAY_OF_WEEK, i -> weekdays[i + 1], i -> false);
    }

    /**
     * @return The widget that will be used for the month-of-year color scale.
     */
    private static LinearLayoutWidget createMonthLegendWidget()
    {
        final String[] months = DateFormatSymbols.getInstance().getShortMonths();
        return createLegendWidget(GlobeMarkerAttributes.COLOR_SCALE_MONTH_OF_YEAR, i -> months[i], i -> false);
    }

    /**
     * Construct a widget to display the color legend.
     *
     * @param colors Colors to use for each entry.  The length of this array determines the number of entries created.
     * @param labelGenerator Function to accept an entry index and return the text label to show for that entry
     * @param useOval Predicate to determine if the entry color marker should be oval or not
     */
    private static LinearLayoutWidget createLegendWidget(Color[] colors, IntFunction<String> labelGenerator,
                                                         IntPredicate useOval)
    {
        final LinearLayoutWidget legend = new LinearLayoutWidget();
        legend.setOrientation(LinearLayoutWidget.VERTICAL);
        legend.setBackingColor(TRANSLUCENT_BLACK);
        setEqualMarginsAndPadding(legend, 0);

        for (int i = 0; i < colors.length; i++)
        {
            legend.addChildWidget(createEntryWidget(colors[i], labelGenerator.apply(i).toUpperCase(), useOval.test(i)));
        }

        return legend;
    }

    /**
     * Draw the color indicator and label text for a legend entry.
     *
     * @param color     Color to use for entry indicator shape
     * @param label     Label text to draw beside indicator
     * @param useOval   Pass {@code true} to use an oval shape, {@code false} to use a rectangle
     */
    private static LinearLayoutWidget createEntryWidget(Color color, String label, boolean useOval)
    {
        LinearLayoutWidget entryWidget = new LinearLayoutWidget();
        entryWidget.setOrientation(LinearLayoutWidget.HORIZONTAL);

        final int h = ENTRY_HEIGHT - ENTRY_MARGIN; // Height of an entry

        BufferedImage buff = new BufferedImage(h, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = buff.createGraphics();
        g2d.setColor(color);

        if (useOval)
        {
            g2d.fillOval(0, 0, h, h);
        } else
        {
            g2d.fillRect(0, 0, h, h);
        }

        Bitmap colorBit = MarshalManager.marshal(ImageUtils.getScaledInstance(buff, h, h), BufferedImage.class, Bitmap.class);
        DrawableWidget colorWidget = new DrawableWidget(new BitmapDrawable(colorBit));
        setEqualMarginsAndPadding(colorWidget, ENTRY_MARGIN);
        colorWidget.setSize(h, h);
        entryWidget.addChildWidget(colorWidget);

        TextWidget textWidget = new TextWidget(label);
        setEqualMarginsAndPadding(textWidget, ENTRY_MARGIN);
        textWidget.setBackground(0);
        entryWidget.addChildWidget(textWidget);

        return entryWidget;
    }

    private static void setEqualMarginsAndPadding(IMapWidget widget, int value)
    {
        widget.setMargins(value, value, value, value);
        widget.setPadding(value, value, value, value);
    }
}
