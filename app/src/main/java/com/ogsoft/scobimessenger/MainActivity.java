package com.ogsoft.scobimessenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ogsoft.scobimessenger.adapters.ConversationListAdapter;
import com.ogsoft.scobimessenger.models.Conversation;
import com.ogsoft.scobimessenger.models.Message;
import com.ogsoft.scobimessenger.models.Token;
import com.ogsoft.scobimessenger.models.User;
import com.ogsoft.scobimessenger.services.APIEndpoints;
import com.ogsoft.scobimessenger.services.ChatDatabaseHelper;
import com.ogsoft.scobimessenger.services.LocalTokenService;
import com.ogsoft.scobimessenger.services.LocalUserService;
import com.ogsoft.scobimessenger.services.Tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rv_conversationList;
    private ConversationListAdapter conversationListAdapter;

    private ArrayList<Conversation> conversationArrayList;

    private ProgressDialog pd;

    private User currentUser;
    private Token token;

    private ChatDatabaseHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndroidNetworking.initialize(getApplicationContext());

        helper = ChatDatabaseHelper.getInstance(this);

        rv_conversationList = findViewById(R.id.rv_conversationList);
        rv_conversationList.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user exists in local db
        currentUser = LocalUserService.getLocalUserFromPreferences(this);
        if (currentUser.username == null) {
            // Send to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Check if token exists in local db
        token = LocalTokenService.getLocalTokenFromPreferences(this);
        if (token.key == null) {
            // Send to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        conversationArrayList = helper.getAllConversations(currentUser);
        conversationListAdapter = new ConversationListAdapter(conversationArrayList, currentUser);
        rv_conversationList.setAdapter(conversationListAdapter);

        View.OnClickListener onConversationClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickConversation(view);
            }
        };

        conversationListAdapter.setOnItemClickListener(onConversationClickListener);

        getConversationsFromApi();
    }

    private void getConversationsFromApi() {
        if (Tools.isNetworkAvailable(this)) {
            if (token.key != null) {
                AndroidNetworking.get(APIEndpoints.getAllConversations)
                        .addHeaders("Authorization", Token.prefix + " " + token.key)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                                conversationArrayList.clear();
                                try {
                                    JSONObject dataObject = response.getJSONObject("data");
                                    JSONArray conversationsArray = dataObject.getJSONArray("conversations");

                                    for (int i = 0; i < conversationsArray.length(); i++) {
                                        JSONObject conversationObject = conversationsArray.getJSONObject(i);
                                        Conversation conversation = new Conversation();

                                        JSONArray participantsArray = conversationObject.getJSONArray("participants");
                                        for (int j = 0; j < participantsArray.length(); j++) {
                                            JSONObject participantObject = participantsArray.getJSONObject(j);
                                            User user = new User();
                                            user.uuid = participantObject.getString("_id");
                                            user.name = participantObject.getString("name");
                                            user.username = participantObject.getString("username");
                                            conversation.participants.add(user);
                                        }

                                        conversation.uuid = conversationObject.getString("_id");
                                        conversation.type = conversationObject.getString("type");
                                        conversation.createdAt = conversationObject.getString("createdAt");
                                        conversation.updatedAt = conversationObject.getString("updatedAt");

                                        JSONObject lastMessageObject = conversationObject.getJSONObject("lastMessage");
                                        Message message = new Message();
                                        message.uuid = lastMessageObject.getString("_id");
                                        message.user = lastMessageObject.getString("user");
                                        message.conversation = lastMessageObject.getString("conversation");
                                        message.text = lastMessageObject.getString("text");
                                        message.createdAt = lastMessageObject.getString("createdAt");
                                        message.updatedAt = lastMessageObject.getString("updatedAt");

                                        conversation.lastMessage = message;

                                        if (conversation.type.equals(Conversation.TYPE_PRIVATE)) {
                                            for (User participant: conversation.participants) {
                                                if (!participant.username.equals(currentUser.username)) {
                                                    conversation.toUser = participant;
                                                    break;
                                                }
                                            }
                                        }

                                        conversationArrayList.add(conversation);

                                        // Save conversation to local db
                                        helper.addOrUpdateConversation(conversation);
                                    }

                                    conversationListAdapter.notifyDataSetChanged();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onError(ANError anError) {

                            }
                        });
            }
        }
    }

    private void onClickConversation(View view) {
        RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) view.getTag();
        int position = viewHolder.getAdapterPosition();
        Conversation conversation = conversationArrayList.get(position);
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationUUID", conversation.uuid);
        intent.putExtra("conversationType", conversation.type);
        intent.putExtra("isNewConversation", false);
        if (conversation.type.equals(Conversation.TYPE_PRIVATE)) {
            if (conversation.toUser != null) {
                if (conversation.toUser.name != null && !conversation.toUser.name.isEmpty()) {
                    intent.putExtra("displayName", conversation.toUser.name);
                } else {
                    intent.putExtra("displayName", conversation.toUser.username);
                }
            }
        }
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_new_message) {
            showNewMessageDialog();
        }

        if (id == R.id.menu_settings) {
            showSettingsActivity();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showNewMessageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_message);

        final EditText et_username = new EditText(this);
        et_username.setInputType(InputType.TYPE_CLASS_TEXT);
        et_username.setHint("Enter recipient username");
        builder.setView(et_username);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton("Start conversation", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startNewConversation(et_username.getText().toString());
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        et_username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable)) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
    }

    private void startNewConversation(String username) {
        // TODO: Check conversation exists in local db
        if (!Tools.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        pd = new ProgressDialog(this);
        pd.setTitle("Loading...");
        pd.setCancelable(false);
        pd.show();

        AndroidNetworking.get(APIEndpoints.getUserByUsername)
                .addPathParameter("username", username)
                .addHeaders("Authorization", Token.prefix + " " + token.key)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        pd.hide();

                        try {
                            JSONObject dataObject = response.getJSONObject("data");
                            JSONObject userObject = dataObject.getJSONObject("user");
                            User user = new User();
                            user.uuid = userObject.getString("_id");
                            user.name = userObject.optString("name");
                            user.username = userObject.getString("username");
                            user.createdAt = userObject.getString("createdAt");
                            user.updatedAt = userObject.getString("updatedAt");

                            // Save user to local db
                            helper.addOrUpdateUser(user);

                            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                            intent.putExtra("conversationType", Conversation.TYPE_PRIVATE);
                            intent.putExtra("isNewConversation", true);
                            if (user.name != null && !user.name.isEmpty()) {
                                intent.putExtra("displayName", user.name);
                            } else {
                                intent.putExtra("displayName", user.username);
                            }
                            intent.putExtra("recipientUsername", user.username);
                            startActivity(intent);
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

    private void showSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}