package gov.takx.commons.swing;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.icon.EmptyIcon;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;

/**
 * A swing component that displays a list of items as "chips" (See <a
 * href="https://m3.material.io/components/chips/overview">Material Design - Chips</a>) - floating labels with an icon, text, and optionally
 * a "remove" button. Contents are driven by a {@link ListModel}. Icons and text are generated using provider functions. See
 * {@link #setIconProvider(Function)} and {@link #setTextProvider(Function)}. By default, the component uses a
 * {@link DefaultListModel} to manage and edit the list. When a remove button is clicked, the given element is removed from
 * the model. Consumers can register listeners on the model if they only need the item index, or register a
 * {@link RemoveListener} directly on this component if the actual removed item is needed. If an external list model is
 * supplied that is not a {@link DefaultListModel} instance, the field will operate in read-only mode (since
 * {@link ListModel} is not editable).
 * <p>
 * Note: While this component does not currently implement it, a fully functional chip field should have optional modes for
 * selecting and unselecting chips, and adding additional / custom chips. This component is built to not preclude those
 * features in the future.
 *
 * @param <T> the type of items in this field
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class ChipField<T> extends JPanel
{
    private final ListDataListener modelListener = new ModelListener();
    private final List<RemoveListener<T>> removeListeners = new ArrayList<>();
    private final Color chipBackground = new Color(UIManager.getColor("Button.background").getRGB());
    private final Border chipBorder = new CompoundBorder(UIManager.getBorder("Button.border"), new EmptyBorder(1, 4, 1, 2));
    private final Color chipHoverBackground = UIManager.getColor("Button.hoverBackground");
    private final Border chipHoverBorder = new CompoundBorder(new LineBorder(UIManager.getColor("Button.hoverBorderColor"), 1, true), new EmptyBorder(1, 4, 1, 2));
    private final ChipPanel emptyChip = new ChipPanel("", new EmptyIcon(SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT), null);
    @VisibleForTesting
    final List<ChipPanel> panels = new ArrayList<>();

    private ListModel<T> model;
    private DefaultListModel<T> editableModel; // A second copy of the model iff it is a DefaultListModel
    private Function<T, String> textProvider = Object::toString;
    private Function<T, Icon> iconProvider = o -> null;

    /**
     * Construct a new component with an empty {@link DefaultListModel}.
     */
    public ChipField()
    {
        initComponents();
        listPanel.setLayout(new WrapLayout(50));
        setModel(new DefaultListModel<>());
        listPanel.add(emptyChip);
    }

    /**
     * @return the list model
     */
    @Nonnull
    public ListModel<T> getModel()
    {
        return model;
    }

    /**
     * @param model the list model to use
     */
    public void setModel(@Nonnull ListModel<T> model)
    {
        if (this.model != null) this.model.removeListDataListener(modelListener);
        this.model = model;
        editableModel = model instanceof DefaultListModel<T> dlm ? dlm : null;
        this.model.addListDataListener(modelListener);
    }

    /**
     * @param elements the objects to set on the model, or {@code null} to clear the model
     * @throws IllegalStateException if the model is not editable
     */
    public void setModelElements(@Nullable Collection<? extends T> elements)
    {
        var model = getEditableModel();
        model.clear();
        if (elements != null) model.addAll(elements);
    }

    /**
     * @param element an object to add to the model
     * @throws IllegalStateException if the model is not editable
     */
    public void addModelElement(@Nonnull T element)
    {
        getEditableModel().addElement(element);
    }

    /**
     * @param element an object to remove from the model
     * @throws IllegalStateException if the model is not editable
     */
    public void removeModelElement(@Nonnull T element)
    {
        getEditableModel().removeElement(element);
    }

    /**
     * @return the List of objects in the model
     */
    @Nonnull
    public List<T> getModelElements()
    {
        return IntStream.range(0, model.getSize()).mapToObj(model::getElementAt).toList();
    }

    /**
     * @return the Function that provides chip icons
     */
    @Nonnull
    public Function<T, Icon> getIconProvider()
    {
        return iconProvider;
    }

    /**
     *
     * @param iconProvider the Function that should provide chip icons
     */
    public void setIconProvider(@Nonnull Function<T, Icon> iconProvider)
    {
        this.iconProvider = iconProvider;
    }

    /**
     * @return the Function that provides chip text
     */
    @Nonnull
    public Function<T, String> getTextProvider()
    {
        return textProvider;
    }

    /**
     *
     * @param textProvider the Function that should provide chip text
     */
    public void setTextProvider(@Nonnull Function<T, String> textProvider)
    {
        this.textProvider = textProvider;
    }

    /**
     * @return the editable list model
     * @throws IllegalStateException if the model is not editable
     */
    @Nonnull
    public DefaultListModel<T> getEditableModel()
    {
        if (editableModel != null)
        {
            return editableModel;
        } else
        {
            throw new IllegalStateException("Component is not using an editable model");
        }
    }

    /**
     * @return {@code true} if this component is using an editable model, otherwise {@code false}
     */
    public boolean isEditable()
    {
        return editableModel != null;
    }

    /**
     * @param listener a listener to receive events when chips are removed
     */
    public void addRemoveListener(@Nonnull RemoveListener<T> listener)
    {
        removeListeners.add(listener);
    }

    /**
     * @param listener a listener that should no longer receive events when chips are removed
     */
    @SuppressWarnings("unused")
    public void removeRemoveListener(@Nonnull RemoveListener<T> listener)
    {
        removeListeners.remove(listener);
    }

    private void addChip(T item)
    {
        listPanel.remove(emptyChip);
        var chip = new ChipPanel(item);
        panels.add(chip);
        listPanel.add(chip);
    }

    private void removeChip(int i)
    {
        var chip = panels.remove(i);
        listPanel.remove(chip);
        if (panels.isEmpty()) listPanel.add(emptyChip);
    }

    /**
     * Event fired when chips are removed.
     *
     * @param <T> the chip item type
     */
    public static class RemoveEvent<T> extends EventObject
    {
        private final T item;
        private final int index;

        private RemoveEvent(@Nonnull ChipField<T> field, @Nonnull T item, int index)
        {
            super(field);
            this.item = item;
            this.index = index;
        }

        /**
         * @return the chip item
         */
        @Nonnull
        public T getItem()
        {
            return item;
        }

        /**
         * @return the chip index
         */
        public int getIndex()
        {
            return index;
        }
    }

    /**
     * Listener called when chips are removed.
     *
     * @param <T> the chip item type
     */
    @FunctionalInterface
    public interface RemoveListener<T> extends EventListener
    {
        /**
         * @param event the remove event
         */
        void itemRemoved(@Nonnull RemoveEvent<T> event);
    }

    private class ModelListener implements ListDataListener
    {
        @Override
        public void intervalAdded(ListDataEvent e)
        {
            for (var i = e.getIndex0(); i <= e.getIndex1(); i++)
            {
                var item = model.getElementAt(i);
                addChip(item);
            }
            listPanel.revalidate();
            listPanel.repaint();
        }

        @Override
        public void intervalRemoved(ListDataEvent e)
        {
            for (var i = e.getIndex1(); i >= e.getIndex0(); i--)
            {
                removeChip(i);
            }
            listPanel.revalidate();
            listPanel.repaint();
        }

        @Override
        public void contentsChanged(ListDataEvent e)
        {
            listPanel.removeAll();
            for (var i = 0; i < model.getSize(); i++)
            {
                var item = model.getElementAt(i);
                addChip(item);
            }
            listPanel.revalidate();
            listPanel.repaint();
        }
    }

    @VisibleForTesting
    class ChipPanel extends JPanel
    {
        @VisibleForTesting
        final T item;
        @VisibleForTesting
        final JButton removeButton;
        @VisibleForTesting
        final JLabel label;

        public ChipPanel(T item)
        {
            this(textProvider.apply(item), iconProvider.apply(item), item);
        }

        public ChipPanel(String text, Icon icon, T item)
        {
            super(new FlowLayout(FlowLayout.CENTER, 1, 1));
            setOpaque(true);
            this.item = item;
            var empty = item == null;
            label = new JLabel(text, icon, SwingConstants.LEFT);
            label.setBorder(new EmptyBorder(0, 0, 0, 5));
            add(label);
            removeButton = new JButton();
            removeButton.setIcon(UIResources.CANCEL_ICON);
            removeButton.setBorder(new EmptyBorder(1, 1, 1, 1));
            removeButton.setBorderPainted(false);
            removeButton.setToolTipText("Remove");
            removeButton.setBackground(null);
            removeButton.setOpaque(false);
            removeButton.addActionListener(this::removeClicked);
            add(removeButton);
            setBorder(chipBorder);
            setBackground(chipBackground);
            if (!empty)
            {
                MouseRolloverListener.addRolloverListener(removeButton, (MouseEnteredExitedListener) this::removeEnteredExited);
            } else
            {
                // Some hackiness for the empty chip style
                removeButton.setVisible(false);
                setMinimumSize(new Dimension(0, icon.getIconHeight() + 6));
                setPreferredSize(getMinimumSize());
                setBackground(null);
            }
        }

        private void removeClicked(ActionEvent actionEvent)
        {
            if (item != null && isEditable())
            {
                var index = editableModel.indexOf(item);
                editableModel.remove(index);
                if (!removeListeners.isEmpty())
                {
                    var event = new RemoveEvent<>(ChipField.this, item, index);
                    removeListeners.forEach(listener -> listener.itemRemoved(event));
                }
            }
        }

        private void removeEnteredExited(MouseEvent event)
        {
            if (event.getID() == MouseEvent.MOUSE_ENTERED)
            {
                setBackground(chipHoverBackground);
                setBorder(chipHoverBorder);
            } else
            {
                setBackground(chipBackground);
                setBorder(chipBorder);
            }
            invalidate();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        listPanel = new JPanel();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== listPanel ========
        {
            listPanel.setBorder(new LineBorder(UIManager.getColor("Component.borderColor")));
            listPanel.setBackground(UIManager.getColor("Panel.background"));
            listPanel.setName("listPanel");
            listPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        }
        add(listPanel, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    @VisibleForTesting
    JPanel listPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
