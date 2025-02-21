package com.example.chatapp.activities;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityRegisterFaceBinding;
import com.example.chatapp.ultilities.Constants;
import com.example.chatapp.ultilities.Preferencemanager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class RegisterFaceActivity extends AppCompatActivity {
    ImageView imgView;
    private ActivityRegisterFaceBinding binding;
    private Preferencemanager preferencemanager;
    Button cameraBt,galleryBt;
    private String imageFace;
    private String embeddingFace;
    Uri image_uri;
    FaceDetector detector;
    Interpreter interpreter;
    private FirebaseFirestore database;
    // High-accuracy landmark detection and face classification
    FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterFaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferencemanager = new Preferencemanager(getApplicationContext());
        try {
            MappedByteBuffer modelBuffer = loadModelFile(this);
            interpreter = new Interpreter(modelBuffer);
        } catch (IOException e) {
            Log.e("TFLite", "Lỗi khi load mô hình: " + e.getMessage());
        }
        loadUserDetails();
        setListeners();
    }
    private void registerFace() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferencemanager.getString(Constants.KEY_USER_ID); // Lấy userId từ SharedPreferences
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_EMBEDDING, embeddingFace); // Thêm embedding vào updates
        database.collection(Constants.KEY_COLLECTION_USER)
                .document(userId) // Sử dụng userId để cập nhật đúng người dùng
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    preferencemanager.putString(Constants.KEY_EMBEDDING, embeddingFace); // Lưu embedding vào SharedPreferences
                })
                .addOnFailureListener(exception -> {
                });
    }
    private void loadUserDetails(){
        binding.textName.setText(preferencemanager.getString(Constants.KEY_NAME));
    }
    private void setListeners(){
        setPermission();
        galleryBt = findViewById(R.id.buttonGallery);
        cameraBt = findViewById(R.id.buttonCamera);
        imgView = findViewById(R.id.imageView);
        galleryBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryActivityResultLauncher.launch(galleryIntent);
            }
        });
        cameraBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, 112);
                    }
                    else {
                        openCamera();
                    }
                }
                else {
                    openCamera();
                }
            }
        });
        detector = FaceDetection.getClient(highAccuracyOpts);
        binding.btDone.setOnClickListener(v -> {
            if (embeddingFace == null || embeddingFace.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy khuôn mặt, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
            } else {
                registerFace();
                onBackPressed();
            }
        });
    }
//Load model
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        // Đảm bảo tên file khớp với tên file trong thư mục assets
        String modelFileName = "mobile_face_net.tflite"; // Thay đổi tên file nếu cần
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    private float[][][][] convertBitmapToFloatArray(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float[][][][] inputArray = new float[1][height][width][3]; // Batch size = 1

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                inputArray[0][y][x][0] = (Color.red(pixel) - 127.5f) / 128.0f;   // Red channel
                inputArray[0][y][x][1] = (Color.green(pixel) - 127.5f) / 128.0f; // Green channel
                inputArray[0][y][x][2] = (Color.blue(pixel) - 127.5f) / 128.0f;  // Blue channel
            }
        }
        return inputArray;
    }
    // Trích xuất đặc trưng khuôn mặt
    private float[] extractFaceEmbedding(Bitmap bitmap) {
        // Resize ảnh về 112x112
        Bitmap resizedBitmap = resizeBitmap(bitmap);
        // Chuyển đổi bitmap thành mảng float
        float[][][][] inputArray = convertBitmapToFloatArray(resizedBitmap);
        // Tạo đầu ra
        float[][] outputArray = new float[1][192];
        // Chạy mô hình
        interpreter.run(inputArray, outputArray);
        // Trả về embedding
        return outputArray[0];
    }
//End
    //TODO get the image from gallery and display it
    ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        image_uri = result.getData().getData();
                        Bitmap inputImage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputImage);
                        imageFace = encodeImage(rotated);
                        imgView.setImageBitmap(rotated);

                        peformFaceDetection(rotated);
                    }
                }
            });
    //TODO capture the image using camera and display it
    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Bitmap inputImage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputImage);
                        imageFace = encodeImage(rotated);
                        imgView.setImageBitmap(rotated);
                        peformFaceDetection(rotated);
                    }
                }
            });
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
    //TODO opens camera so that user can capture image
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        cameraActivityResultLauncher.launch(cameraIntent);
    }
    //TODO takes URI of the image and returns bitmap
    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

            parcelFileDescriptor.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

    //TODO rotate image if image captured on samsung devices
    //TODO Most phone cameras are landscape, meaning if you take the photo in portrait, the resulting photos will be rotated 90 degrees.
    @SuppressLint("Range")
    public Bitmap rotateBitmap(Bitmap input){
        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
        Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
        int orientation = -1;
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
        }
        Log.d("tryOrientation",orientation+"");
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(orientation);
        Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
        return cropped;
    }
    //TODO perform face detection
    public void peformFaceDetection(Bitmap input){
        Bitmap multableBmp = input.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(multableBmp);
        InputImage image = InputImage.fromBitmap(input, 0);
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        Log.d("TryFace","len="+faces.size());
                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            bounds = makeSquare(bounds, input.getWidth(), input.getHeight());
                                            Paint p1 = new Paint();
                                            p1.setColor(Color.RED);
                                            p1.setStyle(Paint.Style.STROKE);
                                            p1.setStrokeWidth(5);
                                            performFaceRecognition(bounds,input);
                                            canvas.drawRect(bounds,p1);
                                        }
//                                        Log.d("AnhBitMap","Bitmap anh cat:"+imageFace);
                                        binding.btDone.setVisibility(View.VISIBLE);
//                                        imgView.setImageBitmap(multableBmp);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }
    //TODO perform face recognition
    public void performFaceRecognition(Rect image,Bitmap input){
        image = makeSquare(image, input.getWidth(), input.getHeight());
        if(image.top<0){
            image.top = 0;
        }
        if(image.left<0){
            image.left = 0;
        }
        if(image.right>input.getWidth()){
            image.right = input.getWidth()-1;
        }
        if(image.bottom>input.getHeight()){
            image.bottom = input.getHeight()-1;
        }
        Bitmap cropperFace =  Bitmap.createBitmap(input,image.left,image.top,image.width(),image.height());
        imgView.setImageBitmap(resizeBitmap(cropperFace));
        float[] embedding = extractFaceEmbedding(cropperFace);
        // In ra embedding (hoặc lưu vào Firebase, v.v.)
        embeddingFace = Arrays.toString(embedding);
        Log.d("FaceEmbedding", "Embedding: " + embeddingFace);
    }
    private Rect makeSquare(Rect rect, int maxWidth, int maxHeight) {
        int width = rect.width();
        int height = rect.height();
        int size = Math.max(width, height); // Chọn kích thước lớn nhất để thành hình vuông

        // Tính toán trung tâm
        int centerX = rect.centerX();
        int centerY = rect.centerY();

        // Điều chỉnh tọa độ để giữ trung tâm
        int left = Math.max(0, centerX - size / 2);
        int top = Math.max(0, centerY - size / 2);
        int right = Math.min(maxWidth, left + size);
        int bottom = Math.min(maxHeight, top + size);

        return new Rect(left, top, right, bottom);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
    private String encodeImage(Bitmap bitmap){
        int previewWidth = 112;
        int previewHeight = bitmap.getHeight()*previewWidth/bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap,previewWidth,previewHeight,false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
        byte[] bytes  = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }
    private Bitmap resizeBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, 112, 112, true);
    }
}


