package com.ogsoft.scobimessenger.services;

public class APIEndpoints {

    public static final String BASE_URL = "http://192.168.1.106:3000";

    public static final String signIn = BASE_URL + "/api/v1/auth/signin";

    public static final String signUp = BASE_URL + "/api/v1/auth/signup";

    public static final String getAllConversations = BASE_URL + "/api/v1/chats";

}
