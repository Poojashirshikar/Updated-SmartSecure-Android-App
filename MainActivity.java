package com.example.homepage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.Manifest;
import android.Manifest;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout;
    private com.example.homepage.DatabaseHelper databaseHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadFragment(new HomeFragment(),true);
        bottomNavigationView = findViewById(R.id.bottomNavView);
        frameLayout = findViewById(R.id.frameLayout);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION}, 100);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, new SOSFragment())
                .commit();


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if (itemId == R.id.navHome) {
                    loadFragment(new HomeFragment(),false);

                } else if (itemId == R.id.navFeatures) {
                    loadFragment(new FeaturesFragment(),false);

                } else if (itemId == R.id.navYourInfo) {
                    loadFragment(new YourInfoFragment(),false);

                }

                //loadFragment(new HomeFragment(),true);
                return true;
            }
        });


        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new HomeFragment())
                .commit();


    }




    private void loadFragment(Fragment fragment, boolean isAppInitialized) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (isAppInitialized) {
            fragmentTransaction.add(R.id.frameLayout,fragment);
        } else {
            fragmentTransaction.replace(R.id.frameLayout,fragment);
        }
        fragmentTransaction.commit();
    }







}