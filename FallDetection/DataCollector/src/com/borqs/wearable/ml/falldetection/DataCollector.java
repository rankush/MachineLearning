/* BORQS Software Solutions Pvt Ltd. CONFIDENTIAL
 * Copyright (c) 2015 All rights reserved.
 *
 * The source code contained or described herein and all documents
 * related to the source code ("Material") are owned by BORQS Software
 * Solutions Pvt Ltd. No part of the Material may be used,copied,
 * reproduced, modified, published, uploaded,posted, transmitted,
 * distributed, or disclosed in any way without BORQS Software
 * Solutions Pvt Ltd. prior written permission.
 *
 * No license under any patent, copyright, trade secret or other
 * intellectual property right is granted to or conferred upon you
 * by disclosure or delivery of the Materials, either expressly, by
 * implication, inducement, estoppel or otherwise. Any license
 * under such intellectual property rights must be express and
 * approved by BORQS Software Solutions Pvt Ltd. in writing.
 *
 */

package com.borqs.wearable.ml.falldetection;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

public class DataCollector extends Activity implements SensorEventListener {


    private static final String LOG_TAG = "TorchActivity";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private Button mStartStop;
    private static boolean isCollectingData = false;

    public static final Object[] DATA_LOCK = new Object[0];
    private File mlFolder = new File(Environment.getExternalStorageDirectory() + "/MLdata");
    private File data;
    private String separator = "ML - Data for fall detection - " + System.currentTimeMillis() + "\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        } else {
            // fai! we dont have an accelerometer!
        }

        try {
            if (!mlFolder.exists()) {
                mlFolder.mkdir();
                File data = new File(mlFolder.getAbsolutePath() + "/falldetection.txt");
                data.createNewFile();
                FileOutputStream fileinput = new FileOutputStream(data);
                PrintStream printstream = new PrintStream(fileinput);
                printstream.print(separator);
                fileinput.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        isCollectingData = false;
        mStartStop = (Button) findViewById(R.id.initiator);
        mStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isCollectingData) {
                    isCollectingData = false;
                    mSensorManager.unregisterListener(DataCollector.this, mAccelerometer);
                    mStartStop.setText(R.string.start);

                } else {
                    mSensorManager.registerListener(DataCollector.this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                    isCollectingData = true;
                    mStartStop.setText(R.string.stop);
                }
            }
        });


    }

    public void onSensorChanged(SensorEvent event) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        final double alpha = 0.8;

        double[] gravity = new double[3];
        double[] linear_acceleration = new double[3];

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        //linear acceleration = acceleration - acceleration due to gravity
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        synchronized (DATA_LOCK) {

            OutputStreamWriter file_writer = null;
            BufferedWriter buffered_writer = null;
            try {
                if (data == null || !data.exists()) {
                    data = new File(mlFolder.getAbsolutePath() + "/falldetection.txt");
                    data.createNewFile();
                }
                file_writer = new OutputStreamWriter(new FileOutputStream(data.getAbsolutePath(), true));
                buffered_writer = new BufferedWriter(file_writer);
                buffered_writer.write(linear_acceleration[0]
                        + " "
                        + linear_acceleration[1]
                        + " "
                        + linear_acceleration[2]
                );
                buffered_writer.newLine();

                buffered_writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCollectingData) {
            mStartStop.setText(R.string.stop);
        } else {
            mStartStop.setText(R.string.start);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        isCollectingData = false;
    }
}
