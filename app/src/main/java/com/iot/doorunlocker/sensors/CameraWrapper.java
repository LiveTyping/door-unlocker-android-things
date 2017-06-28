
package com.iot.doorunlocker.sensors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Collections;

import timber.log.Timber;

import static android.content.ContentValues.TAG;
import static android.content.Context.CAMERA_SERVICE;

public class CameraWrapper {

    // Camera image parameters (device-specific)
    private static final int IMAGE_WIDTH = 720;
    private static final int IMAGE_HEIGHT = 480;
    private static final int MAX_IMAGES = 1;

    // Image result processor
    private ImageReader mImageReader;
    // Active camera device connection
    private CameraDevice mCameraDevice;
    // Active camera capture session
    private CameraCaptureSession mCaptureSession;
    private static CameraWrapper mInstance;

    /**
     * True if is asked for the camera to take a photo;
     */
    private boolean mIsTakingPhotoInProgress;

    @Nullable
    private OnBitmapAvailableListener mOnBitmapAvailableListener;

    public static CameraWrapper getInstance() {
        if (mInstance == null) {
            mInstance = new CameraWrapper();
        }
        return mInstance;
    }

    // Initialize a new camera device connection
    public void initializeCamera(Context context,
                                 Handler backgroundHandler,
                                 OnBitmapAvailableListener onBitmapAvailableListener) {

        mOnBitmapAvailableListener = onBitmapAvailableListener;

        // Discover the camera mInstance
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting IDs", e);
        }
        if (camIds.length < 1) {
            Log.d(TAG, "No cameras found");
            return;
        }
        String id = camIds[0];

        // Initialize image processor
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, backgroundHandler);

        // Open the camera resource
        try {
            manager.openCamera(id, mStateCallback, backgroundHandler);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "Camera access exception", cae);
        }
    }

    public void takePicture() {
        if (mCameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Camera not initialized.");
            return;
        }

        // Here, we create a CameraCaptureSession for capturing still images.
        try {
            mIsTakingPhotoInProgress = true;
            mCameraDevice.createCaptureSession(
                    Collections.singletonList(mImageReader.getSurface()),
                    mSessionCallback,
                    null);
        } catch (CameraAccessException e) {
            mIsTakingPhotoInProgress = false;
            Log.d(TAG, "Access exception while preparing pic", e);
        }
    }

    public boolean isBusy() {
        return mIsTakingPhotoInProgress;
    }

    private void triggerImageCapture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            // TODO IllegalStateException: Session has been closed; further changes are illegal.
            mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera capture exception", e);
        }
    }

    // Callback handling capture progress events
    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {
                    if (session != null) {
                        session.close();
                        mCaptureSession = null;
                        Log.d(TAG, "CaptureSession closed");
                    }
                }
            };

    // Callback handling session state changes
    private final CameraCaptureSession.StateCallback mSessionCallback =
            new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    // When the session is ready, we start capture.
                    mCaptureSession = cameraCaptureSession;
                    triggerImageCapture();
                    mIsTakingPhotoInProgress = false;
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    mIsTakingPhotoInProgress = false;
                    Log.w(TAG, "Failed to configure camera");
                }
            };

    // Callback handling devices state changes
    private final CameraDevice.StateCallback mStateCallback =
            new CameraDevice.StateCallback() {

                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                }

                @Override
                public void onDisconnected(@NonNull final CameraDevice camera) {
                    Timber.d("Camera was disconnected");
                }

                @Override
                public void onError(@NonNull final CameraDevice camera, final int error) {
                    Timber.e("Camera catch the error: %d", error);
                }
            };

    // Close the camera resources
    public void shutDown() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }

    public interface OnBitmapAvailableListener {
        void onBitmapAvailable(@Nullable Bitmap bitmap);
    }

    final private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(final ImageReader reader) {
                    if (mOnBitmapAvailableListener == null) {
                        return;
                    }
                    new AsyncTask<ImageReader, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(final ImageReader... params) {
                            // Get the raw image bytes
                            Image image = reader.acquireLatestImage();
                            ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                            final byte[] imageBytes = new byte[imageBuf.remaining()];
                            imageBuf.get(imageBytes);
                            image.close();

                            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        }

                        @Override
                        protected void onPostExecute(final Bitmap bitmap) {
                            super.onPostExecute(bitmap);
                            if (mOnBitmapAvailableListener != null) {
                                mOnBitmapAvailableListener.onBitmapAvailable(bitmap);
                            }
                        }
                    }.execute();

                }
            };
}