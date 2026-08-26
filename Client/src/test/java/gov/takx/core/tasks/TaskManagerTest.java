package gov.takx.core.tasks;

import gov.takx.api.tasks.ATaskRunnable;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import gov.takx.test.mock.plugin.app.TestAppPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Some simple tests on PluginTask and ITaskRunnable
 *
 */
@TestMethodOrder(DependsOnOrdering.class)
public class TaskManagerTest extends AIntegrationTest
{
    private TestAppPlugin plugin;

    public TaskManagerTest()
    {
        setPluginsToRegister(TestAppPlugin.class);
    }

    /**
     * Use an app plugin for testing TaskRunnable.  While we are at it, test the app plugin delegate injection, since
     * no one else appears to be doing so.
     */
    @Test
    public void bringOnline()
    {
        plugin = getPluginInstance(TestAppPlugin.FAMILY, TestAppPlugin.TYPE);
        assertThat(plugin.getDelegate()).isNotNull();
    }

    /**
     * Test that a task is run after being submitted.
     */
    @Test
    @DependsOn("bringOnline")
    public void testTaskRun()
    {
        TestTaskRunnable taskRunnable = new TestTaskRunnable();
        plugin.getDelegate().submitTask(taskRunnable);
        assertThat(taskRunnable.taskRun).isTrue();
    }

    /**
     * Test that a tasks doCancel method is called after being cancelled in the wrapper
     */
    @Test
    @DependsOn("testTaskRun")
    public void testTaskCancelled()
    {
        TestTaskRunnable taskRunnable = new TestTaskRunnable();
        PluginTask pluginTask = new PluginTask(taskRunnable);

        ITaskManager taskManager = getBean(TaskManager.class);
        taskManager.execute(pluginTask);
        pluginTask.cancel();

        assertThat(taskRunnable.taskCancelled).isTrue();
    }

    /**
     * Test some other stuff
     */
    @Test
    @DependsOn("testTaskCancelled")
    public void testOtherStuff()
    {
        TestTaskRunnable taskRunnable = new TestTaskRunnable();
        PluginTask pluginTask = new PluginTask(taskRunnable);
        ITaskManager taskManager = getBean(TaskManager.class);
        taskManager.execute(pluginTask);

        assertThat(taskRunnable.taskRun).isTrue();
        assertThat(pluginTask.getExitMessage()).isEqualTo("Through The Gift Shop");
        assertThat(pluginTask.getTaskStatusHandler().isComplete()).isTrue();
        assertThat(taskManager.getExitAffectingTasks()).isEmpty(); // All tasks have executed
    }

    private static class TestTaskRunnable extends ATaskRunnable
    {
        private boolean taskRun = false;
        private boolean taskCancelled = false;

        @Override
        public String getName()
        {
            return "Test Task";
        }

        @Override
        public void doCancel()
        {
            taskCancelled = true;
        }

        @Override
        public void run()
        {
            monitor.setComplete();
            taskRun = true;
        }

        @Override
        public String getExitMessage()
        {
            return "Through The Gift Shop";
        }
    }
}
