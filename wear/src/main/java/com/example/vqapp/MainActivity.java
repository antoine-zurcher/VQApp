package com.example.vqapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;


public class MainActivity extends WearableActivity {

    private Model mModel = new Model();
    private Bitmap imageBitmap = null;
    private String TAG = "wearMainActivity";
    private String question = null;

    private long start = 0;
    private long end = 0;

    public static final String
            NOTIFICATION_IMAGE_DATAMAP_RECEIVED =
            "NOTIFICATION_IMAGE_DATAMAP_RECEIVED";
    public static final String
            INTENT_IMAGE_NAME_WHEN_BROADCAST =
            "INTENT_IMAGE_NAME_WHEN_BROADCAST";
    public static final String
            INTENT_QUESTION_WHEN_BROADCAST =
            "INTENT_QUESTION_WHEN_BROADCAST";


    private BroadcastReceiver mBroadcastReveiverImage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Retrieve the PNG-compressed image
            byte[] bytes = intent.getByteArrayExtra(
                    INTENT_IMAGE_NAME_WHEN_BROADCAST);
            question = intent.getStringExtra(INTENT_QUESTION_WHEN_BROADCAST);
            Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            mImageView.setImageBitmap(image);
            imageBitmap = image;

            mModel.setImageBitmap(imageBitmap);


            //check that the image is not null
            if (imageBitmap == null) {
                Toast.makeText(MainActivity.this, "No image has been selected", Toast.LENGTH_SHORT).show();
            }
            else{
                try {
                    start = System.currentTimeMillis();
                    String textOutput = mModel.runModel(MainActivity.this, MainActivity.this, question);
                    end = System.currentTimeMillis();
                    long computationTime = end - start;
                    mTextView.setText(textOutput + " | Time: " + Long.toString(computationTime) + " ms");
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }






        }
    };

    private TextView mTextView;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);
        mImageView = findViewById(R.id.image);
        mImageView.setImageBitmap(imageBitmap);

        mModel.setImageBitmap(imageBitmap);
        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register broadcasts from WearService
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(mBroadcastReveiverImage, new IntentFilter(
                        NOTIFICATION_IMAGE_DATAMAP_RECEIVED));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Un-register broadcasts from WearService
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(mBroadcastReveiverImage);
    }

}
