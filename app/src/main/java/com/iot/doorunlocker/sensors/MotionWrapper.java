package com.iot.doorunlocker.sensors;

import android.support.annotation.Nullable;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

import timber.log.Timber;

/**
 * Sensor name: HC-SR501
 * http://blog.blundellapps.co.uk/tut-android-things-writing-a-pir-motion-sensor-driver/
 */
public class MotionWrapper {

    private @Nullable Gpio mGpio;

    private @Nullable MotionEventListener mMotionEventListener;

    public MotionWrapper(String gpioPin) {
        try {
            mGpio = new PeripheralManagerService().openGpio(gpioPin);
            Timber.i("Start motion sensor driver on %s pin", gpioPin);
        } catch (IOException e) {
            throw new IllegalStateException("Can't open GPIO - can't create app.", e);
        }
    }

    public void setMotionEventListener(@Nullable final MotionEventListener listener) {
        mMotionEventListener = listener;
    }

    public void startup() {
        try {
            mGpio.setDirection(Gpio.DIRECTION_IN);
            mGpio.setActiveType(Gpio.ACTIVE_HIGH);
            mGpio.setEdgeTriggerType(Gpio.EDGE_RISING);
            Timber.d("Motion controller is created");
        } catch (IOException e) {
            throw new IllegalStateException("Sensor can't start - App is foobar'd", e);
        }
        try {
            mGpio.registerGpioCallback(mCallback);
            Timber.d("Motion mCallback is set");
        } catch (IOException e) {
            throw new IllegalStateException("Sensor can't register mCallback - App is foobar'd", e);
        }
    }
    
    public void shutdown() {
        mGpio.unregisterGpioCallback(mCallback);
        try {
            mGpio.close();
        } catch (IOException e) {
            Timber.e(e, "TUT", "Failed to shut down. You might get errors next time you try to start.");
        }
    }
    
    public void onDestroy() {
        try {
            mGpio.close();
        } catch (IOException e) {
            Timber.e(e, "Error on PeripheralIO API");
        } finally {
            mGpio = null;
        }
    }

    private final GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            if (mMotionEventListener != null) {
                mMotionEventListener.onMovement();
            }
            return true;
        }
    };

    public interface MotionEventListener {
        void onMovement();
    }
}