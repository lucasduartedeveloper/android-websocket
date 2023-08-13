package com.example.websocketclient;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
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


    private int profileNo;
    private String[] gameProfileNames;
    private String[] gameProfiles;
    private String[] gameProfilePackages;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        //binding.commandHistory.setMovementMethod(new ScrollingMovementMethod());
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.profileNo = 0;

        this.gameProfileNames = new String[]{
                "Blank Profile",
                "GTA Vice City",
                "Subway Surfers",
                "Tony Hawk 4"
        };
        this.gameProfilePackages = new String[]{
                "none",
                "com.rockstargames.gtavc",
                "com.kiloo.subwaysurf",
                "epsxe"
        };
        this.gameProfiles = new String[]{
                binding.getRoot().getResources().getString(R.string.blank_profile),
                binding.getRoot().getResources().getString(R.string.gta_vicecity),
                binding.getRoot().getResources().getString(R.string.subway_surfers),
                binding.getRoot().getResources().getString(R.string.epsxe_thps4)
        };

        binding.gameProfileSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileNo += 1;
                profileNo = profileNo > (gameProfiles.length-1) ? 0 : profileNo;
                binding.gameStartButton.setText(gameProfileNames[profileNo]);
                binding.gameProfileSettings.setText(gameProfiles[profileNo]);
            }
        });

        binding.gameStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mIntent = getActivity().getPackageManager()
                        .getLaunchIntentForPackage(gameProfilePackages[profileNo]);
                if (mIntent != null) {
                    try {
                        getActivity().startActivity(mIntent);
                        binding.uiCheckBox.setChecked(true);
                    } catch (ActivityNotFoundException err) {
                        Toast t = Toast.makeText(getActivity().getApplicationContext(),
                                "App is not found", Toast.LENGTH_SHORT);
                        t.show();
                    }
                }
            }
        });

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.buttonFirst.getText().equals("Connected")) {
                    gamepadWebSocketClient.Dispose();
                    binding.buttonFirst.setText("Stopped");
                }
                else {
                    binding.buttonFirst.setText("Starting...");
                    binding.uiCheckBox.setChecked(true);
                    gamepadWebSocketClient = new GamepadWebSocketClient(binding, getActivity());
                    gamepadWebSocketClient.createWebSocketClient();
                }
            }
        });

        binding.uiCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                binding.buttonOpen.setEnabled(!isChecked);
                binding.buttonFirst.setEnabled(!isChecked);
                binding.gameProfileSettings.setEnabled(!isChecked);
                binding.commandHistory.setEnabled(!isChecked);
                binding.checkBox.setEnabled(!isChecked);
                binding.gameStartButton.setEnabled(!isChecked);
                binding.gameProfileSelectButton.setEnabled(!isChecked);
                binding.sendeventCheckBox.setEnabled(!isChecked);
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