package gov.takx.core.view.gui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLJPanel;
import gov.nasa.worldwind.Configuration;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.platform.ui.AStandardDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 */
public class WorldWindDiagnostics
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static class GLFrame extends JFrame implements GLEventListener
    {
        private final JTextArea outputArea;
        private final Consumer<String> receiver;

        GLFrame(Consumer<String> receiver)
        {
            this(null, receiver);
        }

        GLFrame(JTextArea outputArea)
        {
            this(outputArea, null);
        }

        GLFrame(JTextArea outputArea, Consumer<String> receiver)
        {
            this.outputArea = outputArea;
            this.receiver = receiver;

            GLCapabilities caps = new GLCapabilities(null);
            caps.setAlphaBits(8);
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            GLJPanel gljPanel = new GLJPanel(caps);
            gljPanel.addGLEventListener(this);
            add(gljPanel);
            setSize(200, 200);
        }

        private static class Attr
        {
            private Attr(Object attr, String name)
            {
                this.attr = attr;
                this.name = name;
            }

            private final Object attr;
            private final String name;
        }

        private static final Attr[] attrs = {
                new Attr(GL.GL_STENCIL_BITS, "stencil bits"),
                new Attr(GL.GL_DEPTH_BITS, "depth bits"),
                new Attr(GL2ES1.GL_MAX_TEXTURE_UNITS, "max texture units"),
                new Attr(GL2.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, "max texture image units"),
                new Attr(GL2.GL_MAX_TEXTURE_COORDS_ARB, "max texture coords"),
                new Attr(GL.GL_MAX_TEXTURE_SIZE, "max texture size"),
                new Attr(GL2GL3.GL_MAX_ELEMENTS_INDICES, "max elements indices"),
                new Attr(GL2GL3.GL_MAX_ELEMENTS_VERTICES, "max elements vertices"),
                new Attr(GL2ES1.GL_MAX_LIGHTS, "max lights")
        };

        public String getWWConfiguration()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(gov.nasa.worldwind.Version.getVersion()).append("\n");

            sb.append("\nSystem Properties\n");
            sb.append("Processors: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
            sb.append("Free memory: ").append(Runtime.getRuntime().freeMemory()).append(" bytes\n");
            sb.append("Max memory: ").append(Runtime.getRuntime().maxMemory()).append(" bytes\n");
            sb.append("Total memory: ").append(Runtime.getRuntime().totalMemory()).append(" bytes\n");

            System.getProperties().forEach((key, value) -> sb.append(key).append(" = ").append(value).append("\n"));

            com.jogamp.opengl.GL gl = GLContext.getCurrent().getGL();

            sb.append("\nOpenGL Values\n");
            String oglVersion = gl.glGetString(GL.GL_VERSION);
            sb.append("OpenGL version: ").append(oglVersion).append("\n");

            int[] intValues = new int[1];
            for (Attr attr : attrs)
            {
                String value = "";

                if (attr.attr instanceof Integer)
                {
                    gl.glGetIntegerv((Integer) attr.attr, intValues, 0);
                    value = Integer.toString(intValues[0]);
                }

                sb.append(attr.name).append(": ").append(value).append("\n");
            }

            String extensionString = gl.glGetString(GL.GL_EXTENSIONS);
            String[] extensions = extensionString.split(" ");
            sb.append("Extensions\n");
            for (String ext : extensions)
            {
                sb.append("    ").append(ext).append("\n");
            }

            sb.append("\nJOGL Values\n");
            String pkgName = "com.jogamp.opengl";
            try
            {
                final Class<?> glClass = getClass().getClassLoader().loadClass(pkgName + ".GL");
                final Package p = glClass.getPackage();

                if (p == null)
                {
                    sb.append("WARNING: Package.getPackage(").append(pkgName).append(") is null\n");
                } else
                {
                    sb.append(p).append("\n");
                    sb.append("Specification Title = ").append(p.getSpecificationTitle()).append("\n");
                    sb.append("Specification Vendor = ").append(p.getSpecificationVendor()).append("\n");
                    sb.append("Specification Version = ").append(p.getSpecificationVersion()).append("\n");
                    sb.append("Implementation Vendor = ").append(p.getImplementationVendor()).append("\n");
                    sb.append("Implementation Version = ").append(p.getImplementationVersion()).append("\n");
                }
            } catch (ClassNotFoundException e)
            {
                sb.append("Unable to load ").append(pkgName).append("\n");
            }

            return sb.toString();
        }

        @Override
        public void init(GLAutoDrawable glAutoDrawable)
        {
            final String wwConfiguration = getWWConfiguration();

            if (outputArea != null)
            {
                outputArea.setText(wwConfiguration);
            } else if (receiver != null)
            {
                receiver.accept(wwConfiguration);
            }
        }

        @Override
        public void display(GLAutoDrawable glAutoDrawable)
        {
            glAutoDrawable.getGL().glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            setVisible(false);
        }

        @Override
        public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height)
        {
        }

        @Override
        public void dispose(GLAutoDrawable glAutoDrawable)
        {
        }
    }

    private static class MainDialog extends AStandardDialog
    {
        private final JTextArea outputArea = new JTextArea(20, 80);

        private class RunAction extends AbstractAction
        {
            public RunAction()
            {
                super("Re-run Test");
            }

            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                outputArea.setText("");
                GLFrame glFrame = new GLFrame(outputArea);
                glFrame.setVisible(true);
            }
        }

        MainDialog()
        {
            setModal(true);

            try
            {
                JPanel mainPanel = new JPanel();
                mainPanel.setLayout(new BorderLayout());
                mainPanel.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
                JPanel outputContainer = new JPanel(new BorderLayout());
                mainPanel.add(outputContainer, java.awt.BorderLayout.CENTER);
                outputArea.setLineWrap(true);
                outputContainer.add(new JScrollPane(outputArea), java.awt.BorderLayout.CENTER);
                outputContainer.setBorder(new javax.swing.border.TitledBorder("Results"));

                getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

                JButton closeButton = new JButton("Close");
                closeButton.addActionListener(e -> dispose());

                JPanel bottomPanel = new JPanel();
                bottomPanel.setLayout(new FlowLayout());
                bottomPanel.add(closeButton);

                mainPanel.add(bottomPanel, java.awt.BorderLayout.SOUTH);

                ArrayList<JButton> buttons = new java.util.ArrayList<>();

                JPanel westPanel = new JPanel(new GridLayout(0, 1, 0, 10));
                westPanel.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
                JPanel opsPanel = new JPanel(new GridLayout(6, 1));
                opsPanel.setBorder(new javax.swing.border.TitledBorder("Operations"));

                Action[] operations = {new RunAction()};

                for (Action action : operations)
                {
                    JPanel p = new JPanel(new BorderLayout());
                    JButton jb = new JButton(action);
                    buttons.add(jb);
                    p.add(jb, BorderLayout.NORTH);
                    opsPanel.add(p);
                }

                westPanel.add(opsPanel);

                JPanel controlContainer = new JPanel(new BorderLayout());
                controlContainer.add(westPanel, BorderLayout.CENTER);

                getContentPane().add(controlContainer, BorderLayout.WEST);
                pack();

                Dimension dim = buttons.get(0).getSize();
                for (JButton btn : buttons)
                {
                    btn.setPreferredSize(dim);
                }

                ComponentUtils.centerComponentOnScreen(this);
                setResizable(true);
            } catch (Exception e)
            {
                logger.error("Failed to create diagnostics dialog", e);
            }
        }

        @Override
        protected void processWindowEvent(WindowEvent e)
        {
            super.processWindowEvent(e);

            if (e.getID() != WindowEvent.WINDOW_OPENED) return;
            GLFrame glFrame = new GLFrame(outputArea);
            glFrame.setVisible(true);
        }
    }

    static
    {
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Diagnostic Program");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
        }
    }

    public static void showInfo()
    {
        final MainDialog frame = new MainDialog();
        frame.setTitle("Graphics Diagnostic");
        frame.setVisible(true);
    }

    public static void getInfo(Consumer<String> receiver)
    {
        GLFrame glFrame = new GLFrame(receiver);
        glFrame.setVisible(true);
    }
}
