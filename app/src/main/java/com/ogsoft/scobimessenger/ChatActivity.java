package com.ogsoft.scobimessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.ogsoft.scobimessenger.adapters.MessageListAdapter;
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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ChatActivity extends AppCompatActivity {

    private EditText et_message;

    private RecyclerView rv_messageList;
    private MessageListAdapter messageListAdapter;

    private String conversationUUID;
    private Conversation conversation;
    private User currentUser;
    private Token token;

    private ArrayList<Message> messageArrayList;

    private Socket socket;
    private boolean isConnected = false;

    private boolean isNewConversation = false;
    String recipientUsername;

    private ChatDatabaseHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        helper = ChatDatabaseHelper.getInstance(this);

        currentUser = LocalUserService.getLocalUserFromPreferences(this);
        token = LocalTokenService.getLocalTokenFromPreferences(this);

        if (currentUser.email == null || token.key == null) {
            // TODO: Authentication error
            finish();
        }

        Toolbar toolbar = findViewById(R.id.toolbarChatActivity);
        setSupportActionBar(toolbar);

        String conversationType = getIntent().getStringExtra("conversationType");

        if (conversationType != null && conversationType.equals(Conversation.TYPE_PRIVATE) && getIntent().hasExtra("displayName")) {
            String displayName = getIntent().getStringExtra("displayName");
            Objects.requireNonNull(getSupportActionBar()).setTitle(displayName);
        }

        // getSupportActionBar().setTitle("Deneme");
        Objects.requireNonNull(getSupportActionBar()).setSubtitle("online");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        isNewConversation = getIntent().getBooleanExtra("isNewConversation", false);

        if (isNewConversation) {
            recipientUsername = getIntent().getStringExtra("recipientUsername");
        }

        et_message = findViewById(R.id.et_message);

        conversationUUID = getIntent().getStringExtra("conversationUUID");
        conversation = new Conversation();
        messageArrayList = new ArrayList<Message>();

        rv_messageList = findViewById(R.id.rv_messageList);
        rv_messageList.setLayoutManager(new LinearLayoutManager(this));
        messageListAdapter = new MessageListAdapter(this, messageArrayList, currentUser, conversation);
        rv_messageList.setAdapter(messageListAdapter);

        if (!isNewConversation) {
            getConversationAndMessagesFromApi();
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentUser = LocalUserService.getLocalUserFromPreferences(this);
        token = LocalTokenService.getLocalTokenFromPreferences(this);

        if (!isNewConversation) {
            conversation = helper.getConversationByUUID(conversationUUID, currentUser);
            messageArrayList = helper.getAllConversationMessages(conversationUUID);
            if (messageArrayList.size() > 0) {
                messageListAdapter = new MessageListAdapter(this, messageArrayList, currentUser, conversation);
                rv_messageList.setAdapter(messageListAdapter);
            }
        }
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

                                helper.addOrUpdateConversation(conversation);

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

                                    helper.addOrUpdateMessage(message);
                                }
                                messageListAdapter.notifyDataSetChanged();
                                initializeSocket();
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

    public void onClickSend(View view) {
        if (!Tools.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection!", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = et_message.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        et_message.setText("");

        JSONObject bodyObject = new JSONObject();

        try {
            bodyObject.put("text", text);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (isNewConversation) {
            AndroidNetworking.post(APIEndpoints.sendMessageToUser)
                    .addPathParameter("username", recipientUsername)
                    .addHeaders("Authorization", Token.prefix + " " + token.key)
                    .addJSONObjectBody(bodyObject)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONObject dataObject = response.getJSONObject("data");
                                JSONObject messageObject = dataObject.getJSONObject("message");

                                Message message = new Message();
                                message.uuid = messageObject.getString("_id");
                                message.user = messageObject.getString("user");
                                message.conversation = messageObject.getString("conversation");
                                message.text = messageObject.getString("text");
                                message.createdAt = messageObject.getString("createdAt");
                                message.updatedAt = messageObject.getString("updatedAt");

                                helper.addOrUpdateMessage(message);

                                if (isNewConversation) {
                                    conversationUUID = message.conversation;
                                    isNewConversation = false;

                                    getConversationAndMessagesFromApi();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(ANError anError) {

                        }
                    });
        } else {
            AndroidNetworking.post(APIEndpoints.sendMessageToConversation)
                    .addPathParameter("id", conversationUUID)
                    .addHeaders("Authorization", Token.prefix + " " + token.key)
                    .addJSONObjectBody(bodyObject)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                        }

                        @Override
                        public void onError(ANError anError) {

                        }
                    });
        }
    }

    private void initializeSocket() {
        IO.Options opts = new IO.Options();
        opts.query = "auth_token=" + token.key;

        try {
            socket = IO.socket(APIEndpoints.BASE_URL, opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        socket.on(Socket.EVENT_CONNECT, onConnect);
        socket.on("chat_message", onChatMessage);
        socket.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (socket != null) {
            socket.disconnect();
            socket.off(Socket.EVENT_CONNECT, onConnect);
            socket.off("chat_message", onChatMessage);
            socket.close();
        }
    }

    // Socket io connect event
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isConnected) {
                        JSONObject dataObject = new JSONObject();
                        try {
                            dataObject.put("id", conversationUUID);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        socket.emit("conversation", dataObject);
                        isConnected = true;
                    }
                }
            });
        }
    };

    // Socket io chat_message event
    private Emitter.Listener onChatMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject dataObject = (JSONObject) args[0];
                    try {
                        JSONObject messageObject = dataObject.getJSONObject("message");
                        Message message = new Message();
                        message.uuid = messageObject.getString("_id");
                        message.user = messageObject.getString("user");
                        message.conversation = messageObject.getString("conversation");
                        message.text = messageObject.getString("text");
                        message.createdAt = messageObject.getString("createdAt");
                        message.updatedAt = messageObject.getString("updatedAt");

                        messageArrayList.add(message);

                        messageListAdapter.notifyDataSetChanged();

                        rv_messageList.smoothScrollToPosition(messageArrayList.size() - 1);

                        helper.addOrUpdateMessage(message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };
}