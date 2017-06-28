package com.iot.doorunlocker.sensors;

import android.support.annotation.Nullable;

import com.google.android.things.contrib.driver.button.Button;

import java.io.IOException;

import timber.log.Timber;

public class ButtonWrapper {

    private @Nullable Button mButton;

    private @Nullable OnButtonClickListener mOnButtonClickListener;

    public ButtonWrapper(final String gpioPin) {
        try {
            mButton = new Button(gpioPin,
                    // high signal indicates the mButton is pressed
                    // use with a pull-down resistor
                    Button.LogicState.PRESSED_WHEN_HIGH
            );

            mButton.setOnButtonEventListener(new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    // do something awesome
                    if (pressed && mOnButtonClickListener != null) {
                        Timber.d("Button was pressed!");
                        mOnButtonClickListener.onClick();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOnButtonClickListener(final OnButtonClickListener onButtonClickListener) {
        mOnButtonClickListener = onButtonClickListener;
    }

    public void onDestroy() {
        if (mButton == null) {
            return;
        }
        try {
            mButton.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mButton = null;
        }
    }

    public interface OnButtonClickListener {
        public void onClick();
    }
}
