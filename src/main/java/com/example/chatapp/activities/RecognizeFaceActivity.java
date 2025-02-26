package com.example.chatapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.example.chatapp.databinding.ActivityRecognizeFaceBinding;
import com.example.chatapp.ultilities.Constants;
import com.example.chatapp.ultilities.Preferencemanager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RecognizeFaceActivity extends CameraActivity {
    private SignInActivity signInActivity;
    private MainActivity mainActivity;
    private ActivityRecognizeFaceBinding binding;
    private FaceRecognition faceRecognition;
    private Preferencemanager preferenceManager;
    CameraBridgeViewBase cameraBridgeViewBase;
    Mat rgb,gray,transpose_gray,transpose_rgb;
    private Mat mRgba;
    private Mat mGray;
    private boolean isSignIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecognizeFaceBinding.inflate(getLayoutInflater());
        signInActivity = new SignInActivity();
        mainActivity = new MainActivity();
        preferenceManager = new Preferencemanager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        cameraBridgeViewBase = binding.cameraView;
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                rgb = new Mat();
                gray = new Mat();
            }
            @Override
            public void onCameraViewStopped() {
                rgb.release();
                gray.release();
            }
            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                mRgba=inputFrame.rgba();
                mGray=inputFrame.gray();
                mRgba=faceRecognition.recognizeImage(mRgba);
                if(faceRecognition.isFaceUser() == true){
                    signIn();
                    isSignIn = true;
                    Log.d("LoginApp","Login successful: "+faceRecognition.isFaceUser());
                }
                else{
                    Log.d("LoginApp","Login : "+faceRecognition.isFaceUser());
                }
                return mRgba;
            }
        });
        if(OpenCVLoader.initDebug()) {
            cameraBridgeViewBase.enableView();
        }
        setContentView(binding.getRoot());
        setPermission();
        try{
            int inputSize = 112;
            faceRecognition = new FaceRecognition(getAssets(),RecognizeFaceActivity.this,"mobile_face_net.tflite",inputSize);
        }
        catch (IOException e){
            e.printStackTrace();
            Log.d("CameraActivity","Model is not loaded");
        }
    }
    private void signIn(){
        if(isSignIn){
            return;
        }
        //Truy cập database bằng getInstace
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        //Truy cập vào bảng chứa "users" và lọc theo gmail mật khẩu
        database.collection(Constants.KEY_COLLECTION_USER)
                .whereEqualTo(Constants.KEY_EMAIL,"nguyenngochoang2k3@gmail.com")
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
                        Log.d("ErrSignIn","Failt");
//                        loading(false);
//                        showToast("Có lỗi khi đăng nhập");
                    }
                });
    }
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList(){
        return Collections.singletonList(cameraBridgeViewBase);
    }
    private void setPermission(){
        //TODO handling permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, 112);
            }
        }
    }
}