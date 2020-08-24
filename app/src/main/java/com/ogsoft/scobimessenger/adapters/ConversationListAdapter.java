package com.ogsoft.scobimessenger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ogsoft.scobimessenger.R;
import com.ogsoft.scobimessenger.models.Conversation;
import com.ogsoft.scobimessenger.models.User;
import com.ogsoft.scobimessenger.services.Tools;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationListAdapter extends RecyclerView.Adapter<ConversationListAdapter.ConversationViewHolder> {

    private ArrayList<Conversation> conversationArrayList;
    private User currentUser;

    private View.OnClickListener onItemClickListener;

    public ConversationListAdapter(ArrayList<Conversation> conversationArrayList, User currentUser) {
        this.conversationArrayList = conversationArrayList;
        this.currentUser = currentUser;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.conversation_row, parent, false);
        return new ConversationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationArrayList.get(position);

        for (User user: conversation.participants) {
            if (currentUser.username != null && !user.username.equals(currentUser.username)) {
                if (user.name != null && user.name.length() > 0) {
                    holder.txt_conversation_displayName.setText(user.name);
                } else {
                    holder.txt_conversation_displayName.setText(user.username);
                }
                break;
            }
        }

        holder.txt_conversation_lastMessageDate.setText(Tools.formatDateToTime(conversation.lastMessage.createdAt));
        holder.txt_conversation_lastMessage.setText(conversation.lastMessage.text);
    }

    @Override
    public int getItemCount() {
        return conversationArrayList.size();
    }

    public void setOnItemClickListener(View.OnClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public class ConversationViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView iv_conversation_avatar;
        public TextView txt_conversation_displayName, txt_conversation_lastMessageDate,
                txt_conversation_lastMessage, txt_conversation_unreadCount;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);

            iv_conversation_avatar = itemView.findViewById(R.id.iv_conversation_avatar);
            txt_conversation_displayName = itemView.findViewById(R.id.tv_conversation_displayName);
            txt_conversation_lastMessageDate = itemView.findViewById(R.id.tv_conversation_lastMessageDate);
            txt_conversation_lastMessage = itemView.findViewById(R.id.tv_conversation_lastMessage);
            txt_conversation_unreadCount = itemView.findViewById(R.id.tv_conversation_unreadCount);

            itemView.setTag(this);
            itemView.setOnClickListener(onItemClickListener);
        }
    }
}
