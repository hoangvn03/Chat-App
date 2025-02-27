package com.example.chatapp.activities;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.example.chatapp.R;
import com.example.chatapp.ultilities.Constants;
import com.example.chatapp.ultilities.LastLoginManager;
import com.google.firebase.firestore.FirebaseFirestore;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class FaceRecognition {
    private Interpreter interpreter;
    private int INPUT_SIZE;
    private int height = 0;
    private int width = 0;
    private float[] embeddingFaceArray;
    private float[] embeddingUserArray;
    private float euclidNum;

    public boolean isFaceUser() {
        return isFaceUser;
    }

    public void setFaceUser(boolean faceUser) {
        isFaceUser = faceUser;
    }

    private boolean isFaceUser;

    private GpuDelegate gpuDeleagate = null;
    private CascadeClassifier cascadeClassifier;
    FaceRecognition(AssetManager assetManager, Context context,String modelPath, int inputSize) throws IOException{
        INPUT_SIZE = inputSize;
        Interpreter.Options options = new Interpreter.Options();
        gpuDeleagate = new GpuDelegate();
        options.setNumThreads(4);
        interpreter = new Interpreter(loadModel(assetManager,modelPath),options);
        Log.d("FaceRecognition","Model is loaded");
        try{
            InputStream inputStream = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade",context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir,"haarcascade_frontalface_alt");
            FileOutputStream outputStream = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            while ((byteRead = inputStream.read(buffer))!= -1){
                outputStream.write(buffer,0,byteRead);
            }
            inputStream.close();
            outputStream.close();
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.d("FaceRecognition","CascadeClassifier is loaded");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
    public Mat recognizeImage(Mat mat_image,String embeddingUser){
        Core.flip(mat_image.t(), mat_image, 0);
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mat_image,grayscaleImage,Imgproc.COLOR_RGBA2GRAY);
        height = grayscaleImage.height();
        width = grayscaleImage.width();
        int absoluteFaceSize = (int)(height*0.1);
        MatOfRect face = new MatOfRect();
        if(cascadeClassifier != null){
            cascadeClassifier.detectMultiScale(grayscaleImage,face,1.1,2,2,
                    new Size(absoluteFaceSize,absoluteFaceSize),new Size());
        }
        Rect[] faceArray = face.toArray();
        embeddingUserArray = convertStringToFloatArray(embeddingUser);
        for(int i = 0;i<faceArray.length;i++){
            Imgproc.rectangle(mat_image,faceArray[i].tl(),faceArray[i].br(),new Scalar(0,255,0,255),2);
            //cắt mặt từ ảnh
            Rect roi = new Rect((int)faceArray[i].tl().x,(int)faceArray[i].tl().y,
                    ((int)faceArray[i].br().x)-((int)faceArray[i].tl().x),
                    ((int)faceArray[i].br().y)-((int)faceArray[i].tl().y));
            Mat cropped_rgb = new Mat(mat_image,roi);
            Bitmap bitmap = null;
            bitmap = Bitmap.createBitmap(cropped_rgb.cols(),cropped_rgb.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgb,bitmap);
            Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,false);
            embeddingFaceArray = extractFaceEmbedding(scaleBitmap);
            euclidNum = calculateEuclideanDistance(embeddingUserArray,embeddingFaceArray);
            Imgproc.putText(mat_image,""+euclidNum,
                    new Point((int)faceArray[i].tl().x+10,
                            (int)faceArray[i].tl().y+20),1,1.5,
                    new Scalar(255,255,255,150),2);
            if(euclidNum < 0.8){
                isFaceUser = true;
                setFaceUser(true);
                Log.d("FaceRecognition1","Out: "+euclidNum);
            }
//            Log.d("FaceRecognition1","Out: "+euclidNum);
        }
        Core.flip(mat_image.t(),mat_image,0);

        return mat_image;
    }
    //Lấy embedding theo lastLogin
    private float calculateEuclideanDistance(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null || embedding1.length != embedding2.length) {
            return Float.MAX_VALUE;
        }
        float sum = 0;
        for (int i = 0; i < embedding1.length; i++) {
            float diff = embedding1[i] - embedding2[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }
    private float[] convertStringToFloatArray(String embeddingString) {
        String[] stringValues = embeddingString.replace("[", "").replace("]", "").split(",");
        float[] floatArray = new float[stringValues.length];
        for (int i = 0; i < stringValues.length; i++) {
            floatArray[i] = Float.parseFloat(stringValues[i].trim());
        }
        return floatArray;
    }
//Load model
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
    private Bitmap resizeBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, 112, 112, true);
    }
//End
    private MappedByteBuffer loadModel(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long declareLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declareLength);
    }

}
