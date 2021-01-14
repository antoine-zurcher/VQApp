package com.example.vqapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.NinePatch;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vqapp.ml.FinalModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    public enum State{
        LIVE,
        FIXED
    }

    private State state = State.LIVE;
    private State formerState = State.LIVE;


    private TextView mModeTextView;
    private TextView mOutput;
    private Button mCompute;
    private static EditText mQuestion;

    Fragment mLiveFragment;
    Fragment mFixedFragment;



    private Handler liveHandler = new Handler(Looper.getMainLooper());
    private Runnable liveRunnable;
    private boolean isModelRunning = false;

    private final int DELAY_MODEL = 150;
    private final int DELAY_BETWEEN_RESULTS_THRESHOLD = 1000;

    private String textOutput = "";

    private long start = 0;
    private long end = 0;

    public static String question = null;

    //create the receiver for the model service
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Log.e("BroadcastReceiver: ", "in the broadcastreceiver");

            //the second condition is here in order to know if we are still in the same state as when the model service was called
            if(bundle != null && formerState == state){
                end = System.currentTimeMillis();
                long computationTime = end-start;
                textOutput = bundle.getString(ModelService.OUTPUT);
                mOutput.setText(textOutput + " | Computation time: " + Long.toString(computationTime) + " ms");
                isModelRunning = false;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLiveFragment = new LiveFragment();
        mFixedFragment = new FixedFragment();

        //set the displayed fragment
        FragmentManager mManager = getSupportFragmentManager();
        mManager.popBackStack();
        FragmentTransaction mTransaction = mManager.beginTransaction();

        mTransaction.replace(R.id.fragment, mLiveFragment); // give your fragment container id in first parameter
        mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
        mTransaction.commit();

        //find the id of the different View
        mModeTextView = findViewById(R.id.mode);
        mOutput = findViewById(R.id.output);
        mCompute = findViewById(R.id.compute);
        mQuestion = findViewById(R.id.question);

        //add Listener for the compute button
        mCompute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean isInputValid = true;
                String question = mQuestion.getText().toString();

                //verify that the question is valid
                if (question.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a question", Toast.LENGTH_SHORT).show();
                    isInputValid = false;
                } else {
                    // check if question is not too long
                    String[] words = question.split(" ");
                    int nbWords = words.length;

                    if (nbWords > 16) {
                        Toast.makeText(MainActivity.this, "The maximum length of the question is 16 words", Toast.LENGTH_SHORT).show();
                        isInputValid = false;
                    }
                }

                //if the question is valid
                if (isInputValid) {

                    switch (state) {
                        case LIVE:
                            FragmentManager fm_live = getSupportFragmentManager();
                            LiveFragment fragment_live = (LiveFragment) fm_live.findFragmentById(R.id.fragment);

                            //check that the image is not null
                            if (fragment_live.getImageBitmap() == null) {
                                Toast.makeText(MainActivity.this, "No image has been selected", Toast.LENGTH_SHORT).show();
                                break;
                            }

                            try {
                                liveHandler.postDelayed(liveRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        liveHandler.postDelayed(liveRunnable, DELAY_MODEL);

                                        if (!isModelRunning) {

                                            isModelRunning = true;
                                            Bitmap image_live = fragment_live.getImageBitmap();

                                            runModelService(question, image_live);
                                        }
                                        else{
                                            end = System.currentTimeMillis();
                                            long computationTime = end-start;
                                            if((int)computationTime > DELAY_BETWEEN_RESULTS_THRESHOLD){
                                                stopService(new Intent(MainActivity.this, ModelService.class));
                                                isModelRunning = false;
                                            }
                                        }
                                    }
                                }, DELAY_MODEL);
                            } catch (Exception e) {
                                Log.e("Error adding handler: ", e.getMessage());
                            }

                            break;

                        case FIXED:
                            FragmentManager fm_fixed = getSupportFragmentManager();
                            FixedFragment fragment_fixed = (FixedFragment) fm_fixed.findFragmentById(R.id.fragment);

                            Bitmap image_fixed = fragment_fixed.getImageBitmap();

                            //check that the image is not null
                            if (image_fixed == null) {
                                Toast.makeText(MainActivity.this, "No image has been selected", Toast.LENGTH_SHORT).show();
                                break;
                            }
                            runModelService(question, image_fixed);

                            break;
                    }
                }
            }
        });
    }

    public static String getQuestion(){
        return mQuestion.getText().toString();
    }


    public void runModelService(String question, Bitmap image){
        formerState = state;
        //compress the image before sending it to the service
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bytes = stream.toByteArray();

        //start model service
        Intent intent = new Intent(MainActivity.this, ModelService.class);
        intent.putExtra(ModelService.QUESTION, question);
        intent.putExtra(ModelService.IMAGE, bytes);

        Log.e("runModelService: ", "Starting the model");
        start = System.currentTimeMillis();
        startService(intent);
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
            if(state == State.LIVE){
                //stop the alarm calling the Model service
                liveHandler.removeCallbacks(liveRunnable);

                mModeTextView.setText("Mode: Fixed");

                //get the actual displayed fragment
                FragmentManager mManager = getSupportFragmentManager();
                mManager.popBackStack();
                FragmentTransaction mTransaction = mManager.beginTransaction();

                mTransaction.replace(R.id.fragment, mFixedFragment ); // give your fragment container id in first parameter
                mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                mTransaction.commit();

                mQuestion.setText("");
                mOutput.setText("");

                //set the new state FIXED
                state = State.FIXED;
            }
            Toast.makeText(this, "Fixed", Toast.LENGTH_LONG).show();
            return(true);
        case R.id.live:
            if(state == State.FIXED){

                mModeTextView.setText("Mode: Live");

                //get the actual displayed fragment
                FragmentManager mManager = getSupportFragmentManager();
                mManager.popBackStack();
                FragmentTransaction mTransaction = mManager.beginTransaction();

                mTransaction.replace(R.id.fragment, mLiveFragment ); // give your fragment container id in first parameter
                mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                mTransaction.commit();

                mQuestion.setText("");
                mOutput.setText("");

                //set the new state LIVE
                state = State.LIVE;
            }
            Toast.makeText(this, "Live", Toast.LENGTH_LONG).show();
            return(true);

    }
        return(super.onOptionsItemSelected(item));
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();

        mOutput.setText("");
        mQuestion.setText("");
        //register the receiver for the model service
        registerReceiver(receiver, new IntentFilter(ModelService.NOTIFICATION));

    }

    @Override
    protected void onPause() {
        super.onPause();


        stopService(new Intent(MainActivity.this, ModelService.class));
        //unregister the receiver for the model service
        unregisterReceiver(receiver);

        //disactivate the Handler
        liveHandler.removeCallbacks(liveRunnable);
    }

}