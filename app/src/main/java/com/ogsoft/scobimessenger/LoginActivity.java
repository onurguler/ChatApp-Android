package com.ogsoft.scobimessenger;

import androidx.appcompat.app.AppCompatActivity;

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
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.ogsoft.scobimessenger.models.User;
import com.ogsoft.scobimessenger.services.Tools;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText et_email, et_password;
    private TextView txt_error;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        txt_error = findViewById(R.id.txt_error);
    }

    public void onClickLogin(View view) {
        txt_error.setVisibility(View.GONE);

        String email = et_email.getText().toString().trim();
        String password = et_password.getText().toString();

        if (!Tools.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
        } else if (email.isEmpty()) {
            et_email.setError("Email cannot be empty");
        } else if (password.isEmpty()) {
            et_password.setError("Password cannot be empty");
        } else {
            pd = new ProgressDialog(this);
            pd.setMessage("Loading...");
            pd.setCancelable(false);
            pd.show();

            // Set the request body
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("email", email);
                jsonObject.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "Uncaughted error. Plaese try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Send post request
            AndroidNetworking.post("http://192.168.1.106:3000/api/v1/auth/signin")
                    .addJSONObjectBody(jsonObject)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            pd.hide();
                            try {
                                String token = response.getString("token");
                                SharedPreferences pref = getApplicationContext().getSharedPreferences("LocalUser", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("token", token);
                                // Save token to local db
                                JSONObject dataObject = response.optJSONObject("data");
                                if (dataObject != null) {
                                    JSONObject userObject = dataObject.optJSONObject("user");

                                    if (userObject != null) {
                                        String uuid = userObject.optString("_id");
                                        String name = userObject.optString("name");
                                        String email = userObject.optString("email");

                                        User loggedInUser = new User();
                                        loggedInUser.uuid = uuid;
                                        loggedInUser.name = name;
                                        loggedInUser.email = email;

                                        // Save loggedInUser to local db
                                        editor.putString("uuid", uuid);
                                        editor.putString("name", name);
                                        editor.putString("email", email);
                                    }
                                }
                                editor.commit();

                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
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
}