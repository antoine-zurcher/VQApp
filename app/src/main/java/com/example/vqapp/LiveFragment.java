package com.example.vqapp;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;


import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.SystemClock;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;


import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.IOException;
import java.util.List;


public class LiveFragment extends Fragment {


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    protected Camera mCamera;
    private CameraPreview mPreview;
    private final String TAG = this.getClass().getName();

    protected Handler handler = new Handler();
    protected Runnable runnable;
    private final int DELAY = 1000;

    public Bitmap image;

    public RunModel mModel;

    private Button mCompute;


    public LiveFragment(RunModel model) {
        // Required empty public constructor
        mModel = model;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_live, container, false);
        // Create an instance of Camera
        mCamera = getCameraInstance();


        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(getContext(), mCamera);
        FrameLayout preview = (FrameLayout) rootView.findViewById(R.id.surfaceView_live);
        preview.addView(mPreview);

        mCompute = rootView.findViewById(R.id.button);
        mCompute.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(View v) {
                //To take picture from camera preview
                mCamera.takePicture(null, null, mPicture);
                /*try {
                    handler.postDelayed(runnable = new Runnable() {
                        @Override
                        public void run() {
                            handler.postDelayed(runnable, DELAY);

                            Log.e("in handler", "method has been called");
                            mCamera.takePicture(null, null, mPicture);
                        }
                    }, DELAY);
                }catch (Exception e){
                    Log.e("error in adding handler: ", e.getMessage());
                }*/


            }
        });

        // Inflate the layout for this fragment
        return rootView;
    }


    @Override
    public void onPause() {
        super.onPause();
        //handler.removeCallbacks(runnable);
        mCamera.stopPreview();
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCamera.startPreview();
            Log.e("on picture taken", "got image");
            image = byteToBitmap(data);
            mModel.setImageBitmap(image);

        }
    };

    public static Bitmap byteToBitmap(byte[] b) {
        return (b == null || b.length == 0) ? null : BitmapFactory
                .decodeByteArray(b, 0, b.length);
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Log.e("getCameraInstance", "success to get camera");
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.e("my activity", "error in getCam " + e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }
}