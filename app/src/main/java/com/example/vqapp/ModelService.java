package com.example.vqapp;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.example.vqapp.ml.FinalModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ModelService extends IntentService {

    private int result = Activity.RESULT_CANCELED;
    public static final String IMAGE = "image";
    public static final String QUESTION = "question";
    public static final String OUTPUT = "output";
    public static final String NOTIFICATION = ".ModelService";

    private Classifier classifier;
    private long lastProcessingTimeMs;
    private int sensorOrientation = 0;
    private List<Classifier.Recognition> results;

    private Classifier.Model model = Classifier.Model.QUANTIZED_MOBILENET;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = 1;

    private final String TAG = this.getClass().getName();

    private Bitmap imageBitmap = null;

    /** Input image TensorBuffer. */
    private TensorImage inputImageBuffer;
    private String vocabJSON;

    private String output = "initial value";

    public ModelService() {
        super("ModelService");
    }




    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            byte[] bytes = intent.getByteArrayExtra(IMAGE);
            imageBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            String question = intent.getStringExtra(QUESTION);


            try {
                FinalModel model = FinalModel.newInstance(getApplicationContext());

                // Initialization code
                // Create an ImageProcessor with all ops required. For more ops, please
                // refer to the ImageProcessor Architecture section in this README.
                ImageProcessor imageProcessor =
                        new ImageProcessor.Builder()
                                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                                .build();

                // Create a TensorImage object. This creates the tensor of the corresponding
                // tensor type (uint8 in this case) that the TensorFlow Lite interpreter needs.
                TensorImage tImage = new TensorImage(DataType.FLOAT32);

                // Analysis code for every frame
                // Preprocess the image
                tImage.load(this.imageBitmap);
                tImage = imageProcessor.process(tImage);


                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                inputFeature0.loadBuffer(tImage.getBuffer());

                vocabJSON = loadJSONFromAsset();
                ByteBuffer bbWord = getWordVector(vocabJSON, question);

                TensorBuffer inputFeature1 = TensorBuffer.createFixedSize(new int[]{1, 16, 300}, DataType.FLOAT32);
                inputFeature1.loadBuffer(bbWord);

                // Runs model inference and gets result.
                FinalModel.Outputs outputs = model.process(inputFeature0, inputFeature1);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                int argMax = findMaxIndex(outputFeature0.getFloatArray());

                output = String.valueOf(argMax);


                // Releases model resources if no longer used.
                model.close();

                result = Activity.RESULT_OK;
            } catch (IOException e) {
                // TODO Handle the exception
                Log.e("catch onHandleIntent: ", "error with the model");
            }
            sendResults();
        }
    }

    private void sendResults(){
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(OUTPUT, output);
        sendBroadcast(intent);
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

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getApplicationContext().getAssets().open("vocab.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public ByteBuffer getWordVector(String jString, String question) {

        float[][] wordVector = new float[16][300];

        // separate the question into words and remove punctuation and put lowercase
        String[] words = question.toLowerCase().replaceAll("\\p{Punct}", "").split("[[ ]*|[,]*|[\\.]*|[?]*|[+]]");

        JSONObject jObj = null;
        try {
            jObj = new JSONObject(jString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for( int i=0; i<words.length; i++)
        {
            String word = words[i];
            try {
                Object jVector = jObj.get(word);
                wordVector[i] = fillData((JSONArray) jVector);


            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return convertFloatToByteBuffer(wordVector);
    }


    private float[] fillData(JSONArray jsonArray){

        float[] dData = new float[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                dData[i] = Float.parseFloat(jsonArray.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return dData;
    }

    ByteBuffer convertFloatToByteBuffer(float[][] floatArray) {

        ByteBuffer bb = ByteBuffer.allocate(16 * 300 * Float.BYTES);

        for (int row = 0; row < floatArray.length; row++) {
            for (int col = 0; col < floatArray[row].length; col++) {
                bb.putFloat(floatArray[row][col]);
            }
        }

        return bb;
    }

    int findMaxIndex(float [] arr) {
        float max = arr[0];
        int maxIdx = 0;
        for(int i = 1; i < arr.length; i++) {
            if(arr[i] > max) {
                max = arr[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
}