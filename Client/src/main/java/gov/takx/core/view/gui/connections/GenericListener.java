package gov.takx.core.view.gui.connections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Box;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeSelectionModel;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Monitors changes to any editable components in a Container's hierarchy. Helps developers avoid having
 * to register each editable Component to listen for changes that would require revalidation.
 *
 * @since 1.14
 */
public class GenericListener implements ActionListener, PropertyChangeListener, ItemListener,
        ContainerListener, DocumentListener, ChangeListener,
        ListSelectionListener, TreeSelectionListener, TableModelListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String VALUE_PROPERTY = "value"; // The key for a text field's name property
    private final Set<Component> listenedTo = new HashSet<>(); // The set of components we're listening to
    private final IUserInputValidator validator; // Handler for component change events
    private boolean ignoreEvents; // Tracks when a change event is being processed

    /**
     * Construct a generic listener on the given container.
     *
     * @param container The container to monitor
     * @param validator Validator to apply when any component change events occur
     * @since 5.1
     */
    public static void attachTo(Container container, IUserInputValidator validator)
    {
        //noinspection ResultOfObjectAllocationIgnored
        new GenericListener(container, validator);
    }

    /**
     * @param container The container to monitor
     * @param validator Handler to call whenever a component's value changes
     * @since 5.1 (made private)
     */
    private GenericListener(Container container, IUserInputValidator validator)
    {
        this.validator = validator;

        attachToHierarchyOf(container);
    }

    /**
     * Crawl the container's hierarchy and add listeners to all editable components.
     *
     * @param container The Container to listen to.
     */
    private void attachToHierarchyOf(Container container)
    {
        // Check to see if this listener has already been added to the top level container.
        if (!Arrays.asList(container.getContainerListeners()).contains(this)) container.addContainerListener(this);

        // Iterate over all the components in the container's hierarchy and add this listener
        for (Component component : container.getComponents()) attachTo(component);
    }

    /**
     * Check to see if the provided Component may be a Container that contains other components.
     *
     * @param c The Component to check.
     * @return {@code true} if the Component is likely a container.
     */
    private boolean isProbablyAContainer(Component c)
    {
        if (c instanceof JComponent)
        {
            return c instanceof JPanel || c instanceof JSplitPane || c instanceof JToolBar ||
                    c instanceof JViewport || c instanceof JScrollPane ||
                    c instanceof JRootPane || c instanceof JTabbedPane ||
                    c instanceof JInternalFrame || c instanceof JLayeredPane ||
                    c instanceof Box;
        } else
        {
            return c instanceof Container;
        }
    }

    /**
     * Add this listener to the specified Component and all the components in its hierarchy, recursively.
     *
     * @param c The Component to listen to.
     */
    private void attachTo(Component c)
    {
        // If it's a container, check the components in its hierarchy.
        if (isProbablyAContainer(c))
        {
            attachToHierarchyOf((Container) c);
        } else if (c instanceof JList<?> jList)
        {
            listenedTo.add(c);
            jList.addListSelectionListener(this);
        } else if (c instanceof JComboBox<?> jComboBox)
        {
            if (jComboBox.getEditor().getEditorComponent() instanceof JTextComponent textComponent)
            {
                textComponent.getDocument().addDocumentListener(this);
                listenedTo.add(textComponent);
            } else
            {
                jComboBox.addActionListener(this);
            }
        } else if (c instanceof JTree jTree)
        {
            listenedTo.add(c);
            jTree.getSelectionModel().addTreeSelectionListener(this);
        } else if (c instanceof JToggleButton jToggleButton)
        {
            jToggleButton.addItemListener(this);
        } else if (c instanceof JFormattedTextField)
        {
            // JFormattedTextField must be tested before JTextComponent
            c.addPropertyChangeListener(VALUE_PROPERTY, this);
        } else if (c instanceof JTextComponent jTextComponent)
        {
            listenedTo.add(c);
            jTextComponent.getDocument().addDocumentListener(this);
        } else if (c instanceof JColorChooser jColorChooser)
        {
            listenedTo.add(c);
            jColorChooser.getSelectionModel().addChangeListener(this);
        } else if (c instanceof JSpinner jSpinner)
        {
            jSpinner.addChangeListener(this);
        } else if (c instanceof JSlider jSlider)
        {
            jSlider.addChangeListener(this);
        } else if (c instanceof JTable jTable)
        {
            listenedTo.add(c);
            jTable.getSelectionModel().addListSelectionListener(this);
        } else if (logger.isTraceEnabled())
        {
            logger.trace("Don't know how to listen to a {}", c.getClass().getName());
        }

        if (logger.isTraceEnabled() && accept(c)) logger.trace("Begin listening to {}", c);
    }

    /**
     * Remove this listener from the specified Component and all the components in its hierarchy.
     *
     * @param c The component to stop listening to.
     */
    private void detachFrom(Component c)
    {
        listenedTo.remove(c);

        if (isProbablyAContainer(c))
        {
            detachFromHierarchyOf((Container) c);
        } else if (c instanceof JList<?> jList)
        {
            jList.removeListSelectionListener(this);
        } else if (c instanceof JComboBox<?> jComboBox)
        {
            if (jComboBox.getEditor().getEditorComponent() instanceof JTextComponent textComponent)
            {
                textComponent.getDocument().removeDocumentListener(this);
                listenedTo.remove(textComponent);
            } else
            {
                jComboBox.removeActionListener(this);
            }
        } else if (c instanceof JTree jTree)
        {
            jTree.getSelectionModel().removeTreeSelectionListener(this);
        } else if (c instanceof JToggleButton jToggleButton)
        {
            jToggleButton.removeActionListener(this);
        } else if (c instanceof JFormattedTextField)
        {
            // JFormattedTextField must be tested before JTextComponent
            c.removePropertyChangeListener(VALUE_PROPERTY, this);
        } else if (c instanceof JTextComponent jTextComponent)
        {
            jTextComponent.getDocument().removeDocumentListener(this);
        } else if (c instanceof JColorChooser jColorChooser)
        {
            jColorChooser.getSelectionModel().removeChangeListener(this);
        } else if (c instanceof JSpinner jSpinner)
        {
            jSpinner.removeChangeListener(this);
        } else if (c instanceof JSlider jSlider)
        {
            jSlider.removeChangeListener(this);
        } else if (c instanceof JTable jTable)
        {
            jTable.getSelectionModel().removeListSelectionListener(this);
        }

        if (logger.isTraceEnabled() && accept(c)) logger.trace("Stop listening to {}", c);
    }

    /**
     * Stop listening to a Container and all the components in its hierarchy.
     *
     * @param container The Container to stop listening to.
     */
    private void detachFromHierarchyOf(Container container)
    {
        container.removeContainerListener(this);

        for (Component component : container.getComponents()) detachFrom(component);
    }

    /**
     * Check to see if the Component is of a type that we are interested in listening to.
     *
     * @param c The component to check.
     * @return {@code true} iff the Component is of a type that we are interested in listening to.
     */
    private boolean accept(Component c)
    {
        // Must at least be a JComponent.
        if (!(c instanceof JComponent))
        {
            return false;
        }

        // Don't listen to cell editors, we can end up listening to them multiple times,
        // and the tree/table model will give us the event we need
        // noinspection SimplifiableIfStatement
        if (c instanceof TableCellEditor || c instanceof TreeCellEditor ||
                SwingUtilities.getAncestorOfClass(JTable.class, c) != null ||
                SwingUtilities.getAncestorOfClass(JTree.class, c) != null ||
                SwingUtilities.getAncestorOfClass(JList.class, c) != null)
        {
            return false;
        }

        return isProbablyAContainer(c) ||
                c instanceof JList ||
                c instanceof JComboBox ||
                c instanceof JTree ||
                c instanceof JToggleButton || //covers toggle, radio, checkbox
                c instanceof JTextComponent ||
                c instanceof JColorChooser ||
                c instanceof JSpinner ||
                c instanceof JSlider;
    }

    /**
     * Set a flag indicating that new events should be temporarily ignored.
     *
     * @param ignore {@code true} if event should be temporarily ignored, {@code false} otherwise.
     */
    private void setIgnoreEvents(boolean ignore)
    {
        ignoreEvents = ignore;
    }

    /**
     * Process an event. Not all events being processed will implement {@link java.util.EventObject}
     * (e.g. {@link DocumentEvent}), so just take a plain Java {@link Object} as the parameters. Some asynchronous
     * events that overlap will be ignored, but the risk is low and gain from not handling that case more gracefully
     * is significant.
     *
     * @param e The event to process.
     *                   todo Is it necessary to verify that the event came from a source that this listener is listening to?
     */
    private void fire(Object e)
    {
        // Verify that no other event is being processed.
        if (!ignoreEvents)
        {
            setIgnoreEvents(true);

            try
            {
                if (e instanceof TreeSelectionEvent treeSelectionEvent)
                {
                    var model = (TreeSelectionModel) treeSelectionEvent.getSource();

                    for (Component o : listenedTo)
                    {
                        if (o instanceof JTree jTree && jTree.getSelectionModel() == model)
                        {
                            validator.validateUserInput();
                            break;
                        }
                    }
                } else if (e instanceof DocumentEvent documentEvent)
                {
                    Document document = documentEvent.getDocument();

                    for (Component o : listenedTo)
                    {
                        if (o instanceof JTextComponent jTextComponent && jTextComponent.getDocument() == document)
                        {
                            validator.validateUserInput();
                            break;
                        }
                    }
                } else if (e instanceof ListSelectionEvent listSelectionEvent)
                {
                    if (listSelectionEvent.getSource() instanceof ListSelectionModel model)
                    {
                        for (Component o : listenedTo)
                        {
                            if (o instanceof JList<?> jList && jList.getSelectionModel() == model)
                            {
                                validator.validateUserInput();
                                break;
                            } else if (o instanceof JTable jTable && jTable.getSelectionModel() == model)
                            {
                                validator.validateUserInput();
                                break;
                            }
                        }
                    } else
                    {
                        validator.validateUserInput();
                    }
                } else
                {
                    validator.validateUserInput();
                }
            } finally
            {
                setIgnoreEvents(false);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        fire(e);
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
        fire(e);
    }

    @Override
    public void componentAdded(ContainerEvent e)
    {
        if (accept(e.getChild())) attachTo(e.getChild());
    }

    @Override
    public void componentRemoved(ContainerEvent e)
    {
        if (accept(e.getChild())) detachFrom(e.getChild());
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        fire(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        fire(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        fire(e);
    }

    @Override
    public void itemStateChanged(ItemEvent e)
    {
        fire(e);
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        fire(e);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        // We only care about value property changes on text fields.
        if (e.getSource() instanceof JFormattedTextField && VALUE_PROPERTY.equals(e.getPropertyName()))
        {
            fire(e);
        }
    }

    @Override
    public void tableChanged(TableModelEvent e)
    {
        fire(e);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e)
    {
        fire(e);
    }
}
