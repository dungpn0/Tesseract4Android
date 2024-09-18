package cz.adaptech.tesseract4android.sample.ui.main;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import cz.adaptech.tesseract4android.sample.Assets;
import cz.adaptech.tesseract4android.sample.Config;
import cz.adaptech.tesseract4android.sample.MainActivity;

public class ScreenCaptureService extends Service {

    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler screenshotHandler;
    private TesseractManager tesseractManager;

    @Override
    public void onCreate() {
        super.onCreate();
        tesseractManager = TesseractManager.getInstance();
        // Initialize Tesseract if not already initialized
        Assets.extractAssets(getApplicationContext());
        if (!tesseractManager.isInitialized()) {
            String dataPath = Assets.getTessDataPath(getApplicationContext());
            tesseractManager.initTesseract(dataPath, Config.TESS_LANG, Config.TESS_ENGINE);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get MediaProjection from Intent
        mediaProjection = MyMediaProjectionManager.getMediaProjection();
        if (mediaProjection != null) {
            startScreenCapture(mediaProjection);
        }
//        startForegroundService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScreenCapture();
        tesseractManager.releaseTesseract();
    }

    public void startScreenCapture(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;

        screenshotHandler = new Handler(Looper.getMainLooper());
        screenshotHandler.post(new Runnable() {
            @Override
            public void run() {
                captureScreenshot();
                screenshotHandler.postDelayed(this, 5000); // Capture every 5 seconds
            }
        });
    }

    private void captureScreenshot() {
        // Configure ImageReader to capture the screen
        imageReader = ImageReader.newInstance(1080, 1080, ImageFormat.JPEG, 2); // Assuming Full HD resolution

        // Initialize VirtualDisplay
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture", 1080, 1080, 300,
                0, imageReader.getSurface(), null, null);  // Now virtualDisplay is initialized

        imageReader.setOnImageAvailableListener(reader -> {
            Bitmap screenshot = captureImageFromReader(reader);
            if (screenshot != null) {
                File screenshotFile = saveBitmapToFile(screenshot);
                if (screenshotFile != null) {
                    recognizeImage(screenshotFile);
                } else {
                    Log.e(TAG, "screenshotFile is null, cannot proceed");
                }
            }

            // Clean up resources once done
            if (reader != null) {
                reader.close(); // Close the ImageReader
            }
            if (virtualDisplay != null) {
                virtualDisplay.release(); // Release the virtual display
                virtualDisplay = null;  // Clear the reference after releasing
            }
        }, screenshotHandler);
    }

    private Bitmap captureImageFromReader(ImageReader reader) {
        Image image = null;
        Bitmap bitmap = null;
        try {
            image = reader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int width = image.getWidth();
                int height = image.getHeight();
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to capture image from reader", e);
        } finally {
            if (image != null) {
                image.close();
            }
        }
        return bitmap;
    }

    private File saveBitmapToFile(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "screenshot.png");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save bitmap to file", e);
            return null;
        }
    }

    private void recognizeImage(File imageFile) {
        // Sử dụng TesseractManager để nhận dạng hình ảnh
        tesseractManager.recognizeImage(imageFile);
    }

    private void stopScreenCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();  // Release the VirtualDisplay properly
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();  // Close the ImageReader properly
            imageReader = null;
        }
    }

    private void startForegroundService() {
        createNotificationChannel(); // Create the notification channel for Android O and above

        // Intent to open the MainActivity when the notification is clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Use NotificationCompat.Builder instead of deprecated NotificationCompat()
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID) // Pass the context and channel ID
                .setContentTitle("Screen Capture Service")
                .setContentText("Đang chụp màn hình...")
//                .setSmallIcon(R.drawable.ic_notification) // Replace with your app icon
                .setContentIntent(pendingIntent) // Handle user interaction with the notification
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Set priority for the notification
                .build(); // Build the notification

        // Start foreground service with the notification
        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        // For API level 26 and above, a notification channel is required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Capture Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

}