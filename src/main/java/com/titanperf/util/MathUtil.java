package com.titanperf.util;

/**
 * Mathematical utility functions for performance calculations.
 *
 * These utilities are optimized for the specific needs of performance
 * modules, avoiding unnecessary object allocation and providing
 * fast approximations where exact values are not required.
 */
public final class MathUtil {

    /**
     * Private constructor prevents instantiation of utility class.
     */
    private MathUtil() {
    }

    /**
     * Clamps an integer value between minimum and maximum bounds.
     *
     * @param value The value to clamp
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return The clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a double value between minimum and maximum bounds.
     *
     * @param value The value to clamp
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return The clamped value
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Calculates squared distance between two 3D points.
     *
     * Using squared distance avoids the expensive square root operation
     * when only relative distances are needed for comparison.
     *
     * @param x1 First point X
     * @param y1 First point Y
     * @param z1 First point Z
     * @param x2 Second point X
     * @param y2 Second point Y
     * @param z2 Second point Z
     * @return Squared distance between points
     */
    public static double distanceSquared(double x1, double y1, double z1,
                                         double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates squared horizontal distance (ignoring Y).
     *
     * Useful for distance checks where vertical distance should be ignored,
     * such as entity culling decisions.
     *
     * @param x1 First point X
     * @param z1 First point Z
     * @param x2 Second point X
     * @param z2 Second point Z
     * @return Squared horizontal distance
     */
    public static double horizontalDistanceSquared(double x1, double z1,
                                                   double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return dx * dx + dz * dz;
    }

    /**
     * Fast approximation of inverse square root.
     *
     * Uses the famous fast inverse square root algorithm for cases
     * where approximate results are acceptable. About 3-4x faster
     * than 1.0 / Math.sqrt(x) with roughly 1% error.
     *
     * @param x The value to compute inverse square root of
     * @return Approximate 1/sqrt(x)
     */
    public static float fastInverseSqrt(float x) {
        float halfX = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x = x * (1.5f - halfX * x * x);
        return x;
    }

    /**
     * Linearly interpolates between two values.
     *
     * @param start Starting value
     * @param end Ending value
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated value
     */
    public static double lerp(double start, double end, double t) {
        return start + t * (end - start);
    }

    /**
     * Checks if a value is within a range (inclusive).
     *
     * @param value The value to check
     * @param min Minimum of range
     * @param max Maximum of range
     * @return true if min <= value <= max
     */
    public static boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Rounds a value to the nearest multiple.
     *
     * @param value The value to round
     * @param multiple The multiple to round to
     * @return The rounded value
     */
    public static int roundToMultiple(int value, int multiple) {
        return ((value + multiple / 2) / multiple) * multiple;
    }

    /**
     * Calculates the next power of two greater than or equal to value.
     *
     * Useful for buffer sizing and texture dimensions.
     *
     * @param value The minimum value needed
     * @return The next power of two
     */
    public static int nextPowerOfTwo(int value) {
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    /**
     * Checks if a value is a power of two.
     *
     * @param value The value to check
     * @return true if value is a power of two
     */
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
}
