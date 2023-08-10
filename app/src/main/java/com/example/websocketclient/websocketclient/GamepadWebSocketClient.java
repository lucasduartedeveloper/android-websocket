package com.example.websocketclient.websocketclient;

import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;

import com.example.websocketclient.databinding.FragmentFirstBinding;
import com.example.websocketclient.websocketclient.common.GamepadButton;
import com.example.websocketclient.websocketclient.common.GamepadState;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

public class GamepadWebSocketClient {
    private FragmentFirstBinding binding;
    private WebSocketClient webSocketClient;

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

    public GamepadWebSocketClient(FragmentFirstBinding binding) {
        requestNo = 0;
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

                                GamepadButton leftStick = state.getButton(99);
                                int x = (int) (pointer.x+(leftStick.getAxis_value()[0]*5f));
                                int y = (int) (pointer.y+(leftStick.getAxis_value()[1]*5f));
                                x = x < 0 ? 0 : x;
                                y = y < 0 ? y : 0;
                                pointer.set(x, y);
                                setBottomText("pointer at: "+pointer.x+", "+pointer.y);

                                String activeButtonsViewText = "";
                                ArrayList<GamepadButton> activeButtons = state.getActiveButtons();
                                for (int n = 0; n < activeButtons.size(); n++) {
                                    activeButtonsViewText +=
                                        "button "+activeButtons.get(n).getIndex()+": "+
                                        activeButtons.get(n).isPressed()+"\n";
                                }
                                setGamepadText(activeButtonsViewText);

                                String[] coordinates =
                                binding.editTextTextMultiLine.getText().toString().split("\\n");

                                ArrayList<String> command = state.createCommand(coordinates);
                                for (int n = 0; n < command.size(); n++) {
                                    runCommand("input "+command.get(n));
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
        }
        catch (Exception e) {
            setListText(e.getMessage(), true);
        }
    }
    private void runCommand(String command) {
        try {
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(command+"\n");
            //outputStream.writeBytes("screenrecord --time-limit 10 /sdcard/MyVideo.mp4\n");
            outputStream.flush();

            //outputStream.writeBytes("exit\n");
            //outputStream.flush();
            //su.waitFor();
        }
        catch (Exception e) {
            e.printStackTrace();
            //setListText(e.getMessage(), true);
        }
    }
}
