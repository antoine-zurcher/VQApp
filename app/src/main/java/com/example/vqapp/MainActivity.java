package com.example.vqapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.lite.examples.classification.tflite.Classifier;

public class MainActivity extends AppCompatActivity {


    private static final String TAG_FRAGMENT = "TAG";
    private String state = "live";
    private int requestCode = 100;

    private Classifier.Model model = Classifier.Model.QUANTIZED_MOBILENET;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = 1;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, requestCode);

        Fragment someFragment = new LiveFragment();
        FragmentManager mManager = getSupportFragmentManager();
        mManager.popBackStack();
        FragmentTransaction mTransaction = mManager.beginTransaction();

        mTransaction.replace(R.id.fragment, someFragment ); // give your fragment container id in first parameter
        mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
        mTransaction.commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { switch(item.getItemId()) {

        case R.id.fixed:
            if(state == "live"){
                Fragment someFragment = new FixedFragment();
                FragmentManager mManager = getSupportFragmentManager();
                mManager.popBackStack();
                FragmentTransaction mTransaction = mManager.beginTransaction();

                mTransaction.replace(R.id.fragment, someFragment ); // give your fragment container id in first parameter
                mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                mTransaction.commit();

                state = "fixed";
            }
            Toast.makeText(this, "Fixed", Toast.LENGTH_LONG).show();
            return(true);
        case R.id.live:
            if(state == "fixed"){
                Fragment someFragment = new LiveFragment();
                FragmentManager mManager = getSupportFragmentManager();
                mManager.popBackStack();
                FragmentTransaction mTransaction = mManager.beginTransaction();

                mTransaction.replace(R.id.fragment, someFragment ); // give your fragment container id in first parameter
                mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                mTransaction.commit();

                state = "live";
            }
            Toast.makeText(this, "Live", Toast.LENGTH_LONG).show();
            return(true);

    }
        return(super.onOptionsItemSelected(item));
    }


}