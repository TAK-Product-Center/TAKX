package gov.takx.api.surfaces;

/**
 * Represents the various attributes of an analytical surface.
 *
 * @since 1.4
 */
public interface ISurfaceAttributes
{
    boolean isDrawInterior();

    void setDrawInterior(boolean b);

    boolean isDrawOutline();

    void setDrawOutline(boolean b);

    boolean isDrawShadow();

    void setDrawShadow(boolean b);

    double getInteriorOpacity();

    void setInteriorOpacity(double v);

    double getOutlineOpacity();

    void setOutlineOpacity(double v);

    double getShadowOpacity();

    void setShadowOpacity(double v);

    double getOutlineWidth();

    void setOutlineWidth(double v);
}
