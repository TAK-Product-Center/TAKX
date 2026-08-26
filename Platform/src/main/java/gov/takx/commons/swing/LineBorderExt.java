package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.border.LineBorder;

import java.awt.Color;

/**
 * Extension of LineBorder that allows setting the properties after construction.
 * 
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class LineBorderExt extends LineBorder
{
    /**
     * Construct a new instance with white line color, 1px thick, and square corners.
     */
    public LineBorderExt()
    {
        this(Color.WHITE, 1, false);
    }

    /**
     * Construct a new instance with the given color, 1px thick, and square corners.
     * 
     * @param color the border color
     */
    @SuppressWarnings("unused")
    public LineBorderExt(@Nonnull Color color)
    {
        this(color, 1);
    }

    /**
     * Construct a new instance with the given color and thickness, and square corners.
     * 
     * @param color the border color
     * @param thickness the border thickness
     */
    public LineBorderExt(@Nonnull Color color, int thickness)
    {
        this(color, thickness, false);
    }

    /**
     * Construct a new instance with the given color, thickness, and corner profile. 
     * 
     * @param color the border color
     * @param thickness the border thickness
     * @param roundedCorners {@code true} for rounded corners, {@code false} for square
     */
    public LineBorderExt(@Nonnull Color color, int thickness, boolean roundedCorners)
    {
        super(color, thickness, roundedCorners);
    }

    /**
     * @param lineColor the line color
     */
    public void setLineColor(@Nonnull Color lineColor)
    {
        this.lineColor = lineColor;
    }

    /**
     * @param thickness the line thickness
     */
    public void setThickness(int thickness)
    {
        this.thickness = thickness;
    }

    /**
     * @param roundedCorners {@code true} for rounded corners, {@code false} for square
     */
    public void setRoundedCorners(boolean roundedCorners)
    {
        this.roundedCorners = roundedCorners;
    }
}
