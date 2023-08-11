package com.example.websocketclient.websocketclient.common;

import android.graphics.Point;

import java.util.ArrayList;

public class TouchEvent {

    public enum Type {
        DOWN, MOVE, UP
    }

    public TouchEvent(Type type, int fingerNo, int x, int y) {
        this.type = type;
        this.fingerNo = fingerNo;
        this.coordinates = new Point(x, y);
        this.pressure = 5;
        this.radius = 50;
    }

    public TouchEvent(Type type, int fingerNo, Point coordinates, int pressure, int radius) {
        this.type = type;
        this.fingerNo = fingerNo;
        this.coordinates = coordinates;
        this.pressure = pressure;
        this.radius = radius;
    }

    private Type type;
    private int fingerNo;
    private Point coordinates;
    private int pressure;
    private int radius;

    public ArrayList<String> toSendeventArray() {
        ArrayList<String> result = new ArrayList<String>();
        if (type == Type.DOWN || type == Type.MOVE) {
            result.add("sendevent /dev/input/event3 3 47 " + fingerNo);
            result.add("sendevent /dev/input/event3 3 57 0");
            result.add("sendevent /dev/input/event3 3 58 " + radius);
            result.add("sendevent /dev/input/event3 3 48 " + pressure);
            result.add("sendevent /dev/input/event3 3 53 " + coordinates.x);
            result.add("sendevent /dev/input/event3 3 54 " + coordinates.y);
            result.add("sendevent /dev/input/event3 1 330 1");
            result.add("sendevent /dev/input/event3 0 0 0");
            //result.add("sleep 0.1");
        }
        else {
            result.add("sendevent /dev/input/event3 3 47 " + fingerNo);
            result.add("sendevent /dev/input/event3 3 57 4294967295");
            result.add("sendevent /dev/input/event3 1 330 0");
            result.add("sendevent /dev/input/event3 0 0 0");
            //result.add("sleep 0.1");
        }
        return result;
    }

    public String toSendeventLine() {
        String result = "";
        if (type == Type.DOWN || type == Type.MOVE) {
            result += ("3 47 " + fingerNo + " ");
            result += ("3 57 0 ");
            result += ("3 58 " + radius + " ");
            result += ("3 48 " + pressure + " ");
            result += ("3 53 " + coordinates.x + " ");
            result += ("3 54 " + coordinates.y + " ");
            result += ("1 330 1 ");
            result += ("0 0 0");
        }
        else {
            result += ("3 47 " + fingerNo + " ");
            result += ("3 57 4294967295 ");
            result += ("1 330 0 ");
            result += ("0 0 0");
        }
        return result;
    }
}
