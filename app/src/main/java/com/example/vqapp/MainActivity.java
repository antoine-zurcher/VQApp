package com.example.vqapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final int LIVE = 0;
    private final int FIXED = 1;
    private int state = LIVE;
    private int requestCode = 100;
    private final String TAG = this.getClass().getName();


    private TextView mModeTextView;
    private TextView mOutput;
    private Button mCompute;

    Fragment mLiveFragment;
    Fragment mFixedFragment;

    public RunModel mModel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, requestCode);

        mModel = new RunModel();

        mLiveFragment = new LiveFragment(mModel);
        mFixedFragment = new FixedFragment(mModel);



        FragmentManager mManager = getSupportFragmentManager();
        mManager.popBackStack();
        FragmentTransaction mTransaction = mManager.beginTransaction();

        mTransaction.replace(R.id.fragment, mLiveFragment); // give your fragment container id in first parameter
        mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
        mTransaction.commit();

        mModeTextView = findViewById(R.id.mode);
        mOutput = findViewById(R.id.output);
        mCompute = findViewById(R.id.compute);

        mCompute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getSupportFragmentManager();
                Fragment fragment = manager.findFragmentById(R.id.fragment);
                if(state == LIVE){
                    mOutput.setText(mModel.runModel(mLiveFragment.getActivity()));
                }
                else if(state == FIXED){
                    mOutput.setText(mModel.runModel(mFixedFragment.getActivity()));
                }
            }
        });
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
            if(state == LIVE){
                mModeTextView.setText("Mode: Fixed");

                FragmentManager mManager = getSupportFragmentManager();
                mManager.popBackStack();
                FragmentTransaction mTransaction = mManager.beginTransaction();

                mTransaction.replace(R.id.fragment, mFixedFragment ); // give your fragment container id in first parameter
                mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                mTransaction.commit();

                state = FIXED;
            }
            Toast.makeText(this, "Fixed", Toast.LENGTH_LONG).show();
            return(true);
        case R.id.live:
            if(state == FIXED){
                mModeTextView.setText("Mode: Live");

                FragmentManager mManager = getSupportFragmentManager();
                mManager.popBackStack();
                FragmentTransaction mTransaction = mManager.beginTransaction();

                mTransaction.replace(R.id.fragment, mLiveFragment ); // give your fragment container id in first parameter
                mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                mTransaction.commit();

                state = LIVE;
            }
            Toast.makeText(this, "Live", Toast.LENGTH_LONG).show();
            return(true);

    }
        return(super.onOptionsItemSelected(item));
    }


}