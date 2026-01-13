package com.titanperf.rendering;

import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * High-performance frustum culling implementation.
 *
 * Uses the Gribb/Hartmann method (published 2001, public domain) for extracting
 * frustum planes from the combined view-projection matrix. This is a standard
 * computer graphics technique used in virtually all 3D engines.
 *
 * The frustum is defined by 6 planes: left, right, bottom, top, near, far.
 * An AABB (Axis-Aligned Bounding Box) is considered inside the frustum if it
 * is not completely outside any of these planes.
 *
 * Performance Optimizations:
 * - Uses normalized planes for accurate distance calculations
 * - Early exit when box is completely outside any plane
 * - Plane coefficients stored in flat array for cache efficiency
 * - Avoids object allocations during culling checks
 */
public class FrustumCuller {

    /**
     * Frustum plane indices for readability.
     */
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    private static final int NEAR = 4;
    private static final int FAR = 5;

    /**
     * Frustum planes stored as [A, B, C, D] coefficients.
     * Plane equation: Ax + By + Cz + D = 0
     * 6 planes * 4 coefficients = 24 floats
     */
    private final float[] planes = new float[24];

    /**
     * Temporary vector for matrix operations.
     */
    private final Vector4f tempVec = new Vector4f();

    /**
     * Flag indicating if frustum has been extracted.
     */
    private boolean initialized = false;

    /**
     * Extracts frustum planes from the combined view-projection matrix.
     *
     * Uses the Gribb/Hartmann method:
     * - Left plane:   row4 + row1
     * - Right plane:  row4 - row1
     * - Bottom plane: row4 + row2
     * - Top plane:    row4 - row2
     * - Near plane:   row4 + row3
     * - Far plane:    row4 - row3
     *
     * @param viewProjection The combined view * projection matrix
     */
    public void extractPlanes(Matrix4f viewProjection) {
        // Extract matrix elements (column-major order in JOML)
        float m00 = viewProjection.m00();
        float m01 = viewProjection.m01();
        float m02 = viewProjection.m02();
        float m03 = viewProjection.m03();
        float m10 = viewProjection.m10();
        float m11 = viewProjection.m11();
        float m12 = viewProjection.m12();
        float m13 = viewProjection.m13();
        float m20 = viewProjection.m20();
        float m21 = viewProjection.m21();
        float m22 = viewProjection.m22();
        float m23 = viewProjection.m23();
        float m30 = viewProjection.m30();
        float m31 = viewProjection.m31();
        float m32 = viewProjection.m32();
        float m33 = viewProjection.m33();

        // Left plane: row4 + row1
        setPlane(LEFT, m03 + m00, m13 + m10, m23 + m20, m33 + m30);

        // Right plane: row4 - row1
        setPlane(RIGHT, m03 - m00, m13 - m10, m23 - m20, m33 - m30);

        // Bottom plane: row4 + row2
        setPlane(BOTTOM, m03 + m01, m13 + m11, m23 + m21, m33 + m31);

        // Top plane: row4 - row2
        setPlane(TOP, m03 - m01, m13 - m11, m23 - m21, m33 - m31);

        // Near plane: row4 + row3
        setPlane(NEAR, m03 + m02, m13 + m12, m23 + m22, m33 + m32);

        // Far plane: row4 - row3
        setPlane(FAR, m03 - m02, m13 - m12, m23 - m22, m33 - m32);

        initialized = true;
    }

    /**
     * Sets and normalizes a frustum plane.
     *
     * @param index Plane index (0-5)
     * @param a Plane coefficient A
     * @param b Plane coefficient B
     * @param c Plane coefficient C
     * @param d Plane coefficient D
     */
    private void setPlane(int index, float a, float b, float c, float d) {
        // Normalize the plane for accurate distance calculations
        float length = (float) Math.sqrt(a * a + b * b + c * c);
        if (length > 0.0001f) {
            float invLength = 1.0f / length;
            a *= invLength;
            b *= invLength;
            c *= invLength;
            d *= invLength;
        }

        int offset = index * 4;
        planes[offset] = a;
        planes[offset + 1] = b;
        planes[offset + 2] = c;
        planes[offset + 3] = d;
    }

    /**
     * Tests if an axis-aligned bounding box is inside or intersects the frustum.
     *
     * Uses the "p-vertex" optimization: for each plane, we only need to test
     * the vertex that is most in the direction of the plane normal. If this
     * vertex is outside the plane, the entire box is outside.
     *
     * @param minX Minimum X coordinate of the box
     * @param minY Minimum Y coordinate of the box
     * @param minZ Minimum Z coordinate of the box
     * @param maxX Maximum X coordinate of the box
     * @param maxY Maximum Y coordinate of the box
     * @param maxZ Maximum Z coordinate of the box
     * @return true if the box is inside or intersects the frustum
     */
    public boolean isBoxInFrustum(double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
        if (!initialized) {
            return true; // Don't cull if frustum not set up
        }

        // Test against all 6 planes
        for (int i = 0; i < 6; i++) {
            int offset = i * 4;
            float a = planes[offset];
            float b = planes[offset + 1];
            float c = planes[offset + 2];
            float d = planes[offset + 3];

            // Find the p-vertex (vertex most in direction of plane normal)
            double px = a >= 0 ? maxX : minX;
            double py = b >= 0 ? maxY : minY;
            double pz = c >= 0 ? maxZ : minZ;

            // If p-vertex is outside this plane, entire box is outside frustum
            if (a * px + b * py + c * pz + d < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests if a Minecraft Box is inside or intersects the frustum.
     *
     * @param box The bounding box to test
     * @return true if the box is inside or intersects the frustum
     */
    public boolean isBoxInFrustum(Box box) {
        return isBoxInFrustum(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /**
     * Tests if a point is inside the frustum.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if the point is inside the frustum
     */
    public boolean isPointInFrustum(double x, double y, double z) {
        if (!initialized) {
            return true;
        }

        for (int i = 0; i < 6; i++) {
            int offset = i * 4;
            float a = planes[offset];
            float b = planes[offset + 1];
            float c = planes[offset + 2];
            float d = planes[offset + 3];

            if (a * x + b * y + c * z + d < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests if a sphere is inside or intersects the frustum.
     *
     * @param x Center X coordinate
     * @param y Center Y coordinate
     * @param z Center Z coordinate
     * @param radius Sphere radius
     * @return true if the sphere is inside or intersects the frustum
     */
    public boolean isSphereInFrustum(double x, double y, double z, double radius) {
        if (!initialized) {
            return true;
        }

        for (int i = 0; i < 6; i++) {
            int offset = i * 4;
            float a = planes[offset];
            float b = planes[offset + 1];
            float c = planes[offset + 2];
            float d = planes[offset + 3];

            // Distance from center to plane (signed)
            double distance = a * x + b * y + c * z + d;

            // If center is further than radius behind plane, sphere is outside
            if (distance < -radius) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns whether the frustum has been initialized.
     *
     * @return true if extractPlanes() has been called
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Resets the frustum state.
     */
    public void reset() {
        initialized = false;
    }
}
