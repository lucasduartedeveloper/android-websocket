package com.example.websocketclient.websocketclient.common;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

public class GamepadState {

    private ArrayList<GamepadButton> last_activeButtons;
    private ArrayList<GamepadButton> activeButtons;
    private ArrayList<GamepadButton> buttonsDown;
    private ArrayList<GamepadButton> buttonsUp;

    public GamepadState() {
        activeButtons = new ArrayList<GamepadButton>();
        last_activeButtons = new ArrayList<GamepadButton>();
        buttonsDown = new ArrayList<GamepadButton>();
        buttonsUp = new ArrayList<GamepadButton>();
    }

    public ArrayList<GamepadButton> getActiveButtons() {
        return activeButtons;
    }

    public void loadJson(String json) {
        last_activeButtons = activeButtons;
        activeButtons = new ArrayList<GamepadButton>();
        if (json.equals("[]")) return;
        String[] buttons = json.split("\\},");
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
            if (button.isPressed())
                activeButtons.add(button);
        }

        buttonsDown = new ArrayList<GamepadButton>();
        for (int n = 0; n < activeButtons.size(); n++) {
            boolean buttonDown = true;
            for (int k = 0; k < last_activeButtons.size(); k++) {
                if (activeButtons.get(n).getIndex() ==
                last_activeButtons.get(k).getIndex()) {
                    buttonDown = false;
                }
            }
            if (buttonDown) buttonsDown.add(activeButtons.get(n));
        }

        buttonsUp = new ArrayList<GamepadButton>();
        for (int n = 0; n < last_activeButtons.size(); n++) {
            boolean buttonUp = true;
            for (int k = 0; k < activeButtons.size(); k++) {
                if (last_activeButtons.get(n).getIndex() ==
                        activeButtons.get(k).getIndex()) {
                    buttonUp = false;
                }
            }
            if (buttonUp) buttonsUp.add(last_activeButtons.get(n));
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

    public ArrayList<String> createCommand(String[] coordinates) {
        ArrayList<String> command = new ArrayList<>();
        for (int n = 0; n < coordinates.length; n++) {
            String[] line = coordinates[n].split(":");
            int index = Integer.valueOf(line[0].replace(" ", ""));

            // buttons down
            if (line[1].equals("open")) {
                for (int k = 0; k < buttonsDown.size(); k++) {
                    GamepadButton button = buttonsDown.get(k);
                    if (index == button.getIndex()) {
                        String[] sequence = line[2].split(";");
                        for (int m = 0; m < sequence.length; m++) {
                            if (sequence[m].length() > 0)
                            command.add(sequence[m]);
                        }
                    }
                }
                continue;
            }

            // buttons down
            for (int k = 0; k < buttonsDown.size(); k++) {
                GamepadButton button = buttonsDown.get(k);
                if (index == button.getIndex()) {
                    command.add(line[1]);
                }
            }

            // buttons up
            for (int k = 0; k < buttonsUp.size(); k++) {
                GamepadButton button = buttonsUp.get(k);
                if (index == button.getIndex()) {
                    command.add(line[2]);
                }
            }

            // analog button
            if (line.length == 4)
            for (int k = 0; k < activeButtons.size(); k++) {
                GamepadButton button = activeButtons.get(k);
                if ((index == 99 || index == 98) && index == button.getIndex()) {
                    String result = line[3].replace("{0}", String.valueOf(button.getAxis_value()[0]));
                    result = result.replace("{1}", String.valueOf(button.getAxis_value()[1]));
                    command.add(result);
                }
            }
        }
        return command;
    }

    public GamepadButton getButton(int index) {
        GamepadButton button = new GamepadButton();
        boolean isActive = false;
        for (int n = 0; n < activeButtons.size(); n++) {
            if (activeButtons.get(n).getIndex() == index) {
                isActive = true;
                button = activeButtons.get(n);
            }
        }
        if (!isActive) {
            button.setIndex(index);
            button.setValue(0);
            float[] blank = { 0f, 0f };
            button.setAxis_value(blank);
            button.setPressed(false);
        }
        return button;
    }
}

