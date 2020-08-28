package com.ogsoft.scobimessenger;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.gson.JsonObject;
import com.ogsoft.scobimessenger.models.Token;
import com.ogsoft.scobimessenger.models.User;
import com.ogsoft.scobimessenger.services.APIEndpoints;
import com.ogsoft.scobimessenger.services.ChatDatabaseHelper;
import com.ogsoft.scobimessenger.services.LocalTokenService;
import com.ogsoft.scobimessenger.services.LocalUserService;
import com.ogsoft.scobimessenger.services.Tools;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class ProfileEditActivity extends AppCompatActivity {

    private TextView tv_name, tv_username, tv_email;

    private User currentUser;

    private ChatDatabaseHelper helper;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        helper = ChatDatabaseHelper.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbarProfileEditActivity);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.profile);
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
        tv_email = findViewById(R.id.tv_email);

        updateUI();
    }

    public void onClickName(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.name);

        final EditText et_name = new EditText(this);
        et_name.setInputType(InputType.TYPE_CLASS_TEXT);
        et_name.setHint(R.string.enter_name);
        et_name.setText(currentUser.name);
        builder.setView(et_name);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendUpdateUserRequest(et_name.getText().toString(), tv_username.getText().toString(), tv_email.getText().toString());
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        et_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals(currentUser.name)) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
    }

    public void onClickUsername(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.username);

        final EditText et_username = new EditText(this);
        et_username.setInputType(InputType.TYPE_CLASS_TEXT);
        et_username.setHint(R.string.enter_username);
        et_username.setText(currentUser.username);
        builder.setView(et_username);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendUpdateUserRequest(tv_name.getText().toString(), et_username.getText().toString(), tv_email.getText().toString());
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        et_username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals(currentUser.username) || editable.toString().trim().isEmpty()) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
    }

    public void onClickEmail(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.email);

        final EditText et_email = new EditText(this);
        et_email.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        et_email.setHint(R.string.enter_email);
        et_email.setText(currentUser.email);
        builder.setView(et_email);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendUpdateUserRequest(tv_name.getText().toString(), tv_username.getText().toString(), et_email.getText().toString());
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        et_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals(currentUser.email) || editable.toString().trim().isEmpty()) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
    }

    private void sendUpdateUserRequest(String name, String username, String email) {
        Token token = LocalTokenService.getLocalTokenFromPreferences(this);

        if (!Tools.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject bodyObject = new JSONObject();

        try {
            bodyObject.put("name", name);
            bodyObject.put("username", username);
            bodyObject.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        pd = new ProgressDialog(this);
        pd.setTitle("Loading...");
        pd.setCancelable(false);
        pd.show();

        AndroidNetworking.patch(APIEndpoints.updateMe)
                .addHeaders("Authorization", Token.prefix + " " + token.key)
                .addJSONObjectBody(bodyObject)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject dataObject = response.getJSONObject("data");
                            JSONObject userObject = dataObject.getJSONObject("user");

                            User user = new User();
                            user.uuid = userObject.getString("_id");
                            user.name = userObject.getString("name");
                            user.email = userObject.getString("email");
                            user.username = userObject.optString("username");
                            user.createdAt = userObject.optString("createdAt");
                            user.updatedAt = userObject.optString("updatedAt");

                            // update user on sqlite
                            helper.addOrUpdateUser(user);

                            // save user to local db
                            SharedPreferences pref = getApplicationContext().getSharedPreferences("LocalUser", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();

                            editor.putString("uuid", user.uuid);
                            editor.putString("name", user.name);
                            editor.putString("username", user.username);
                            editor.putString("email", user.email);

                            editor.apply();
                            editor.commit();

                            updateUI();
                            pd.hide();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        pd.hide();
                    }
                });
    }

    private void updateUI() {
        currentUser = LocalUserService.getLocalUserFromPreferences(this);

        if (currentUser.name != null) {
            tv_name.setText(currentUser.name);
        } else {
            tv_name.setText("");
        }

        if (currentUser.username != null) {
            tv_username.setText(currentUser.username);
        } else {
            tv_username.setText("");
        }

        if (currentUser.email != null) {
            tv_email.setText(currentUser.email);
        } else {
            tv_email.setText("");
        }
    }
}