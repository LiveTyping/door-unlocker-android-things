package com.iot.doorunlocker.sensors;

import android.support.annotation.Nullable;

import com.google.android.things.contrib.driver.button.Button;

import java.io.IOException;

import timber.log.Timber;

public class BrightWrapper {

    private @Nullable Button mLightDetector;

    private @Nullable OnLightStateChangeListener mOnLightStateChangeListener;

    public BrightWrapper(final String gpioPin) {
        try {
            mLightDetector = new Button(gpioPin,
                    // high signal indicates the mLightDetector is pressed
                    // use with a pull-down resistor
                    Button.LogicState.PRESSED_WHEN_HIGH
            );

            mLightDetector.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean isLighted) {
                    // do something awesome
                    if (mOnLightStateChangeListener != null) {
                        mOnLightStateChangeListener.onLightStateChange(isLighted);
                    }
                }
            });

            Timber.i("Start PhotoResistor driver on %s pin", gpioPin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOnLightStateListener(@Nullable final OnLightStateChangeListener listener) {
        mOnLightStateChangeListener = listener;
    }

    public void onDestroy() {
        if (mLightDetector == null) {
            return;
        }
        try {
            mLightDetector.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mLightDetector = null;
        }
    }

    public interface OnLightStateChangeListener {
        public void onLightStateChange(boolean isLighted);
    }
}
