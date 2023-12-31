package com.example.websocketclient.websocketclient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.example.websocketclient.R;
import com.example.websocketclient.databinding.FragmentFirstBinding;
import com.example.websocketclient.websocketclient.common.GamepadButton;
import com.example.websocketclient.websocketclient.common.GamepadState;
import com.example.websocketclient.websocketclient.common.MathUtils;
import com.example.websocketclient.websocketclient.common.TouchCommand;
import com.example.websocketclient.websocketclient.common.TouchEvent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

public class GamepadWebSocketClient {

    private Activity activity;
    private FragmentFirstBinding binding;
    private WebSocketClient webSocketClient;

    private boolean isLandscape;

    private int requestNo;
    private ArrayList<String> receivedMessages;

    private String requestIdentifier() {
        requestNo += 1;
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < 8; n++) {
            sb.append('0');
        }
        String inputString = String.valueOf(requestNo);
        String result =
                "RQ-"+(sb.substring(inputString.length()) + inputString)+"-"+
                new Date().getTime();
        return result;
    }

    public GamepadWebSocketClient(FragmentFirstBinding binding, Activity activity) {
        this.activity = activity;
        this.requestNo = 0;
        this.isLandscape = false;
        this.receivedMessages = new ArrayList<String>();

        this.binding = binding;

        binding.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isLandscape = isChecked;
            }
        });

        binding.sendeventCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                defaultProgram = !isChecked;
            }
        });
    }

    public void createWebSocketClient() {
        URI uri;
        try {
            // Connect to local host
            uri = new URI("ws://192.168.15.6:3000/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            GamepadState state = new GamepadState();

            Thread messageThread;
            @Override
            public void onOpen() {
                Log.i("WebSocket", "Session is starting");
                setButtonText("Connected");
                //setListText("WebSocket Client started", true);

                requestSuperuser();
                pointer = new Point(0, 0);
                webSocketClient.send("PAPER|android-app|remote-gamepad-attach");

                if (messageThread != null) {
                    messageThread.stop();
                    messageThread = null;
                }
                messageThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                if (receivedMessages.size() > 0) {
                                    String message = receivedMessages.remove(0);
                                    processMessage(message);
                                }
                                //Thread.sleep(100);
                            }
                            catch (Exception e) {
                                setButtonText("Error");
                                e.printStackTrace();
                            }
                        }
                    }
                });
                messageThread.start();
            }

            private int currentNo = 0;
            private void processMessage(String s) throws InterruptedException {
                String[] msg = s.split("\\|");
                if (!msg[1].startsWith("android-app")) {
                    if (msg[2].startsWith("remote-gamepad-battery")) {
                        setBattery(Integer.valueOf(msg[3]));
                    } else {
                        if (msg[2].startsWith("remote-gamepad-data")) {
                            //startTimer();
                            if (Integer.valueOf(msg[3]) < currentNo) return;

                            currentNo = Integer.valueOf(msg[3]);
                            state.loadJson(msg[4]);

                            ArrayList<GamepadButton> buttons = state.getActiveButtons();
                            for (int n = 0; n < buttons.size(); n++) {
                                if (buttons.get(n).getIndex() < 90)
                                    setGamepadText("button " + buttons.get(n).getIndex() + ": " + buttons.get(n).getValue());
                                else {
                                    BigDecimal x = new BigDecimal(buttons.get(n).getAxis_value()[0])
                                            .setScale(2, RoundingMode.HALF_EVEN);
                                    BigDecimal y = new BigDecimal(buttons.get(n).getAxis_value()[1])
                                            .setScale(2, RoundingMode.HALF_EVEN);

                                    setGamepadText("button " + buttons.get(n).getIndex() + ": " +
                                            "[" + x + "," + y + "]");
                                }
                            }
                            if (buttons.size() == 0) setGamepadText("");

                            if (buttons.size() == 2) {
                                String indexes =
                                    String.valueOf(buttons.get(0).getIndex())+
                                    String.valueOf(buttons.get(1).getIndex());
                                if (indexes.contains("8") && indexes.contains("5")) {
                                    requestSuperuser();
                                    return;
                                }
                            }

                            String[] coordinates =
                                    binding.gameProfileSettings.getText().toString().split("\\n");

                            ArrayList<String> command = state.createCommand(coordinates);
                            for (int n = 0; n < command.size(); n++) {
                                setCommandHistoryText("", false);
                                String text = command.get(n);
                                if (text.startsWith("drag")) {
                                    text = text.replace("drag(", "");
                                    text = text.replace(")", "");
                                    String[] textValue = text.split(",");
                                    ArrayList<String> evArray = drag(
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2]),
                                            Integer.valueOf(textValue[3]),
                                            Integer.valueOf(textValue[4])
                                    );
                                    for (int k = 0; k < evArray.size(); k++) {
                                        setCommandHistoryText(evArray.get(k), false);
                                        runCommand(evArray.get(k));
                                    }
                                } else if (text.startsWith("tap")) {
                                    text = text.replace("tap(", "");
                                    text = text.replace(")", "");
                                    String[] textValue = text.split(",");
                                    ArrayList<String> evArray = tap(
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2])
                                    );
                                    for (int k = 0; k < evArray.size(); k++) {
                                        setCommandHistoryText(evArray.get(k), false);
                                        runCommand(evArray.get(k));
                                    }
                                } else if (text.startsWith("move")) {
                                    text = text.replace("move(", "");
                                    text = text.replace(")", "");
                                    String[] textValue = text.split(",");
                                    ArrayList<String> evArray = move(
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2])
                                    );
                                    for (int k = 0; k < evArray.size(); k++) {
                                        setCommandHistoryText(evArray.get(k), false);
                                        runCommand(evArray.get(k));
                                    }
                                } else if (text.startsWith("down")) {
                                    text = text.replace("down(", "");
                                    text = text.replace(")", "");
                                    String[] textValue = text.split(",");
                                    ArrayList<String> evArray = down(
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2])
                                    );
                                    for (int k = 0; k < evArray.size(); k++) {
                                        setCommandHistoryText(evArray.get(k), false);
                                        runCommand(evArray.get(k));
                                    }
                                } else if (text.startsWith("up")) {
                                    text = text.replace("up(", "");
                                    text = text.replace(")", "");
                                    String[] textValue = text.split(",");
                                    ArrayList<String> evArray = up(
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2])
                                    );
                                    for (int k = 0; k < evArray.size(); k++) {
                                        setCommandHistoryText(evArray.get(k), false);
                                        runCommand(evArray.get(k));
                                    }
                                } else if (text.startsWith("analog")) {
                                    text = text.replace("analog(", "");
                                    text = text.replace(")", "");
                                    String[] textValue = text.split(",");
                                    ArrayList<String> evArray = analog(
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2]),
                                            Float.valueOf(textValue[3]),
                                            Float.valueOf(textValue[4]),
                                            Integer.valueOf(textValue[5])
                                    );
                                    for (int k = 0; k < evArray.size(); k++) {
                                        setCommandHistoryText(evArray.get(k), false);
                                        runCommand(evArray.get(k));
                                    }
                                } else if (text.startsWith("circle")) {
                                    text = text.replace("circle(", "");
                                    text = text.replace(")", "");
                                    String[] textValue = text.split(",");
                                    ArrayList<String> evArray = circle(
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2]),
                                            Integer.valueOf(textValue[3]),
                                            Integer.valueOf(textValue[4]),
                                            Integer.valueOf(textValue[5]),
                                            textValue[6],
                                            textValue[7]
                                    );
                                    for (int k = 0; k < evArray.size(); k++) {
                                        setCommandHistoryText(evArray.get(k), false);
                                        runCommand(evArray.get(k));
                                        Thread.sleep(Integer.valueOf(textValue[8]));
                                    }
                                } else if (text.startsWith("drag_connect")) {
                                    text = text.replace("drag_connect(", "");
                                    text = text.replace(")", "");
                                    String[] textValue = text.split(",");
                                    ArrayList<String> evArray = drag_connect(
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2]),
                                            Integer.valueOf(textValue[3]),
                                            Integer.valueOf(textValue[4])
                                    );
                                    for (int k = 0; k < evArray.size(); k++) {
                                        setCommandHistoryText(evArray.get(k), false);
                                        runCommand(evArray.get(k));
                                    }
                                } else if (text.startsWith("su#")) {
                                    text = text.replace("su#", "");
                                    setCommandHistoryText(text, false);
                                    runCommand(text);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onTextReceived(String s) {
                receivedMessages.add(s);
            }

            private void runOnUiThread(Runnable runnable) {
            }

            @Override
            public void onBinaryReceived(byte[] data) {
            }

            @Override
            public void onPingReceived(byte[] data) {
            }

            @Override
            public void onPongReceived(byte[] data) {
            }

            @Override
            public void onException(Exception e) {
                setButtonText("Error");
                System.out.println(e.getMessage());
                //setListText(e.getMessage(), true);
            }

            @Override
            public void onCloseReceived(int reason, String description) {

            }

            @Override
            public void onCloseReceived() {
                Log.i("WebSocket", "Closed ");
                System.out.println("onCloseReceived");
            }
        };

        webSocketClient.setConnectTimeout(5000);
        webSocketClient.setReadTimeout(180000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }

    private Thread timerThread;
    private long timeStarted;
    public void startTimer() {
        if (timerThread != null) return;
        timeStarted = new Date().getTime();
        timerThread = new Thread(new Runnable() {
            public void run() {
                while(true) {
                    long ms = new Date().getTime() - timeStarted;
                    int minutes = (int)((ms/1000)/60);
                    int seconds = (int)((ms/1000)%60);
                    setTimerText(
                        String.format("%02d", minutes)+":"+
                        String.format("%02d", seconds)
                    );
                }
            }
        });
        timerThread.start();
    }

    public void stopTimer() {
        timerThread.stop();
        timerThread = null;
    }

    private void setTimerText(String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                binding.timerView.setText(text);
                binding.timerView.invalidate();
            }
        });
    }

    private void setButtonText(String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                binding.buttonFirst.setText(text);
            }
        });
    }

    private void setBottomText(String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                binding.textViewPointer.setText(text);
            }
        });
    }

    private void setGamepadText(String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                binding.gamepadState.setText("Gamepad:\n" +text);
            }
        });
    }

    private void setCommandHistoryText(String text, boolean append) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                String newText = "";
                if (append) {
                    newText = binding.commandHistory.getText().toString();
                    newText += text + "\n";
                }
                else {
                    newText = text;
                }
                binding.commandHistory.setText(newText);
            }
        });
    }

    private void setListText(String text, boolean append) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                String newText = "";
                if (append) {
                    newText = binding.gameProfileSettings.getText().toString();
                    newText += text + "\n";
                }
                else {
                    newText = text;
                }
                binding.gameProfileSettings.setText(newText);
            }
        });
    }

    private void setBattery(int value) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                binding.textView.setText("Battery: "+((100/5)*value)+"%");
            }
        });
    }

    private int eventCount = 0;

    private void setEventCount(String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                binding.eventCountView.setText(text);
            }
        });
    }

    boolean released;
    private Point pointer;
    private Process su;
    private void requestSuperuser() {
        try {
            su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes("cd /data/local/tmp\n");
            outputStream.flush();
        }
        catch (Exception e) {
            setListText(e.getMessage(), true);
        }
    }

    private void runCommand(String command) {
        runCommand(command, false);
    }

    private void runCommand(String command, boolean read) {
        try {
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(command+"\n");
            outputStream.flush();

            Log.i("root", command);
            setEventCount(String.valueOf(eventCount));
            eventCount += 1;

            DataInputStream inputStream = new DataInputStream(su.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            //su.waitFor();

            String line;
            if (read)
            while ((line = reader.readLine()) != null) {
                setCommandHistoryText(line, true);
            }

            if (eventCount % 300 == 0) {
                su.destroy();
                requestSuperuser();
            }
            //outputStream.writeBytes("exit\n");
            //outputStream.flush();
            //su.waitFor();
        }
        catch (Exception e) {
            e.printStackTrace();
            //setListText(e.getMessage(), true);
        }
    }

    private static boolean defaultProgram = false;
    public ArrayList<String> drag(int layerNo, int x1, int y1, int x2, int y2) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);
        Point p2 = new Point(x2, y2);
        if (isLandscape) p2 = rotateCoordinates(p2.x, p2.y);

        TouchCommand dragCommand = new TouchCommand(layerNo);
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, p1.x, p1.y);
        TouchEvent move = new TouchEvent(TouchEvent.Type.MOVE, p2.x, p2.y);
        TouchEvent up = new TouchEvent(TouchEvent.Type.UP, p2.x, p2.y);
        dragCommand.add(down);
        dragCommand.add(move);
        dragCommand.add(up);
        return defaultProgram ? dragCommand.toSendeventArray() : dragCommand.toSendeventLine();
    }

    public ArrayList<String> tap(int layerNo, int x1, int y1) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        TouchCommand tapCommand = new TouchCommand(layerNo);
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, p1.x, p1.y);
        TouchEvent up = new TouchEvent(TouchEvent.Type.UP, p1.x, p1.y);
        tapCommand.add(down);
        tapCommand.add(up);
        return defaultProgram ? tapCommand.toSendeventArray() : tapCommand.toSendeventLine();
    }

    public ArrayList<String> down(int layerNo, int x1, int y1) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        TouchCommand downCommand = new TouchCommand(layerNo);
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, p1.x, p1.y);
        downCommand.add(down);
        return defaultProgram ? downCommand.toSendeventArray() : downCommand.toSendeventLine();
    }

    public ArrayList<String> up(int layerNo, int x1, int y1) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        TouchCommand upCommand = new TouchCommand(layerNo);
        TouchEvent up = new TouchEvent(TouchEvent.Type.UP, p1.x, p1.y);
        upCommand.add(up);
        return defaultProgram ? upCommand.toSendeventArray() : upCommand.toSendeventLine();
    }

    public ArrayList<String> analog(int layerNo, int x1, int y1, float movX, float movY, int radius) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        Point position = new Point(p1.x, p1.y);
        PointF vector = new PointF(movX, movY);
        PointF normalizedVector = MathUtils.normalize(vector, 1);
        if (isLandscape)
        normalizedVector = MathUtils.rotate2d(new PointF(0f, 0f), normalizedVector, -90, true);
        position.set(
            (int) (position.x + (normalizedVector.x * radius)),
            (int) (position.y + (normalizedVector.y * radius))
        );

        TouchCommand moveCommand = new TouchCommand(layerNo);
        TouchEvent move = new TouchEvent(TouchEvent.Type.MOVE, position.x, position.y);
        moveCommand.add(move);
        return defaultProgram ? moveCommand.toSendeventArray() : moveCommand.toSendeventLine();
    }

    // complex methods
    public ArrayList<String> move(int layerNo, int x1, int y1) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        TouchCommand moveCommand = new TouchCommand(layerNo);
        TouchEvent move = new TouchEvent(TouchEvent.Type.DOWN, p1.x, p1.y);
        moveCommand.add(move);
        return defaultProgram ? moveCommand.toSendeventArray() : moveCommand.toSendeventLine();
    }

    public ArrayList<String> circle(int layerNo, int x1, int y1, float radius, int steps, int turns, String mode, String direction) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        PointF center = new PointF(p1.x, p1.y);
        PointF vector = new PointF(p1.x, p1.y-radius);
        if (isLandscape)
        vector = MathUtils.rotate2d(center, vector, -90, true);

        ArrayList<String> command = new ArrayList<String>();
        TouchCommand circleCommand = new TouchCommand(layerNo);
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, (int) vector.x, (int) vector.y);
        circleCommand.add(down);
        command.addAll(defaultProgram ? circleCommand.toSendeventArray() : circleCommand.toSendeventLine());
        for (int n = 0; n < ((turns*steps)+1); n++) {
            circleCommand = new TouchCommand(layerNo);
            if (mode.equals("open"))
            vector.y = p1.y - (radius-(radius-(n*((radius/(steps*turns))))));
            else if (mode.equals("close"))
            vector.y = p1.y - (radius-(n*((radius/(steps*turns)))));

            PointF rotation = new PointF(0, 0);
            if (direction.equals("left"))
            rotation = MathUtils.rotate2d(center, vector, (n*(360/steps)), true);
            else if (direction.equals("right"))

            rotation = MathUtils.rotate2d(center, vector, -(n*(360/steps)), true);
            TouchEvent move = new TouchEvent(TouchEvent.Type.DOWN, (int) rotation.x, (int) rotation.y);
            circleCommand.add(move);
            command.addAll(defaultProgram ? circleCommand.toSendeventArray() : circleCommand.toSendeventLine());
        }

        return command;
    }

    public ArrayList<String> drag_connect(int layerNo, int x1, int y1, int x2, int y2) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);
        Point p2 = new Point(x2, y2);
        if (isLandscape) p2 = rotateCoordinates(p2.x, p2.y);

        TouchCommand dragCommand = new TouchCommand(layerNo);
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, p1.x, p1.y);
        TouchEvent move = new TouchEvent(TouchEvent.Type.MOVE, p2.x, p2.y);
        dragCommand.add(down);
        dragCommand.add(move);
        return defaultProgram ? dragCommand.toSendeventArray() : dragCommand.toSendeventLine();
    }

    private Point rotateCoordinates(int x1, int y1) {
        int new_x1 = 720-y1;
        int new_y1 = x1;
        return new Point(new_x1, new_y1);
    }

    public void Dispose() {
        webSocketClient.close(0, 0, "user");
    }
}
