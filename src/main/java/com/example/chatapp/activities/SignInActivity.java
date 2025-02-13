package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.databinding.ActivitySignInBinding;
import com.example.chatapp.ultilities.Constants;
import com.example.chatapp.ultilities.Preferencemanager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
    //Biến này sẽ liên kết gọi vào file activity_sign_in.xml <=> ActivitySignInBinding
    private ActivitySignInBinding binding;
    private Preferencemanager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //khởi tạo
        preferenceManager = new Preferencemanager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
//        EdgeToEdge.enable(this);
        //thiết lập bố cục
        setContentView(binding.getRoot());
        setListeners();
    }
    private void setListeners(){
        binding.textCreateNewAccount.setOnClickListener(v->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.buttonSignIn.setOnClickListener(v->{
            if(isValidSignInDetail()){
                signIn();
            }
        });
    }
    private void signIn(){
        loading(true);
        //Truy cập database bằng getInstace
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        //Truy cập vào bảng chứa "users" và lọc theo gmail mật khẩu
        database.collection(Constants.KEY_COLLECTION_USER)
                .whereEqualTo(Constants.KEY_EMAIL,binding.inputGmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD,binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    //Kiểm tra truy vấn thành công k ,đảm bảo kết quả truy vấn k rỗng, đảm bảo có 1 user trùng
                    if(task.isSuccessful() && task.getResult()!=null && task.getResult().getDocuments().size() >0){
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        //đánh dấu đã đăng nhập
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                        //lưu id,tên,ảnh đại diện
                        preferenceManager.putString(Constants.KEY_USER_ID,documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE,documentSnapshot.getString(Constants.KEY_IMAGE));
                        //Chuyển sang màn hình chính
                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                        //Xóa các activity trước k cho quay lại bằng nút back
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    else{
                        loading(false);
                        showToast("Có lỗi khi đăng nhập");
                    }
                });
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private Boolean isValidSignInDetail(){
        if(binding.inputGmail.getText().toString().trim().isEmpty()){
            showToast("Vui lòng nhập gmail");
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputGmail.getText().toString()).matches()){
            showToast("Vui lòng nhập đúng định dạng gmail");
            return false;
        }
        else if(binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Vui lòng nhập mật khẩu");
            return false;
        }
        else{
            return true;
        }
    }
}