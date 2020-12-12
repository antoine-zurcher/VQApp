package com.example.vqapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import org.tensorflow.lite.examples.classification.tflite.Classifier;

import java.io.IOException;
import java.util.List;

public class Model {

    private Classifier classifier;
    private long lastProcessingTimeMs;
    private int sensorOrientation = 0;
    private List<Classifier.Recognition> results;

    private Classifier.Model model = Classifier.Model.QUANTIZED_MOBILENET;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = 1;

    private final String TAG = this.getClass().getName();

    private Uri imageUri = null;
    private Bitmap imageBitmap = null;

    public void setImageBitmap(Bitmap imageBitmap) {
        Log.e("Set image", "set");
        this.imageBitmap = imageBitmap;
    }

    public void setImageUri(Uri imageUri, Activity activity) throws IOException {
        Log.e("Set image Uri", "set");
        this.imageUri = imageUri;
        this.imageBitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
    }

    public Bitmap getImageBitmap() {
        return this.imageBitmap;
    }

    public String runModel(Activity activity) throws IOException {
        String text = "NO IMAGE";
        if(this.imageBitmap != null) {
            results = runClassification(this.imageBitmap, activity);
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
