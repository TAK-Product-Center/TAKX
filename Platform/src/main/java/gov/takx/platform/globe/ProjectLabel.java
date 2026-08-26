package gov.takx.platform.globe;

import gov.takx.api.globe.IProjectLabel;
import gov.takx.api.globe.IProjectLabelService;
import gov.takx.api.globe.ProjectLabelLocation;

import java.awt.Color;

/**
 * Definition of a project label (screen annotation).  The label configuration includes its text, color, size, and
 * visibility.
 *
 * @see IProjectLabelService
 * @see ProjectLabelLocation
 * @since 1.4
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ProjectLabel implements IProjectLabel
{
    private String label;
    private boolean visible;
    private int size;
    private Color color;

    public ProjectLabel(String label)
    {
        this(label, IProjectLabel.DEFAULT_SIZE);
    }

    public ProjectLabel(String label, int size)
    {
        this(label, size, IProjectLabel.DEFAULT_COLOR);
    }

    public ProjectLabel(String label, int size, Color color)
    {
        this(label, size, color, true);
    }

    public ProjectLabel(String label, int size, Color color, boolean visible)
    {
        this.color = color;
        this.label = label;
        this.size = size;
        this.visible = visible;
    }

    @Override
    public Color getColor()
    {
        return color;
    }

    @Override
    public void setColor(Color color)
    {
        this.color = color;
    }

    @Override
    public String getLabel()
    {
        return label;
    }

    @Override
    public void setLabel(String label)
    {
        this.label = label;
    }

    @Override
    public int getSize()
    {
        return size;
    }

    @Override
    public void setSize(int size)
    {
        this.size = size;
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    @Override
    public String toString()
    {
        return "ProjectLabel{" +
                "color=" + color +
                ", label='" + label + '\'' +
                ", visible=" + visible +
                ", size=" + size +
                '}';
    }
}
