package com.example.vqapp;

import android.Manifest;
import android.hardware.Camera;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LiveFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LiveFragment extends Fragment {


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private FrameLayout preview;
    private Camera mCamera;
    private CameraPreview mPreview;
    private final String TAG = this.getClass().getName();
    private static int requestCode = 100;



    public LiveFragment() {
        // Required empty public constructor
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

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        mCamera.stopPreview();
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