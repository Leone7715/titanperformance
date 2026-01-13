package com.titanperf.core.hardware;

/**
 * Represents the detected hardware capabilities of the system.
 *
 * The hardware profile is used throughout Titan Performance to make intelligent
 * decisions about optimization strategies. Different hardware configurations
 * benefit from different optimization approaches. For example, systems with
 * many CPU cores can use more aggressive parallelization, while systems with
 * limited RAM need more aggressive memory optimization.
 *
 * The profile categorizes systems into tiers for simplified decision making:
 * LOW: Older or budget hardware needing aggressive optimization
 * MEDIUM: Average gaming hardware with balanced optimization
 * HIGH: Modern gaming hardware with headroom for quality
 * ULTRA: High-end hardware where optimization is less critical
 *
 * Values in this class are populated by HardwareDetector during mod initialization
 * and remain constant for the lifetime of the game session.
 */
public class HardwareProfile {

    /**
     * Hardware tier classification for simplified decision making.
     */
    public enum HardwareTier {
        /**
         * Low-end hardware requiring aggressive optimization.
         * Typically older CPUs, integrated graphics, or less than 8GB RAM.
         * All optimization modules should be enabled with aggressive settings.
         */
        LOW,

        /**
         * Medium tier hardware with balanced optimization needs.
         * Typical gaming laptop or entry-level gaming desktop.
         * Most modules enabled with moderate settings.
         */
        MEDIUM,

        /**
         * High-end hardware with room for quality settings.
         * Modern gaming desktop with dedicated GPU.
         * Optimization focused on smoothness rather than raw FPS.
         */
        HIGH,

        /**
         * Ultra high-end hardware where optimization is optional.
         * Latest hardware with significant headroom.
         * Some aggressive optimizations may be disabled to preserve quality.
         */
        ULTRA
    }

    private final int cpuCores;
    private final int cpuThreads;
    private final long totalMemoryMB;
    private final long availableMemoryMB;
    private final long maxJvmMemoryMB;
    private final String cpuModel;
    private final String osName;
    private final String osVersion;
    private final String javaVersion;
    private final boolean is64Bit;
    private final HardwareTier tier;

    /**
     * Constructs a new HardwareProfile with the specified values.
     * This constructor is package-private because only HardwareDetector
     * should create profile instances.
     */
    HardwareProfile(int cpuCores, int cpuThreads, long totalMemoryMB,
                    long availableMemoryMB, long maxJvmMemoryMB,
                    String cpuModel, String osName, String osVersion,
                    String javaVersion, boolean is64Bit, HardwareTier tier) {
        this.cpuCores = cpuCores;
        this.cpuThreads = cpuThreads;
        this.totalMemoryMB = totalMemoryMB;
        this.availableMemoryMB = availableMemoryMB;
        this.maxJvmMemoryMB = maxJvmMemoryMB;
        this.cpuModel = cpuModel;
        this.osName = osName;
        this.osVersion = osVersion;
        this.javaVersion = javaVersion;
        this.is64Bit = is64Bit;
        this.tier = tier;
    }

    /**
     * Returns the number of physical CPU cores.
     * This determines the maximum parallelization for CPU-bound tasks.
     *
     * @return Number of physical cores
     */
    public int getCpuCores() {
        return cpuCores;
    }

    /**
     * Returns the number of logical CPU threads.
     * This may be higher than cores if hyperthreading is enabled.
     *
     * @return Number of logical threads
     */
    public int getCpuThreads() {
        return cpuThreads;
    }

    /**
     * Returns the total system memory in megabytes.
     *
     * @return Total RAM in MB
     */
    public long getTotalMemoryMB() {
        return totalMemoryMB;
    }

    /**
     * Returns the available system memory at detection time.
     * This is an approximation and changes as the system runs.
     *
     * @return Available RAM in MB at detection time
     */
    public long getAvailableMemoryMB() {
        return availableMemoryMB;
    }

    /**
     * Returns the maximum memory the JVM can use.
     * This is the -Xmx value and represents the ceiling for Java heap.
     *
     * @return Maximum JVM heap in MB
     */
    public long getMaxJvmMemoryMB() {
        return maxJvmMemoryMB;
    }

    /**
     * Returns the CPU model name if available.
     *
     * @return CPU model string or "Unknown" if not detectable
     */
    public String getCpuModel() {
        return cpuModel;
    }

    /**
     * Returns the operating system name.
     *
     * @return OS name such as "Windows 10" or "Linux"
     */
    public String getOsName() {
        return osName;
    }

    /**
     * Returns the operating system version.
     *
     * @return OS version string
     */
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * Returns the Java version string.
     *
     * @return Java version such as "21.0.1"
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * Returns whether the JVM is running in 64-bit mode.
     *
     * @return true if 64-bit JVM, false if 32-bit
     */
    public boolean is64Bit() {
        return is64Bit;
    }

    /**
     * Returns the calculated hardware tier.
     * Use this for simplified optimization decisions.
     *
     * @return The hardware tier classification
     */
    public HardwareTier getTier() {
        return tier;
    }

    /**
     * Returns the recommended number of worker threads for parallel operations.
     * This is calculated based on available CPU threads while leaving
     * headroom for the main game thread and system processes.
     *
     * @return Recommended thread count for worker pools
     */
    public int getRecommendedWorkerThreads() {
        // Leave at least 2 threads for main game thread and system
        // Use at most half of available threads for workers
        int available = cpuThreads - 2;
        int halfThreads = cpuThreads / 2;
        return Math.max(1, Math.min(available, halfThreads));
    }

    /**
     * Returns the recommended chunk render distance based on hardware.
     *
     * @return Recommended render distance in chunks
     */
    public int getRecommendedRenderDistance() {
        return switch (tier) {
            case LOW -> 6;
            case MEDIUM -> 10;
            case HIGH -> 16;
            case ULTRA -> 24;
        };
    }

    /**
     * Returns whether aggressive memory optimization should be enabled.
     * Systems with limited RAM benefit more from memory optimization
     * even though it may have a small CPU cost.
     *
     * @return true if aggressive memory optimization is recommended
     */
    public boolean shouldUseAggressiveMemoryOptimization() {
        return maxJvmMemoryMB < 4096 || tier == HardwareTier.LOW;
    }

    /**
     * Returns a human-readable summary of the hardware profile.
     *
     * @return Summary string for logging
     */
    public String getSummary() {
        return String.format(
            "Tier=%s, CPU=%d cores/%d threads, RAM=%dMB/%dMB JVM, OS=%s",
            tier, cpuCores, cpuThreads, maxJvmMemoryMB, totalMemoryMB, osName
        );
    }

    @Override
    public String toString() {
        return "HardwareProfile{" +
                "tier=" + tier +
                ", cpuCores=" + cpuCores +
                ", cpuThreads=" + cpuThreads +
                ", totalMemoryMB=" + totalMemoryMB +
                ", maxJvmMemoryMB=" + maxJvmMemoryMB +
                ", cpuModel='" + cpuModel + '\'' +
                ", osName='" + osName + '\'' +
                ", javaVersion='" + javaVersion + '\'' +
                ", is64Bit=" + is64Bit +
                '}';
    }
}
