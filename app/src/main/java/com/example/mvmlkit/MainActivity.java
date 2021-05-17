package com.example.mvmlkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.VideoCapture;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.view.CameraView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import com.example.mvmlkit.Helper.GraphicOverlay;
import com.example.mvmlkit.Helper.RectOverlay;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import static androidx.camera.core.VideoCapture.*;


public class MainActivity extends AppCompatActivity {

    static Button btnVideo,btnStop;
    Button faceDetectButton;
    GraphicOverlay graphicOverlay;
    CameraView cameraView;
    android.app.AlertDialog alertDialog;

    private Executor executor = Executors.newSingleThreadExecutor();
    CameraSelector cameraSelector;
    CameraView mCameraView;

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.RECORD_AUDIO"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS);
        }
    }


    private void startCamera() {
        btnVideo = findViewById(R.id.btnVideo);
        btnStop = findViewById(R.id.btnStop);
        mCameraView = findViewById(R.id.view_finder);
        mCameraView.setFlash(ImageCapture.FLASH_MODE_AUTO);
        ImageCapture.Builder builder = new ImageCapture.Builder();
        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);
        // if has hdr (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable hdr.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mCameraView.bindToLifecycle((LifecycleOwner) MainActivity.this);

        btnVideo.setOnClickListener(v -> {
            if(mCameraView.isRecording()){return;}

            SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            File file = new File(getBatchDirectoryName(), mDateFormat.format(new Date()) + ".mp4");

            mCameraView.setCaptureMode(CameraView.CaptureMode.VIDEO);
            mCameraView.startRecording(file, executor, new VideoCapture.OnVideoSavedCallback() {



                @Override
                public void onVideoSaved(@NonNull OutputFileResults outputFileResults) {
                    galleryAddPic(file, 1);
                }

                @Override
                public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                    //Log.i("TAG",message);
                    mCameraView.stopRecording();
                }

            }); //image saved callback end
        }); //video listener end


        btnStop.setOnClickListener(v -> {
            if (mCameraView.isRecording()) {
                mCameraView.stopRecording();
            }
        });

    } //start camera end


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraView.isRecording()) {
            mCameraView.stopRecording();
        }
        finish();
    }


    public boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }


    public String getBatchDirectoryName() {
        String app_folder_path;
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            app_folder_path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
        } else {
            app_folder_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
        }

        File dir = new File(app_folder_path);
        if (!dir.exists() && !dir.mkdirs()) {
        }
        return app_folder_path;
    }


    private void galleryAddPic(File originalFile, int mediaType) {
        if (!originalFile.exists()) {
            return;
        }

        int pathSeparator = String.valueOf(originalFile).lastIndexOf('/');
        int extensionSeparator = String.valueOf(originalFile).lastIndexOf('.');
        String filename = pathSeparator >= 0 ? String.valueOf(originalFile).substring(pathSeparator + 1) : String.valueOf(originalFile);
        String extension = extensionSeparator >= 0 ? String.valueOf(originalFile).substring(extensionSeparator + 1) : "";

        // Credit: https://stackoverflow.com/a/31691791/2373034
        String mimeType = extension.length() > 0 ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ENGLISH)) : null;

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.TITLE, filename);
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);

        if (mimeType != null && mimeType.length() > 0)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        Uri externalContentUri;
        if (mediaType == 0)
            externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        else if (mediaType == 1)
            externalContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        else
            externalContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // Android 10 restricts our access to the raw filesystem, use MediaStore to save media in that case
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera");
            values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.MediaColumns.IS_PENDING, true);

            Uri uri = getContentResolver().insert(externalContentUri, values);
            if (uri != null) {
                try {
                    if (WriteFileToStream(originalFile, getContentResolver().openOutputStream(uri))) {
                        values.put(MediaStore.MediaColumns.IS_PENDING, false);
                        getContentResolver().update(uri, values, null, null);
                    }
                } catch (Exception e) {
                    getContentResolver().delete(uri, null, null);
                }
            }
            originalFile.delete();
        } else {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(originalFile));
            sendBroadcast(mediaScanIntent);
        }

    } //gallery add end


    private static boolean WriteFileToStream(File file, OutputStream out){
        try
        {
            InputStream in = new FileInputStream( file );
            try
            {
                byte[] buf = new byte[1024];
                int len;
                while( ( len = in.read( buf ) ) > 0 )
                    out.write( buf, 0, len );
            }
            finally
            {
                try
                {
                    in.close();
                }
                catch( Exception e )
                {
                    //Log.e( "Unity", "Exception:", e );
                }
            }
        }
        catch( Exception e )
        {
            //Log.e( "Unity", "Exception:", e );
            return false;
        }
        finally
        {
            try
            {
                out.close();
            }
            catch( Exception e )
            {
                //Log.e( "Unity", "Exception:", e );
            }
        }
        return true;
    }//write end

    private void processFacedetection(Bitmap bitmap) {
        // instead of bitmap I need to pass media.video file or bytearray, not sure which input type i should be using sir. 
        FirebaseVisionImage firebaseVisionImage=FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionFaceDetectorOptions firebaseVisionFaceDetectorOptions=new FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build();
        FirebaseVisionFaceDetector firebaseVisionFaceDetector= FirebaseVision.getInstance()
                .getVisionFaceDetector(firebaseVisionFaceDetectorOptions);
        firebaseVisionFaceDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                getFaceResults(firebaseVisionFaces);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(detectface.this, "Error"+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getFaceResults(List<FirebaseVisionFace> firebaseVisionFaces) {
        int counter=0;
        for(FirebaseVisionFace face : firebaseVisionFaces)
        {
            Rect rect=face.getBoundingBox();
            float rotY=face.getHeadEulerAngleY();
            float rotZ=face.getHeadEulerAngleZ();

            //EYE
            List<FirebaseVisionPoint> lefteyecontour=face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
            System.out.println("Left eye Contour"+lefteyecontour);
            List<FirebaseVisionPoint> righteyecontour=face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints();
            System.out.println("Right eye Contour"+righteyecontour);

            if(face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY)
            {
                float rightEyeOpenProbability=face.getRightEyeOpenProbability();
                System.out.println("Right eye"+rightEyeOpenProbability);
            }
            if(face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY)
            {
                float leftEyeOpenProbability=face.getLeftEyeOpenProbability();
                System.out.println("Left eye"+leftEyeOpenProbability);
            }

            float earl,earr;
            float r1= (float) Math.sqrt(Math.pow((righteyecontour.get(15).getY() - righteyecontour.get(1).getY()),2) + Math.pow((righteyecontour.get(15).getX() - righteyecontour.get(1).getX()),2));
            float r2= (float) Math.sqrt(Math.pow((righteyecontour.get(14).getY() - righteyecontour.get(2).getY()),2) + Math.pow((righteyecontour.get(14).getX() - righteyecontour.get(2).getX()),2));
            float r3= (float) Math.sqrt(Math.pow((righteyecontour.get(13).getY() - righteyecontour.get(3).getY()),2) + Math.pow((righteyecontour.get(13).getX() - righteyecontour.get(3).getX()),2));
            float r4= (float) Math.sqrt(Math.pow((righteyecontour.get(12).getY() - righteyecontour.get(4).getY()),2) + Math.pow((righteyecontour.get(12).getX() - righteyecontour.get(4).getX()),2));
            float r5= (float) Math.sqrt(Math.pow((righteyecontour.get(11).getY() - righteyecontour.get(5).getY()),2) + Math.pow((righteyecontour.get(11).getX() - righteyecontour.get(5).getX()),2));
            float r6= (float) Math.sqrt(Math.pow((righteyecontour.get(10).getY() - righteyecontour.get(6).getY()),2) + Math.pow((righteyecontour.get(10).getX() - righteyecontour.get(6).getX()),2));
            float r7= (float) Math.sqrt(Math.pow((righteyecontour.get(9).getY() - righteyecontour.get(7).getY()),2) + Math.pow((righteyecontour.get(9).getX() - righteyecontour.get(7).getX()),2));
            float r8= (float) Math.sqrt(Math.pow((righteyecontour.get(0).getY() - righteyecontour.get(8).getY()),2) + Math.pow((righteyecontour.get(0).getX() - righteyecontour.get(8).getX()),2));

            earr=(r1+r2+r3+r4+r5+r6+r7)/(2*r8);
            System.out.println("Right Eye Aspect Ratio = "+earr);

            float l1= (float) Math.sqrt(Math.pow((lefteyecontour.get(15).getY() - lefteyecontour.get(1).getY()),2) + Math.pow((lefteyecontour.get(15).getX() - lefteyecontour.get(1).getX()),2));
            float l2= (float) Math.sqrt(Math.pow((lefteyecontour.get(14).getY() - lefteyecontour.get(2).getY()),2) + Math.pow((lefteyecontour.get(14).getX() - lefteyecontour.get(2).getX()),2));
            float l3= (float) Math.sqrt(Math.pow((lefteyecontour.get(13).getY() - lefteyecontour.get(3).getY()),2) + Math.pow((lefteyecontour.get(13).getX() - lefteyecontour.get(3).getX()),2));
            float l4= (float) Math.sqrt(Math.pow((lefteyecontour.get(12).getY() - lefteyecontour.get(4).getY()),2) + Math.pow((lefteyecontour.get(12).getX() - lefteyecontour.get(4).getX()),2));
            float l5= (float) Math.sqrt(Math.pow((lefteyecontour.get(11).getY() - lefteyecontour.get(5).getY()),2) + Math.pow((lefteyecontour.get(11).getX() - lefteyecontour.get(5).getX()),2));
            float l6= (float) Math.sqrt(Math.pow((lefteyecontour.get(10).getY() - lefteyecontour.get(6).getY()),2) + Math.pow((lefteyecontour.get(10).getX() - lefteyecontour.get(6).getX()),2));
            float l7= (float) Math.sqrt(Math.pow((lefteyecontour.get(9).getY() - lefteyecontour.get(7).getY()),2) + Math.pow((lefteyecontour.get(9).getX() - lefteyecontour.get(7).getX()),2));
            float l8= (float) Math.sqrt(Math.pow((lefteyecontour.get(0).getY() - lefteyecontour.get(8).getY()),2) + Math.pow((lefteyecontour.get(0).getX() - lefteyecontour.get(8).getX()),2));

            earl=(l1+l2+l3+l4+l5+l6+l7)/(2*l8);
            System.out.println("Left Eye Aspect Ratio = "+earl);

            //MOUTH

            List<FirebaseVisionPoint> upperLiptopContour=face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints();
            System.out.println("Upper Lip Top"+upperLiptopContour);
            List<FirebaseVisionPoint> upperLipBottomContour=face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();
            System.out.println("Upper Lip Bottom"+upperLipBottomContour);

            List<FirebaseVisionPoint> lowerLiptopContour=face.getContour(FirebaseVisionFaceContour.LOWER_LIP_TOP).getPoints();
            System.out.println("Lower Lip Top"+lowerLiptopContour);
            List<FirebaseVisionPoint> lowerLipBottomContour=face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints();
            System.out.println("Lower Lip Bottom"+lowerLipBottomContour);

            if(face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY)
            {
                float smileProb=face.getSmilingProbability();
                System.out.println("Smile Probability"+smileProb);
            }


            //EAR
            FirebaseVisionFaceLandmark leftear=face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
            if(leftear != null)
            {
                FirebaseVisionPoint leftEarPos=leftear.getPosition();
                System.out.println("Left Ear Position"+leftEarPos);
            }

            FirebaseVisionFaceLandmark rightear=face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);
            if(leftear != null)
            {
                FirebaseVisionPoint rightEarPos=rightear.getPosition();
                System.out.println("Right Ear Position"+rightEarPos);
            }

            if(face.getTrackingId() != FirebaseVisionFace.INVALID_ID)
            {
                int id=face.getTrackingId();
            }

            RectOverlay rectOverlay=new RectOverlay(graphicOverlay,rect);
            graphicOverlay.add(rectOverlay);
            counter =counter+1;
        }
        alertDialog.dismiss();
    }


}//Main activity end
       