package com.ogsoft.scobimessenger.services;

public class APIEndpoints {

    public static final String BASE_URL = "http://192.168.1.106:3000";

    public static final String signIn = BASE_URL + "/api/v1/auth/signin";

    public static final String signUp = BASE_URL + "/api/v1/auth/signup";

    public static final String getAllConversations = BASE_URL + "/api/v1/chats";

    public static final String getConversation = BASE_URL + "/api/v1/chats/conversations/{id}";

    public static final String getConversationMessages = BASE_URL + "/api/v1/chats/conversations/{id}/messages";

    public static final String sendMessageToConversation = BASE_URL + "/api/v1/chats/conversations/{id}";

    public static final String sendMessageToUser = BASE_URL + "/api/v1/chats/users/{username}";

    public static final String getUserByUsername = BASE_URL + "/api/v1/users/user/{username}";

    public static final String updateMe = BASE_URL + "/api/v1/users/me";

}
