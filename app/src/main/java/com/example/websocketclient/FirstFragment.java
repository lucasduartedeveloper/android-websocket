package com.example.websocketclient;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.websocketclient.databinding.FragmentFirstBinding;
import com.example.websocketclient.websocketclient.GamepadWebSocketClient;
import com.example.websocketclient.websocketclient.service.FloatingViewService;
import com.example.websocketclient.websocketclient.service.FloatingWidgetShowService;
import com.example.websocketclient.websocketclient.service.FloatingWindowService;

public class FirstFragment extends Fragment {

    private GamepadWebSocketClient gamepadWebSocketClient;
    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        getActivity().setTitle("Gamepad Server");
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        //binding.commandHistory.setMovementMethod(new ScrollingMovementMethod());
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.buttonFirst.getText().equals("Connected")) {
                    gamepadWebSocketClient.Dispose();
                    binding.buttonFirst.setText("Stopped");
                }
                else {
                    binding.buttonFirst.setText("Starting...");
                    gamepadWebSocketClient = new GamepadWebSocketClient(binding, getActivity());
                    gamepadWebSocketClient.createWebSocketClient();
                }
            }
        });

        binding.buttonIme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static final int SYSTEM_ALERT_WINDOW_PERMISSION = 7;
    public void RuntimePermissionForUser() {

        Intent PermissionIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getContext().getPackageName()));

        startActivityForResult(PermissionIntent, SYSTEM_ALERT_WINDOW_PERMISSION);
    }

}