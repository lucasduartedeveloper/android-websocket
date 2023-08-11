package com.example.websocketclient.websocketclient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Point;
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
import com.example.websocketclient.websocketclient.common.TouchCommand;
import com.example.websocketclient.websocketclient.common.TouchEvent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

public class GamepadWebSocketClient {

    private Activity activity;
    private FragmentFirstBinding binding;
    private WebSocketClient webSocketClient;

    private boolean isLandscape;

    private int profileNo;
    private String[] gameProfileNames;
    private String[] gameProfiles;
    private String[] gameProfilePackages;

    private int requestNo;

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
        this.profileNo = 0;
        this.isLandscape = false;

        this.gameProfileNames = new String[]{
            "Subway Surfers",
            "Tony Hawk 4"
        };
        this.gameProfilePackages = new String[]{
            "com.kiloo.subwaysurf",
            "epsxe"
        };
        this.gameProfiles = new String[]{
            binding.getRoot().getResources().getString(R.string.subway_surfers),
            binding.getRoot().getResources().getString(R.string.epsxe_thps4)
        };

        this.binding = binding;
        binding.gameProfileSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileNo += 1;
                profileNo = profileNo > gameProfiles.length ? 0 : profileNo;
                binding.gameStartButton.setText(gameProfileNames[profileNo]);
                binding.gameProfileSettings.setText(gameProfiles[profileNo]);
            }
        });

        binding.gameStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mIntent = activity.getPackageManager()
                        .getLaunchIntentForPackage(gameProfilePackages[profileNo]);
                if (mIntent != null) {
                    try {
                        activity.startActivity(mIntent);
                    } catch (ActivityNotFoundException err) {
                        Toast t = Toast.makeText(activity.getApplicationContext(),
                                "App is not found", Toast.LENGTH_SHORT);
                        t.show();
                    }
                }
            }
        });

        binding.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isLandscape = isChecked;
            }
        });

        binding.uiCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                binding.buttonIme.setEnabled(!isChecked);
                binding.buttonFirst.setEnabled(!isChecked);
                binding.gameProfileSettings.setEnabled(!isChecked);
                binding.commandHistory.setEnabled(!isChecked);
                binding.checkBox.setEnabled(!isChecked);
                binding.gameStartButton.setEnabled(!isChecked);
                binding.gameProfileSelectButton.setEnabled(!isChecked);
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
            @Override
            public void onOpen() {
                Log.i("WebSocket", "Session is starting");
                setButtonText("Connected");
                //setListText("WebSocket Client started", true);

                requestSuperuser();
                pointer = new Point(0, 0);
                webSocketClient.send("PAPER|android-app|remote-gamepad-attach");
            }

            GamepadState state = new GamepadState();
            @Override
            public void onTextReceived(String s) {
                Log.i("WebSocket", "Message received");
                try {
                    String[] msg = s.split("\\|");
                    if (!msg[1].startsWith("android-app")) {
                        if (msg[2].startsWith("remote-gamepad-battery")) {
                            setBattery(Integer.valueOf(msg[3]));
                        }
                        else {
                            if (msg[2].startsWith("remote-gamepad-seq")) {
                                state.loadJson(msg[4]);

                                ArrayList<GamepadButton> buttons = state.getActiveButtons();
                                for (int n = 0; n < buttons.size(); n++) {
                                    setGamepadText("button "+buttons.get(n).getIndex()+": "+buttons.get(n).getAxis_value());
                                }

                                String[] coordinates =
                                binding.gameProfileSettings.getText().toString().split("\\n");

                                ArrayList<String> command = state.createCommand(coordinates);
                                for (int n = 0; n < command.size(); n++) {
                                    setCommandHistoryText("", false);
                                    String text = command.get(n);
                                    if (text.startsWith("drag")) {
                                        text = text.replace("drag(","");
                                        text = text.replace(")","");
                                        String[] textValue = text.split(",");
                                        ArrayList<String> evArray = drag(
                                            0,
                                            Integer.valueOf(textValue[0]),
                                            Integer.valueOf(textValue[1]),
                                            Integer.valueOf(textValue[2]),
                                            Integer.valueOf(textValue[3])
                                        );
                                        for (int k = 0; k < evArray.size(); k++) {
                                            setCommandHistoryText(evArray.get(k), true);
                                            runCommand(evArray.get(k));
                                        }
                                    }
                                    else if (text.startsWith("tap")) {
                                        text = text.replace("tap(","");
                                        text = text.replace(")","");
                                        String[] textValue = text.split(",");
                                        ArrayList<String> evArray = tap(
                                                0,
                                                Integer.valueOf(textValue[0]),
                                                Integer.valueOf(textValue[1])
                                        );
                                        for (int k = 0; k < evArray.size(); k++) {
                                            setCommandHistoryText(evArray.get(k), true);
                                            runCommand(evArray.get(k));
                                        }
                                    }
                                    else if (text.startsWith("down")) {
                                        text = text.replace("down(","");
                                        text = text.replace(")","");
                                        String[] textValue = text.split(",");
                                        ArrayList<String> evArray = down(
                                                0,
                                                Integer.valueOf(textValue[0]),
                                                Integer.valueOf(textValue[1])
                                        );
                                        for (int k = 0; k < evArray.size(); k++) {
                                            setCommandHistoryText(evArray.get(k), true);
                                            runCommand(evArray.get(k));
                                        }
                                    }
                                    else if (text.startsWith("up")) {
                                        text = text.replace("up(","");
                                        text = text.replace(")","");
                                        String[] textValue = text.split(",");
                                        ArrayList<String> evArray = up(
                                                0,
                                                Integer.valueOf(textValue[0]),
                                                Integer.valueOf(textValue[1])
                                        );
                                        for (int k = 0; k < evArray.size(); k++) {
                                            setCommandHistoryText(evArray.get(k), true);
                                            runCommand(evArray.get(k));
                                        }
                                    }
                                }
                            }

                            String nextRequest =
                                    "PAPER|android-app|remote-gamepad-get|" + requestIdentifier();
                            webSocketClient.send(nextRequest);
                        }
                    }
                } catch (Exception e) {
                    setButtonText("Error");
                    e.printStackTrace();
                }
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
                System.out.println(e.getMessage());
                Log.i("WebSocket", "Message received");
                setButtonText("Error");
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
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
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
                binding.textView2.setText(text);
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

    boolean released;
    private Point pointer;
    private Process su;
    private void requestSuperuser() {
        try {
            su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes("cd /storage/emulated/0/Download/touch-events/\n");
            outputStream.flush();
        }
        catch (Exception e) {
            setListText(e.getMessage(), true);
        }
    }
    private void runCommand(String command) {
        try {
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(command+"\n");
            outputStream.flush();
            //su.waitFor();

            DataInputStream inputStream = new DataInputStream(su.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            //su.waitFor();

            //outputStream.writeBytes("exit\n");
            //outputStream.flush();
            //su.waitFor();
        }
        catch (Exception e) {
            e.printStackTrace();
            //setListText(e.getMessage(), true);
        }
    }

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
        return dragCommand.toSendeventArray();
    }

    public ArrayList<String> tap(int layerNo, int x1, int y1) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        TouchCommand tapCommand = new TouchCommand(layerNo);
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, p1.x, p1.y);
        TouchEvent up = new TouchEvent(TouchEvent.Type.UP, p1.x, p1.y);
        tapCommand.add(down);
        tapCommand.add(up);
        return tapCommand.toSendeventArray();
    }

    public ArrayList<String> down(int layerNo, int x1, int y1) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        TouchCommand downCommand = new TouchCommand(layerNo);
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, p1.x, p1.y);
        downCommand.add(down);
        return downCommand.toSendeventArray();
    }

    public ArrayList<String> up(int layerNo, int x1, int y1) {
        Point p1 = new Point(x1, y1);
        if (isLandscape) p1 = rotateCoordinates(p1.x, p1.y);

        TouchCommand upCommand = new TouchCommand(layerNo);
        TouchEvent up = new TouchEvent(TouchEvent.Type.UP, p1.x, p1.y);
        upCommand.add(up);
        return upCommand.toSendeventArray();
    }

    private Point rotateCoordinates(int x1, int y1) {
        int new_x1 = 720-y1;
        int new_y1 = x1;
        return new Point(new_x1, new_y1);
    }
}
