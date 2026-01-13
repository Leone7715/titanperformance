package com.titanperf.core.budget;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Frame time budget management system.
 *
 * This system tracks how much time is spent in each frame and ensures
 * that expensive operations don't cause frame drops. When the budget
 * is exceeded, work is deferred to future frames.
 *
 * Budget Allocation (for 60 FPS target = 16.67ms per frame):
 * - Rendering: 60% (10ms) - Drawing entities, chunks, particles
 * - Updates: 30% (5ms) - Chunk rebuilds, entity ticks
 * - Misc: 10% (1.67ms) - Input, audio, networking
 *
 * The system uses exponential moving average to smooth out frame time
 * measurements and adapt to changing performance conditions.
 *
 * Deferred Work System:
 * When operations exceed their budget, they can be queued for execution
 * in subsequent frames. This prevents "thundering herd" problems where
 * many expensive operations happen at once.
 */
public class FrameBudgetManager {

    /**
     * Singleton instance.
     */
    private static final FrameBudgetManager INSTANCE = new FrameBudgetManager();

    /**
     * Target frame time in nanoseconds (60 FPS = 16.67ms).
     */
    private static final long TARGET_FRAME_TIME_NS = 16_666_666L;

    /**
     * Target frame time in nanoseconds (30 FPS = 33.33ms).
     */
    private static final long TARGET_FRAME_TIME_30_NS = 33_333_333L;

    /**
     * Budget allocation percentages.
     */
    private static final float RENDER_BUDGET_PERCENT = 0.60f;
    private static final float UPDATE_BUDGET_PERCENT = 0.30f;
    private static final float MISC_BUDGET_PERCENT = 0.10f;

    /**
     * Minimum budget in nanoseconds (prevents starvation).
     */
    private static final long MIN_BUDGET_NS = 1_000_000L; // 1ms

    /**
     * Exponential moving average factor for frame time smoothing.
     */
    private static final float EMA_FACTOR = 0.1f;

    /**
     * Maximum deferred tasks per frame to prevent unbounded growth.
     */
    private static final int MAX_DEFERRED_PER_FRAME = 16;

    /**
     * Frame start timestamp.
     */
    private long frameStartTime;

    /**
     * Current frame's target time (adapts to FPS target).
     */
    private long currentTargetFrameTime;

    /**
     * Budgets in nanoseconds.
     */
    private long renderBudgetNs;
    private long updateBudgetNs;
    private long miscBudgetNs;

    /**
     * Time spent in each category this frame.
     */
    private long renderTimeSpent;
    private long updateTimeSpent;
    private long miscTimeSpent;

    /**
     * Smoothed average frame time.
     */
    private float averageFrameTime;

    /**
     * Last recorded frame time.
     */
    private long lastFrameTime;

    /**
     * Frame counter for statistics.
     */
    private long frameCount;

    /**
     * Deferred work queue.
     */
    private final Deque<DeferredTask> deferredTasks;

    /**
     * Work completed this frame counter.
     */
    private int workCompletedThisFrame;

    /**
     * Work deferred this frame counter.
     */
    private int workDeferredThisFrame;

    /**
     * Gets the singleton instance.
     */
    public static FrameBudgetManager getInstance() {
        return INSTANCE;
    }

    private FrameBudgetManager() {
        this.deferredTasks = new ArrayDeque<>();
        this.averageFrameTime = TARGET_FRAME_TIME_NS;
        this.currentTargetFrameTime = TARGET_FRAME_TIME_NS;
    }

    /**
     * Called at the start of each frame.
     * Sets up budgets and processes deferred work from previous frames.
     */
    public void beginFrame() {
        frameStartTime = System.nanoTime();
        frameCount++;

        // Reset per-frame counters
        renderTimeSpent = 0;
        updateTimeSpent = 0;
        miscTimeSpent = 0;
        workCompletedThisFrame = 0;
        workDeferredThisFrame = 0;

        // Calculate budgets based on target frame time
        renderBudgetNs = Math.max(MIN_BUDGET_NS, (long) (currentTargetFrameTime * RENDER_BUDGET_PERCENT));
        updateBudgetNs = Math.max(MIN_BUDGET_NS, (long) (currentTargetFrameTime * UPDATE_BUDGET_PERCENT));
        miscBudgetNs = Math.max(MIN_BUDGET_NS, (long) (currentTargetFrameTime * MISC_BUDGET_PERCENT));

        // Process some deferred tasks if we have headroom
        processDeferredTasks();
    }

    /**
     * Called at the end of each frame.
     * Records frame time for budget adaptation.
     */
    public void endFrame() {
        lastFrameTime = System.nanoTime() - frameStartTime;

        // Update exponential moving average
        averageFrameTime = averageFrameTime * (1.0f - EMA_FACTOR) + lastFrameTime * EMA_FACTOR;

        // Adapt target frame time based on recent performance
        // If we're consistently over budget, increase target (lower FPS)
        // If we're consistently under budget, decrease target (higher FPS)
        adaptTargetFrameTime();
    }

    /**
     * Adapts the target frame time based on recent performance.
     */
    private void adaptTargetFrameTime() {
        // If average is more than 20% over 60 FPS target, switch to 30 FPS target
        if (averageFrameTime > TARGET_FRAME_TIME_NS * 1.2f) {
            currentTargetFrameTime = TARGET_FRAME_TIME_30_NS;
        }
        // If average is consistently under 30 FPS target, switch back to 60 FPS
        else if (averageFrameTime < TARGET_FRAME_TIME_NS * 0.9f) {
            currentTargetFrameTime = TARGET_FRAME_TIME_NS;
        }
    }

