package com.titanperf.util;

import java.util.function.Consumer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Utility for scheduling work to be executed on specific tick intervals.
 *
 * Many optimization tasks do not need to run every tick. The TickScheduler
 * allows spreading work across ticks to reduce per-tick overhead while
 * maintaining overall functionality.
 *
 * Usage patterns:
 * 1. Periodic tasks: Run a task every N ticks
 * 2. Deferred tasks: Schedule a task to run after N ticks
 * 3. Distributed work: Spread a large task across multiple ticks
 */
public class TickScheduler {

    /**
     * Queue of tasks waiting to execute.
     */
    private final ConcurrentLinkedQueue<ScheduledTask> pendingTasks;

    /**
     * Current tick counter.
     */
    private long currentTick;

    /**
     * Constructs a new TickScheduler.
     */
    public TickScheduler() {
        this.pendingTasks = new ConcurrentLinkedQueue<>();
        this.currentTick = 0;
    }

    /**
     * Advances the scheduler by one tick and executes due tasks.
     *
     * Call this once per game tick from the module's tick method.
     */
    public void tick() {
        currentTick++;

        // Process and remove tasks that are ready
        pendingTasks.removeIf(task -> {
            if (task.shouldExecute(currentTick)) {
                try {
                    task.execute();
                } catch (Exception e) {
                    // Log but don't crash on task failure
                }

                // Remove if not repeating, keep if repeating
                return !task.isRepeating();
            }
            return false;
        });
    }

    /**
     * Schedules a task to run once after a delay.
     *
     * @param delayTicks Number of ticks to wait before execution
     * @param action The action to execute
     */
    public void scheduleDelayed(int delayTicks, Runnable action) {
        pendingTasks.offer(new ScheduledTask(
                currentTick + delayTicks,
                0,
                action,
                false
        ));
    }

    /**
     * Schedules a task to run repeatedly at a fixed interval.
     *
     * @param intervalTicks Ticks between executions
     * @param action The action to execute
     */
    public void scheduleRepeating(int intervalTicks, Runnable action) {
        pendingTasks.offer(new ScheduledTask(
                currentTick + intervalTicks,
                intervalTicks,
                action,
                true
        ));
    }

    /**
     * Schedules a task to run repeatedly starting immediately.
     *
     * @param intervalTicks Ticks between executions
     * @param action The action to execute
     */
    public void scheduleRepeatingImmediate(int intervalTicks, Runnable action) {
        pendingTasks.offer(new ScheduledTask(
                currentTick,
                intervalTicks,
                action,
                true
        ));
    }

    /**
     * Cancels all pending tasks.
     */
    public void cancelAll() {
        pendingTasks.clear();
    }

    /**
     * Returns the number of pending tasks.
     *
     * @return Count of scheduled tasks
     */
    public int getPendingCount() {
        return pendingTasks.size();
    }

    /**
     * Returns the current tick count.
     *
     * @return Current tick
     */
    public long getCurrentTick() {
        return currentTick;
    }

    /**
     * Checks if a tick interval has elapsed.
     *
     * Utility method for modules that want simple interval checking
     * without full task scheduling.
     *
     * @param interval The tick interval to check
     * @return true if currentTick is a multiple of interval
     */
    public boolean isInterval(int interval) {
        return interval > 0 && currentTick % interval == 0;
    }

    /**
     * Represents a scheduled task.
     */
    private static class ScheduledTask {
        private long nextExecutionTick;
        private final int intervalTicks;
        private final Runnable action;
        private final boolean repeating;

        ScheduledTask(long nextExecutionTick, int intervalTicks,
                     Runnable action, boolean repeating) {
            this.nextExecutionTick = nextExecutionTick;
            this.intervalTicks = intervalTicks;
            this.action = action;
            this.repeating = repeating;
        }

        boolean shouldExecute(long currentTick) {
            return currentTick >= nextExecutionTick;
        }

        void execute() {
            action.run();
            if (repeating) {
                nextExecutionTick += intervalTicks;
            }
        }

        boolean isRepeating() {
            return repeating;
        }
    }
}
