package com.example.vqapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.IOException;
import java.util.List;

public class RunModel {

    private Classifier classifier;
    private long lastProcessingTimeMs;
    private int sensorOrientation = 0;
    private List<Classifier.Recognition> results;

    private Classifier.Model model = Classifier.Model.QUANTIZED_MOBILENET;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = 1;

    private final String TAG = this.getClass().getName();

    private Bitmap imageBitmap = null;

    public void setImageBitmap(Bitmap imageBitmap) {
        Log.e("Set image", "set");
        this.imageBitmap = imageBitmap;
    }

    public String runModel(Activity activity){
        String text = "NO IMAGE";
        if(imageBitmap != null) {
            results = runClassification(imageBitmap, activity);
            Classifier.Recognition recognition = results.get(0);
            text = recognition.getTitle() + " / " + recognition.getConfidence();
        }

        return text;
    }


    private List<Classifier.Recognition> runClassification(Bitmap imageBitmap, Activity activity) {
        try {
            classifier = Classifier.create(activity, model, device, numThreads);
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
