package com.bergerkiller.bukkit.common.math;

import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.utils.MathUtil;

/**
 * A quaternion for performing rotations in 3D space.
 * The quaternion is automatically normalized.
 */
public class Quaternion implements Cloneable {
    private double x, y, z, w;

    public Quaternion() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.w = 1.0;
    }

    public Quaternion(Quaternion quat) {
        this.x = quat.x;
        this.y = quat.y;
        this.z = quat.z;
        this.w = quat.w;
    }

    public Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.normalize();
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getW() {
        return this.w;
    }

    /**
     * Calculates the dot product of this Quaternion with another
     * 
     * @param q other quaternion
     * @return dot product
     */
    public double dot(Quaternion q) {
        return this.x * q.x + this.y * q.y + this.z * q.z + this.w * q.w;
    }

    /**
     * Transforms a point, applying the rotation of this quaternion with 0,0,0 as origin.
     * 
     * @param point to rotate using this quaternion
     */
    public void transformPoint(Vector point) {
        double px = point.getX();
        double py = point.getY();
        double pz = point.getZ();
        point.setX( px + 2.0 * (px*(-y*y-z*z) + py*(x*y-z*w) + pz*(x*z+y*w)) );
        point.setY( py + 2.0 * (px*(x*y+z*w) + py*(-x*x-z*z) + pz*(y*z-x*w)) );
        point.setZ( pz + 2.0 * (px*(x*z-y*w) + py*(y*z+x*w) + pz*(-x*x-y*y)) );
    }

    /**
     * Retrieves the right vector, which is the result of transforming a (1,0,0) point
     * with this Quaternion.
     * 
     * @return right vector
     */
    public Vector rightVector() {
        return new Vector(1.0 + 2.0 * (-y*y-z*z), 2.0 * (x*y+z*w), 2.0 * (x*z-y*w));
    }

    /**
     * Retrieves the up vector, which is the result of transforming a (0,1,0) point
     * with this Quaternion.
     * 
     * @return up vector
     */
    public Vector upVector() {
        return new Vector(2.0 * (x*y-z*w), 1.0 + 2.0 * (-x*x-z*z), 2.0 * (y*z+x*w));
    }

    /**
     * Retrieves the forward vector, which is the result of transforming a (0,0,1) point
     * with this Quaternion.
     * 
     * @return forward vector
     */
    public Vector forwardVector() {
        return new Vector(2.0 * (x*z+y*w), 2.0 * (y*z-x*w), 1.0 + 2.0 * (-x*x-y*y));
    }

    /**
     * Multiplies this quaternion with another quaternion. The result is stored in this quaternion.
     * 
     * @param quat to multiply with
     */
    public void multiply(Quaternion quat) {
        double x = this.w * quat.x + this.x * quat.w + this.y * quat.z - this.z * quat.y;
        double y = this.w * quat.y + this.y * quat.w + this.z * quat.x - this.x * quat.z;
        double z = this.w * quat.z + this.z * quat.w + this.x * quat.y - this.y * quat.x;
        double w = this.w * quat.w - this.x * quat.x - this.y * quat.y - this.z * quat.z;
        this.x = x; this.y = y; this.z = z; this.w = w;
        this.normalize();
    }

    /**
     * Multiplies this quaternion with a rotation transformation in yaw/pitch/roll, based on the Minecraft
     * coordinate system. This will differ slightly from the standard rotateX/Y/Z functions.
     * 
     * @param rotation (x=pitch, y=yaw, z=roll)
     */
    public final void rotateYawPitchRoll(Vector3 rotation) {
        rotateYawPitchRoll(rotation.y, rotation.x, rotation.z);
    }

    /**
     * Multiplies this quaternion with a rotation transformation in yaw/pitch/roll, based on the Minecraft
     * coordinate system. This will differ slightly from the standard rotateX/Y/Z functions.
     * 
     * @param rotation (x=pitch, y=yaw, z=roll)
     */
    public final void rotateYawPitchRoll(Vector rotation) {
        rotateYawPitchRoll(rotation.getX(), rotation.getY(), rotation.getZ());
    }

    /**
     * Multiplies this quaternion with a rotation transformation in yaw/pitch/roll, based on the Minecraft
     * coordinate system. This will differ slightly from the standard rotateX/Y/Z functions.
     * 
     * @param pitch rotation (X)
     * @param yaw rotation (Y)
     * @param roll rotation (Z)
     */
    public final void rotateYawPitchRoll(double pitch, double yaw, double roll) {
        this.rotateY(-yaw);
        this.rotateX(pitch);
        this.rotateZ(roll);
    }

    /**
     * Deduces the yaw/pitch/roll values in degrees that this quaternion transforms objects with
     * 
     * @return axis rotations: {x=pitch, y=yaw, z=roll}
     */
    public final Vector getYawPitchRoll() {
        final double roll;
        final double pitch;
        double yaw;
        final double test = w * x - y * z;
        if (Math.abs(test) < 0.4999) {
            roll = Math.atan2(2 * (w * z + x * y), 1 - 2 * (x * x + z * z));
            pitch = Math.asin(2 * test);
            yaw = Math.atan2(2 * (w * y + z * x), 1 - 2 * (x * x + y * y));
        } else {
            final int sign = (test < 0) ? -1 : 1;
            roll = 0;
            pitch = sign * Math.PI / 2;
            yaw = -sign * 2 * Math.atan2(z, w);
        }
        if (yaw > Math.PI) {
            yaw -= 2.0 * Math.PI;
        } else if (yaw < -Math.PI) {
            yaw += 2.0 * Math.PI;
        }
        return new Vector(Math.toDegrees(pitch), Math.toDegrees(-yaw), Math.toDegrees(roll));        
    }

    public final void rotateX(double angleDegrees) {
        if (angleDegrees != 0.0) {
            double r = 0.5 * Math.toRadians(angleDegrees);
            rotateX_unsafe(Math.cos(r), Math.sin(r));
        }
    }

    public final void rotateX(double y, double z) {
        double r = halfcosatan2(z, y);
        rotateX_unsafe(Math.sqrt(0.5 + r), Math.sqrt(0.5 - r));
    }

    private final void rotateX_unsafe(double fy, double fz) {
        double x = this.x * fy + this.w * fz;
        double y = this.y * fy + this.z * fz;
        double z = this.z * fy - this.y * fz;
        double w = this.w * fy - this.x * fz;
        this.x = x; this.y = y; this.z = z; this.w = w;
        this.normalize();
    }

    public final void rotateY(double angleDegrees) {
        if (angleDegrees != 0.0) {
            double r = 0.5 * Math.toRadians(angleDegrees);
            rotateY_unsafe(Math.cos(r), Math.sin(r));
        }
    }

    public final void rotateY(double x, double z) {
        double r = halfcosatan2(z, x);
        rotateY_unsafe(Math.sqrt(0.5 + r), Math.sqrt(0.5 - r));
    }

    private final void rotateY_unsafe(double fx, double fz) {
        double x = this.x * fx - this.z * fz;
        double y = this.y * fx + this.w * fz;
        double z = this.z * fx + this.x * fz;
        double w = this.w * fx - this.y * fz;
        this.x = x; this.y = y; this.z = z; this.w = w;
        this.normalize();
    }

    public final void rotateZ(double angleDegrees) {
        if (angleDegrees != 0.0) {
            double r = 0.5 * Math.toRadians(angleDegrees);
            rotateZ_unsafe(Math.cos(r), Math.sin(r));
        }
    }

    public final void rotateZ(double x, double y) {
        double r = halfcosatan2(y, x);
        rotateZ_unsafe(Math.sqrt(0.5 + r), Math.sqrt(0.5 - r));
    }

    private final void rotateZ_unsafe(double fx, double fy) {
        double x = this.x * fx + this.y * fy;
        double y = this.y * fx - this.x * fy;
        double z = this.z * fx + this.w * fy;
        double w = this.w * fx - this.z * fy;
        this.x = x; this.y = y; this.z = z; this.w = w;
        this.normalize();
    }

    /**
     * Converts the rotation transformations defined in this quaternion to a 
     * 4x4 transformation matrix. This is as if the unit matrix was multiplied
     * with this quaternion.
     * 
     * @return 4x4 transformation matrix.
     */
    public Matrix4x4 toMatrix4x4() {
        return new Matrix4x4(
                1.0 - 2.0*y*y - 2.0*z*z, 2.0*x*y - 2.0*z*w, 2.0*x*z + 2.0*y*w, 0.0,
                2.0*x*y + 2.0*z*w, 1.0 - 2.0*x*x - 2.0*z*z, 2.0*y*z - 2.0*x*w, 0.0,
                2.0*x*z - 2.0*y*w, 2.0*y*z + 2.0*x*w, 1.0 - 2.0*x*x - 2.0*y*y, 0.0,
                0.0, 0.0, 0.0, 1.0
        );
    }

    public void invert() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
    }

    private void normalize() {
        double f = MathUtil.getNormalizationFactor(this.x, this.y, this.z, this.w);
        this.x *= f; this.y *= f; this.z *= f; this.w *= f;
    }

    @Override
    public Quaternion clone() {
        return new Quaternion(this);
    }

    @Override
    public String toString() {
        return "{" + this.x + ", " + this.y + ", " + this.z + ", " + this.w + "}";
    }

    /**
     * Creates a quaternion for a rotation around an axis
     * 
     * @param axis
     * @param angleDegrees
     * @return quaternion for the rotation around the axis
     */
    public static Quaternion fromAxisAngles(Vector3 axis, double angleDegrees) {
        return fromAxisAngles(axis.x, axis.y, axis.z, angleDegrees);
    }

    /**
     * Creates a quaternion for a rotation around an axis
     * 
     * @param axis
     * @param angleDegrees
     * @return quaternion for the rotation around the axis
     */
    public static Quaternion fromAxisAngles(Vector axis, double angleDegrees) {
        return fromAxisAngles(axis.getX(), axis.getY(), axis.getZ(), angleDegrees);
    }

    /**
     * Creates a quaternion for a rotation around an axis
     * 
     * @param axisX
     * @param axisY
     * @param axisZ
     * @param angleDegrees
     * @return quaternion for the rotation around the axis
     */
    public static Quaternion fromAxisAngles(double axisX, double axisY, double axisZ, double angleDegrees) {
        double r = 0.5 * Math.toRadians(angleDegrees);
        double f = Math.sin(r);
        return new Quaternion(f*axisX, f*axisY, f*axisZ, Math.cos(r));
    }

    /**
     * Creates a quaternion that transforms the input vector (u) into the output vector (v).
     * The vectors do not have to be unit vectors for this function to work.
     * 
     * @param u input vector (from)
     * @param v expected output vector (to)
     * @return quaternion that rotates u to become v
     */
    public static Quaternion fromToRotation(Vector u, Vector v) {
        // xyz = cross(u, v), w = dot(u, v)
        // add magnitude of quaternion to w, then normalize it
        Quaternion q = new Quaternion();
        q.x = u.getY() * v.getZ() - v.getY() * u.getZ();
        q.y = u.getZ() * v.getX() - v.getZ() * u.getX();
        q.z = u.getX() * v.getY() - v.getX() * u.getY();
        q.w = u.dot(v);
        q.w += Math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w);
        q.normalize();
        return q;
    }

    /**
     * Creates a quaternion that transforms a forward vector (0, 0, 1) into the output vector (v).
     * The vector does not have to be a unit vector for this function to work.
     * 
     * @param v expected output vector (to)
     * @return quaternion that rotates (0,0,1) to become v
     */
    public static Quaternion fromForwardToRotation(Vector v) {
        return new Quaternion(-v.getY(), v.getX(), 0.0, v.getZ() + v.length());
    }

    /**
     * Creates a quaternion that 'looks' into a given direction, with a known 'up' vector
     * to dictate roll around that direction axis.
     * 
     * @param dir to look into
     * @param up direction
     * @return Quaternion with the look-direction transformation
     */
    public static Quaternion fromLookDirection(Vector dir, Vector up) {
        Vector v = up.clone();
        v.normalize();
        v.multiply(-v.dot(dir));
        v.add(dir);
        Quaternion r = Quaternion.fromToRotation(v, dir);
        r.multiply(fromForwardToRotation(v));
        return r;
    }

    /**
     * Performs a linear interpolation between two quaternions.
     * Separate theta values can be specified to set how much of each quaternion to keep
     * For smoother interpolation, {@link #slerp(q0, q1, theta)} can be used instead.
     * 
     * @param q0 quaternion at theta=0
     * @param q1 quaternion at theta=1
     * @param t0 theta value for q0 amount (range 0 to 1)
     * @param t1 theta value for q1 amount (range 0 to 1)
     * @return lerp result
     */
    public static Quaternion lerp(Quaternion q0, Quaternion q1, double t0, double t1) {
        return new Quaternion(t0 * q0.x + t1 * q1.x,
                t0 * q0.y + t1 * q1.y,
                t0 * q0.z + t1 * q1.z,
                t0 * q0.w + t1 * q1.w);
    }

    /**
     * Performs a linear interpolation between two quaternions.
     * For smoother interpolation, {@link #slerp(q0, q1, theta)} can be used instead.
     * 
     * @param q0 quaternion at theta=0
     * @param q1 quaternion at theta=1
     * @param theta value (range 0 to 1)
     * @return lerp result
     */
    public static Quaternion lerp(Quaternion q0, Quaternion q1, double theta) {
        return lerp(q0, q1, 1.0 - theta, theta);
    }

    /**
     * Performs a spherical interpolation between two quaternions.
     * 
     * @param q0 quaternion at theta=0
     * @param q1 quaternion at theta=0
     * @param theta value (range 0 to 1)
     * @return slerp result
     */
    public static Quaternion slerp(Quaternion q0, Quaternion q1, double theta) {
        Quaternion qs = q1.clone();
        double dot = q0.dot(q1);

        // Invert quaternion when dot < 0 to simplify maths
        if (dot < 0.0) {
            dot = -dot;
            qs.x = -qs.x;
            qs.y = -qs.y;
            qs.z = -qs.z;
            qs.w = -qs.w;
        }

        // Above this a lerp is adequate
        if (dot >= 0.95) {
            return lerp(q0, qs, theta);
        }

        // Linear interpolation using sines
        double angle = Math.acos(dot);
        double qd = 1.0 / Math.sin(angle);
        double q0f = qd * Math.sin(angle*(1.0-theta));
        double qsf = qd * Math.sin(angle*theta);
        return lerp(q0, qs, q0f, qsf);
    }

    // This method is used often for the two-arg rotateX/Y/Z functions
    // Optimized equivalent of 0.5 * Math.cos(Math.atan2(y, x))
    private static final double halfcosatan2(double y, double x) {
        double tmp = y / x;
        tmp *= tmp;
        tmp += 1.0;
        return ((x < 0.0) ? -0.5 : 0.5) / Math.sqrt(tmp);
    }
}
