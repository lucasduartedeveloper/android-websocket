package com.example.websocketclient.websocketclient.common;

import android.graphics.Point;
import android.graphics.PointF;

public class MathUtils {

    public static float PI = 3.141592653589793f;

    public static PointF rotate2d(PointF center, PointF point, float angle, boolean isDegrees) {
        float cx = center.x;
        float cy = center.y;
        float x = point.x;
        float y = point.y;
        float radians = isDegrees ? (MathUtils.PI / 180) * angle : angle;
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double nx = (cos * (x - cx)) + (sin * (y - cy)) + cx;
        double ny = (cos * (y - cy)) - (sin * (x - cx)) + cy;
        return new PointF((float) nx, (float)ny);
    }

    public static float angle2d(float opposite, float adjacent) {
        float hyp = (float) Math.sqrt(
            (float) Math.abs(Math.pow(opposite, 2))+
                    Math.abs(Math.pow(adjacent, 2))
        );

        float sinA = opposite/hyp;
        float angle = (float) Math.asin(sinA);
        angle = opposite == 0 && adjacent > 0 ? 1.5707963267948966f * 2 : angle;
        angle = opposite > 0 && adjacent > 0 ? 1.5707963267948966f * 2 - angle : angle;
        angle = opposite < 0 && adjacent > 0 ? 1.5707963267948966f * 2 - angle : angle;

        return angle;
    }

    public static float clip (float value, float max) {
        if (value > max) return max;
        if (value < 0) return 0;
        return value;
    }

    public static PointF normalize (PointF vector, float max) {
        float openning = ((Math.abs(vector.x)+Math.abs(vector.y))/2)*2;
        openning = MathUtils.clip(openning, max);
        float angle = MathUtils.angle2d(vector.x, vector.y);
        PointF center = new PointF(0, 0 );
        PointF point = new PointF(0, -openning );
        PointF n = MathUtils.rotate2d(center, point, -angle, false);
        return n;
    }

}
