package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.databinding.ActivityMainBinding;
import com.example.chatapp.ultilities.Constants;
import com.example.chatapp.ultilities.Preferencemanager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private Preferencemanager preferencemanager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferencemanager = new Preferencemanager(getApplicationContext());
        loadUserDetails();
        getToken();
        setListeners();
    }
    private void setListeners(){
        binding.imageSignOut.setOnClickListener(v->signOut());
        binding.fabNewChat.setOnClickListener(v->
                startActivity(new Intent(getApplicationContext(),UsersActivity.class)));
        binding.fabResFace.setOnClickListener(v->
                startActivity(new Intent(getApplicationContext(), RegisterFaceActivity.class)));
    }
    private void loadUserDetails(){
        binding.textName.setText(preferencemanager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferencemanager.getString(Constants.KEY_IMAGE),Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void getToken(){
        //Lấy FCM token(getToken()) từ firebase message khi thành công(addOnSuccessListener) đưa vào updateToken
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private void updateToken(String token){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USER).document(
                        preferencemanager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
//                .addOnSuccessListener(unused -> showToast("Token cập nhật thành công"))
                .addOnFailureListener(e->showToast("Cập nhật Token thất bại"));
    }
    private void signOut(){
        showToast("Đang đăng xuất...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTION_USER).document(
                            preferencemanager.getString(Constants.KEY_USER_ID)
                    );
        HashMap<String,Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(usused ->{
                    preferencemanager.clear();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e->showToast("Đăng xuất thất bại"));
    }
}