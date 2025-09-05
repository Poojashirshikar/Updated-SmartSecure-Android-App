package com.example.homepage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Locale;

import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.provider.Telephony;


public class HomeFragment extends Fragment {

    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    private static final int PERMISSIONS_REQUEST = 200;

    private ImageButton imageButton;
    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0.0, longitude = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageButton = view.findViewById(R.id.image_mick);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        requestPermissions();
        checkAndRequestDefaultSmsApp();

        imageButton.setOnClickListener(v -> startVoiceInput());
    }


    private void checkAndRequestDefaultSmsApp() {
        if (!requireContext().getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(requireContext()))) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, requireContext().getPackageName());
            startActivity(intent);
            Toast.makeText(requireContext(), "Please set this app as the default SMS app to enable emergency SMS", Toast.LENGTH_LONG).show();
        }
    }


    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        ArrayList<String> listPermissionsNeeded = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST);
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the contact name...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Your device does not support Speech Input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == requireActivity().RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenName = result.get(0);
                findContactAndSendSMS(spokenName);
            }
        }
    }

    private void findContactAndSendSMS(String name) {
        ContentResolver cr = requireActivity().getContentResolver();
        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);

        if (cursor != null) {
            boolean found = false;
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                if (contactName != null && contactName.equalsIgnoreCase(name.trim())) {
                    found = true;
                    getLocationAndSendSMS(phoneNumber);
                    break;
                }
            }
            cursor.close();

            if (!found) {
                Toast.makeText(requireContext(), "Contact not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocationAndSendSMS(String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        sendSMS(phoneNumber);
                    } else {
                        Toast.makeText(requireContext(), "Could not get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendSMS(String phoneNumber) {
        String message = "Emergency! I need help. My current location: " +
                "https://maps.google.com/?q=" + latitude + "," + longitude;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(SubscriptionManager.getDefaultSubscriptionId());
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);

        Toast.makeText(requireContext(), "Emergency SMS sent!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(requireContext(), "All permissions are required for the app to function!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
