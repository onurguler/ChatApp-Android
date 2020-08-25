package com.ogsoft.scobimessenger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.ogsoft.scobimessenger.models.User;
import com.ogsoft.scobimessenger.services.APIEndpoints;
import com.ogsoft.scobimessenger.services.ChatDatabaseHelper;
import com.ogsoft.scobimessenger.services.Tools;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText et_name, et_username, et_email, et_password, et_password_confirm;
    private TextView txt_error;

    private ProgressDialog pd;

    private ChatDatabaseHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        helper = ChatDatabaseHelper.getInstance(this);

        et_name = findViewById(R.id.et_name);
        et_username = findViewById(R.id.et_username);
        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        et_password_confirm = findViewById(R.id.et_password_confirm);
        txt_error = findViewById(R.id.txt_error);
    }

    public void onClickSignUp(View view) {
        txt_error.setVisibility(View.GONE);

        String name = et_name.getText().toString().trim();
        String username = et_username.getText().toString().trim();
        String email = et_email.getText().toString().trim();
        String password = et_password.getText().toString();
        String password_confirm = et_password_confirm.getText().toString();

        if (!Tools.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
        } else if (name.isEmpty()) {
            et_name.setError("Name cannot be empty");
        } else if (username.isEmpty()) {
            et_username.setError("Username cannot be empty");
        } else if (email.isEmpty()) {
            et_email.setError("Email cannot be empty");
        } else if (password.isEmpty()) {
            et_password.setError("Password cannot be empty");
        } else if (!password.equals(password_confirm)) {
            et_password.setError("Passwords does not match");
        } else {
            pd = new ProgressDialog(this);
            pd.setTitle("Loading...");
            pd.setCancelable(false);
            pd.show();

            // Set the request body
            JSONObject body = new JSONObject();

            try {
                body.put("name", name);
                body.put("username", username);
                body.put("email", email);
                body.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
                txt_error.setText("Uncaughted error. Please try again.");
                txt_error.setVisibility(View.VISIBLE);
            }

            // Send post request to signup endpoint
            AndroidNetworking.post(APIEndpoints.signUp)
                    .addJSONObjectBody(body)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            pd.hide();
                            try {
                                String token = response.getString("token");

                                // Save token to local db
                                SharedPreferences pref = getApplicationContext().getSharedPreferences("LocalUser", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("token", token);

                                JSONObject dataObject = response.optJSONObject("data");
                                if (dataObject != null) {
                                    JSONObject userObject = dataObject.optJSONObject("user");

                                    if (userObject != null) {
                                        String uuid = userObject.optString("_id");
                                        String name = userObject.optString("name");
                                        String email = userObject.optString("email");
                                        String username = userObject.optString("username");
                                        String createdAt = userObject.optString("createdAt");
                                        String updatedAt = userObject.optString("updatedAt");

                                        User loggedInUser = new User();
                                        loggedInUser.uuid = uuid;
                                        loggedInUser.name = name;
                                        loggedInUser.email = email;
                                        loggedInUser.username = username;
                                        loggedInUser.createdAt = createdAt;
                                        loggedInUser.updatedAt = updatedAt;

                                        // Save loggedInUser to local db
                                        helper.addOrUpdateUser(loggedInUser);

                                        editor.putString("uuid", uuid);
                                        editor.putString("name", name);
                                        editor.putString("username", username);
                                        editor.putString("email", email);
                                    }
                                }
                                editor.commit();

                                setResult(Activity.RESULT_OK);
                                finish();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                txt_error.setText("Uncaughted error. Please try again.");
                                txt_error.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onError(ANError anError) {
                            pd.hide();
                            try {
                                JSONObject errorObject = new JSONObject(anError.getErrorBody());
                                String message = errorObject.optString("message");
                                txt_error.setText(message);
                                txt_error.setVisibility(View.VISIBLE);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                txt_error.setText("Uncaughted error. Please try again.");
                                txt_error.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }
    }

    public void onClickLogin(View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
}