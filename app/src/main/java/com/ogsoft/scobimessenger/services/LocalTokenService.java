package com.ogsoft.scobimessenger.services;

import android.content.Context;
import android.content.SharedPreferences;

import com.ogsoft.scobimessenger.models.Token;

public class LocalTokenService {

    public static Token getLocalTokenFromPreferences(Context context) {
        SharedPreferences pref = context.getSharedPreferences("LocalUser", Context.MODE_PRIVATE);
        Token token = new Token();
        token.key = pref.getString("token", null);
        return token;
    }

}
