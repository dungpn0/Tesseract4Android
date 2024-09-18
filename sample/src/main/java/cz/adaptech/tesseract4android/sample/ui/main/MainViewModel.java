package cz.adaptech.tesseract4android.sample.ui.main;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;

public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "MainViewModel";

    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);

    private final MutableLiveData<String> progress = new MutableLiveData<>();

    private final MutableLiveData<String> result = new MutableLiveData<>();

    private final TesseractManager tesseractManager;  // Use the TesseractManager

    public MainViewModel(@NonNull Application application) {
        super(application);
        tesseractManager = TesseractManager.getInstance();
    }

    @Override
    protected void onCleared() {
        tesseractManager.releaseTesseract();
    }

    // Initialize Tesseract through the manager
    public void initTesseract(@NonNull String dataPath, @NonNull String language, int engineMode) {
        tesseractManager.initTesseract(dataPath, language, engineMode);
        if (tesseractManager.isInitialized()) {
            progress.setValue("Tesseract initialized.");
        } else {
            progress.setValue("Tesseract initialization failed.");
        }
    }

    // Recognize image using TesseractManager asynchronously
    public void recognizeImage(@NonNull File imageFile) {
        tesseractManager.recognizeImageAsync(imageFile, result, progress, processing);
    }

    public boolean isInitialized() {
        return tesseractManager.isInitialized();
    }

    public void stop() {
        if (!tesseractManager.isTessProcessing()) {
            return;
        }
        progress.setValue("Stopping...");

        tesseractManager.stopTesseract();
    }

    @NonNull
    public LiveData<Boolean> getProcessing() {
        return processing;
    }

    @NonNull
    public LiveData<String> getProgress() {
        return progress;
    }

    @NonNull
    public LiveData<String> getResult() {
        return result;
    }
}
