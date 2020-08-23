package com.ogsoft.scobimessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

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
import com.ogsoft.scobimessenger.services.LocalTokenService;
import com.ogsoft.scobimessenger.services.LocalUserService;
import com.ogsoft.scobimessenger.services.Tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ConversationListAdapter conversationListAdapter;

    private ArrayList<Conversation> conversationArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndroidNetworking.initialize(getApplicationContext());

        RecyclerView rv_conversationList = findViewById(R.id.rv_conversationList);
        conversationArrayList = new ArrayList<Conversation>();
        conversationListAdapter = new ConversationListAdapter(conversationArrayList, LocalUserService.getLocalUserFromPreferences(this));
        rv_conversationList.setLayoutManager(new LinearLayoutManager(this));
        rv_conversationList.setAdapter(conversationListAdapter);

        getConversationsFromApi();
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

    private void getConversationsFromApi() {
        if (Tools.isNetworkAvailable(this)) {
            Token token = LocalTokenService.getLocalTokenFromPreferences(this);

            if (token.key != null) {
                AndroidNetworking.get(APIEndpoints.getAllConversations)
                        .addHeaders("Authorization", Token.prefix + " " + token.key)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
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
                                            user.email = participantObject.getString("email");
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

                                        conversationArrayList.add(conversation);
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
}