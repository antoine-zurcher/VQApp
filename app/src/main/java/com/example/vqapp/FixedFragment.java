package com.example.vqapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.IOException;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FixedFragment} factory method to
 * create an instance of this fragment.
 */
public class FixedFragment extends Fragment {

    private static final String TAG = "FIXED_FRAGMENT";
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
                if (image == null) {
                    Toast.makeText(getActivity(), "No image has been selected", Toast.LENGTH_SHORT).show();
                }
                else{
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
                        Toast.makeText(getActivity(), "Sending...", Toast.LENGTH_SHORT).show();
                        sendImageAndQuestionToWatch();
                    }
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
                if (resultCode == RESULT_OK) {
                    Uri selectedImageUri = imageReturnedIntent.getData();
                    image = createFile(getContext(), selectedImageUri);
                    imageView.setImageBitmap(image);

                }
                break;
        }
    }

    private void sendImageAndQuestionToWatch() {
        Intent intentWear = new Intent(getActivity(),WearService.class);
        intentWear.setAction(WearService.ACTION_SEND.SEND_MODEL.name());
        intentWear.putExtra(WearService.MODEL_BITMAP, image);
        intentWear.putExtra(WearService.MODEL_QUESTION, question);
        getActivity().startService(intentWear);
    }

    public Bitmap getImageBitmap(){
        return image;
    }

    /**
     * Loads a bitmap and avoids using too much memory loading big images (e.g.: 2560*1920)
     */
    private static Bitmap createFile(Context context, Uri theUri) {
        Bitmap outputBitmap = null;
        AssetFileDescriptor fileDescriptor;

        try {
            fileDescriptor = context.getContentResolver().openAssetFileDescriptor(theUri, "r");

            BitmapFactory.Options options = new BitmapFactory.Options();
            outputBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
            options.inJustDecodeBounds = true;

            int actualHeight = options.outHeight;
            int actualWidth = options.outWidth;

            float maxHeight = 228.0f;
            float maxWidth = 171.0f;
            float imgRatio = actualWidth / actualHeight;
            float maxRatio = maxWidth / maxHeight;

            if (actualHeight > maxHeight || actualWidth > maxWidth) {
                if (imgRatio < maxRatio) {
                    imgRatio = maxHeight / actualHeight;
                    actualWidth = (int) (imgRatio * actualWidth);
                    actualHeight = (int) maxHeight;
                } else if (imgRatio > maxRatio) {
                    imgRatio = maxWidth / actualWidth;
                    actualHeight = (int) (imgRatio * actualHeight);
                    actualWidth = (int) maxWidth;
                } else {
                    actualHeight = (int) maxHeight;
                    actualWidth = (int) maxWidth;

                }
            }
            options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);
            options.inJustDecodeBounds = false;
            options.inTempStorage = new byte[16 * 1024];
            outputBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
            if (outputBitmap != null) {
                Log.d(TAG, "Loaded image with sample size " + options.inSampleSize + "\t\t"
                        + "Bitmap width: " + outputBitmap.getWidth()
                        + "\theight: " + outputBitmap.getHeight());
            }
            fileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputBitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }
}