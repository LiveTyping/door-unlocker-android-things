package com.iot.doorunlocker.sensors;

import android.os.Handler;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

import timber.log.Timber;

/**
 * See example:
 * https://androidthings.rocks/2017/01/08/your-first-blinking-led/
 */
public class LedWrapper {

    private Handler mHandler = new Handler();
    private Gpio mGpio;

    public LedWrapper(String gpioPin) {
        try {
            PeripheralManagerService service = new PeripheralManagerService();
            mGpio = service.openGpio(gpioPin);
            mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            Timber.i("Start blinking LED driver on %s pin", gpioPin);
        } catch (IOException e) {
            Timber.e(e, "Error on PeripheralIO API");
        }
    }

    public void turnOn() {
        turnOn(0);
    }

    public void turnOn(long delayMillis) {
        if (mGpio == null) {
            return;
        }
        try {
            mGpio.setValue(true);
        } catch (IOException e) {
            Timber.e(e, "Error on PeripheralIO API");
        }

        mHandler.removeCallbacks(mBlinkGreenRunnable);
        if (delayMillis > 0) {
            mHandler.postDelayed(mBlinkGreenRunnable, delayMillis);
        }
    }

    public void turnOff() {
        if (mGpio == null) {
            return;
        }
        try {
            // Toggle the GPIO state
            mGpio.setValue(false);
        } catch (IOException e) {
            Timber.e(e, "Error on PeripheralIO API");
        }
    }

    public void onDestroy() {
        turnOff();
        mHandler.removeCallbacks(mBlinkGreenRunnable);
        try {
            mGpio.close();
        } catch (IOException e) {
            Timber.e(e, "Error on PeripheralIO API");
        } finally {
            mGpio = null;
        }
    }

    private Runnable mBlinkGreenRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(mBlinkGreenRunnable);
            turnOff();
        }
    };

}
