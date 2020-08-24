package com.ogsoft.scobimessenger.services;

import android.content.Context;
import android.content.SharedPreferences;

import com.ogsoft.scobimessenger.models.User;

public class LocalUserService {

    public static User getLocalUserFromPreferences(Context context) {
        SharedPreferences pref = context.getSharedPreferences("LocalUser", Context.MODE_PRIVATE);
        User user = new User();
        user.id = pref.getInt("id", 0);
        user.uuid = pref.getString("uuid", null);
        user.name = pref.getString("name", null);
        user.username = pref.getString("username", null);
        user.email = pref.getString("email", null);
        return user;
    }

}
