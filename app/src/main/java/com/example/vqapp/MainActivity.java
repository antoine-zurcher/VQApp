package com.example.vqapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.NinePatch;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    public enum State{
        LIVE,
        FIXED
    }

    private State state = State.LIVE;
    private int requestCode = 100;
    private final String TAG = this.getClass().getName();


    private TextView mModeTextView;
    private TextView mOutput;
    private Button mCompute;
    private EditText mQuestion;

    Fragment mLiveFragment;
    Fragment mFixedFragment;

    public Model mModel;

    private Handler liveHandler = new Handler();
    private Runnable liveRunnable;
    private boolean isModelRunning = false;

    private final int DELAY_MODEL = 500;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, requestCode);

        mModel = new Model();

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
        mQuestion = findViewById(R.id.question);

        mCompute.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(View v) {
                FragmentManager manager = getSupportFragmentManager();
                Fragment fragment = manager.findFragmentById(R.id.fragment);

                String question = mQuestion.getText().toString();

                if(question.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a question", Toast.LENGTH_SHORT).show();
                }
                else {
                    // check if question is not too long
                    String[] words= question.split(" ");
                    int nbWords = words.length;

                    if(nbWords > 16){
                        Toast.makeText(MainActivity.this, "The maximum length of the question is 16 words", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        if(state == State.LIVE){
                            try {
                                liveHandler.postDelayed(liveRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        liveHandler.postDelayed(liveRunnable, DELAY_MODEL);
                                        Log.e("in handler", "method compute model");
                                        try {
                                            mOutput.setText(mModel.runModel(mLiveFragment.getActivity(), mLiveFragment.getContext(), question));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, DELAY_MODEL);
                            }catch (Exception e){
                                Log.e("error in adding handler: ", e.getMessage());
                            }
                        }
                        else if(state == State.FIXED){

                            try {
                                mOutput.setText(mModel.runModel(mFixedFragment.getActivity(), mFixedFragment.getContext(), question));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
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
            liveHandler.removeCallbacks(liveRunnable);
            if(state == State.LIVE){
                mModeTextView.setText("Mode: Fixed");

                FragmentManager mManager = getSupportFragmentManager();
                mManager.popBackStack();
                FragmentTransaction mTransaction = mManager.beginTransaction();

                mTransaction.replace(R.id.fragment, mFixedFragment ); // give your fragment container id in first parameter
                mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                mTransaction.commit();

                state = State.FIXED;
            }
            Toast.makeText(this, "Fixed", Toast.LENGTH_LONG).show();
            return(true);
        case R.id.live:
            if(state == State.FIXED){
                mModeTextView.setText("Mode: Live");

                FragmentManager mManager = getSupportFragmentManager();
                mManager.popBackStack();
                FragmentTransaction mTransaction = mManager.beginTransaction();

                mTransaction.replace(R.id.fragment, mLiveFragment ); // give your fragment container id in first parameter
                mTransaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                mTransaction.commit();

                state = State.LIVE;
            }
            Toast.makeText(this, "Live", Toast.LENGTH_LONG).show();
            return(true);

    }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    protected void onPause() {
        super.onPause();

        liveHandler.removeCallbacks(liveRunnable);
    }
}