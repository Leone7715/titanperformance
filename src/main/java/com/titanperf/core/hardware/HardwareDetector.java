package com.titanperf.core.hardware;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

/**
 * Detects hardware capabilities and creates a HardwareProfile.
 *
 * Hardware detection runs once during mod initialization and collects
 * information about the system that remains constant during the session.
 * This information drives auto-configuration decisions and allows modules
 * to adapt their optimization strategies to available resources.
 *
 * Detection methods vary by operating system. Some values may not be
 * available on all platforms and will be estimated or set to defaults.
 * The detector is designed to fail gracefully if system information
 * is not accessible due to security restrictions.
 *
 * Implementation Note: This class uses com.sun.management APIs which
 * are available in standard Oracle/OpenJDK distributions but may not
 * be present in alternative JVMs. Fallback logic handles these cases.
 */
public final class HardwareDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("TitanPerf/Hardware");

    /**
     * Bytes per megabyte for memory calculations.
     */
    private static final long BYTES_PER_MB = 1024L * 1024L;

    /**
     * Private constructor prevents instantiation of utility class.
     */
    private HardwareDetector() {
    }

    /**
     * Performs hardware detection and returns a HardwareProfile.
     * This method should be called once during mod initialization.
     *
     * @return A HardwareProfile containing detected hardware information
     */
    public static HardwareProfile detect() {
        LOGGER.info("Starting hardware detection");

        int cpuCores = detectCpuCores();
        int cpuThreads = detectCpuThreads();
        String cpuModel = detectCpuModel();
        long totalMemoryMB = detectTotalMemory();
        long availableMemoryMB = detectAvailableMemory();
        long maxJvmMemoryMB = detectMaxJvmMemory();
        String osName = System.getProperty("os.name", "Unknown");
        String osVersion = System.getProperty("os.version", "Unknown");
        String javaVersion = System.getProperty("java.version", "Unknown");
        boolean is64Bit = detect64Bit();

        HardwareProfile.HardwareTier tier = calculateTier(
            cpuCores, cpuThreads, totalMemoryMB, maxJvmMemoryMB
        );

        LOGGER.info("Hardware detection complete: {} cores, {} threads, {}MB RAM, tier {}",
            cpuCores, cpuThreads, totalMemoryMB, tier);

        return new HardwareProfile(
            cpuCores, cpuThreads, totalMemoryMB, availableMemoryMB,
            maxJvmMemoryMB, cpuModel, osName, osVersion, javaVersion,
            is64Bit, tier
        );
    }

    /**
     * Detects the number of physical CPU cores.
     * Falls back to logical processor count if physical cores cannot be determined.
     *
     * @return Number of physical cores or best estimate
     */
    private static int detectCpuCores() {
        // Runtime.availableProcessors() returns logical processors (threads)
        // For physical cores we need platform-specific detection
        // As a reasonable approximation, we assume hyperthreading is 2x
        int logicalProcessors = Runtime.getRuntime().availableProcessors();

        // Attempt to detect if hyperthreading is likely enabled
        // Most modern CPUs with more than 4 logical processors have HT
        if (logicalProcessors > 4) {
            // Assume hyperthreading with 2 threads per core
            return Math.max(1, logicalProcessors / 2);
        }

        return logicalProcessors;
    }

    /**
     * Detects the number of logical CPU threads.
     *
     * @return Number of logical threads
     */
    private static int detectCpuThreads() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Attempts to detect the CPU model name.
     * This is platform-specific and may not be available on all systems.
     *
     * @return CPU model name or "Unknown" if not detectable
     */
    private static String detectCpuModel() {
        // The CPU model is not directly available through standard Java APIs
        // Return a generic value; more sophisticated detection would require
        // platform-specific native code or reading /proc/cpuinfo on Linux
        String arch = System.getProperty("os.arch", "unknown");
        String version = System.getProperty("java.vm.version", "");

        return "Java " + arch + " processor";
    }

    /**
     * Detects total physical memory in megabytes.
     *
     * @return Total RAM in MB or estimate if not detectable
     */
    private static long detectTotalMemory() {
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();
            return osBean.getTotalMemorySize() / BYTES_PER_MB;
        } catch (Exception e) {
            LOGGER.warn("Could not detect total memory, estimating from JVM max");
            // Fall back to estimating based on max JVM memory
            // Assume JVM is allocated roughly 1/4 to 1/2 of system RAM
            return detectMaxJvmMemory() * 3;
        }
    }

    /**
     * Detects currently available (free) memory in megabytes.
     *
     * @return Available RAM in MB or 0 if not detectable
     */
    private static long detectAvailableMemory() {
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();
            return osBean.getFreeMemorySize() / BYTES_PER_MB;
        } catch (Exception e) {
            LOGGER.warn("Could not detect available memory");
            return 0;
        }
    }

    /**
     * Detects the maximum memory available to the JVM.
     *
     * @return Max JVM heap in MB
     */
    private static long detectMaxJvmMemory() {
        return Runtime.getRuntime().maxMemory() / BYTES_PER_MB;
    }

    /**
     * Detects whether the JVM is running in 64-bit mode.
     *
     * @return true if 64-bit JVM
     */
    private static boolean detect64Bit() {
        String arch = System.getProperty("os.arch", "");
        String dataModel = System.getProperty("sun.arch.data.model", "");

        return arch.contains("64") || "64".equals(dataModel);
    }

    /**
     * Calculates the hardware tier based on detected specifications.
     *
     * The tier calculation considers CPU power and available memory to
     * classify the system. This classification drives auto-configuration
     * decisions throughout the mod.
     *
     * Tier thresholds are based on typical Minecraft performance requirements:
     * LOW: Struggles to maintain 30 FPS at default settings
     * MEDIUM: Can run at 60 FPS with moderate settings
     * HIGH: Can run at high FPS with quality settings
     * ULTRA: Excess capacity beyond what Minecraft can utilize
     *
     * @param cpuCores Number of physical cores
     * @param cpuThreads Number of logical threads
     * @param totalMemoryMB Total system RAM
     * @param maxJvmMemoryMB Maximum JVM heap
     * @return The calculated hardware tier
     */
    private static HardwareProfile.HardwareTier calculateTier(
            int cpuCores, int cpuThreads, long totalMemoryMB, long maxJvmMemoryMB) {

        // Calculate a composite score based on hardware capabilities
        // CPU score: based on core count (modern Minecraft benefits from 4+ cores)
        int cpuScore;
        if (cpuCores >= 8) {
            cpuScore = 4;
        } else if (cpuCores >= 6) {
            cpuScore = 3;
        } else if (cpuCores >= 4) {
            cpuScore = 2;
        } else {
            cpuScore = 1;
        }

        // Memory score: based on available JVM memory (Minecraft needs 2-4GB minimum)
        int memScore;
        if (maxJvmMemoryMB >= 8192) {
            memScore = 4;
        } else if (maxJvmMemoryMB >= 4096) {
            memScore = 3;
        } else if (maxJvmMemoryMB >= 2048) {
            memScore = 2;
        } else {
            memScore = 1;
        }

        // System RAM score (affects OS and background process overhead)
        int ramScore;
        if (totalMemoryMB >= 32768) {
            ramScore = 4;
        } else if (totalMemoryMB >= 16384) {
            ramScore = 3;
        } else if (totalMemoryMB >= 8192) {
            ramScore = 2;
        } else {
            ramScore = 1;
        }

        // Calculate weighted average (CPU is most important for MC)
        double weightedScore = (cpuScore * 0.5) + (memScore * 0.3) + (ramScore * 0.2);

        // Map score to tier
        if (weightedScore >= 3.5) {
            return HardwareProfile.HardwareTier.ULTRA;
        } else if (weightedScore >= 2.5) {
            return HardwareProfile.HardwareTier.HIGH;
        } else if (weightedScore >= 1.5) {
            return HardwareProfile.HardwareTier.MEDIUM;
        } else {
            return HardwareProfile.HardwareTier.LOW;
        }
    }
}
