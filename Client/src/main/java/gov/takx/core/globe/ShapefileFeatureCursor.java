package gov.takx.core.globe;

import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataSource.Content;
import com.atakmap.map.layer.feature.FeatureDataSource.Content.ContentPointer;
import com.atakmap.map.layer.feature.FeatureDataStore3;
import com.atakmap.map.layer.feature.FeatureDefinition;
import com.atakmap.map.layer.feature.FeatureDefinition2;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.ogr.style.FeatureStyleParser;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.nasa.worldwind.util.WWUtil;
import gov.tak.platform.graphics.Color;

/**
 * A FeatureCursor for content that renders shape colors in accordance with existing Shapefile behavior.
 *
 * @since 3.4
 */
class ShapefileFeatureCursor implements FeatureCursor
{
    private static int shapeFileColorIndex = 0;
    private static final int[] SHAPEFILE_COLORS = {Color.YELLOW, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.MAGENTA};

    private final Content content;
    private final long fsid;
    private boolean initialized;
    private boolean isPolygon;
    private int color;
    private FeatureDataSource.FeatureDefinition def;
    private boolean isClosed;

    final Envelope.Builder envBuilder;

    public ShapefileFeatureCursor(Content content, long fsid, Envelope.Builder envBuilder)
    {
        this.content = content;
        this.fsid = fsid;
        this.envBuilder = envBuilder;
    }

    @Override
    public Object getRawGeometry()
    {
        return def.getRawGeometry();
    }

    @Override
    public int getGeomCoding()
    {
        return def.getGeomCoding();
    }

    @Override
    public String getName()
    {
        return def.getName();
    }

    @Override
    public int getStyleCoding()
    {
        return def.getStyleCoding();
    }

    @Override
    public Object getRawStyle()
    {
        return createStyle(def, color);
    }

    @Override
    public AttributeSet getAttributes()
    {
        return def.getAttributes();
    }

    @Override
    public Feature get()
    {
        return def.get();
    }

    @Override
    public long getId()
    {
        return FeatureDataStore3.FEATURE_ID_NONE;
    }

    @Override
    public long getVersion()
    {
        return 0;
    }

    @Override
    public long getFsid()
    {
        return fsid;
    }

    @Override
    public boolean moveToNext()
    {
        boolean success = content.moveToNext(ContentPointer.FEATURE);
        if (success)
        {
            def = content.get();
            if (!initialized)
            {
                isPolygon = isPolygon(def);
                if (!isPolygon) color = nextColor();
                initialized = true;
            }
            if (isPolygon) color = nextColor();
            envBuilder.add(def.get().getGeometry().getEnvelope());
        }
        return success;
    }

    @Override
    public void close()
    {
        isClosed = true;
    }

    @Override
    public boolean isClosed()
    {
        return isClosed;
    }

    private static Object createStyle(FeatureDefinition def, int color)
    {
        int fillColor = 0x7F000000 | (0x00FFFFFF & color); // set alpha to 50%
        BasicFillStyle fillStyle = new BasicFillStyle(fillColor);
        int strokeColor = WWUtil.makeColorBrighter(new java.awt.Color(color)).getRGB();
        BasicStrokeStyle strokeStyle = new BasicStrokeStyle(strokeColor, 2);
        Style style = new CompositeStyle(new Style[]{fillStyle, strokeStyle});
        return def.getStyleCoding() == FeatureDefinition2.STYLE_OGR ? FeatureStyleParser.pack(style) : style;
    }

    private static boolean isPolygon(FeatureDefinition def)
    {
        return def.get().getGeometry() instanceof Polygon;
    }

    private static int nextColor()
    {
        return SHAPEFILE_COLORS[shapeFileColorIndex++ % SHAPEFILE_COLORS.length];
    }
}
