package gov.takx.core.kml;

import com.atakmap.map.layer.feature.geometry.Envelope;
import de.micromata.opengis.kml.v_2_2_0.Container;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.LookAt;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.Snippet;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.explorer.Explorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.explorer.common.INonGroupableExplorable;
import gov.takx.core.explorer.common.PathExplorableGroup;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.kml.event.KmlModelAdded;
import gov.takx.core.kml.event.KmlModelRemoved;
import gov.takx.core.kml.event.NetworkLinkUpdated;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.globe.CameraEyeFactory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;

import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.FILE_OVERLAYS_PATH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;
import static gov.takx.platform.ui.UIResources.STANDARD_IMAGE_SIZE;

/**
 * Controller to handle the entries in Overlays for imported KML/KMZ documents.  Each imported file is represented
 * by a node underneath {@code File Overlays -> KML}.
 *
 * @see KmlModel
 * @since 5.5
 */
@ApplicationScoped
public class KmlOverlaysController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final BufferedImage KML_ICON = UIResources.getSizedImage(TAK_ASSETS_PNG, "KML.png", STANDARD_IMAGE_SIZE, true);
    private static final BufferedImage LINK_ICON = UIResources.getSizedImage(PLATFORM_ICONS, "network_link.png", STANDARD_IMAGE_SIZE, true);

    private final IExplorerManager explorerManager;
    private final IExtendedGlobeManager globeManager;
    private final NetworkLinkRefresher networkLinkRefresher;
    private final Event<KmlModelRemoved> kmlModelRemovedEvent;

    private final Comparator<Object> insertionOrderComparator = Comparator.comparing(node -> ((KmlFeatureExplorable) node).index);

    private IExplorer explorer;
    private KmlGroupExplorable kmlGroup;

    @Inject
    KmlOverlaysController(IExplorerManager explorerManager, IExtendedGlobeManager globeManager,
                          NetworkLinkRefresher networkLinkRefresher, Event<KmlModelRemoved> kmlModelRemovedEvent)
    {
        this.explorerManager = explorerManager;
        this.globeManager = globeManager;
        this.networkLinkRefresher = networkLinkRefresher;
        this.kmlModelRemovedEvent = kmlModelRemovedEvent;
    }

    /**
     * Must start after ExplorerManager.
     */
    @InvokeOnEDT
    private void startService(@Observes @Priority(ServiceOrdering.START_ExplorerManager + 1) StartProjectServices ignored)
    {
        explorer = explorerManager.getDefaultExplorerDelegate();
        kmlGroup = new KmlGroupExplorable(explorer.getController(), explorer.getController().getRoot().getOrCreatePath(FILE_OVERLAYS_PATH));
    }

    private void stopService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StopProjectServices ignored)
    {
        explorer = null;
        kmlGroup = null;
    }

    /**
     * Process a new KML model being imported.
     */
    @InvokeOnEDT
    private void onKmlModelAdded(@Observes KmlModelAdded event)
    {
        new TreeBuilder(event.kmlModel(), null).build();
    }

    /**
     * Process an update to a {@link NetworkLink}.
     */
    @InvokeOnEDT
    private void onNetworkLinkUpdated(@Observes NetworkLinkUpdated event)
    {
        new TreeBuilder(event.kmlModel(), event.networkLink()).build();
    }

    /**
     * Responsible for constructing the overlays tree (or subtree) for an imported KML model.
     */
    private class TreeBuilder
    {
        final KmlModel model;
        final KmlFeatureExplorable parentExplorable;
        final boolean refreshVisibility;
        final boolean parentVisible;

        private TreeBuilder(@Nonnull KmlModel model, @Nullable NetworkLink networkLink)
        {
            this.model = model;

            parentExplorable = networkLink == null ? null : model.getAttachment(networkLink, KmlFeatureExplorable.class);
            refreshVisibility = networkLink == null || Boolean.TRUE.equals(networkLink.isRefreshVisibility());
            parentVisible = parentExplorable == null || parentExplorable.isVisible();

            logger.debug("build {} refreshVisibility {} parentVisible {}", model.sourceFile(), refreshVisibility, parentVisible);
        }

        void build()
        {
            // If the imported file is being attached to a Network Link node (indicated by a non-null parentExplorable),
            // then we use a NetworkLinkExplorable instead of a KmlFileExplorable to represent it.

            final AKmlExplorable rootNode;
            if (parentExplorable == null)
            {
                rootNode = new KmlFileExplorable(model);
                kmlGroup.addChild(rootNode);
            } else
            {
                // Need to remove old nodes and add the new one
                parentExplorable.getChildren().forEach(parentExplorable::removeChild);

                rootNode = new NetworkLinkExplorable(model);
                rootNode.setVisible(refreshVisibility ? Boolean.TRUE.equals(parentExplorable.feature.isVisibility()) : parentVisible);
                parentExplorable.addChild(rootNode);
            }

            // Special handling for the root feature.  If it is a Document, then we don't want to create a child node of
            // the root node, and we set the root node's name to the document name, if it has one. Same for description.
            final var rootFeature = model.kml().getFeature();
            if (rootFeature instanceof Document document)
            {
                final String name = document.getName();
                if (!StringUtils.isBlank(name)) rootNode.setDisplayName(name);

                final String detailsText = makeDetailsText(document);
                if (!StringUtils.isBlank(detailsText)) rootNode.setDetailsTextHtml(detailsText);

                // No node for the document, add children directly to the root node
                document.getFeature().forEach(feature -> addFeature(feature, rootNode));
            } else
            {
                addFeature(rootFeature, rootNode);
            }
            explorer.expandTreeToNode(rootNode);
        }

        /**
         * Process a given feature, adding appropriate tree nodes based on the feature type.
         */
        private void addFeature(Feature feature, Explorable parent)
        {
            if (feature instanceof Container container)
            {
                addContainer(container, parent);
            } else if (feature instanceof NetworkLink networkLink)
            {
                // Add a reference to the explorable to the NetworkLink feature
                model.addAttachment(networkLink, addFeatureNode(feature, parent, true));
            } else
            {
                addFeatureNode(feature, parent, false);
            }
        }

        /**
         * Create an explorable node for the given container, and then add all its contained features to that node.
         *
         * @param container Container to process
         * @param parent Parent node to which to add the new container node
         */
        private void addContainer(Container container, Explorable parent)
        {
            final String folderName = container.getName();
            final String displayName = StringUtils.isBlank(folderName) ? "Folder" : folderName;

            final var containerNode = new KmlContainerExplorable(model, container, displayName, true);
            containerNode.setVisible(refreshVisibility ? Boolean.TRUE.equals(container.isVisibility()) : parentVisible);
            parent.addChild(containerNode);

            if (container instanceof Folder folder)
            {
                folder.getFeature().forEach(feature -> addFeature(feature, containerNode));
            } else if (container instanceof Document document)
            {
                document.getFeature().forEach(feature -> addFeature(feature, containerNode));
            }

        }

        /**
         * Add an explorable node for the given {@code feature} to the given {@code parent}.
         */
        private KmlFeatureExplorable addFeatureNode(Feature feature, Explorable parent, boolean asGroup)
        {
            final String featureName = feature.getName();
            final String displayName = StringUtils.isBlank(featureName) ? "[no name]" : featureName;
            final var featureNode = new KmlFeatureExplorable(model, feature, displayName, asGroup);
            featureNode.setVisible(refreshVisibility ? Boolean.TRUE.equals(feature.isVisibility()) : parentVisible);
            parent.addChild(featureNode);
            return featureNode;
        }
    }

    /**
     * @return the details text to should display for the given {@code feature}, based on the feature's description
     * or snippet, according to the KML specification. {@code null} is returned if the calculated text is empty.
     */
    @Nullable
    private static String makeDetailsText(@Nonnull Feature feature)
    {
        final String text;
        final int limit;

        // If there's a snippet, that overrides the description.  Note that there are two forms of the snippet
        // element, only differing in case, 'Snippet' vs. 'snippet'.  The OGC 2.2 standard deprecated 'Snippet',
        // but the Google documentation still shows it, and doesn't mention the 'snippet' element.  Also, the
        // 'Snippet' element has a 'maxLines' property, while the 'snippet' element is just a plain string.

        //noinspection deprecation
        final Snippet ucSnippet = feature.getSnippet(); // Deprecated, but supported
        final String lcSnippet = feature.getSnippetd(); // Preferred

        if (lcSnippet != null || ucSnippet != null)
        {
            final String snippetText = lcSnippet != null ? lcSnippet : ucSnippet.getValue();
            final int maxLines = ucSnippet != null ? ucSnippet.getMaxLines() : 0;

            // The snippet text doesn't support html markup, but we will be displaying it in a html document.
            text = StringUtils.isBlank(snippetText) ? "" : snippetText.replaceAll("\n", "<br>");
            limit = lcSnippet != null
                    ? 0
                    : maxLines > 0 ? maxLines : 2;
        } else
        {
            final String description = feature.getDescription();
            text = StringUtils.isBlank(description) ? "" : description;
            limit = 2; // Only show the first 2 lines of the description
        }

        int index = 0;
        int count = limit;
        while (index < text.length() && count-- > 0)
        {
            // Look for just "<br" (no closing ">") in case the text uses "<br/>"
            final int next = text.indexOf("<br", index);
            if (next < 0) break;
            index = next + 1;
        }

        final String result = index > 0
                ? text.substring(0, index)
                : text.isBlank() ? null : text;

        // Remove 'img' tags from the result.  Parity with Google Earth.
        return result != null ? result.replaceAll("<img\\s+[^>]*>", "") : null;
    }

    /**
     * Explorable for the {@code File Overlays -> KML} group node in the Overlays. Custom handling for deletion of
     * all imported KML files.
     */
    private static class KmlGroupExplorable extends PathExplorableGroup
    {
        public KmlGroupExplorable(@Nonnull IExplorerController controller, @Nullable IExplorable parent)
        {
            super(controller, parent, "KML");
            setIconImage(KML_ICON);
        }

        @Override
        public void requestDelete()
        {
            final int count = getChildrenCount();
            String message = "<html><center>Are you sure you want to delete ALL (" + count + ") imported KML files?</b>"
                    + "<br><font color=red>This action cannot be undone.</font></center></html>";
            final int confirm = AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0], message, "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION)
            {
                getChildren().forEach(child -> ((KmlFileExplorable) child).delete());
            }
        }
    }

    /**
     * Abstract base for our all of our explorables.
     *
     * @see IExplorable#getDetailsTextHtml()
     */
    private abstract static class AKmlExplorable extends Explorable implements INonGroupableExplorable
    {
        protected final KmlModel model;
        protected String detailsTextHtml;

        AKmlExplorable(@Nonnull KmlModel model, @Nonnull String displayName, @Nullable BufferedImage iconImage, boolean group)
        {
            super(displayName, null, iconImage, group);
            this.model = model;
        }

        @Nullable
        @Override
        public String getDetailsTextHtml()
        {
            return detailsTextHtml;
        }

        void setDetailsTextHtml(@Nullable String detailsTextHtml)
        {
            this.detailsTextHtml = detailsTextHtml;
        }
    }

    /**
     * Explorable representing an imported KML file. Deletion is supported.
     */
    private class KmlFileExplorable extends AKmlExplorable implements IExplorableGroup
    {
        KmlFileExplorable(KmlModel model)
        {
            super(model, model.sourceFile().getName(), KML_ICON, true);
            supportsDeletion = true;
            supportsSilentDeletion = true;
        }

        /**
         * Remove this explorable and fire a {@link KmlModelRemoved} event to notify others.
         */
        private void delete()
        {
            logger.debug("delete {}", this);
            explorer.removeExplorable(this, false);
            kmlModelRemovedEvent.fire(new KmlModelRemoved(model));
        }

        /**
         * @since 5.7
         */
        @Override
        public void deleteSilently()
        {
            delete();
        }

        @Override
        public void requestDelete()
        {
            final long descendantCount = getDescendantsStream().count();

            String message = "<html><center>Are you sure you want to delete <b>" + getDisplayName()
                    + " </b> containing " + descendantCount + " item" + (descendantCount != 1 ? "s" : "") + "?"
                    + "<br><font color=red>This action cannot be undone.</font></center></html>";
            final int confirm = AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0], message, "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) delete();
        }
    }

    /**
     * Explorable representing the content of a downloaded {@link NetworkLink}, it's parent will be a normal feature
     * explorable whose feature <em>is</em> the {@link NetworkLink}.
     */
    private static class NetworkLinkExplorable extends AKmlExplorable
    {
        NetworkLinkExplorable(KmlModel model)
        {
            super(model, model.sourceFile().getName(), KML_ICON, true);
            setSupportsDeletion(false);
        }
    }

    /**
     * Explorable representing a specific KML {@link Feature}.  Individual features cannot be deleted.
     * The given KML model will have been enriched by {@link KmlVisualManager} which adds
     * links to {@link KmlVisual visuals} in each visual feature's {@link Feature#getObjectSimpleExtension() simple extension}.
     */
    private class KmlFeatureExplorable extends AKmlExplorable
    {
        private static int NEXT_INDEX; // Tracks order within the KML document

        private final int index = ++NEXT_INDEX;
        private final Feature feature;
        private final KmlVisual kmlVisual;
        private boolean refreshPaused;

        KmlFeatureExplorable(KmlModel model, Feature feature, String displayName, boolean group)
        {
            super(model, displayName, feature instanceof NetworkLink ? LINK_ICON : null, group);

            this.feature = feature;
            kmlVisual = model.getAttachment(feature, KmlVisual.class);

            setSupportsDeletion(false);
            setDetailsTextHtml(makeDetailsText(feature));
        }

        @Override
        public void setVisible(boolean visible)
        {
            if (kmlVisual != null)
            {
                kmlVisual.setVisible(visible);
                super.setVisible(visible);
            }
        }

        @Override
        public boolean isVisible()
        {
            return kmlVisual != null ? kmlVisual.isVisible() : super.isVisible();
        }

        /**
         * On a double click (which invokes this method), perform a "go to" on the feature.
         */
        @Override
        public void showDialog()
        {
            final var goToAction = makeGoToAction();
            if (goToAction != null) goToAction.actionPerformed(null);
        }

        @Override
        public JPopupMenu createPopupMenu(@Nonnull MouseEvent mouseEvent)
        {
            final var goToAction = makeGoToAction();

            final boolean isNetworkLink = feature instanceof NetworkLink;
            final var refreshAction = isNetworkLink ? new RefreshAction() : null;
            final var toggleRefreshAction = isNetworkLink ? new ToggleRefreshAction() : null;

            if (goToAction != null || isNetworkLink)
            {
                final var menu = new JPopupMenu();
                if (goToAction != null) menu.add(new JMenuItem(goToAction));
                if (refreshAction != null) menu.add(new JMenuItem(refreshAction));
                if (toggleRefreshAction != null) menu.add(new JMenuItem(toggleRefreshAction));
                return menu;
            }
            return null;
        }

        /**
         * @return the root model from the containing KmlFileExplorable
         */
        private KmlModel getRootModel()
        {
            var rootModel = model;
            var parent = getParent();
            while (parent != null)
            {
                if (parent instanceof KmlFileExplorable fileExplorable)
                {
                    rootModel = fileExplorable.model;
                    break;
                }
                parent = parent.getParent();
            }
            return rootModel;
        }

        /**
         * @return An action to navigate the globe to the LookAt or Envelope for our feature, {@code null} if neither are available
         */
        @Nullable
        private AbstractAction makeGoToAction()
        {
            if (feature.getAbstractView() instanceof LookAt lookAt)
            {
                return new GoToLookAtAction(lookAt);
            } else if (kmlVisual != null)
            {
                final var envelope = kmlVisual.getEnvelope();
                if (envelope != null)
                {
                    return new GoToEnvelopeAction(envelope);
                }
            }
            return null;
        }

        /**
         * Action to refresh a Network Link on demand.
         */
        private class RefreshAction extends AbstractAction
        {
            RefreshAction()
            {
                super("Refresh Now");
            }

            @Override
            public void actionPerformed(ActionEvent ignored)
            {
                networkLinkRefresher.refresh(getRootModel(), (NetworkLink) feature, refreshPaused);
            }
        }

        /**
         * Action to pause/resume refresh operations for a Network Link.
         */
        private class ToggleRefreshAction extends AbstractAction
        {
            ToggleRefreshAction()
            {
                super(refreshPaused ? "Resume auto-refresh" : "Pause auto-refresh");
            }

            @Override
            public void actionPerformed(ActionEvent ignored)
            {
                final boolean wasPaused = refreshPaused;
                refreshPaused = !refreshPaused;

                if (wasPaused)
                {
                    networkLinkRefresher.refresh(getRootModel(), (NetworkLink) feature, false);
                } else
                {
                    networkLinkRefresher.pauseRefresh((NetworkLink) feature);
                }
            }
        }

        /**
         * Action to navigate the globe to a given {@link LookAt} configuration.
         */
        private class GoToLookAtAction extends AbstractAction
        {
            private final LookAt lookAt;

            GoToLookAtAction(LookAt lookAt)
            {
                super("Go To");
                this.lookAt = lookAt;
            }

            @Override
            public void actionPerformed(ActionEvent ignored)
            {
                final var cameraEye = CameraEyeFactory.createCameraEye(lookAt.getLatitude(), lookAt.getLongitude(),
                        lookAt.getAltitude(), lookAt.getHeading(), lookAt.getTilt(), lookAt.getRange());

                //noinspection DataFlowIssue
                globeManager.getLastFocusedGlobe().goTo(cameraEye);
            }
        }

        /**
         * Action to navigate the globe to a given {@link Envelope}.
         */
        private class GoToEnvelopeAction extends AbstractAction
        {
            private final Envelope envelope;

            GoToEnvelopeAction(Envelope envelope)
            {
                super("Go To");
                this.envelope = envelope;
            }

            @Override
            public void actionPerformed(ActionEvent ignored)
            {
                //noinspection DataFlowIssue
                globeManager.getLastFocusedGlobe().goTo(Sector.fromDegrees(envelope.minY, envelope.maxY, envelope.minX, envelope.maxX));
            }
        }
    }

    /**
     * Explorable to represent KML feature containers, ensuring they are always visible in the explorer tree.
     * 
     * @since 5.7
     */
    private class KmlContainerExplorable extends KmlFeatureExplorable implements IExplorableGroup
    {
        KmlContainerExplorable(KmlModel model, Feature feature, String displayName, boolean group)
        {
            super(model, feature, displayName, group);
            supportsVisibility = false;
        }
    }
}
