package cz.adaptech.tesseract4android.sample.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;

import cz.adaptech.tesseract4android.sample.Assets;
import cz.adaptech.tesseract4android.sample.Config;
import cz.adaptech.tesseract4android.sample.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Copy sample image and language data to storage
        Assets.extractAssets(requireContext());

        if (!viewModel.isInitialized()) {
            String dataPath = Assets.getTessDataPath(requireContext());
            viewModel.initTesseract(dataPath, Config.TESS_LANG, Config.TESS_ENGINE);
        }

        // Initialize MediaProjectionManager
        mediaProjectionManager = (MediaProjectionManager) requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // Initialize ActivityResultLauncher to handle the result of screen capture intent
        screenCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                        MyMediaProjectionManager.setMediaProjection(mediaProjection);
//                        if (mediaProjection != null) {
//                            // Start screen capture when MediaProjection is available
//                            viewModel.startScreenCapture(mediaProjection);
//                        }

                        // Start the foreground service to capture the screen
                        Intent serviceIntent = new Intent(requireContext(), ScreenCaptureService.class);
                        requireContext().startService(serviceIntent);

                    } else {
                        // Handle the case where screen capture permission is denied
                        Log.e("ScreenCapture", "Permission denied for screen capture");
                    }
                }
        );

        // Request permission for screen capture
        startScreenCaptureRequest();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.image.setImageBitmap(Assets.getImageBitmap(requireContext()));
        binding.start.setOnClickListener(v -> {
            File imageFile = Assets.getImageFile(requireContext());
            viewModel.recognizeImage(imageFile);
        });
        binding.stop.setOnClickListener(v -> viewModel.stop());
        binding.text.setMovementMethod(new ScrollingMovementMethod());

        viewModel.getProcessing().observe(getViewLifecycleOwner(), processing -> {
            binding.start.setEnabled(!processing);
            binding.stop.setEnabled(processing);
        });

        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> binding.status.setText(progress));
        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            binding.text.setText(result);
            Log.d("ResultObserver", "Result: " + result); // Log the result to the console
        });
    }

    private void startScreenCaptureRequest() {
        // Start the screen capture request using ActivityResultLauncher
        if (mediaProjectionManager != null) {
            Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
            screenCaptureLauncher.launch(screenCaptureIntent);
        } else {
            Log.e("ScreenCapture", "MediaProjectionManager is not available");
        }
    }

}
