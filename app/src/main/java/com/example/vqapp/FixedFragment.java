package com.example.vqapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FixedFragment} factory method to
 * create an instance of this fragment.
 */
public class FixedFragment extends Fragment {

    private Button btn_camera, btn_gallery, btn_send;
    private ImageView imageView;

    private Bitmap image;

    String question = null;


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
        btn_send = (Button) rootView.findViewById(R.id.btn_send);

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
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                question = MainActivity.getQuestion();

                boolean isInputValid = true;
                //verify that the question is valid
                if (question.isEmpty()) {
                    Toast.makeText(getActivity(), "Please enter a question", Toast.LENGTH_SHORT).show();
                    isInputValid = false;
                } else {
                    // check if question is not too long
                    String[] words = question.split(" ");
                    int nbWords = words.length;

                    if (nbWords > 16) {
                        Toast.makeText(getActivity(), "The maximum length of the question is 16 words", Toast.LENGTH_SHORT).show();
                        isInputValid = false;
                    }
                }

                //if the question is valid
                if (isInputValid) {
                    sendImageAndQuestionToWatch();
                }
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
                }

                break;
            case 1:
                // antoine ca te sert ca encore
                if (resultCode == RESULT_OK) {
                    Uri selectedImageUri = imageReturnedIntent.getData();
                    imageView.setImageURI(selectedImageUri);

                }
                break;
        }
    }

    private void sendImageAndQuestionToWatch() {
        Intent intentWear = new Intent(getActivity(),WearService.class);
        intentWear.setAction(WearService.ACTION_SEND.SEND_MODEL_BITMAP.name());
        intentWear.putExtra(WearService.MODEL_BITMAP, image);
        intentWear.putExtra(WearService.MODEL_QUESTION, question);
        getActivity().startService(intentWear);
    }

    public Bitmap getImageBitmap(){
        return image;
    }
}