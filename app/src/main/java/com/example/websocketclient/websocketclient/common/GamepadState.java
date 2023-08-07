package com.example.websocketclient.websocketclient.common;

import java.util.ArrayList;
import java.util.List;

public class GamepadState {

    private ArrayList<GamepadButton> activeButtons;

    public GamepadState(String json) {
        activeButtons = new ArrayList<GamepadButton>();
        String[] buttons = json.split("\\},");
        if (json == "[]") return;
        for (int n = 0; n < buttons.length; n++) {
            GamepadButton button = new GamepadButton();
            String text = buttons[n];
            int start = text.indexOf("\"index\":")+8;
            int end = text.indexOf(",");
            String edit = text.substring(start, end);
            button.setIndex(Integer.valueOf(text.substring(start, end)));

            start = text.indexOf("\"value\":")+8;
            text = text.substring(start);
            end = text.indexOf(",");
            String value = text.substring(0, end);
            if (!value.startsWith("[")) {
                button.setValue(Float.valueOf(value));
            }
            else {
                end = text.indexOf("]");
                value = text.substring(0, end);
                String[] axis_value = value.split(",");
                for (int k = 0; k < axis_value.length; k++) {
                    axis_value[k] = axis_value[k].replace("[","");
                    axis_value[k] = axis_value[k].replace("]","");
                }
                float[] result = {
                    Float.valueOf(axis_value[0]),
                    Float.valueOf(axis_value[1])
                };
                button.setAxis_value(result);
            }

            start = text.indexOf("\"value\":")+8;
            text = text.substring(start);
            end = text.indexOf(",");
            button.setPressed(text.contains("true"));
            activeButtons.add(button);
        }
    }

    public String toString() {
        String text = "";
        for (int n = 0; n < activeButtons.size(); n++) {
            int index = activeButtons.get(n).getIndex();
            text += "button "+index+": ";
            if (index < 90) {
                text += activeButtons.get(n).getValue();
            }
            else {
                float[] axis_value = activeButtons.get(n).getAxis_value();
                text += "["+axis_value[0]+","+axis_value[1]+"]";
            }
            text += "\n";
        }
        return text;
    }
}

