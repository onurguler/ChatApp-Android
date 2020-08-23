package com.ogsoft.scobimessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.ogsoft.scobimessenger.adapters.MessageListAdapter;
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

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rv_messageList;
    private MessageListAdapter messageListAdapter;

    private String conversationUUID;
    private Conversation conversation;
    private User currentUser;
    private Token token;

    private ArrayList<Message> messageArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUser = LocalUserService.getLocalUserFromPreferences(this);
        token = LocalTokenService.getLocalTokenFromPreferences(this);

        if (currentUser.email == null || token.key == null) {
            // TODO: Authentication error
            finish();
        }

        conversationUUID = getIntent().getStringExtra("conversationUUID");
        conversation = new Conversation();
        messageArrayList = new ArrayList<Message>();

        rv_messageList = findViewById(R.id.rv_messageList);
        rv_messageList.setLayoutManager(new LinearLayoutManager(this));
        messageListAdapter = new MessageListAdapter(this, messageArrayList, currentUser, conversation);
        rv_messageList.setAdapter(messageListAdapter);

        getConversationAndMessagesFromApi();
    }

    private void getConversationAndMessagesFromApi() {
        if (Tools.isNetworkAvailable(this)) {
            AndroidNetworking.get(APIEndpoints.getConversation)
                    .addPathParameter("id", conversationUUID)
                    .addHeaders("Authorization", Token.prefix + " " + token.key)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONObject dataObject = response.getJSONObject("data");
                                JSONObject conversationObject = dataObject.getJSONObject("conversation");

                                conversation.participants.clear();

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

                                if (conversation.type.equals(Conversation.TYPE_PRIVATE)) {
                                    for (User participant: conversation.participants) {
                                        if (!participant.email.equals(currentUser.email)) {
                                            conversation.toUser = participant;
                                            break;
                                        }
                                    }
                                }
                                getConversationMessagesFromApi();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                System.out.println("JSON error");
                            }
                        }

                        @Override
                        public void onError(ANError anError) {

                        }
                    });
        }
    }

    private void getConversationMessagesFromApi() {
        if (Tools.isNetworkAvailable(this)) {
            AndroidNetworking.get(APIEndpoints.getConversationMessages)
                    .addPathParameter("id", conversationUUID)
                    .addHeaders("Authorization", Token.prefix + " " + token.key)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            messageArrayList.clear();

                            try {
                                JSONObject dataObject = response.getJSONObject("data");
                                JSONArray messagesArray = dataObject.getJSONArray("messages");

                                for (int i = 0; i < messagesArray.length(); i++) {
                                    JSONObject messageObject = messagesArray.getJSONObject(i);
                                    Message message = new Message();
                                    message.uuid = messageObject.getString("_id");
                                    message.user = messageObject.getString("user");
                                    message.conversation = messageObject.getString("conversation");
                                    message.text = messageObject.getString("text");
                                    message.createdAt = messageObject.getString("createdAt");
                                    message.updatedAt = messageObject.getString("updatedAt");

                                    messageArrayList.add(message);
                                }
                                messageListAdapter.notifyDataSetChanged();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                System.out.println("JSON error");
                            }
                        }

                        @Override
                        public void onError(ANError anError) {

                        }
                    });
        }
    }
}