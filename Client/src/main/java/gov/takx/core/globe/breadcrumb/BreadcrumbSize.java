package gov.takx.core.globe.breadcrumb;

/**
 * Defines the {@link gov.takx.api.breadcrumb.IBreadcrumb} rendering size options.
 *
 * @since 5.5
 */
public enum BreadcrumbSize
{
    Normal(20),
    Large(30),
    XLarge(45);

    private final int size;

    BreadcrumbSize(int size)
    {
        this.size = size;
    }

    /**
     * @return The size at which to render {@link gov.takx.api.breadcrumb.IBreadcrumb breadcrumbs}, in pixels
     */
    public int getSize()
    {
        return size;
    }
}
