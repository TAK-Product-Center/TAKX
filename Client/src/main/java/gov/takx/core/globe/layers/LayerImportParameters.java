package gov.takx.core.globe.layers;

/**
 * @param copyToVfs {@code true} to copy the imagery to the VFS prior to importing.
 *                  {@code false} to keep the imagery in its user-provided location.
 * @since 5.7
 */
public record LayerImportParameters(boolean copyToVfs)
{
}