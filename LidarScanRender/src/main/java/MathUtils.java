/*
 * Helps the FPSCamera work correctly
 *
 *
 */


public class MathUtils {
    public static final double DBL_EPSILON = 2.220446049250313E-16d;
    public static final double ZERO_TOLERANCE = 0.0001d;
    public static final double ONE_THIRD = 1d / 3d;
    public static final double TAU = (Math.PI * 2.0);
    public static final double HALF_TAU = Math.PI;
    public static final double QUARTER_TAU = (Math.PI / 2.0);
    public static final double INVERSE_TAU = (1.0 / Math.PI);
    public static final double PI = Math.PI;
    public static final double TWO_PI = 2.0d * PI;
    public static final double INV_TWO_PI = 1.0d / TWO_PI;
    public static final double HALF_PI = 0.5d * PI;
    public static final double QUARTER_PI = 0.25d * PI;
    public static final double INV_PI = 1.0d / PI;
    public static final double DEG_TO_RAD = PI / 180.0d;
    public static final double RAD_TO_DEG = 180.0d / PI;

    public static boolean isWithinEpsilon(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    public static boolean isWithinEpsilon(double a, double b) {
        return isWithinEpsilon(a, b, ZERO_TOLERANCE);
    }

    public static boolean isPowerOfTwo(int number) {
        return (number > 0) && (number & (number - 1)) == 0;
    }

    public static int nearestPowerOfTwo(int number) {
        return (int) Math.pow(2, Math.ceil(Math.log(number) / Math.log(2)));
    }


    public static javafx.geometry.Point3D computeNormal(javafx.geometry.Point3D v1, javafx.geometry.Point3D v2, javafx.geometry.Point3D v3) {
        javafx.geometry.Point3D a1 = v1.subtract(v2);
        javafx.geometry.Point3D a2 = v3.subtract(v2);
        return a2.crossProduct(a1).normalize();
    }

    public static javafx.geometry.Point3D sphericalToCartesian(javafx.geometry.Point3D sphereCoords) {
        double a, x, y, z;
        y = sphereCoords.getX() * Math.sin(sphereCoords.getZ());
        a = sphereCoords.getX() * Math.cos(sphereCoords.getZ());
        x = a * Math.cos(sphereCoords.getY());
        z = a * Math.sin(sphereCoords.getY());
        return new javafx.geometry.Point3D(x, y, z);
    }

    public static javafx.geometry.Point3D cartesianToSpherical(javafx.geometry.Point3D cartCoords) {
        double x = cartCoords.getX();
        double storex, storey, storez;
        if (x == 0) {
            x = DBL_EPSILON;
        }
        storex = Math.sqrt((x * x)
                + (cartCoords.getY() * cartCoords.getY())
                + (cartCoords.getZ() * cartCoords.getZ()));
        storey = Math.atan(cartCoords.getZ() / x);
        if (x < 0) {
            storey += PI;
        }
        storez = Math.asin(cartCoords.getY() / storex);
        return new javafx.geometry.Point3D(storex, storey, storez);
    }

    public static float clamp(float input, float min, float max) {
        return (input < min) ? min : (input > max) ? max : input;
    }

    public static double clamp(double input, double min, double max) {
        return (input < min) ? min : (input > max) ? max : input;
    }
}