    /**
     * Checks if there's remaining render budget.
     *
     * @return true if more rendering can be done within budget
     */
    public boolean hasRenderBudget() {
        long elapsed = System.nanoTime() - frameStartTime;
        return renderTimeSpent < renderBudgetNs && elapsed < currentTargetFrameTime;
    }

    /**
     * Checks if there's remaining update budget.
     *
     * @return true if more updates can be done within budget
     */
    public boolean hasUpdateBudget() {
        long elapsed = System.nanoTime() - frameStartTime;
        return updateTimeSpent < updateBudgetNs && elapsed < currentTargetFrameTime;
    }

    /**
     * Records time spent on rendering.
     *
     * @param nanoseconds Time spent in nanoseconds
     */
    public void addRenderTime(long nanoseconds) {
        renderTimeSpent += nanoseconds;
    }

    /**
     * Records time spent on updates.
     *
     * @param nanoseconds Time spent in nanoseconds
     */
    public void addUpdateTime(long nanoseconds) {
        updateTimeSpent += nanoseconds;
    }

    /**
     * Records time spent on miscellaneous operations.
     *
     * @param nanoseconds Time spent in nanoseconds
     */
    public void addMiscTime(long nanoseconds) {
        miscTimeSpent += nanoseconds;
    }

    /**
     * Executes work if budget allows, otherwise defers it.
     *
     * @param work The work to execute
     * @param estimatedTimeNs Estimated time the work will take
     * @param priority Priority for deferred execution (higher = sooner)
     * @return true if work was executed immediately
     */
    public boolean executeOrDefer(Runnable work, long estimatedTimeNs, int priority) {
        if (hasUpdateBudget()) {
            long start = System.nanoTime();
            work.run();
            addUpdateTime(System.nanoTime() - start);
            workCompletedThisFrame++;
            return true;
        } else {
            deferWork(work, priority);
            workDeferredThisFrame++;
            return false;
        }
    }

    /**
     * Defers work for execution in a future frame.
     *
     * @param work The work to defer
     * @param priority Priority (higher = executed sooner)
     */
    public void deferWork(Runnable work, int priority) {
        // Insert based on priority (simple insertion sort for small queues)
        DeferredTask task = new DeferredTask(work, priority, frameCount);

        if (deferredTasks.isEmpty() || priority <= deferredTasks.peekLast().priority) {
            deferredTasks.addLast(task);
        } else {
            // Find insertion point for higher priority
            ArrayDeque<DeferredTask> temp = new ArrayDeque<>();
            while (!deferredTasks.isEmpty() && deferredTasks.peekFirst().priority < priority) {
                temp.addLast(deferredTasks.pollFirst());
            }
            deferredTasks.addFirst(task);
            while (!temp.isEmpty()) {
                deferredTasks.addFirst(temp.pollLast());
            }
        }
    }

    /**
     * Processes deferred tasks from the queue.
     */
    private void processDeferredTasks() {
        int processed = 0;

        while (!deferredTasks.isEmpty() && processed < MAX_DEFERRED_PER_FRAME && hasUpdateBudget()) {
            DeferredTask task = deferredTasks.pollFirst();
            if (task != null) {
                long start = System.nanoTime();
                try {
                    task.work.run();
                } catch (Exception e) {
                    // Log but continue processing
                }
                addUpdateTime(System.nanoTime() - start);
                processed++;
            }
        }

        workCompletedThisFrame += processed;
    }

    /**
     * Gets the number of pending deferred tasks.
     *
     * @return Number of tasks in the deferred queue
     */
    public int getDeferredTaskCount() {
        return deferredTasks.size();
    }

    /**
     * Gets the percentage of update budget used this frame.
     *
     * @return Budget usage as a percentage (0-100+)
     */
    public float getUpdateBudgetUsage() {
        return (float) updateTimeSpent / updateBudgetNs * 100.0f;
    }

    /**
     * Gets the percentage of render budget used this frame.
     *
     * @return Budget usage as a percentage (0-100+)
     */
    public float getRenderBudgetUsage() {
        return (float) renderTimeSpent / renderBudgetNs * 100.0f;
    }

    /**
     * Gets the smoothed average frame time.
     *
     * @return Average frame time in nanoseconds
     */
    public float getAverageFrameTime() {
        return averageFrameTime;
    }

    /**
     * Gets the last frame time.
     *
     * @return Last frame time in nanoseconds
     */
    public long getLastFrameTime() {
        return lastFrameTime;
    }

    /**
     * Gets the current target frame time.
     *
     * @return Target frame time in nanoseconds
     */
    public long getTargetFrameTime() {
        return currentTargetFrameTime;
    }

    /**
     * Gets work completed this frame.
     *
     * @return Number of work items completed
     */
    public int getWorkCompletedThisFrame() {
        return workCompletedThisFrame;
    }

    /**
     * Gets work deferred this frame.
     *
     * @return Number of work items deferred
     */
    public int getWorkDeferredThisFrame() {
        return workDeferredThisFrame;
    }

    /**
     * Deferred task wrapper.
     */
    private static class DeferredTask {
        final Runnable work;
        final int priority;
        final long createdFrame;

        DeferredTask(Runnable work, int priority, long createdFrame) {
            this.work = work;
            this.priority = priority;
            this.createdFrame = createdFrame;
        }
    }
}
