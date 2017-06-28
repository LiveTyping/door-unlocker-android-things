package com.iot.doorunlocker.sensors;

import android.os.Handler;
import android.util.Log;

import com.google.android.things.contrib.driver.pwmservo.Servo;

import java.io.IOException;

import timber.log.Timber;

import static android.content.ContentValues.TAG;

public class ServoWrapper {

    private static final float ANGLE_CLOSE = 0f;
    private static final float ANGLE_OPEN = 180f;

    private Servo mServo;
    private Handler mHandler = new Handler();

    public ServoWrapper(final String gpioPin) {
        try {
            mServo = new Servo(gpioPin);
            mServo.setAngleRange(ANGLE_CLOSE, ANGLE_OPEN);
            mServo.setEnabled(true);
            Timber.i("Start servo driver on %s pin", gpioPin);
        } catch (IOException e) {
            Timber.e(e, "Error creating Servo");
        }

    }

    public void open(final long delayMillis) {
        try {
            mServo.setAngle(ANGLE_OPEN);
        } catch (IOException e) {
            Timber.e(TAG, "Error setting Servo angle");
        }

        mHandler.removeCallbacks(mMoveServoRunnable);
        if (delayMillis > 0) {
            mHandler.postDelayed(mMoveServoRunnable, delayMillis);
        }
    }

    public void close() {
        if (mServo == null) {
            return;
        }

        try {
            mServo.setAngle(ANGLE_CLOSE);
        } catch (IOException e) {
            Timber.e(TAG, "Error setting Servo angle");
        }
    }

    public void onDestroy() {
        mHandler.removeCallbacks(mMoveServoRunnable);
        mMoveServoRunnable = null;

        if (mServo != null) {
            try {
                mServo.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Servo");
            } finally {
                mServo = null;
            }
        }
    }

    private Runnable mMoveServoRunnable = new Runnable() {

        @Override
        public void run() {
            mHandler.removeCallbacks(this);
            close();
        }
    };
}
