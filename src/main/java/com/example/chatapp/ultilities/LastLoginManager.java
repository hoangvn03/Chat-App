package com.example.chatapp.ultilities;

import android.content.Context;
import android.content.SharedPreferences;

public class LastLoginManager {
    private final SharedPreferences sharedPreferences;

    public LastLoginManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_LAST_LOGIN, Context.MODE_PRIVATE);
    }
    public String getString(String key){
        return sharedPreferences.getString(key,null);
    }
    public void putString(String key,String value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key,value);
        editor.apply();
    }
}
