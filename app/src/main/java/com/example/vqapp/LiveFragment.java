package com.example.vqapp;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;


import androidx.core.app.ActivityCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;


import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class LiveFragment extends Fragment {


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    protected CameraDevice mCamera;
    private final String TAG = this.getClass().getName();

    protected Handler handler = new Handler();
    protected Runnable runnable;
    private final int DELAY = 10;

    public Bitmap image;


    private TextureView mTextureView;

    private Surface previewSurface;  //The surface to which the preview will be drawn.
    private Size[] mSizes; //The sizes supported by the Camera. 1280x720, 1024x768, etc.  This must be set.
    private CaptureRequest.Builder mRequestBuilder;  //Builder to create a request for a camera capture.

    private int mWidth;
    private int mHeight;

    public LiveFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @SuppressLint("LongLogTag")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_live, container, false);


        // Create our Preview view and set it as the content of our activity.
        //mPreview = new CameraPreview(getContext(), mCamera);
        mTextureView = (TextureView) rootView.findViewById(R.id.surfaceView_live);
        mTextureView.setSurfaceTextureListener(surfaceTextureListener);


        return rootView;
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {

        /*The surface texture is available, so this is where we will create and open the camera, as
        well as create the request to start the camera preview.
         */
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            previewSurface = new Surface(surface);

            CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

            try {
                //The capabilities of the specified camera. On my Nexus 5, 1 is back camera.
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics("0");

                /*A map that contains all the supported sizes and other information for the camera.
                Check the documentation for more information on what is available.
                 */
                StreamConfigurationMap streamConfigurationMap = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);

                mWidth = width;
                mHeight = height;



                /*Request that the manager open and create a camera object.
                cameraDeviceCallback.onOpened() is called now to do this.
                 */
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                cameraManager.openCamera("0", cameraDeviceCallback, null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    /**
     * Callbacks to notify us of the status of the Camera device.
     */
    CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            /*
            This where we create our capture session.  Our Camera is ready to go.
             */
            mCamera = camera;

            try {
                //Used to create the surface for the preview.
                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();

                /*VERY IMPORTANT.  THIS MUST BE SET FOR THE APP TO WORK.  THE CAMERA NEEDS TO KNOW ITS PREVIEW SIZE.*/
                surfaceTexture.setDefaultBufferSize(mWidth, mHeight);

                /*A list of surfaces to which we would like to receive the preview.  We can specify
                more than one.*/
                List<Surface> surfaces = new ArrayList<>();
                surfaces.add(previewSurface);

                /*We humbly forward a request for the camera.  We are telling it here the type of
                capture we would like to do.  In this case, a live preview.  I could just as well
                have been CameraDevice.TEMPLATE_STILL_CAPTURE to take a singe picture.  See the CameraDevice
                docs.*/
                mRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mRequestBuilder.addTarget(previewSurface);

                //A capture session is now created. The capture session is where the preview will start.
                camera.createCaptureSession(surfaces, cameraCaptureSessionStateCallback, new Handler());

            } catch (CameraAccessException e) {
                Log.e("Camera Exception", e.getMessage());
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    /**
     * The CameraCaptureSession.StateCallback class  This is where the preview request is set and started.
     */
    CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                /* We humbly set a repeating request for images.  i.e. a preview. */
                session.setRepeatingRequest(mRequestBuilder.build(), cameraCaptureSessionCallback, new Handler());
            } catch (CameraAccessException e) {
                Log.e("Camera Exception", e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private CameraCaptureSession.CaptureCallback cameraCaptureSessionCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    @SuppressLint("LongLogTag")
    @Override
    public void onResume() {
        super.onResume();

        /*try {
            handler.postDelayed(runnable = new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(runnable, DELAY);
                    Log.e("in handler", "method has been called");
                    mModel.setImageBitmap(mTextureView.getBitmap());
                }
            }, DELAY);
        }catch (Exception e){
            Log.e("error in adding handler: ", e.getMessage());
        }*/
    }

    @Override
    public void onPause() {
        super.onPause();
        //handler.removeCallbacks(runnable);
    }


    public Bitmap getImageBitmap() {
        return mTextureView.getBitmap();
    }

    public static Bitmap byteToBitmap(byte[] b) {
        return (b == null || b.length == 0) ? null : BitmapFactory
                .decodeByteArray(b, 0, b.length);
    }


}