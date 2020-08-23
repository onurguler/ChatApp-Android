package com.ogsoft.scobimessenger.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ogsoft.scobimessenger.R;
import com.ogsoft.scobimessenger.models.Conversation;
import com.ogsoft.scobimessenger.models.Message;
import com.ogsoft.scobimessenger.models.User;
import com.ogsoft.scobimessenger.services.Tools;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageListAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context context;
    private ArrayList<Message> messageArrayList;
    private User currentUser;
    private Conversation conversation;

    public MessageListAdapter(Context context, ArrayList<Message> messageArrayList, User currentUser, Conversation conversation) {
        this.context = context;
        this.messageArrayList = messageArrayList;
        this.currentUser = currentUser;
        this.conversation = conversation;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_sent_row, parent, false);
            return new SentMessageHolder(itemView);
        }

        itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_received_row, parent, false);
        return new ReceivedMessageHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageArrayList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageArrayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageArrayList.get(position);

        if (message.user.equals(currentUser.uuid)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    private static class SentMessageHolder extends RecyclerView.ViewHolder {

        private TextView messageText, timeText;

        public SentMessageHolder(@NonNull View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.text_message_body);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
        }

        void bind(Message message) {
            messageText.setText(message.text);
            timeText.setText(Tools.formatDateToTime(message.createdAt));
        }
    }

    private class ReceivedMessageHolder extends  RecyclerView.ViewHolder {

        private TextView messageText, timeText, nameText;
        private CircleImageView profileImage;

        public ReceivedMessageHolder(@NonNull View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.text_message_body);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            nameText = (TextView) itemView.findViewById(R.id.text_message_name);
            profileImage = (CircleImageView) itemView.findViewById(R.id.image_message_profile);
        }

        void bind (Message message) {
            messageText.setText(message.text);
            timeText.setText(Tools.formatDateToTime(message.createdAt));
            nameText.setText(message.user);

            if (conversation.type.equals(Conversation.TYPE_PRIVATE)) {
                nameText.setText(conversation.toUser.name);
            }
        }
    }
}
