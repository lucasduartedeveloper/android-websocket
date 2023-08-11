package com.example.websocketclient.websocketclient.common;

import java.util.ArrayList;

public class TouchCommand {

    public static int touchId = -1;
    public int layerNo;

    private ArrayList<TouchEvent> touchEvents;

    public TouchCommand(int layerNo) {
        TouchCommand.touchId += 1;
        this.layerNo = layerNo;
        touchEvents = new ArrayList<TouchEvent>();
    }

    public void add(TouchEvent event) {
        event.command = this;
        touchEvents.add(event);
    }

    public ArrayList<String> toSendeventArray() {
        ArrayList<String> result = new ArrayList<String>();
        for (int n = 0; n < touchEvents.size(); n++) {
            ArrayList<String> evArray = touchEvents.get(n).toSendeventArray();
            for (int k = 0; k < evArray.size(); k++) {
                result.add(evArray.get(k));
            }
        }
        return result;
    }

    public ArrayList<String> toSendeventLine() {
        ArrayList<String> result = new ArrayList<String>();
        String text = "./sendevent_line /dev/input/event3 ";
        for (int n = 0; n < touchEvents.size(); n++) {
            text += touchEvents.get(n).toSendeventLine() + " ";
        }
        result.add(text);
        return result;
    }
}
