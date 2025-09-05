package com.example.homepage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class SOSFragment extends Fragment {

    private static final int PRESS_INTERVAL = 3000; // 3 seconds
    private int volumePressCount = 0;
    private Handler resetHandler = new Handler();
    private Runnable resetRunnable;

    private String selectedPhoneNumber = null;
    private TextView contactInfo;
    private Button selectContactBtn;

    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri contactUri = result.getData().getData();
                    if (contactUri != null) {
                        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
                        Cursor cursor = requireContext().getContentResolver().query(contactUri, projection, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            selectedPhoneNumber = cursor.getString(numberIndex);
                            contactInfo.setText("Contact: " + selectedPhoneNumber);
                            cursor.close();
                        }
                    }
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        contactInfo = view.findViewById(R.id.contactInfo1);
        selectContactBtn = view.findViewById(R.id.selectContactBtn1);

        selectContactBtn.setOnClickListener(v -> {
            Intent contactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            contactPickerLauncher.launch(contactIntent);
        });

        requireActivity().getWindow().getDecorView().setFocusableInTouchMode(true);
        requireActivity().getWindow().getDecorView().requestFocus();
        requireActivity().getWindow().getDecorView().setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                handleVolumePress();
                return true;
            }
            return false;
        });

        return view;
    }

    private void handleVolumePress() {
        volumePressCount++;

        if (volumePressCount == 1) {
            resetRunnable = () -> volumePressCount = 0;
            resetHandler.postDelayed(resetRunnable, PRESS_INTERVAL);
        }

        if (volumePressCount == 3) {
            volumePressCount = 0;
            resetHandler.removeCallbacks(resetRunnable);
            sendLocationSMS();
        }
    }

    private void sendLocationSMS() {
        if (selectedPhoneNumber == null) {
            Toast.makeText(requireContext(), "Please select an emergency contact first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Please grant SMS and Location permissions", Toast.LENGTH_LONG).show();
            return;
        }

        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        if (locationManager != null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        if (location != null) {
            String message = "SOS! I need help. My location: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();

            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(selectedPhoneNumber, null, message, null, null);
                Toast.makeText(requireContext(), "SOS sent to " + selectedPhoneNumber, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
        }
    }
}
