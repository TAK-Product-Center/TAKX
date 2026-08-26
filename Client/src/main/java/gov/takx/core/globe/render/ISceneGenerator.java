package gov.takx.core.globe.render;

import gov.nasa.worldwind.geom.Sector;

import java.util.List;

/**
 * Contract for renderable scene generators.
 *
 * @since 2.8
 */
public interface ISceneGenerator
{
    /**
     * Perform the calculations and determine the elements to be rendered in the scene.
     */
    void generateScene() throws InterruptedException;

    /**
     * @return The frame number
     */
    int getFrame();

    /**
     * @return The list of rendered detail tree sectors, used to annotate the scene with tree boundaries
     */
    List<Sector> getDetailTreeSectors();

    /**
     * @return {@code true} if the calculation was interrupted via {@link Thread#interrupt()}
     */
    boolean isInterrupted();
}
