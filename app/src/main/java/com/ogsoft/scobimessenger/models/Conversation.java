package com.ogsoft.scobimessenger.models;

import java.util.ArrayList;

public class Conversation {

    public static final String TYPE_PRIVATE = "private";
    public static final String TYPE_GROUP = "group";

    public int id;
    public String uuid;
    public String type;
    public User toUser;
    public ArrayList<User> participants = new ArrayList<>();
    public Message lastMessage;
    public String createdAt;
    public String updatedAt;

}
