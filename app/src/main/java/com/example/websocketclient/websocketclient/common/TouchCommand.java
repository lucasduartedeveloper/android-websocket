package com.example.websocketclient.websocketclient.common;

import java.util.ArrayList;

public class TouchCommand {

    private ArrayList<TouchEvent> touchEvents;

    public TouchCommand() {
        touchEvents = new ArrayList<TouchEvent>();
    }

    public void add(TouchEvent event) {
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
        //result.add("sendevent /dev/input/event3 0 2 0");
        //result.add("sendevent /dev/input/event3 0 0 0"); // end of report
        return result;
    }

    public String toSendeventLine() {
        String result = "sendevent_line /dev/input/event3 ";
        for (int n = 0; n < touchEvents.size(); n++) {
             result += touchEvents.get(n).toSendeventLine() + " ";
        }
        return result;
    }
}
