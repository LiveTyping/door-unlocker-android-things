package com.iot.doorunlocker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import com.iot.doorunlocker.data.DataRepository;
import com.iot.doorunlocker.data.DataRepositoryImpl;
import com.iot.doorunlocker.sensors.BrightWrapper;
import com.iot.doorunlocker.sensors.ButtonWrapper;
import com.iot.doorunlocker.sensors.CameraWrapper;
import com.iot.doorunlocker.sensors.LedWrapper;
import com.iot.doorunlocker.sensors.MotionWrapper;
import com.iot.doorunlocker.sensors.ServoWrapper;
import com.iot.doorunlocker.ui.ViewHolder;
import com.iot.doorunlocker.ui.ViewHolderImpl;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class MainActivity extends Activity {

    /**
     * Yes, this sample without normal localization, but it doesn't matter here;
     */
    private static final String S_NOT_RECOGNIZED = "THE FACE HASN'T RECOGNIZED!";
    private static final String S_RECOGNIZED = "OPEN THE DOOR!";
    private static final String S_NOT_ENOUGH_LIGHT = "Not enough light!";
    private static final String S_EMPTY = "";

    /**
     * Delay between finishing of last process and staring of a new one;
     */
    private static final long DELAY_FOR_NEXT_ATTEMPT = 5000L; // ms

    /**
     * Minimum interval between photos into one process;
     */
    private static final long DELAY_BETWEEN_PHOTOS = 500L; // ms

    /**
     * Time interval how long a led will be switched on;
     */
    private static final long DELAY_LED_MS = 2500L; // ms

    /**
     * Time interval how long a servo will be opened;
     */
    private static final long DELAY_SERVO_MS = 2500L; // ms

    /**
     * Count of photos for one process;
     */
    private static final int COUNT_OF_PHOTO_SEQUENCE = 20;

    /**
     * Count of parallelled recognition requests and taken photos;
     */
    private static final int SIZE_OF_THREAD_POOL = 3;

    /**
     * List of the pins from RaspberryPi3;
     */
    private static final String GPIO_PIN_SERVO = "PWM1";
    private static final String GPIO_PIN_BUTTON = "BCM21";
    private static final String GPIO_PIN_LED_RED = "BCM26";
    private static final String GPIO_PIN_LED_GREEN = "BCM16";
    private static final String GPIO_PIN_LED_YELLOW = "BCM5";
    private static final String GPIO_PIN_MOTION_SENSOR = "BCM6";
    private static final String GPIO_PIN_LIGHT_DETECTOR = "BCM25";

    private ViewHolderImpl mUiViewHolder = ViewHolderImpl.DEFAULT;

    /**
     * Handler for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * Additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private Handler mTakingPhotoHandler = new Handler();

    private ExecutorService mDataExecutorService;

    // Sensor wrappers;
    private CameraWrapper mCameraWrapper;
    private ButtonWrapper mButtonWrapper;
    private BrightWrapper mBrightWrapper;
    private MotionWrapper mMotionWrapper;
    private ServoWrapper mServoWrapper;
    private LedWrapper mGreenLedWrapper;
    private LedWrapper mRedLedWrapper;
    private LedWrapper mYellowLedWrapper;

    // Data repository;
    private DataRepositoryImpl mDataRepository;

    /**
     * The count of already taken photos.
     * One of the value from range [0, {@link #COUNT_OF_PHOTO_SEQUENCE}];
     */
    private int mCountOfTakenPhotos;

    /**
     * Count of parallel taken photos.
     * One of the value from range [0, {@link #SIZE_OF_THREAD_POOL}];
     */
    private int mCountOfParallelTakenPhotos;

    /**
     * Timestamp of last taken photo;
     * It's needed to disable system on {@link #DELAY_FOR_NEXT_ATTEMPT}
     * millis before starting of next job cycle if system finished
     * by one of the legal ways: the door was opened or stayed locked.
     */
    private long mTimeLastPhotoTaken;

    /**
     * True if the process of photo taking has already started;
     */
    private boolean mIsTakePhotoStarted;

    /**
     * True if process of photo taking is allowed;
     */
    private boolean mIsTakePhotoAllowed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.APPLY_UI) {
            setContentView(R.layout.activity_main);
            mUiViewHolder = new ViewHolder(this);
        }

        mDataRepository = ProjectApplication.getDataRepository();

        initSensors();
    }

    private void initSensors() {
        mCameraWrapper = CameraWrapper.getInstance();
        mCameraWrapper.initializeCamera(this, mBackgroundHandler, mOnBitmapAvailableListener);

        mButtonWrapper = new ButtonWrapper(GPIO_PIN_BUTTON);
        mButtonWrapper.setOnButtonClickListener(new ButtonWrapper.OnButtonClickListener() {
            @Override
            public void onClick() {
                Timber.d("BUTTON WAS CLICKED");
                startTakingPhotos();
            }
        });

        mBrightWrapper = new BrightWrapper(GPIO_PIN_LIGHT_DETECTOR);
        mBrightWrapper.setOnLightStateListener(new BrightWrapper.OnLightStateChangeListener() {
            @Override
            public void onLightStateChange(final boolean isLighted) {
                Timber.d("BRIGHT STATE WAS CHANGED: %b", isLighted);
                boolean b = mIsTakePhotoAllowed;
                mIsTakePhotoAllowed = isLighted;
                if (!mIsTakePhotoAllowed && b) {
                    stopTakingPhotos();
                    mRedLedWrapper.turnOn();
                }

                if (mIsTakePhotoAllowed && !b) {
                    stopTakingPhotos();
                    mRedLedWrapper.turnOff();
                }
            }
        });

        mMotionWrapper = new MotionWrapper(GPIO_PIN_MOTION_SENSOR);
        mMotionWrapper.setMotionEventListener(new MotionWrapper.MotionEventListener() {
            @Override
            public void onMovement() {
                Timber.d("MOVEMENT WAS DETECTED");
                startTakingPhotos();
            }
        });
        mMotionWrapper.startup();

        mServoWrapper = new ServoWrapper(GPIO_PIN_SERVO);
        mGreenLedWrapper = new LedWrapper(GPIO_PIN_LED_GREEN);
        mRedLedWrapper = new LedWrapper(GPIO_PIN_LED_RED);
        mYellowLedWrapper = new LedWrapper(GPIO_PIN_LED_YELLOW);

        mGreenLedWrapper.turnOff();
        mRedLedWrapper.turnOff();
        mYellowLedWrapper.turnOff();

        startBackgroundThread();
    }

    /**
     * Starts a background thread and its Handler.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("InputThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopTakingPhotos() {

        if (!mIsTakePhotoStarted) {
            return;
        }

        mIsTakePhotoStarted = false;

        mTakingPhotoHandler.removeCallbacksAndMessages(null);
        mCountOfTakenPhotos = 0;
        mCountOfParallelTakenPhotos = 0;
        mDataRepository.cancelAllCalls();

        mTimeLastPhotoTaken = System.currentTimeMillis();

        mYellowLedWrapper.turnOff();
        if (mDataExecutorService != null && !mDataExecutorService.isShutdown()) {
            mDataExecutorService.shutdownNow();
        }

        mUiViewHolder.setNumber(0);
        mUiViewHolder.showProgress(false);
        mUiViewHolder.setImage(null);
        mUiViewHolder.setTitle(mIsTakePhotoAllowed ? S_EMPTY : S_NOT_ENOUGH_LIGHT);
    }

    private void startTakingPhotos() {

        if (mIsTakePhotoStarted) {
            return;
        }

        if (!mIsTakePhotoAllowed) {
            return;
        }

        if (System.currentTimeMillis() - mTimeLastPhotoTaken < DELAY_FOR_NEXT_ATTEMPT) {
            return;
        }

        stopTakingPhotos();
        mDataExecutorService = Executors.newFixedThreadPool(SIZE_OF_THREAD_POOL);
        mYellowLedWrapper.turnOn();
        mIsTakePhotoStarted = true;
        mTakingPhotoHandler.post(mTakingPhotoRunnable);
        mUiViewHolder.setTitle(S_EMPTY);
    }

    private void takeNextPhoto() {

        if (mCountOfTakenPhotos >= COUNT_OF_PHOTO_SEQUENCE) {
            if (mCountOfParallelTakenPhotos == 0) {
                onFaceRecognized(false);
            }
            return;
        }

        if (mCountOfParallelTakenPhotos >= SIZE_OF_THREAD_POOL) {
            return;
        }

        if (mCameraWrapper.isBusy()) {
            return;
        }

        if (!mIsTakePhotoAllowed) {
            return;
        }

        if (!mIsTakePhotoStarted) {
            return;
        }

        mCountOfTakenPhotos++;
        mCountOfParallelTakenPhotos++;
        Timber.d("Take picture #%d!, parallel count photos=%d", mCountOfTakenPhotos, mCountOfParallelTakenPhotos);
        mCameraWrapper.takePicture();
        mUiViewHolder.setNumber(mCountOfTakenPhotos);
        mUiViewHolder.showProgress(true);
    }

    @UiThread
    private void openDoor() {
        mGreenLedWrapper.turnOn(DELAY_LED_MS);
        mRedLedWrapper.turnOff();
        mServoWrapper.open(DELAY_SERVO_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy()");

        mCameraWrapper.shutDown();
        mBackgroundThread.quitSafely();

        mTakingPhotoHandler.removeCallbacksAndMessages(null);
        if (mDataExecutorService != null && !mDataExecutorService.isShutdown()) {
            mDataExecutorService.shutdownNow();
        }

        mMotionWrapper.shutdown();
        mMotionWrapper.onDestroy();

        mButtonWrapper.onDestroy();
        mBrightWrapper.onDestroy();
        mServoWrapper.onDestroy();
        mGreenLedWrapper.onDestroy();
        mRedLedWrapper.onDestroy();
        mYellowLedWrapper.onDestroy();
    }

    @UiThread
    private void onPictureTaken(Bitmap bitmap) {

        if (bitmap == null) {
            return;
        }

        Timber.d("Picture was taken");
        mUiViewHolder.setImage(bitmap);

        try {
            mDataExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    mDataRepository.sendPhotoMessage(bitmap, mMainHandler, mRecognitionCallback);
                }
            });
        } catch (Exception e) {
            Timber.e("Next task is rejected", e);
        }
    }

    @UiThread
    private void onFaceRecognized(final boolean isRecognized) {
        stopTakingPhotos();
        if (isRecognized) {
            Timber.d(S_RECOGNIZED);
            mUiViewHolder.setTitle(S_RECOGNIZED);
            openDoor();
        } else {
            Timber.d(S_NOT_RECOGNIZED);
            mUiViewHolder.setTitle(S_NOT_RECOGNIZED);
            mRedLedWrapper.turnOn(DELAY_LED_MS);
        }
    }

    /**
     * Callback to receive captured camera image data;
     */

    final private CameraWrapper.OnBitmapAvailableListener mOnBitmapAvailableListener =
            new CameraWrapper.OnBitmapAvailableListener() {
                @Override
                public void onBitmapAvailable(@Nullable final Bitmap bitmap) {
                    if (!mIsTakePhotoStarted) return;
                    onPictureTaken(bitmap);
                }
            };

    final private Runnable mTakingPhotoRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsTakePhotoStarted || !mIsTakePhotoAllowed) {
                return;
            }
            takeNextPhoto();
            mTakingPhotoHandler.postDelayed(this, DELAY_BETWEEN_PHOTOS);
        }
    };

    final private DataRepository.OnRecognitionListener mRecognitionCallback =
            new DataRepository.OnRecognitionListener() {
                @Override
                synchronized public void onRecognized() {
                    if (!mIsTakePhotoStarted) return;
                    onFaceRecognized(true);
                    mCountOfParallelTakenPhotos--;
                    takeNextPhoto();
                }

                @Override
                synchronized public void onDisallowed() {
                    mCountOfParallelTakenPhotos--;
                    takeNextPhoto();
                }

                @Override
                synchronized public void onQuality(final double quality) {
                    mUiViewHolder.setQuality(String.format(Locale.getDefault(), "%.3f", quality));
                }

                @Override
                synchronized public void onError(final Throwable t) {
                    Timber.e(t, "Response error!");
                    mCountOfParallelTakenPhotos--;
                    takeNextPhoto();
                }
            };

}
