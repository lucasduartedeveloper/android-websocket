package com.example.websocketclient.websocketclient.common;

public class GamepadButton {

    private int index;

    private float value;

    public float[] getAxis_value() {
        return axis_value;
    }

    public void setAxis_value(float[] axis_value) {
        this.axis_value = axis_value;
    }

    private float[] axis_value;

    public boolean isPressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    private boolean pressed;

    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
