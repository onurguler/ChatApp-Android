package com.ogsoft.scobimessenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.androidnetworking.AndroidNetworking;
import com.ogsoft.scobimessenger.models.Token;
import com.ogsoft.scobimessenger.models.User;
import com.ogsoft.scobimessenger.services.LocalTokenService;
import com.ogsoft.scobimessenger.services.LocalUserService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AndroidNetworking.initialize(getApplicationContext());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user exists in local db
        User user = LocalUserService.getLocalUserFromPreferences(this);
        if (user.email == null) {
            // Send to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Check if token exists in local db
        Token token = LocalTokenService.getLocalTokenFromPreferences(this);
        if (token.key == null) {
            // Send to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
    }
}