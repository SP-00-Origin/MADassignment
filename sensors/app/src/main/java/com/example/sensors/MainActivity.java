package com.example.sensors;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "Sensors";
    private static final long PROXIMITY_WARMUP_MS = 500L;

    private SensorManager sensorManager;
    private Sensor accelerometer, lightSensor, proximitySensor;
    private TextView tvAccelerometer, tvLight, tvProximity;
    private long proximityRegisteredAtMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAccelerometer = findViewById(R.id.tvAccelerometer);
        tvLight = findViewById(R.id.tvLight);
        tvProximity = findViewById(R.id.tvProximity);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (proximitySensor != null) {
            Log.d(TAG, "Proximity sensor=" + proximitySensor.getName()
                    + ", maxRange=" + proximitySensor.getMaximumRange()
                    + ", resolution=" + proximitySensor.getResolution());
        }

        //hardware check
        if (proximitySensor == null) {
            tvProximity.setText("Hardware Missing ❌");
            tvProximity.setTextColor(Color.GRAY);
        } else {
            tvProximity.setText("FAR ✅"); //default state if sensor exists
        }
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            tvAccelerometer.setText(String.format("X: %.2f\nY: %.2f\nZ: %.2f",
                    event.values[0], event.values[1], event.values[2]));
        }
        else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            tvLight.setText(String.format("%.1f lx", event.values[0]));
        }
        else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            float maxRange = proximitySensor != null ? proximitySensor.getMaximumRange() : 0f;
            long elapsedSinceRegister = SystemClock.elapsedRealtime() - proximityRegisteredAtMs;

            if (elapsedSinceRegister < PROXIMITY_WARMUP_MS) {
                tvProximity.setText("Calibrating...");
                tvProximity.setTextColor(Color.parseColor("#B7CCD6"));
                Log.d(TAG, "Proximity warm-up: distance=" + distance
                        + " maxRange=" + maxRange
                        + " elapsedMs=" + elapsedSinceRegister);
                return;
            }

            // Device-robust logic:
            // - Most phones: NEAR = 0, FAR = maxRange
            // - Some devices: continuous cm values
            boolean isNear;
            if (maxRange > 0f && !Float.isNaN(maxRange)) {
                // Treat anything meaningfully below max as NEAR
                isNear = distance < (maxRange - 0.01f);
            } else {
                // Fallback if maxRange is unavailable/invalid
                isNear = distance < 1.0f;
            }

            if (isNear) {
                tvProximity.setText("NEAR 🛑");
                tvProximity.setTextColor(Color.parseColor("#FF5252"));
            } else {
                tvProximity.setText("FAR ✅");
                tvProximity.setTextColor(Color.parseColor("#00C853"));
            }

            Log.d(TAG, "Proximity distance=" + distance
                    + " maxRange=" + maxRange
                    + " near=" + isNear
                    + " elapsedMs=" + elapsedSinceRegister);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (lightSensor != null) sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        if (proximitySensor != null) {
            proximityRegisteredAtMs = SystemClock.elapsedRealtime();
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}