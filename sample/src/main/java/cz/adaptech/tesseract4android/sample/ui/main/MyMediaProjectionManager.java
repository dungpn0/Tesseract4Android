package cz.adaptech.tesseract4android.sample.ui.main;

import android.media.projection.MediaProjection;

public class MyMediaProjectionManager {
    private static MediaProjection mediaProjection;

    public static MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    public static void setMediaProjection(MediaProjection projection) {
        mediaProjection = projection;
    }
}
