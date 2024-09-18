package cz.adaptech.tesseract4android.sample.ui.main;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.Locale;

public class TesseractManager {
    private static final String TAG = "TesseractManager";
    private TessBaseAPI tessApi;
    private boolean tessInit;

    private static TesseractManager instance = null;
    private volatile boolean tessProcessing;
    private final Object recycleLock = new Object(); // Recycle lock added here

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Singleton pattern to ensure a single instance of TesseractManager
    public static TesseractManager getInstance() {
        if (instance == null) {
            instance = new TesseractManager();
        }
        return instance;
    }

    // Initialize Tesseract with the given data path and language
    public void initTesseract(@NonNull String dataPath, @NonNull String language, int engineMode) {
        synchronized (recycleLock) {
            if (tessApi == null) {
                tessApi = new TessBaseAPI();
            }
            try {
                tessInit = tessApi.init(dataPath, language, engineMode);
                Log.i(TAG, "Tesseract initialized with: dataPath = [" + dataPath + "], language = [" + language + "]");
            } catch (IllegalArgumentException e) {
                tessInit = false;
                Log.e(TAG, "Cannot initialize Tesseract:", e);
            }
        }
    }

    // Recognize image asynchronously and post the result to MutableLiveData
    public void recognizeImageAsync(@NonNull File imageFile,
                                    MutableLiveData<String> result,
                                    MutableLiveData<String> progress,
                                    MutableLiveData<Boolean> processing) {

        synchronized (recycleLock) { // Ensure the recognition process is synchronized
            if (!tessInit) {
                Log.e(TAG, "recognizeImage: Tesseract is not initialized");
                progress.postValue("Tesseract is not initialized.");
                return;
            }


            result.setValue("");
            tessProcessing = true;
            processing.postValue(true);
            progress.postValue("Processing...");

            new Thread(() -> {
                long startTime = System.currentTimeMillis();

                tessApi.setImage(imageFile);  // Set image for Tesseract
                String recognizedText = tessApi.getUTF8Text();  // Perform OCR
                tessApi.clear();  // Clear image data from Tesseract

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                // Post results back to the main thread
                handler.post(() -> {
                    if (recognizedText != null) {
                        result.postValue(recognizedText);
                        progress.postValue(String.format(Locale.ENGLISH, "Completed in %.3fs.", (duration / 1000f)));
                    } else {
                        progress.postValue("No text recognized.");
                    }
                    processing.postValue(false);
                    tessProcessing = false;
                });

            }).start();
        }
    }

    public void recognizeImage(@NonNull File imageFile) {

        synchronized (recycleLock) { // Ensure the recognition process is synchronized
            if (!tessInit) {
                Log.e(TAG, "recognizeImage: Tesseract is not initialized");
                return;
            }

            tessProcessing = true;
            new Thread(() -> {
                long startTime = System.currentTimeMillis();

                tessApi.setImage(imageFile);  // Set image for Tesseract
                String recognizedText = tessApi.getUTF8Text();  // Perform OCR
                tessApi.clear();  // Clear image data from Tesseract

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                Log.d("ResultObserver", String.format(Locale.ENGLISH, "Completed in %.3fs.", (duration / 1000f)));
                Log.d("Result: ", recognizedText);
                tessProcessing = false;

            }).start();
        }
    }

    public void stopTesseract() {
        synchronized (recycleLock) {
            if (tessApi != null) {
                tessApi.stop();  // Use recycle instead of end
            }
            tessProcessing = false;
        }
    }

    // Release Tesseract resources when done
    public void releaseTesseract() {
        synchronized (recycleLock) {
            if (tessApi != null) {
                tessApi.recycle();  // Use recycle instead of end
                tessApi = null;
            }
            tessInit = false;
        }
    }

    // Check if Tesseract is initialized
    public boolean isInitialized() {
        return tessInit;
    }

    // Check if Tesseract is initialized
    public boolean isTessProcessing() {
        return tessProcessing;
    }
}
