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
import java.nio.ByteBuffer;
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
    private RunModel mModel;

    private Bitmap image;



    public FixedFragment(RunModel model) {
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
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_fixed, container, false);

        btn_camera = (Button) rootView.findViewById(R.id.btn_camera_fixed);
        btn_gallery = (Button) rootView.findViewById(R.id.btn_gallery_fixed);

        imageView = (ImageView) rootView.findViewById(R.id.imageView);


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
                    image = (Bitmap) extras.get("data");
                    imageView.setImageBitmap(image);

                    mModel.setImageBitmap(image);
                }

                break;
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    imageView.setImageURI(selectedImage);

                    try {
                        // Convert URI to Bitmap
                        image = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImage);
                        mModel.setImageBitmap(image);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    public static byte[] bitmapToByteArray(Bitmap bm) {
        // Create the buffer with the correct size
        int iBytes = bm.getWidth() * bm.getHeight() * 4;
        ByteBuffer buffer = ByteBuffer.allocate(iBytes);

        // Log.e("DBG", buffer.remaining()+""); -- Returns a correct number based on dimensions
        // Copy to buffer and then into byte array
        bm.copyPixelsToBuffer(buffer);
        // Log.e("DBG", buffer.remaining()+""); -- Returns 0
        return buffer.array();
    }
}