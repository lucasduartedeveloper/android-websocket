package com.example.websocketclient.websocketclient.common;

import android.graphics.Point;

import java.util.ArrayList;

public class TouchEvent {

    public TouchCommand command;
    public static int downCount = 0;

    public enum Type {
        DOWN, MOVE, UP
    }

    public TouchEvent(Type type, int x, int y) {
        this.type = type;
        this.coordinates = new Point(x, y);
        this.pressure = 5;
        this.radius = 50;
    }

    public TouchEvent(Type type, Point coordinates, int pressure, int radius) {
        this.type = type;
        this.coordinates = coordinates;
        this.pressure = pressure;
        this.radius = radius;
    }

    private Type type;
    private Point coordinates;
    private int pressure;
    private int radius;

    public ArrayList<String> toSendeventArray() {
        ArrayList<String> result = new ArrayList<String>();
        if (type == Type.DOWN || type == Type.MOVE) {
            result.add("sendevent /dev/input/event3 3 47 " + command.layerNo);
            result.add("sendevent /dev/input/event3 3 57 " + command.layerNo);
            result.add("sendevent /dev/input/event3 3 58 " + radius);
            result.add("sendevent /dev/input/event3 3 48 " + pressure);
            result.add("sendevent /dev/input/event3 3 53 " + coordinates.x);
            result.add("sendevent /dev/input/event3 3 54 " + coordinates.y);
            if (TouchEvent.downCount == 0)
            result.add("sendevent /dev/input/event3 1 330 1");
            result.add("sendevent /dev/input/event3 0 0 0");
            //result.add("sleep 0.1");
            if (type == Type.DOWN)
            TouchEvent.downCount += 1;
        }
        else {
            if (type == Type.UP) TouchEvent.downCount -= 1;
            result.add("sendevent /dev/input/event3 3 47 " + command.layerNo);
            result.add("sendevent /dev/input/event3 3 57 4294967295");
            result.add("sendevent /dev/input/event3 3 58 0");
            result.add("sendevent /dev/input/event3 3 48 0");
            if (TouchEvent.downCount == 0)
            result.add("sendevent /dev/input/event3 1 330 0");
            result.add("sendevent /dev/input/event3 0 0 0");
            //result.add("sleep 0.1");
        }
        return result;
    }

    public String toSendeventLine() {
        String result = "";
        if (type == Type.DOWN || type == Type.MOVE) {
            result += ("3 47 " + command.layerNo + " ");
            result += ("3 57 " + command.layerNo + " ");
            result += ("3 58 " + radius + " ");
            result += ("3 48 " + pressure + " ");
            result += ("3 53 " + coordinates.x + " ");
            result += ("3 54 " + coordinates.y + " ");
            if (TouchEvent.downCount == 0)
            result += ("1 330 1 ");
            result += ("0 0 0");
            if (type == Type.DOWN)
            TouchEvent.downCount += 1;
        }
        else {
            if (type == Type.UP) TouchEvent.downCount -= 1;
            result += ("3 47 " + command.layerNo + " ");
            result += ("3 57 4294967295 ");
            result += ("3 58 0 ");
            result += ("3 48 0 ");
            if (TouchEvent.downCount == 0)
            result += ("1 330 0 ");
            result += ("0 0 0");
        }
        return result;
    }
}
