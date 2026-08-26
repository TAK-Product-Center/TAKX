package gov.takx.core.view.gui.toolbar;

import gov.takx.core.view.gui.UIInstaller;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import java.awt.Container;
import java.awt.Label;

/**
 * Test for new {@link JScrollableToolBar} component.
 *
 * @since 3.3
 */
public class JScrollableToolBarTest
{
    public static void main(String[] args)
    {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        SwingUtilities.invokeLater(() -> {
            UIInstaller.installUI();

            JFrame frame = new JFrame("Scrollable Toolbar");
            final Container contentPane = frame.getContentPane();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

            frame.add(new Label("Regular JToolBar for reference:"));

            final JToolBar jToolBar = new JToolBar();
            jToolBar.setFloatable(false);
            jToolBar.add(new JButton("Button 1"));
            jToolBar.add(new JButton("Button 2"));
            jToolBar.add(new JToolBar.Separator());
            jToolBar.add(new JButton("Button 3"));
            jToolBar.add(new JButton("Button 4"));

            contentPane.add(jToolBar);

            contentPane.add(new Label("Custom JScrollableToolBar:"));

            final JScrollableToolBar jScrollableToolBar = new JScrollableToolBar();
            jScrollableToolBar.setFloatable(false);
            jScrollableToolBar.addTool(new JButton("Button 1"));
            jScrollableToolBar.addTool(new JButton("Button 2"));
            jScrollableToolBar.addTool(new JToolBar.Separator());
            jScrollableToolBar.addTool(new JButton("Button 3"));
            jScrollableToolBar.addTool(new JButton("Button 4"));

            contentPane.add(jScrollableToolBar);

            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}