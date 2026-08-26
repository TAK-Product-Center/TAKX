package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.perspective.CControlPerspectiveBlop;
import bibliothek.gui.dock.common.perspective.CPerspective;
import bibliothek.gui.dock.dockable.DefaultDockablePerspective;
import bibliothek.gui.dock.perspective.PerspectiveElement;
import bibliothek.util.xml.XElement;
import bibliothek.util.xml.XIO;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.project.IProjectManager;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.stream.Stream;

import static gov.takx.core.configuration.ProgramEnvironment.DEFAULT_DOCKING_XML;
import static gov.takx.core.configuration.ProgramEnvironment.DOCKING_XML;

/**
 * Handler to manage the saving/loading of the Docking layout.
 *
 * @since 2.25
 */
@ApplicationScoped
public class DockingLayoutHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CONFIG = "config";
    private static final String RESOURCES = "resources";
    private static final String CLOSED_ELEMENT = "closed";
    private static final String SCREEN_CONFIG_ELEMENT = "screenconfig";
    private static final String UNPINNED_ELEMENT = "unpinned";
    private static final String PINNED_ELEMENT = "pinned";
    private static final String DOCKABLE_ID = "dockableId";
    private static final String PANEL = "panel";
    private static final String VALUE = "value";

    @Inject
    private IProjectManager projectManager;

    @Inject
    private IProgramEnvironment programEnvironment;

    @Inject
    private DockingManager dockingManager;

    @Inject
    private PinnedDockableManager pinnedDockableManager;

    @Inject
    @ProgramPreference(PreferenceEntry.CHECK_SCREEN_CONFIG)
    private boolean checkScreenConfig;

    /**
     * Save the current layout to disk.
     *
     * @param dockControl The dock control whose layout should be persisted
     */
    public void saveLayout(CControl dockControl)
    {
        if (dockControl == null) return;

        final File projectFile = new File(projectManager.getCurrentOpenProject().getProjectDirectory(), DOCKING_XML);
        final File globalFile = new File(programEnvironment.getDockingFilePath());

        try (FileOutputStream projectFOS = new FileOutputStream(projectFile);
             FileOutputStream globalFOS = new FileOutputStream(globalFile))
        {
            final XElement config = new XElement(CONFIG);
            dockControl.getResources().writeXML(config.addElement(RESOURCES));
            addXMLExtras(config);
            XIO.writeUTF(config, projectFOS);
            XIO.writeUTF(config, globalFOS);
        } catch (IOException e)
        {
            logger.error("Failed to write Docking layout", e);
        }
    }

    /**
     * Add our "extra" content to the XML structure.  This includes the lists of closed and pinned panels.
     *
     * @param config the xml content being constructed
     */
    private void addXMLExtras(XElement config)
    {
        final XElement closed = config.addElement(CLOSED_ELEMENT);
        final XElement unpinned = config.addElement(UNPINNED_ELEMENT);
        final XElement pinned = config.addElement(PINNED_ELEMENT);

        final boolean autoPin = pinnedDockableManager.isAutoPin();

        dockingManager.matchDockableAndProps(dap -> true).forEach(dap -> {
            final String dockableId = dap.getId();
            final ComponentDockable dockable = dap.getDockable();
            if (dockable == null)
            {
                closed.addElement(PANEL).addString(DOCKABLE_ID, dockableId);
            } else if (pinnedDockableManager.isPinned(dockable))
            {
                pinned.addElement(PANEL).addString(DOCKABLE_ID, dockableId);
            } else if (autoPin)
            {
                unpinned.addElement(PANEL).addString(DOCKABLE_ID, dockableId);
            }
        });

        config.addElement(SCREEN_CONFIG_ELEMENT).addElement("layout").addString(VALUE, getScreenConfig());
    }

    /**
     * Load the docking layout from the Docking.xml file. We also need to reload any panels that got closed during the
     * load process. The DockingFrames read method will close all of the dockables before loading them up again. And if
     * an app or other panel unknown to the file is closed it will not be automatically opened.
     * For example; auto-started apps after a fresh install when the Docking.xml file was created from the default.
     */
    @InvokeOnEDT
    public void loadLayout(@Nonnull CControl dockControl)
    {
        final XElement xml = findLayoutToLoad();

        if (xml != null)
        {
            XElement layout = xml.getElement(RESOURCES);
            layout = fixXML(dockControl, layout);
            dockControl.getResources().readXML(layout);
            loadPinnedSettings(xml);
            applyPostLoadRules(xml);

            if (logger.isDebugEnabled()) logger.debug("Docking layout loaded");
        }
    }

    /**
     * Find the XML that we need to load.
     *
     * @return XElement read from appropriate file, {@code null} if nothing found
     */
    @Nullable
    private XElement findLayoutToLoad()
    {
        File f = new File(projectManager.getCurrentOpenProject().getProjectDirectory(), DOCKING_XML);
        if (!f.exists())
        {
            f = new File(programEnvironment.getDockingFilePath());
        }

        if (logger.isDebugEnabled()) logger.debug("Loading docking layout from {}", f.getPath());

        XElement xml = readFrom(f);

        if (xml == null)
        {
            logger.warn("Failed to read docking layout from [{}]; will try reverting to default.", f.getAbsolutePath());
            xml = readFrom(new File(DEFAULT_DOCKING_XML));
        }

        if (xml == null)
        {
            logger.warn("Unable to load any docking layout.");
            return null;
        }

        if (checkScreenConfig && screenConfigIsChanged(xml))
        {
            logger.info("A screen layout change was detected. Reverting to default docking configuration.");
            xml = readFrom(new File(DEFAULT_DOCKING_XML));
        }

        return xml;
    }

    /**
     * Read the contents of the specified file into an XElement.
     *
     * @param file File to read
     * @return XElement from file, or {@code null} if it can't be read
     */
    @Nullable
    private XElement readFrom(@Nonnull File file)
    {
        try (FileInputStream fis = new FileInputStream(file))
        {
            return XIO.readUTF(fis);
        } catch (FileNotFoundException e)
        {
            logger.warn("docking settings file not found: {}", file);
        } catch (IOException e)
        {
            logger.error("Failed to load Docking layout: {}", file, e);
        }

        return null;
    }

    /**
     * Not really sure how this happens, but when it does the result is empty dockables in the flap dock. The following
     * code is merely cleaning those out when it finds them.
     *
     * @param input the "resource" node from the xml settings file
     * @return a scrubbed resource node
     */
    @Nonnull
    private XElement fixXML(@Nonnull CControl dockControl, @Nonnull XElement input)
    {
        boolean foundEvilElement = false;
        // loading the data without affecting the CControl.
        CControlPerspectiveBlop perspectives = dockControl.getPerspectives().readAllXML(input);

        // represents the layout that was shown when the file was stored
        CPerspective perspective = perspectives.getPerspective();

        // iterate over all elements, find the corrupted entries
        Iterator<PerspectiveElement> perspectiveElements = perspective.elements();
        while (perspectiveElements.hasNext())
        {
            PerspectiveElement next = perspectiveElements.next();
            // all instances of DefaultDockablePerspective are corrupted entries
            if (next instanceof DefaultDockablePerspective evil)
            {
                logger.warn("Evil element detected: {}", evil);
                // removing them from their parent will delete them from the layout
                evil.getParent().remove(evil);
                foundEvilElement = true;
            }
        }

        if (foundEvilElement)
        {
            // apply the fixed perspective
            perspectives.setPerspective(perspective);

            // write the settings back to some xml file
            XElement output = new XElement(input.getName());
            perspectives.writeXML(output);
            return output;
        }
        return input;
    }

    /**
     * Anything that is in the component map but has a {@code null} dockable should be loaded, provided it is not
     * in the closed list
     *
     * @param xml The main element being saved
     */
    private void applyPostLoadRules(@Nonnull XElement xml)
    {
        dockingManager.matchDockableAndProps(dap -> !dap.hasDockable() && !dockableIsClosed(dap.getId(), xml))
                .forEach(dap -> dockingManager.restoreComponent(dap));
    }

    /**
     * Check the saved xml to see if the supplied component was unpinned
     *
     * @param dockableID the id
     * @param xml        the xml
     * @return true if the panel is unpinned.
     * @since 2.24
     */
    private boolean dockableUnpinned(@Nonnull String dockableID, @Nonnull XElement xml)
    {
        final XElement unpinned = xml.getElement(UNPINNED_ELEMENT);

        return unpinned != null && Stream.of(unpinned.children())
                .filter(panel -> panel.getAttribute(DOCKABLE_ID) != null)
                .anyMatch(panel -> dockableID.equals(panel.getAttribute(DOCKABLE_ID).getValue()));
    }

    /**
     * Check the saved xml to see if the supplied component was pinned
     *
     * @param dockableID the id
     * @param xml        the xml
     * @return true if the panel is pinned.
     * @since 2.25
     */
    private boolean dockablePinned(@Nonnull String dockableID, @Nonnull XElement xml)
    {
        final XElement pinned = xml.getElement(PINNED_ELEMENT);

        return pinned != null && Stream.of(pinned.children())
                .filter(panel -> panel.getAttribute(DOCKABLE_ID) != null)
                .anyMatch(panel -> dockableID.equals(panel.getAttribute(DOCKABLE_ID).getValue()));
    }

    /**
     * Check the saved xml to see if the supplied component is closed
     *
     * @param dockableID the id
     * @param xml        the xml
     * @return true if closed
     */
    private boolean dockableIsClosed(@Nonnull String dockableID, @Nonnull XElement xml)
    {
        final XElement closed = xml.getElement(CLOSED_ELEMENT);

        return Stream.of(closed.children())
                .anyMatch(panel -> dockableID.equals(panel.getAttribute(DOCKABLE_ID).getValue()));
    }

    /**
     * Loads information about what panels should be pinned and unpinned.
     *
     * @param xml the xml
     */
    private void loadPinnedSettings(@Nonnull XElement xml)
    {
        boolean autoPin = UserPreferenceEntry.AUTO_PIN.getBooleanValue();

        dockingManager.findDockableAndPropsWithDockable()
                .filter(dap -> ((autoPin && !dockableUnpinned(dap.getId(), xml))
                        || dockablePinned(dap.getId(), xml)))
                .forEach(pinnedDockableManager::autoPin);

        pinnedDockableManager.setAutoPin(autoPin);
    }

    /**
     * Check to see if the layout of the monitors has changed since the last time we loaded the docking layout
     *
     * @param xml the current docking xml layout
     * @return return true if the layout is changed
     */
    private boolean screenConfigIsChanged(@Nonnull XElement xml)
    {
        boolean changed = true;
        XElement layout = xml.getElement(SCREEN_CONFIG_ELEMENT);
        if (layout != null)
        {
            String currConfig = getScreenConfig();
            String lastConfig = null;
            if (layout.getElementCount() > 0)
            {
                lastConfig = layout.getElement(0).getAttribute(VALUE).getValue();
            }

            if (currConfig.equals(lastConfig)) changed = false;
            if (changed && logger.isDebugEnabled())
            {
                logger.debug("Old Screen Config:{}", lastConfig);
                logger.debug("New Screen Config:{}", currConfig);
            }
        }
        return changed;
    }

    /**
     * Get a string representing the current screen layout
     *
     * @return the layout as a string
     */
    @Nonnull
    private String getScreenConfig()
    {
        StringBuilder screenConfig = new StringBuilder();

        // verify that it is not offscreen
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();

        for (GraphicsDevice curGs : gs)
        {
            GraphicsConfiguration[] gc = curGs.getConfigurations();
            for (GraphicsConfiguration curGc : gc)
            {
                screenConfig.append(curGc.getBounds());
            }
        }
        screenConfig.append(ScreenTaskbar.getTaskbar());

        return screenConfig.toString();
    }

    /**
     * Representation of the current taskbar location
     */
    private static final class ScreenTaskbar
    {
        /**
         * Taskbar location
         */
        private enum Location
        {
            TOP, RIGHT, BOTTOM, LEFT
        }

        /**
         * Get a Taskbar object representing the current taskbar location
         *
         * @return the Taskbar object
         */
        @Nonnull
        public static ScreenTaskbar getTaskbar()
        {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle windowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            return new ScreenTaskbar(windowBounds.x != 0 ? Location.TOP
                    : (windowBounds.y != 0 ? Location.LEFT
                            : (windowBounds.width == screenSize.width ? Location.BOTTOM : Location.RIGHT)),
                    screenSize.width - windowBounds.width, screenSize.height - windowBounds.height);
        }

        private final Location location;
        private final int width;
        private final int height;

        private ScreenTaskbar(Location location, int width, int height)
        {
            this.location = location;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString()
        {
            return "ScreenTaskbar{" +
                    "location=" + location +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}
