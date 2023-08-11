package com.example.websocketclient.websocketclient;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.websocketclient.R;
import com.example.websocketclient.databinding.FragmentFirstBinding;
import com.example.websocketclient.websocketclient.common.GamepadButton;
import com.example.websocketclient.websocketclient.common.GamepadState;
import com.example.websocketclient.websocketclient.common.TouchCommand;
import com.example.websocketclient.websocketclient.common.TouchEvent;

import org.json.JSONArray;
import org.json.JSONObject;

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

        this.gameProfileNames = new String[]{
            "Subway Surfers"
        };
        this.gameProfilePackages = new String[]{
            "com.kiloo.subwaysurf"
        };
        this.gameProfiles = new String[]{
            binding.getRoot().getResources().getString(R.string.subway_surfers)
        };

        binding.gameProfile.setOnClickListener(new View.OnClickListener() {
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
        this.binding = binding;
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

                                String[] coordinates =
                                binding.editTextTextMultiLine.getText().toString().split("\\n");

                                ArrayList<String> command = state.createCommand(coordinates);
                                for (int n = 0; n < command.size(); n++) {
                                    setCommandHistoryText("", false);
                                    String text = command.get(n);
                                    if (text.startsWith("drag")) {
                                        text = text.replace("drag(","");
                                        text = text.replace(")","");
                                        String[] textValue = text.split(",");
                                        ArrayList<String> evArray = drag(
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
                    newText = binding.editTextTextMultiLine.getText().toString();
                    newText += text + "\n";
                }
                else {
                    newText = text;
                }
                binding.editTextTextMultiLine.setText(newText);
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

    public ArrayList<String> drag(int x1, int y1, int x2, int y2) {
        TouchCommand dragCommand = new TouchCommand();
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, 0, x1, y1);
        TouchEvent move = new TouchEvent(TouchEvent.Type.MOVE, 0, x2, y2);
        TouchEvent up = new TouchEvent(TouchEvent.Type.UP, 0, x2, y2);
        dragCommand.add(down);
        dragCommand.add(move);
        dragCommand.add(up);
        return dragCommand.toSendeventArray();
    }

    public ArrayList<String> tap(int x1, int y1) {
        TouchCommand tapCommand = new TouchCommand();
        TouchEvent down = new TouchEvent(TouchEvent.Type.DOWN, 0, x1, y1);
        TouchEvent up = new TouchEvent(TouchEvent.Type.UP, 0, x1, y1);
        tapCommand.add(down);
        tapCommand.add(up);
        return tapCommand.toSendeventArray();
    }
}
