package com.example.homepage;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.SmsManager;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

public class VolumeButtonReceiver extends BroadcastReceiver {

    private static final int PRESS_INTERVAL = 1000; // 1 second
    private int volumePressCount = 0;
    private long lastPressTime = 0;
    private final Context context;

    public VolumeButtonReceiver(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPressTime <= PRESS_INTERVAL) {
            volumePressCount++;
        } else {
            volumePressCount = 1;
        }
        lastPressTime = currentTime;

        if (volumePressCount >= 3) {
            sendEmergencySMS();
            volumePressCount = 0;
        }
    }

    private void sendEmergencySMS() {
        SharedPreferences prefs = context.getSharedPreferences("SOSApp", Context.MODE_PRIVATE);
        String emergencyContact = prefs.getString("emergency_contact", "");

        if (emergencyContact.isEmpty()) {
            Toast.makeText(context, "No emergency contact selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permissions not granted!", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null)
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        String message = "Emergency! I need help. ";
        if (location != null) {
            message += "My location: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
        } else {
            message += "Location not available.";
        }

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(emergencyContact, null, message, null, null);
        Toast.makeText(context, "SOS message sent!", Toast.LENGTH_SHORT).show();
    }
}
