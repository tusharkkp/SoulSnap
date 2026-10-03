package com.mit.tushar_kaldate.ml;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Prepares cropped face bitmaps for the VGG19 Emotion Recognition TFLite model.
 * Input specification:
 * - Dimensions: 224 x 224 pixels
 * - Channels: 3 (RGB)
 * - Data type: Float32 (4 bytes per channel)
 * - Normalization: pixel_val / 255.0f
 */
public class ModelInputProcessor {

    public static final int INPUT_IMAGE_SIZE = 224;
    private static final int CHANNELS = 3;
    private static final int BYTES_PER_CHANNEL = 4; // float32

    /**
     * Converts a face bitmap into a direct ByteBuffer matching the model's expected input.
     */
    public static ByteBuffer preprocessFaceBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        // 1. Resize to 224x224
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        // 2. Allocate direct ByteBuffer
        int bufferSize = INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE * CHANNELS * BYTES_PER_CHANNEL;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();

        // 3. Extract RGB pixels and normalize to [0.0, 1.0]
        int[] intValues = new int[INPUT_IMAGE_SIZE * INPUT_IMAGE_SIZE];
        scaledBitmap.getPixels(intValues, 0, INPUT_IMAGE_SIZE, 0, 0, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE);

        for (int pixelValue : intValues) {
            // Extract RGB components
            float r = ((pixelValue >> 16) & 0xFF) / 255.0f;
            float g = ((pixelValue >> 8) & 0xFF) / 255.0f;
            float b = (pixelValue & 0xFF) / 255.0f;

            byteBuffer.putFloat(r);
            byteBuffer.putFloat(g);
            byteBuffer.putFloat(b);
        }

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle();
        }

        return byteBuffer;
    }
}
