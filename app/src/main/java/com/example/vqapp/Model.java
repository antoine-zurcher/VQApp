package com.example.vqapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.vqapp.ml.FinalModel;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
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

    /** Input image TensorBuffer. */
    private TensorImage inputImageBuffer;
    private String vocabJSON;

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

    public String runModel(Activity activity, Context context, String question) throws IOException {

        String text = "NO IMAGE";


        try {
            FinalModel model = FinalModel.newInstance(context);

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

            vocabJSON = loadJSONFromAsset(activity);
            ByteBuffer bbWord = getWordVector(vocabJSON, question);

            TensorBuffer inputFeature1 = TensorBuffer.createFixedSize(new int[]{1, 16, 300}, DataType.FLOAT32);
            inputFeature1.loadBuffer(bbWord);

            // Runs model inference and gets result.
            FinalModel.Outputs outputs = model.process(inputFeature0, inputFeature1);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            int argMax = findMaxIndex(outputFeature0.getFloatArray());

            text = String.valueOf(argMax);


            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
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

    public String loadJSONFromAsset(Activity activity) {
        String json = null;
        try {
            InputStream is = activity.getAssets().open("vocab.json");
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
