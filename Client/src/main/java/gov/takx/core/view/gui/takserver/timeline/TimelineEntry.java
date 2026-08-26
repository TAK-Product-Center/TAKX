package gov.takx.core.view.gui.takserver.timeline;

import jakarta.annotation.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import java.util.function.Supplier;

/**
 * Holds the data required to display a data feed timeline entry.
 *
 * @since 5.0
 * @see DataFeedTimelinePanel
 */
public record TimelineEntry(ImageIcon icon, String title, String date, String description, @Nullable Supplier<JPanel> extraDetailsProvider)
{
}