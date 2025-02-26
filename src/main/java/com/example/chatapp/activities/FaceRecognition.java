package com.example.chatapp.activities;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.example.chatapp.R;

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
//import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceRecognition {
    private Interpreter interpreter;
    private int INPUT_SIZE;
    private int height = 0;
    private int width = 0;
    private String embeddingUser = "[-0.0117716575, 0.0029385488, -0.013235952, -0.008681348, -0.0011246706, -0.059569716, -0.06425935, -0.08500872, -0.033078525, 0.005048551, -0.0030850742, 0.009122329, -0.0048236777, 0.001207673, -0.0018804191, 0.046359863, -0.0065139583, -9.599176E-4, 0.0032867815, -7.299839E-4, 0.010544035, -0.017531201, -0.05396767, 0.0072280886, 0.08331869, 0.014836314, -0.006674759, -0.08152011, 0.20083167, -0.059118483, 0.008462358, 0.075127736, 0.10696012, -0.0067019504, -0.10258483, -0.110336244, 0.08491343, 2.030501E-4, 0.002602329, -0.1023966, 9.627743E-4, 0.0015476393, 0.010204806, -0.013114451, 0.002855306, -0.0032435816, 0.18870805, -0.027887855, 6.0816787E-5, -0.05145604, 0.15254581, 0.0033608766, -0.013939259, 0.0014281176, 0.00567966, -0.008290289, -0.050038178, 0.0028117278, -0.01753065, 0.009568703, -0.0616522, 0.017239874, 0.08399086, 0.12646277, 0.0032218453, 0.25048485, -0.0053943736, 0.037920356, 0.0022094394, -0.00444352, -0.012722575, -0.34146744, 0.032383624, 0.003882804, 0.02931616, 0.009909916, -0.0027220696, -0.0010745568, 0.15855265, 0.12086245, 0.0012054722, -0.061146427, -0.017276939, 0.07117398, -0.13615654, -0.001721251, -0.0029254786, -0.10168723, 0.14107563, 0.14842711, -0.15131924, 0.004306774, -0.0062578125, 0.0063266577, -0.04746937, 0.018364836, 0.085736774, 0.07198303, -0.004051217, -0.01651823, 0.0017610191, -0.0050933366, 0.0015016057, -5.41494E-4, 0.0053089643, 0.010172477, 0.07746949, -0.0036321094, -0.002908704, -2.6787072E-4, 0.013148324, 0.0115827685, -0.014156356, -0.023100253, -0.0023156449, -0.14975914, 0.00316611, -0.0152929025, 0.11011579, 0.16321123, -0.2034257, -0.025676817, -0.29563293, 0.002955866, -0.001374601, -0.0032756277, -0.0014391467, 0.0031200028, -0.0102957925, 0.031242264, 0.002920063, 0.0012711281, 0.0010709623, 0.011700409, 0.059378374, -0.0011087267, 0.044298273, -0.038443893, -0.010727486, 0.0077594216, 0.0015705395, -0.0070338855, 0.0044592237, 0.06541888, -0.024506781, -0.10065087, -0.015185996, -0.010235051, -0.01154319, 1.5723893E-4, 0.008135351, 0.023305554, -0.10921987, -0.007410488, 0.0015037489, 0.0026595024, 0.002281235, 0.013072057, 0.064290985, -0.007025236, 0.004127948, -5.4009765E-4, -4.0372636E-4, -8.7994337E-4, -1.6263932E-4, -4.8246646E-5, 3.7239975E-4, 0.21067134, -0.0046937414, -0.002961833, -0.0016163933, -0.04203918, 0.005100584, 0.022066351, -0.03693597, -0.0010280268, -0.12276521, -0.012106974, -3.780485E-5, 0.005216269, -0.17673264, 0.21729848, 0.008425687, 0.006326198, 0.005553599, -0.2109817, -0.009298314, 0.04391617, -0.016193729, -0.022816446, -0.014948006, -0.0042340145]";
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
    public Mat recognizeImage(Mat mat_image){
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
    private float calculateEuclideanDistance(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null || embedding1.length != embedding2.length) {
            Log.e("calculateEuclideanDistance", "Embeddings are null or have different lengths");
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
