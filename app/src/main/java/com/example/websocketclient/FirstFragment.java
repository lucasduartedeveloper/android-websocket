package com.example.websocketclient;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.websocketclient.databinding.FragmentFirstBinding;
import com.example.websocketclient.websocketclient.FloatingWidgetShowService;
import com.example.websocketclient.websocketclient.GamepadWebSocketClient;

public class FirstFragment extends Fragment {

    private GamepadWebSocketClient gamepadWebSocketClient;
    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.buttonFirst.setText("Starting...");
                binding.textviewFirst.setText("");
                gamepadWebSocketClient = new GamepadWebSocketClient(binding);
                gamepadWebSocketClient.createWebSocketClient();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    getActivity().startService(new Intent(getContext(), FloatingWidgetShowService.class));
                    //getActivity().finish();
                } else if (Settings.canDrawOverlays(getContext())) {
                    getActivity().startService(new Intent(getContext(), FloatingWidgetShowService.class));
                    //getActivity().finish();
                } else {
                    RuntimePermissionForUser();
                    Toast.makeText(getContext(), "System Alert Window Permission Is Required For Floating Widget.", Toast.LENGTH_LONG).show();
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