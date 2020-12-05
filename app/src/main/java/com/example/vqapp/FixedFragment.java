package com.example.vqapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FixedFragment} factory method to
 * create an instance of this fragment.
 */
public class FixedFragment extends Fragment {

    private Button btn_camera, btn_gallery;
    private ImageView imageView;
    private Classifier classifier;
    private static final String TAG = "FixedFragment";
    private long lastProcessingTimeMs;
    private int sensorOrientation = 0;
    private List<Classifier.Recognition> results;
    private TextView tv_output;

    private Classifier.Model model = Classifier.Model.QUANTIZED_MOBILENET;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = 1;


    public FixedFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_fixed, container, false);

        btn_camera = (Button) rootView.findViewById(R.id.btn_camera_fixed);
        btn_gallery = (Button) rootView.findViewById(R.id.btn_gallery_fixed);

        imageView = (ImageView) rootView.findViewById(R.id.imageView);

        tv_output = (TextView) rootView.findViewById(R.id.output_fixed);

        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //To take picture from camera
                Log.e("on Click for btn camera", "try to get image");
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePicture, 0);//zero can be replaced with any action code

            }
        });
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //To pick photo from gallery

                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto, 1);//one can be replaced with any action code
            }
        });

        return rootView;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    Bundle extras = imageReturnedIntent.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    imageView.setImageBitmap(imageBitmap);

                    results = runClassification(imageBitmap);
                    showClassificationResults(results);
                }

                break;
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    imageView.setImageURI(selectedImage);

                    try {
                        // Convert URI to Bitmap
                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImage);
                        results = runClassification(imageBitmap);
                        showClassificationResults(results);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    private void showClassificationResults(List<Classifier.Recognition> results) {
        Classifier.Recognition recognition = results.get(0);
        tv_output.setText(recognition.getTitle());
    }

    private List<Classifier.Recognition> runClassification(Bitmap imageBitmap) {
        try {
            classifier = Classifier.create(getActivity(), model, device, numThreads);
            Log.v(TAG, "created classifier");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (classifier != null) {
            final long startTime = SystemClock.uptimeMillis();
            results =
                    classifier.recognizeImage(imageBitmap, sensorOrientation);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            Log.e(TAG, "time "+String.valueOf(lastProcessingTimeMs));

            return results;
        }

        return null;
    }
}