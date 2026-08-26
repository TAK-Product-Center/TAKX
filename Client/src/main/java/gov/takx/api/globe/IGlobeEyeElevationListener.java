package gov.takx.api.globe;

/**
 * Date: Jan 21, 2010
 * Time: 2:23:10 PM
 * Subject to terms of Non-disclosure agreement
 */
public interface IGlobeEyeElevationListener
{
    /**
     * Fired whenever the eye elevation changes on the globe.
     *
     * @param newElevation - the globes new eye elevation
     */
    public void elevationChanged(double newElevation);
}
