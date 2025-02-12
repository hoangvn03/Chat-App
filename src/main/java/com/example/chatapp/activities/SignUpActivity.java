package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivitySignInBinding;
import com.example.chatapp.databinding.ActivitySignUpBinding;

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;
    private String encodedImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        setListeners();
    }
    private void setListeners(){
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v->{
            if(isvalidSignUpDetails()){
                signUp();
            }
        });
    }
    //Create function show notification
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void signUp(){

    }
    private Boolean isvalidSignUpDetails(){
        if(encodedImage == null){
            showToast("Chọn ảnh đại diện");
            return false;
        }
        else if(binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Nhập tên của bạn");
            return false;
        }
        //matches so sánh input từ Edittext và textview với Patterns.EMAIL..(biểu thức kiểm tra xem có định dạng gamil không
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputGmail.getText().toString()).matches()){
            showToast("Nhập đúng định dạng gmail");
            return false;
        }
        else if(binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Nhập mật khẩu của bạn");
            return false;
        }
        else if(binding.inputConfirmPassword.getText().toString().trim().isEmpty()){
            showToast("Nhập mật khẩu của bạn");
            return false;
        }
        else if(!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())){
            showToast("Nhập sai mật khẩu");
            return false;
        }
        else{
            return true;
        }
    }
}