package gov.takx.platform.tracks;

import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ISize;
import gov.takx.dataservices.persist.ConnectingLineDefinition;
import gov.takx.platform.schema.connectingline.ConnectingLineDefinitionType;
import gov.takx.platform.schema.connectingline.ConnectingLineDefinitionsCollection;
import gov.takx.platform.schema.connectingline.ScopeType;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder pattern for describing and building an {@link IConnectingLineDefinition}. Connecting
 * line definitions are by design immutable; to change an existing connecting line definition, use one of the cloning
 * constructors, {@link #ConnectingLineDefinitionBuilder(IConnectingLineDefinition, String)} or
 * {@link #ConnectingLineDefinitionBuilder(String, String, String, IConnectingLineDefinition)}.
 * <p/>
 * The uniqueKey assigned to a track style definition is truly arbitrary. Developers are encouraged to use a UUID
 * for the unique key.
 * <p/>
 * <b>Note:</b> For complete explanations of the various connecting line attributes, please refer to the JavaDocs found
 * in {@link IConnectingLineDefinition}.
 *
 * @since 1.12
 */
public class ConnectingLineDefinitionBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean reachBackToParent;
    private boolean hasDefinedLineColor;
    private boolean hasDefinedLineOpacity;
    private boolean hasDefinedLineWidth;
    private boolean hasDefinedLineStyle;
    private boolean hasDefinedElevationThreshold;
    private boolean hasDefinedDirectionalArrow;

    private String family;
    private String type;
    private String uniqueKey;

    private boolean isUserDefined = false;

    private String displayName = "New Connecting Line Definition";
    private Color lineColor = Color.WHITE;
    private boolean showDirectionalArrow = false;
    private float lineOpacity = 1.0f;
    private int lineWidth = ISize.NORMAL;
    private LineStyle lineStyle = LineStyle.None;
    private double elevationThreshold = 0.0;
    private IConnectingLineDefinition parent = null;

    /**
     * Create a map of track style definitions built from an XML document.  Each definition is in a pre-persist state,
     * allowing the additional definition(s) to occur before the styles are persisted by the core.
     *
     * @param xmlConnectingLineDefn URL that points to an XML document that can be validated against the
     *                              http://www.doe.gov/2011/RaptorTrackStyleSchema schema.
     * @return Map of pre-persist track style definitions contained within the XML document.
     * @throws IllegalArgumentException if URL is {@code null}
     * @throws IOException if the xmlTrackStyleDefn URL cannot be accessed
     * @throws JAXBException if the XML document pointed to by xmlTrackStyleDefn is invalid and cannot be
     *                                      bound to Java objects
     */
    public static Map<String, IConnectingLineDefinition> buildFromXML(@Nonnull URL xmlConnectingLineDefn)
            throws IOException, JAXBException
    {
        Map<String, IConnectingLineDefinition> defnMap = new HashMap<>();

        InputStream is = xmlConnectingLineDefn.openStream();
        ConnectingLineDefinitionsCollection lineCollection = processXML(is);

        for (ConnectingLineDefinitionType cld : lineCollection.getConnectingLineDefinition())
        {
            boolean reachBack = cld.getScope() == ScopeType.USER;
            ConnectingLineDefinitionBuilder builder = new ConnectingLineDefinitionBuilder(cld.getFamily(),
                    cld.getType(),
                    cld.getUniqueKey(),
                    reachBack);

            // In the case of XML, a user defined scope is automatically user-defined and supports reach-back
            builder.setUserDefined(reachBack);
            builder.setDisplayName(cld.getDisplayName());

            if (null != cld.getColor()) builder.setLineColor(new Color(cld.getColor(), true));
            if (null != cld.getElevationThreshold()) builder.setElevationThreshold(cld.getElevationThreshold());
            if (null != cld.getOpacity()) builder.setLineOpacity(cld.getOpacity());
            if (null != cld.getWidth())
            {
                switch (cld.getWidth())
                {
                    case MINIMUM -> builder.setLineWidth(ISize.MINIMUM);
                    case SMALL -> builder.setLineWidth(ISize.SMALL);
                    case NORMAL -> builder.setLineWidth(ISize.NORMAL);
                    case LARGE -> builder.setLineWidth(ISize.LARGE);
                    case MAXIMUM -> builder.setLineWidth(ISize.MAXIMUM);
                }
            }
            if (null != cld.isShowDirectionalArrow()) builder.setShowDirectionalArrow(cld.isShowDirectionalArrow());
            if (null != cld.getStyle())
            {
                switch (cld.getStyle())
                {
                    case SOLID -> builder.setLineStyle(LineStyle.Solid);
                    case DASHED, LONG_DASHED, DOTTED ->
                    {
                        logger.warn("The line style {} is not supported. Using {}", cld.getStyle().value(), LineStyle.Solid.name());
                        builder.setLineStyle(LineStyle.Solid);
                    }
                }
            }

            IConnectingLineDefinition defn = builder.build();

            if (logger.isDebugEnabled())
            {
                logger.debug("Successfully parsed connecting line definition {}", defn.getUniqueKey());
            }

            defnMap.put(defn.getUniqueKey(), defn);
        }

        return defnMap;
    }

    private static ConnectingLineDefinitionsCollection processXML(InputStream stream) throws JAXBException
    {
        JAXBContext context = JAXBContext.newInstance(ConnectingLineDefinitionsCollection.class.getPackage().getName());
        Unmarshaller unmarshaller = context.createUnmarshaller();

        JAXBElement<ConnectingLineDefinitionsCollection> root = unmarshaller.unmarshal(new StreamSource(stream), ConnectingLineDefinitionsCollection.class);
        return root.getValue();
    }

    /**
     * Constructs a new connecting line definition using strictly default values under the given context.
     *
     * @param family    Family
     * @param type      Type
     * @param uniqueKey Unique key, typically a UUID
     */
    public ConnectingLineDefinitionBuilder(@Nonnull String family, @Nonnull String type, @Nonnull String uniqueKey)
    {
        this(family, type, uniqueKey, false);
    }

    public ConnectingLineDefinitionBuilder(@Nonnull String family, @Nonnull String type, @Nonnull String uniqueKey, boolean reachBackToParent)
    {
        this.family = family;
        this.type = type;
        this.uniqueKey = uniqueKey;

        this.reachBackToParent = reachBackToParent;

        if (!reachBackToParent)
        {
            hasDefinedLineColor = true;
            hasDefinedLineOpacity = true;
            hasDefinedLineWidth = true;
            hasDefinedLineStyle = true;
            hasDefinedElevationThreshold = true;
            hasDefinedDirectionalArrow = true;
        }
    }

    /**
     * Clones an existing connecting line definition, maintaining the same family and type but replacing the uniqueKey
     * with a new value.  Any implicit reach back to a parent connecting line definition for an attribute value is lost
     * in the clone.  Even if the source connecting line definition used reach back, values at the time of creation
     * would be used to construct the cloned connecting line definition.
     *
     * @param connectingLineDefinition Model connecting line definition to clone attribute definitions from
     * @param uniqueKey                New unique key to assign, typically a UUID
     */
    public ConnectingLineDefinitionBuilder(@Nonnull IConnectingLineDefinition connectingLineDefinition, @Nonnull String uniqueKey)
    {
        this(connectingLineDefinition.getFamily(), connectingLineDefinition.getType(), uniqueKey, connectingLineDefinition);
    }

    /**
     * Clones an existing connecting line definition, fully replacing the model's family, type, and uniqueKey with the
     * new values specified; there is no implicit reach back to a parent connecting line definition. Even if the
     * source connecting line definition used reach back, values at the time of creation would be used to construct the
     * cloned track style definition.
     *
     * @param family                   New family to assign
     * @param type                     New type to assign
     * @param uniqueKey                New unique key to assign, typically a UUID
     * @param connectingLineDefinition Model connecting line definition to clone attribute definitions from
     */
    public ConnectingLineDefinitionBuilder(@Nonnull String family, @Nonnull String type, @Nonnull String uniqueKey,
                                           @Nonnull IConnectingLineDefinition connectingLineDefinition)
    {
        this(family, type, uniqueKey);

        cloneStyleDefinition(connectingLineDefinition);
    }

    private void cloneStyleDefinition(@Nonnull IConnectingLineDefinition connectingLineDefinition)
    {
        displayName = connectingLineDefinition.getDisplayName();
        lineColor = connectingLineDefinition.getLineColor();
        lineOpacity = connectingLineDefinition.getLineOpacity();
        lineStyle = connectingLineDefinition.getLineStyle();
        elevationThreshold = connectingLineDefinition.getElevationThreshold();
        lineWidth = connectingLineDefinition.getLineWidth();
        showDirectionalArrow = connectingLineDefinition.showDirectionalArrow();
    }

    public ConnectingLineDefinitionBuilder setUserDefined(boolean isUserDefined)
    {
        this.isUserDefined = isUserDefined;
        return this;
    }

    public boolean isUserDefined()
    {
        return isUserDefined;
    }

    public ConnectingLineDefinitionBuilder setDisplayName(@Nonnull String displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public ConnectingLineDefinitionBuilder setLineColor(@Nonnull Color lineColor)
    {
        this.lineColor = lineColor;
        hasDefinedLineColor = true;
        return this;
    }

    public ConnectingLineDefinitionBuilder setLineOpacity(float lineOpacity)
    {
        if (lineOpacity < 0.0f || lineOpacity > 1.0f)
        {
            throw new IllegalArgumentException("Opacity must be in the range 0.0 through 1.0, inclusive");
        }

        hasDefinedLineOpacity = true;
        this.lineOpacity = lineOpacity;
        return this;
    }

    public ConnectingLineDefinitionBuilder setLineStyle(@Nonnull LineStyle lineStyle)
    {
        hasDefinedLineStyle = true;
        this.lineStyle = lineStyle;
        return this;
    }

    public ConnectingLineDefinitionBuilder setLineWidth(int lineWidth)
    {
        if (lineWidth < 1 || lineWidth > 10)
        {
            throw new IllegalArgumentException("Line width must be in the range 1 through 10, inclusive");
        }

        hasDefinedLineWidth = true;
        this.lineWidth = lineWidth;
        return this;
    }

    public ConnectingLineDefinitionBuilder setShowDirectionalArrow(boolean showDirectionalArrow)
    {
        hasDefinedDirectionalArrow = true;
        this.showDirectionalArrow = showDirectionalArrow;
        return this;
    }

    public ConnectingLineDefinitionBuilder setElevationThreshold(int elevationThreshold)
    {
        if (elevationThreshold < 0)
        {
            throw new IllegalArgumentException("Parameter 'elevationThreshold' must be >= 0");
        }

        hasDefinedElevationThreshold = true;
        this.elevationThreshold = elevationThreshold;
        return this;
    }

    /**
     * Set the parent CLD.
     *
     * @param parent Parent to install
     * @since 2.28
     */
    public ConnectingLineDefinitionBuilder setParent(IConnectingLineDefinition parent)
    {
        this.parent = parent;
        return this;
    }

    public IConnectingLineDefinition build()
    {
        if (null == displayName)
        {
            String err = "A display name must be set on a connecting line definition";
            logger.error(err);
            throw new SmartTrackBuilderException(err);
        }

        ConnectingLineDefinition cld = new ConnectingLineDefinition(getFamily(), getType(), getUniqueKey(), isUserDefined());

        cld.setDisplayName(displayName);

        if (!reachBackToParent)
        {
            // Need to assign default values to ensure that there is NO REACH BACK

            cld.setDirectionalArrow(!hasDefinedDirectionalArrow || showDirectionalArrow);
            cld.setElevationThreshold(hasDefinedElevationThreshold ? elevationThreshold : 0);
            cld.setLineColor(hasDefinedLineColor ? lineColor : Color.WHITE);
            cld.setLineWidth(hasDefinedLineWidth ? lineWidth : ISize.NORMAL);
            cld.setLineOpacity(hasDefinedLineOpacity ? lineOpacity : 1.0f);
            cld.setLineStyle(hasDefinedLineStyle ? lineStyle : LineStyle.Solid);
        } else
        {
            // Need to leave all properties null EXCEPT for those properties explicitly set on the builder

            if (hasDefinedParent()) cld.setParent(getParent());
            if (hasDefinedDirectionalArrow) cld.setDirectionalArrow(showDirectionalArrow);
            if (hasDefinedElevationThreshold) cld.setElevationThreshold(elevationThreshold);
            if (hasDefinedLineColor) cld.setLineColor(lineColor);
            if (hasDefinedLineWidth) cld.setLineWidth(lineWidth);
            if (hasDefinedLineOpacity) cld.setLineOpacity(lineOpacity);
            if (hasDefinedLineStyle) cld.setLineStyle(lineStyle);
        }

        return cld;
    }

    public String getFamily()
    {
        return family;
    }

    public String getType()
    {
        return type;
    }

    public String getUniqueKey()
    {
        return uniqueKey;
    }

    public boolean isDirectionalArrowShown()
    {
        return showDirectionalArrow;
    }

    public int getLineWidth()
    {
        return lineWidth;
    }

    public LineStyle getLineStyle()
    {
        return lineStyle;
    }

    public float getLineOpacity()
    {
        return lineOpacity;
    }

    public Color getLineColor()
    {
        return lineColor;
    }

    public double getElevationThreshold()
    {
        return elevationThreshold;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public IConnectingLineDefinition getParent()
    {
        return parent;
    }

    public boolean isReachingBackToParent()
    {
        return reachBackToParent;
    }

    public boolean hasDefinedLineColor()
    {
        return hasDefinedLineColor;
    }

    public boolean hasDefinedLineOpacity()
    {
        return hasDefinedLineOpacity;
    }

    public boolean hasDefinedLineWidth()
    {
        return hasDefinedLineWidth;
    }

    public boolean hasDefinedLineStyle()
    {
        return hasDefinedLineStyle;
    }

    public boolean hasDefinedElevationThreshold()
    {
        return hasDefinedElevationThreshold;
    }

    public boolean hasDefinedDirectionalArrow()
    {
        return hasDefinedDirectionalArrow;
    }

    public boolean hasDefinedParent()
    {
        return parent != null;
    }

    @SuppressWarnings("unused")
    private ConnectingLineDefinitionBuilder()
    {
    }
}
