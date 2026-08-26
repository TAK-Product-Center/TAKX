package gov.takx.core.globe;

import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import gov.nasa.worldwind.WorldWindowImpl;
import gov.nasa.worldwind.event.NoOpInputHandler;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.takx.api.globe.IWorldWindowGL;

import java.awt.Component;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.List;

/**
 * @since 1.49
 */
public final class HeadlessWorldWindow extends WorldWindowImpl implements IWorldWindowGL
{
    public HeadlessWorldWindow()
    {
        setInputHandler(new NoOpInputHandler());
        final BasicOrbitView view = new HeadlessBasicOrbitView(this);
        view.setEyePosition(Position.ZERO); // Set arbitrary eye position to avoid NPEs under test
        setView(view); // Install a view to avoid NPEs within unit tests
    }

    @Override
    public Component getComponent()
    {
        return null;
    }

    @Override
    public void setSize(int i, int i1)
    {
    }

    @Override
    public void repaint()
    {
    }

    @Override
    public void addComponentListener(ComponentListener componentListener)
    {
    }

    @Override
    public void removeComponentListener(ComponentListener componentListener)
    {
    }

    @Override
    public void addFocusListener(FocusListener focusListener)
    {
    }

    @Override
    public void removeFocusListener(FocusListener focusListener)
    {
    }

    @Override
    public void addHierarchyBoundsListener(HierarchyBoundsListener hierarchyBoundsListener)
    {
    }

    @Override
    public void removeHierarchyBoundsListener(HierarchyBoundsListener hierarchyBoundsListener)
    {
    }

    @Override
    public void addHierarchyListener(HierarchyListener hierarchyListener)
    {
    }

    @Override
    public void removeHierarchyListener(HierarchyListener hierarchyListener)
    {
    }

    @Override
    public void addInputMethodListener(InputMethodListener inputMethodListener)
    {
    }

    @Override
    public void removeInputMethodListener(InputMethodListener inputMethodListener)
    {
    }

    @Override
    public void addKeyListener(KeyListener keyListener)
    {
    }

    @Override
    public void removeKeyListener(KeyListener keyListener)
    {
    }

    @Override
    public void addMouseListener(MouseListener mouseListener)
    {
    }

    @Override
    public void removeMouseListener(MouseListener mouseListener)
    {
    }

    @Override
    public void addMouseMotionListener(MouseMotionListener mouseMotionListener)
    {
    }

    @Override
    public void removeMouseMotionListener(MouseMotionListener mouseMotionListener)
    {
    }

    @Override
    public void addMouseWheelListener(MouseWheelListener mouseWheelListener)
    {
    }

    @Override
    public void removeMouseWheelListener(MouseWheelListener mouseWheelListener)
    {
    }

    @Override
    public GLDrawable getDelegatedDrawable()
    {
        return null;
    }

    @Override
    public GLContext setContext(GLContext glContext, boolean b)
    {
        return null;
    }

    @Override
    public void addGLEventListener(GLEventListener glEventListener)
    {
    }

    @Override
    public void addGLEventListener(int i, GLEventListener glEventListener) throws IndexOutOfBoundsException
    {
    }

    @Override
    public int getGLEventListenerCount()
    {
        return 0;
    }

    @Override
    public GLEventListener getGLEventListener(int i) throws IndexOutOfBoundsException
    {
        return null;
    }

    @Override
    public boolean getGLEventListenerInitState(GLEventListener glEventListener)
    {
        return false;
    }

    @Override
    public void setGLEventListenerInitState(GLEventListener glEventListener, boolean b)
    {
    }

    @Override
    public GLEventListener disposeGLEventListener(GLEventListener glEventListener, boolean b)
    {
        return null;
    }

    @Override
    public GLEventListener removeGLEventListener(GLEventListener glEventListener)
    {
        return null;
    }

    @Override
    public void setAnimator(GLAnimatorControl glAnimatorControl) throws GLException
    {
    }

    @Override
    public GLAnimatorControl getAnimator()
    {
        return null;
    }

    @Override
    public Thread setExclusiveContextThread(Thread thread) throws GLException
    {
        return null;
    }

    @Override
    public Thread getExclusiveContextThread()
    {
        return null;
    }

    @Override
    public boolean invoke(boolean b, GLRunnable glRunnable)
    {
        return false;
    }

    @Override
    public boolean invoke(boolean b, List<GLRunnable> list)
    {
        return false;
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void display()
    {
    }

    @Override
    public void setAutoSwapBufferMode(boolean b)
    {
    }

    @Override
    public boolean getAutoSwapBufferMode()
    {
        return false;
    }

    @Override
    public void setContextCreationFlags(int i)
    {
    }

    @Override
    public int getContextCreationFlags()
    {
        return 0;
    }

    @Override
    public GLContext createContext(GLContext glContext)
    {
        return null;
    }

    @Override
    public GL getGL()
    {
        return null;
    }

    @Override
    public GL setGL(GL gl)
    {
        return null;
    }

    @Override
    public Object getUpstreamWidget()
    {
        return null;
    }

    @Override
    public void setRealized(boolean b)
    {
    }

    @Override
    public boolean isRealized()
    {
        return false;
    }

    @Override
    public int getSurfaceWidth()
    {
        return 0;
    }

    @Override
    public int getSurfaceHeight()
    {
        return 0;
    }

    @Override
    public boolean isGLOriented()
    {
        return false;
    }

    @Override
    public void swapBuffers() throws GLException
    {
    }

    @Override
    public GLCapabilitiesImmutable getChosenGLCapabilities()
    {
        return null;
    }

    @Override
    public GLProfile getGLProfile()
    {
        return null;
    }

    @Override
    public NativeSurface getNativeSurface()
    {
        return null;
    }

    @Override
    public long getHandle()
    {
        return 0;
    }

    @Override
    public GLDrawableFactory getFactory()
    {
        return null;
    }

    @Override
    public GLContext getContext()
    {
        return null;
    }

    @Override
    public boolean isEnableGpuCacheReinitialization()
    {
        return false;
    }

    @Override
    public void setEnableGpuCacheReinitialization(boolean enableGpuCacheReinitialization)
    {
    }

    @Override
    public boolean isThreadGLCapable()
    {
        return false;
    }

    @Override
    public boolean areAllGLEventListenerInitialized()
    {
        return true;
    }

    @Override
    public void flushGLRunnables()
    {
    }

    @Override
    public RecursiveLock getUpstreamLock()
    {
        return null;
    }

    @Override
    public GLCapabilitiesImmutable getRequestedGLCapabilities()
    {
        return null;
    }

    /**
     * Used in tests to avoid errors due to lastEyePosition being {@code null} (since the globe was never rendered when headless)
     */
    private static class HeadlessBasicOrbitView extends BasicOrbitView
    {
        public HeadlessBasicOrbitView(HeadlessWorldWindow headlessWorldWindow)
        {
            viewInputHandler.setWorldWindow(headlessWorldWindow);
        }

        @Override
        public Position getEyePosition()
        {
            // No way to set lastEyePosition without drawing, so just return arbitrary value to keep tests from throwing NPEs
            return Position.ZERO;
        }
    }
}
