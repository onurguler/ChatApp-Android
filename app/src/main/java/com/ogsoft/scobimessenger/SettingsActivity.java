package com.ogsoft.scobimessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ogsoft.scobimessenger.models.Token;
import com.ogsoft.scobimessenger.models.User;
import com.ogsoft.scobimessenger.services.LocalTokenService;
import com.ogsoft.scobimessenger.services.LocalUserService;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private TextView tv_name, tv_username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbarSettingsActivity);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        tv_name = findViewById(R.id.tv_name);
        tv_username = findViewById(R.id.tv_username);
    }

    @Override
    protected void onStart() {
        super.onStart();

        User currentUser = LocalUserService.getLocalUserFromPreferences(this);

        if (currentUser.name != null) {
            tv_name.setText(currentUser.name);
            tv_name.setVisibility(View.VISIBLE);
        } else {
            tv_name.setText("");
            tv_name.setVisibility(View.GONE);
        }

        if (currentUser.username != null) {
            tv_username.setText(currentUser.username);
            tv_username.setVisibility(View.VISIBLE);
        } else {
            tv_username.setText("");
            tv_username.setVisibility(View.GONE);
        }
    }

    public void onClickProfile(View view) {
        Intent intent = new Intent(this, ProfileEditActivity.class);
        startActivity(intent);
    }
}