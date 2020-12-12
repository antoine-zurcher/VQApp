package com.example.vqapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;

public class MainActivity extends WearableActivity {

    private Model mModel = new Model();
    private Bitmap imageBitmap = null;

    public static final String
            NOTIFICATION_IMAGE_DATAMAP_RECEIVED =
            "NOTIFICATION_IMAGE_DATAMAP_RECEIVED";
    public static final String
            INTENT_IMAGE_NAME_WHEN_BROADCAST =
            "INTENT_IMAGE_NAME_WHEN_BROADCAST";


    private BroadcastReceiver mBroadcastReveiverImage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Retrieve the PNG-compressed image
            byte[] bytes = intent.getByteArrayExtra(
                    INTENT_IMAGE_NAME_WHEN_BROADCAST);
            Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            mImageView.setImageBitmap(image);
            imageBitmap = image;


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

        mModel.setImageBitmap(imageBitmap);

        try {
            mTextView.setText(mModel.runModel(this));
        } catch (IOException e) {
            e.printStackTrace();
        }


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
