package com.silentguard.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 12f;
    private static final int SHAKE_COUNT_RESET_MS = 3000;
    private static final int SHAKES_REQUIRED = 5;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private OnShakeListener listener;

    private int shakeCount = 0;
    private long lastShakeTime = 0;
    private long firstShakeTime = 0;

    public interface OnShakeListener {
        void onShakeDetected();
    }

    public ShakeDetector(Context context, OnShakeListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (acceleration > SHAKE_THRESHOLD) {
            long now = System.currentTimeMillis();

            if (shakeCount == 0) {
                firstShakeTime = now;
            }

            // Reset if too much time passed since first shake
            if (now - firstShakeTime > SHAKE_COUNT_RESET_MS) {
                shakeCount = 0;
                firstShakeTime = now;
            }

            // Debounce — ignore shakes too close together
            if (now - lastShakeTime < 300) return;

            lastShakeTime = now;
            shakeCount++;

            if (shakeCount >= SHAKES_REQUIRED) {
                shakeCount = 0;
                listener.onShakeDetected();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
